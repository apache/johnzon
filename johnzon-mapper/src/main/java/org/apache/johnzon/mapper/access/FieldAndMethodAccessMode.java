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
import org.apache.johnzon.mapper.JohnzonIgnore;
import org.apache.johnzon.mapper.JohnzonProperty;
import org.apache.johnzon.mapper.ObjectConverter;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

// annotated entity overrides the other one, methods are used instead of field if both are there
public class FieldAndMethodAccessMode extends BaseAccessMode {
    private final FieldAccessMode fields;
    private final MethodAccessMode methods;

    public FieldAndMethodAccessMode(final boolean useConstructor, final boolean acceptHiddenConstructor,
                                    final boolean useGettersAsWriter) {
        super(useConstructor, acceptHiddenConstructor);
        this.fields = new FieldAccessMode(useConstructor, acceptHiddenConstructor);
        this.methods = new MethodAccessMode(useConstructor, acceptHiddenConstructor, useGettersAsWriter);
    }

    @Override
    public Map<String, Reader> doFindReaders(final Class<?> clazz) {
        final Map<String, Reader> fieldsReaders = this.fields.findReaders(clazz);
        final Map<String, Reader> methodReaders = this.methods.findReaders(clazz);

        final Map<String, Reader> readers = new HashMap<String, Reader>();

        for (final Map.Entry<String, Reader> entry : fieldsReaders.entrySet()) {
            final String key = entry.getKey();
            Method m = getMethod("get" + Character.toUpperCase(key.charAt(0)) + (key.length() > 1 ? key.substring(1) : ""), clazz);
            if (m == null && (boolean.class == entry.getValue().getType() || Boolean.class == entry.getValue().getType())) {
                m = getMethod("is" + Character.toUpperCase(key.charAt(0)) + (key.length() > 1 ? key.substring(1) : ""), clazz);
            }
            boolean skip = false;
            if (m != null) {
                for (final Reader w : methodReaders.values()) {
                    if (MethodAccessMode.MethodDecoratedType.class.cast(w).getMethod().equals(m)) {
                        if (w.getAnnotation(JohnzonProperty.class) != null || w.getAnnotation(JohnzonIgnore.class) != null) {
                            skip = true;
                        }
                        break;
                    }
                }
            }
            if (skip) {
                continue;
            }
            readers.put(entry.getKey(), entry.getValue());
        }

        for (final Map.Entry<String, Reader> entry : methodReaders.entrySet()) {
            final Method mr = MethodAccessMode.MethodDecoratedType.class.cast(entry.getValue()).getMethod();
            final String fieldName = Introspector.decapitalize(mr.getName().startsWith("is") ? mr.getName().substring(2) : mr.getName().substring(3));
            final Field f = getField(fieldName, clazz);
            boolean skip = false;
            if (f != null) {
                for (final Reader w : fieldsReaders.values()) {
                    if (FieldAccessMode.FieldDecoratedType.class.cast(w).getField().equals(f)) {
                        if (w.getAnnotation(JohnzonProperty.class) != null || w.getAnnotation(JohnzonIgnore.class) != null) {
                            skip = true;
                        }
                        break;
                    }
                }
            }
            if (skip) {
                continue;
            }

            final Reader existing = readers.get(entry.getKey());
            if (existing == null) {
                readers.put(entry.getKey(), entry.getValue());
            } else {
                readers.put(entry.getKey(), new CompositeReader(entry.getValue(), existing));
            }
        }

        return readers;
    }

