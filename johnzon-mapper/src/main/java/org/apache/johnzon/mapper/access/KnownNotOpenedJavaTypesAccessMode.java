/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.mapper.access;

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.ObjectConverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Collections.emptyMap;

// on last java releases, throwable, stacktraceelement etc are no more opened by default
// so hardcode the model we want for serialization/deserialization
//
// Note: it is still a bad habit to serialize exceptions but it happens
public class KnownNotOpenedJavaTypesAccessMode implements AccessMode {
    private final AccessMode delegate;

    public KnownNotOpenedJavaTypesAccessMode(final AccessMode delegate) {
        this.delegate = delegate;
    }

    private boolean isInaccessibleAndThrowable(final Class<?> clazz, final RuntimeException ioe) {
        return "java.lang.reflect.InaccessibleObjectException".equals(ioe.getClass().getName()) &&
                Throwable.class.isAssignableFrom(clazz);
    }

    private Map<String, Writer> findThrowableWriters(final Class<?> clazz) {
        final Map<String, Writer> map = new HashMap<>();
        // map.put("message", ...); // constructor
        map.put("stackTrace", new InlineWriter<>(Throwable::setStackTrace, StackTraceElement[].class));
        // todo: if (SubTypeException.class == clazz) addSpecificFields(map);
        return map;
    }

    private Map<String, Reader> findThrowableReaders(final Class<?> clazz) {
        final Map<String, Reader> map = new HashMap<>();
        map.put("message", new InlineReader<>(Throwable::getMessage, String.class));
        map.put("stackTrace", new InlineReader<>(Throwable::getStackTrace, StackTraceElement[].class));
        // todo: if (SubTypeException.class == clazz) addSpecificFields(map);
        return map;
    }

    private Map<String, Reader> findStackTraceElementReaders(final Class<?> clazz) {
        final Map<String, Reader> map = new HashMap<>();
        map.put("className", new InlineReader<>(StackTraceElement::getClassName, String.class));
        map.put("methodName", new InlineReader<>(StackTraceElement::getMethodName, String.class));
        map.put("fileName", new InlineReader<>(StackTraceElement::getFileName, String.class));
        map.put("lineNumber", new InlineReader<>(StackTraceElement::getLineNumber, int.class));
        return map;
    }

    // for now we only support "roots" and delegate to system "openness" the others
    // but strictly speaking we can add all java.* exception there
    //
    // in practise exceptions are generally serialized but not deserialized so "ok-ish"
    private Factory findThrowableFactory(final Class<?> clazz) {
        if (Throwable.class == clazz) {
            return new ExceptionMessageFactory() {
                @Override
                public Object create(final Object[] params) {
                    return new Throwable(String.class.cast(params[0]));
                }
            };
        }
        if (Exception.class == clazz) {
            return new ExceptionMessageFactory() {
                @Override
                public Object create(final Object[] params) {
                    return new Exception(String.class.cast(params[0]));
                }
            };
        }
        if (Error.class == clazz) {
            return new ExceptionMessageFactory() {
                @Override
                public Object create(final Object[] params) {
                    return new Error(String.class.cast(params[0]));
                }
            };
        }
        // todo: add more
        try {
            return delegate.findFactory(clazz);
        } catch (final RuntimeException | Error e) {
            return null;
        }
    }

    private boolean isJavaThrowable(final Class<?> clazz) {
        return clazz.getName().startsWith("java.") && Throwable.class.isAssignableFrom(clazz);
    }

    private boolean isStackTraceElement(final Class<?> clazz) {
        return clazz == StackTraceElement.class;
    }

    @Override
    public Factory findFactory(final Class<?> clazz, final Function<AnnotatedElement, String>... parameterNameExtractors) {
        return delegate.findFactory(clazz, parameterNameExtractors);
    }

    @Override
    public Factory findFactory(final Class<?> clazz) {
        if (isJavaThrowable(clazz)) {
            return findThrowableFactory(clazz);
        }
        if (isStackTraceElement(clazz)) {
            return new BaseFactory() {
                @Override
                public Object create(final Object[] params) {
                    return new StackTraceElement(
                            String.class.cast(params[0]),
                            String.class.cast(params[1]),
                            String.class.cast(params[2]),
                            params.length < 4 || params[3] == null ? -1 : Integer.class.cast(params[3]));
                }

                @Override
                public Type[] getParameterTypes() {
                    return new Type[]{String.class, String.class, String.class, int.class};
                }

                @Override
                public String[] getParameterNames() {
                    return new String[]{"className", "methodName", "fileName", "lineNumber"};
                }

                @Override
                public Adapter<?, ?>[] getParameterConverter() {
                    return new Adapter[]{null, null, null, null};
                }

                @Override
                public Adapter<?, ?>[] getParameterItemConverter() {
                    return new Adapter[]{null, null, null, null};
                }

                @Override
                public ObjectConverter.Codec<?>[] getObjectConverter() {
                    return new ObjectConverter.Codec[]{null, null, null, null};
                }
            };
        }
        return delegate.findFactory(clazz);
    }