    private Method getMethod(final String methodName, final Class<?> type, final Class<?>... args) {
        try {
            return type.getMethod(methodName, args);
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    private Field getField(final String fieldName, final Class<?> type) {
        Class<?> t = type;
        while (t != Object.class && t != null) {
            try {
                return t.getDeclaredField(fieldName);
            } catch (final NoSuchFieldException e) {
                // no-op
            }
            t = t.getSuperclass();
        }
        return null;
    }

    @Override
    public Map<String, Writer> doFindWriters(final Class<?> clazz) {
        final Map<String, Writer> fieldWriters = this.fields.findWriters(clazz);
        final Map<String, Writer> metodWriters = this.methods.findWriters(clazz);

        final Map<String, Writer> writers = new HashMap<String, Writer>();

        for (final Map.Entry<String, Writer> entry : fieldWriters.entrySet()) {
            final String key = entry.getKey();
            final Method m = getMethod("set" + Character.toUpperCase(key.charAt(0)) + (key.length() > 1 ? key.substring(1) : ""), clazz, toType(entry.getValue().getType()));
            boolean skip = false;
            if (m != null) {
                for (final Writer w : metodWriters.values()) {
                    if (MethodAccessMode.MethodDecoratedType.class.cast(w).getMethod().equals(m)) {
                        if (w.getAnnotation(JohnzonProperty.class) != null) {
                            skip = true;
                        }
                        break;
                    }
                }
            }
            if (skip) {
                continue;
            }
            writers.put(entry.getKey(), entry.getValue());
        }

        for (final Map.Entry<String, Writer> entry : metodWriters.entrySet()) {
            final Method mr = MethodAccessMode.MethodDecoratedType.class.cast(entry.getValue()).getMethod();
            final String fieldName = Introspector.decapitalize(mr.getName().startsWith("is") ? mr.getName().substring(2) : mr.getName().substring(3));
            final Field f = getField(fieldName, clazz);
            boolean skip = false;
            if (f != null) {
                for (final Writer w : fieldWriters.values()) {
                    if (FieldAccessMode.FieldDecoratedType.class.cast(w).getField().equals(f)) {
                        if (w.getAnnotation(JohnzonProperty.class) != null) {
                            skip = true;
                        }
                        break;
                    }
                }
            }
            if (skip) {
                continue;
            }

            final Writer existing = writers.get(entry.getKey());
            if (existing == null) {
                writers.put(entry.getKey(), entry.getValue());
            } else {
                writers.put(entry.getKey(), new CompositeWriter(entry.getValue(), existing));
            }
        }
        return writers;
    }

    private Class<?> toType(final Type type) {
        return Class.class.isInstance(type) ? Class.class.cast(type) :
                (ParameterizedType.class.isInstance(type) ? toType(ParameterizedType.class.cast(type).getRawType()) :
                        Object.class /*fallback*/);
    }

    public static abstract class CompositeDecoratedType<T extends DecoratedType> implements DecoratedType {
        protected final T type1;
        protected final T type2;

        private CompositeDecoratedType(final T type1, final T type2) {
            this.type1 = type1;
            this.type2 = type2;
        }

        @Override
        public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
            final T found = type1.getClassOrPackageAnnotation(clazz);
            return found == null ? type2.getClassOrPackageAnnotation(clazz) : found;
        }

        @Override
        public Adapter<?, ?> findConverter() {
            final Adapter<?, ?> converter = type1.findConverter();
            return converter != null ? converter : type2.findConverter();
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
            final T found = type1.getAnnotation(clazz);
            return found == null ? type2.getAnnotation(clazz) : found;
        }

        @Override
        public Type getType() {
            return type1.getType();
        }

        @Override
        public boolean isNillable() {
            return type1.isNillable() || type2.isNillable();
        }

        public DecoratedType getType1() {
            return type1;
        }

        public DecoratedType getType2() {
            return type2;
        }
    }

    public static final class CompositeReader extends CompositeDecoratedType<Reader> implements Reader {
        private CompositeReader(final Reader type1, final Reader type2) {
            super(type1, type2);
        }

        @Override
        public Object read(final Object instance) {
            return type1.read(instance);
        }

        @Override
        public ObjectConverter.Writer<?> findObjectConverterWriter() {
            final ObjectConverter.Writer<?> objectConverter = type2.findObjectConverterWriter();
            return objectConverter == null ? type1.findObjectConverterWriter() : objectConverter;
        }
    }

    public static final class CompositeWriter extends CompositeDecoratedType<Writer> implements Writer {
        private CompositeWriter(final Writer type1, final Writer type2) {
            super(type1, type2);
        }

        @Override
        public void write(final Object instance, final Object value) {
            type1.write(instance, value);
        }

        @Override
        public ObjectConverter.Reader<?> findObjectConverterReader() {
            final ObjectConverter.Reader<?> objectConverter = type2.findObjectConverterReader();
            return objectConverter == null ? type1.findObjectConverterReader() : objectConverter;
        }
    }
}