    @Override
    public Map<String, Reader> findReaders(final Class<?> clazz) {
        if (isJavaThrowable(clazz)) {
            return findThrowableReaders(clazz);
        }
        if (isStackTraceElement(clazz)) {
            return findStackTraceElementReaders(clazz);
        }
        try {
            return delegate.findReaders(clazz);
        } catch (final RuntimeException ioe) {
            if (isInaccessibleAndThrowable(clazz, ioe)) {
                return findThrowableReaders(clazz);
            }
            throw ioe;
        }
    }

    @Override
    public Map<String, Writer> findWriters(final Class<?> clazz) {
        if (isJavaThrowable(clazz)) {
            return findThrowableWriters(clazz);
        }
        if (isStackTraceElement(clazz)) {
            return emptyMap();
        }
        try {
            return delegate.findWriters(clazz);
        } catch (final RuntimeException ioe) {
            if (isInaccessibleAndThrowable(clazz, ioe)) {
                return findThrowableWriters(clazz);
            }
            throw ioe;
        }
    }

    @Override
    public ObjectConverter.Reader<?> findReader(final Class<?> clazz) {
        if (isJavaThrowable(clazz) || isStackTraceElement(clazz)) {
            return null;
        }
        return delegate.findReader(clazz);
    }

    @Override
    public ObjectConverter.Writer<?> findWriter(final Class<?> clazz) {
        if (isJavaThrowable(clazz) || isStackTraceElement(clazz)) {
            return null;
        }
        return delegate.findWriter(clazz);
    }

    @Override
    public Method findAnyGetter(final Class<?> clazz) {
        if (isJavaThrowable(clazz) || isStackTraceElement(clazz)) {
            return null;
        }
        return delegate.findAnyGetter(clazz);
    }

    @Override
    public Method findAnySetter(final Class<?> clazz) {
        if (isJavaThrowable(clazz) || isStackTraceElement(clazz)) {
            return null;
        }
        return delegate.findAnySetter(clazz);
    }

    @Override
    public Field findAnyField(final Class<?> clazz) {
        if (isJavaThrowable(clazz) || isStackTraceElement(clazz)) {
            return null;
        }
        return delegate.findAnyField(clazz);
    }

    @Override
    public Method findMapAdder(final Class<?> clazz) {
        if (isJavaThrowable(clazz) || isStackTraceElement(clazz)) {
            return null;
        }
        return delegate.findMapAdder(clazz);
    }

    @Override
    public Adapter<?, ?> findAdapter(final Class<?> clazz) {
        return delegate.findAdapter(clazz);
    }

    @Override
    public Comparator<String> fieldComparator(final Class<?> clazz) {
        return delegate.fieldComparator(clazz);
    }

    @Override
    public void afterParsed(final Class<?> clazz) {
        delegate.afterParsed(clazz);
    }

    private static class InlineWriter<T, F> implements Writer {
        private final BiConsumer setter;
        private final Type type;

        private InlineWriter(final BiConsumer<T, F> setter, final Type type) {
            this.setter = setter;
            this.type = type;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
            return null;
        }

        @Override
        public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
            return null;
        }

        @Override
        public Adapter<?, ?> findConverter() {
            return null;
        }

        @Override
        public boolean isNillable(final boolean globalConfig) {
            return false;
        }

        @Override
        public void write(final Object instance, final Object value) {
            setter.accept(instance, value);
        }

        @Override
        public ObjectConverter.Reader<?> findObjectConverterReader() {
            return null;
        }
    }

    private static class InlineReader<T, F> implements Reader {
        private final Function accessor;
        private final Class<F> type;

        private InlineReader(final Function<T, F> accessor, final Class<F> type) {
            this.accessor = Function.class.cast(accessor);
            this.type = type;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
            return null;
        }

        @Override
        public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
            return null;
        }

        @Override
        public Adapter<?, ?> findConverter() {
            return null;
        }

        @Override
        public boolean isNillable(final boolean globalConfig) {
            return false;
        }

        @Override
        public Object read(final Object instance) {
            return accessor.apply(instance);
        }

        @Override
        public ObjectConverter.Writer<?> findObjectConverterWriter() {
            return null;
        }
    }

    private static abstract class BaseFactory implements Factory {
        @Override
        public Type[] getParameterTypes() {
            return new Type[0];
        }

        @Override
        public String[] getParameterNames() {
            return new String[0];
        }

        @Override
        public Adapter<?, ?>[] getParameterConverter() {
            return new Adapter[0];
        }

        @Override
        public Adapter<?, ?>[] getParameterItemConverter() {
            return new Adapter[0];
        }

        @Override
        public ObjectConverter.Codec<?>[] getObjectConverter() {
            return new ObjectConverter.Codec[0];
        }
    }

    private static abstract class ExceptionMessageFactory extends BaseFactory {
        @Override
        public Type[] getParameterTypes() {
            return new Type[]{String.class};
        }

        @Override
        public String[] getParameterNames() {
            return new String[]{"message"};
        }

        @Override
        public Adapter<?, ?>[] getParameterConverter() {
            return new Adapter[]{null};
        }

        @Override
        public Adapter<?, ?>[] getParameterItemConverter() {
            return new Adapter[]{null};
        }

        @Override
        public ObjectConverter.Codec<?>[] getObjectConverter() {
            return new ObjectConverter.Codec[]{null};
        }
    }
}
