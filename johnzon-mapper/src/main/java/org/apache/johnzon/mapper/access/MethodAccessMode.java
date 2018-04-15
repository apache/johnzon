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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.JohnzonAny;
import org.apache.johnzon.mapper.JohnzonProperty;
import org.apache.johnzon.mapper.MapperException;
import org.apache.johnzon.mapper.ObjectConverter;

public class MethodAccessMode extends BaseAccessMode {
    private final boolean supportGetterAsWritter;

    public MethodAccessMode(final boolean useConstructor, final boolean acceptHiddenConstructor, final boolean supportGetterAsWritter) {
        super(useConstructor, acceptHiddenConstructor);
        this.supportGetterAsWritter = supportGetterAsWritter;
    }

    @Override
    public Map<String, Reader> doFindReaders(final Class<?> clazz) {
        final Map<String, Reader> readers = new HashMap<String, Reader>();
        final PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(clazz);
        for (final PropertyDescriptor descriptor : propertyDescriptors) {
            final Method readMethod = descriptor.getReadMethod();
            if (readMethod != null && readMethod.getDeclaringClass() != Object.class) {
                if (isIgnored(descriptor.getName()) || Meta.getAnnotation(readMethod, JohnzonAny.class) != null) {
                    continue;
                }
                readers.put(extractKey(descriptor.getName(), readMethod, null), new MethodReader(readMethod, readMethod.getGenericReturnType()));
            }
        }
        return readers;
    }

    @Override
    public Map<String, Writer> doFindWriters(final Class<?> clazz) {
        final Map<String, Writer> writers = new HashMap<String, Writer>();
        final PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(clazz);
        for (final PropertyDescriptor descriptor : propertyDescriptors) {
            if (descriptor.getPropertyType() == Class.class || isIgnored(descriptor.getName())) {
                continue;
            }
            final Method writeMethod = descriptor.getWriteMethod();
            if (writeMethod != null) {
                writers.put(extractKey(descriptor.getName(), writeMethod, descriptor.getReadMethod()),
                        new MethodWriter(writeMethod, writeMethod.getGenericParameterTypes()[0]));
            } else if (supportGetterAsWritter
                    && Collection.class.isAssignableFrom(descriptor.getPropertyType())
                    && descriptor.getReadMethod() != null) {
                final Method readMethod = descriptor.getReadMethod();
                writers.put(extractKey(descriptor.getName(), readMethod, null), new MethodGetterAsWriter(readMethod, readMethod.getGenericReturnType()));
            }
        }
        return writers;
    }

    private String extractKey(final String name, final Method from, final Method or) {
        JohnzonProperty property = Meta.getAnnotation(from, JohnzonProperty.class);
        if (property == null && or != null) {
            property = Meta.getAnnotation(or, JohnzonProperty.class);
        }
        return property != null ? property.value() : name;
    }

    protected boolean isIgnored(final String name) {
        return name.equals("metaClass") || name.contains("$");
    }

    private PropertyDescriptor[] getPropertyDescriptors(final Class<?> clazz) {
        final PropertyDescriptor[] propertyDescriptors;
        try {
            propertyDescriptors = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
        } catch (final IntrospectionException e) {
            throw new IllegalStateException(e);
        }
        return propertyDescriptors;
    }

    public static abstract class MethodDecoratedType implements DecoratedType {
        protected final Method method;
        protected final Type type;

        public MethodDecoratedType(final Method method, final Type type) {
            this.method = method;
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            this.type = type;
        }

        @Override
        public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
            final Class<?> declaringClass = method.getDeclaringClass();
            final T annotation = Meta.getAnnotation(declaringClass, clazz);
            return annotation == null ? Meta.getAnnotation(declaringClass.getPackage(), clazz) : annotation;
        }

        @Override
        public Adapter<?, ?> findConverter() {
            return null;
        }

        public Method getMethod() {
            return method;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
            return Meta.getAnnotation(method, clazz);
        }

        @Override
        public boolean isNillable() {
            return false;
        }
    }

    public static class MethodWriter extends MethodDecoratedType implements Writer {
        public MethodWriter(final Method method, final Type type) {
            super(method, type);
        }

        @Override
        public void write(final Object instance, final Object value) {
            try {
                method.invoke(instance, value);
            } catch (final Exception e) {
                throw new MapperException(e);
            }
        }

        @Override
        public ObjectConverter.Reader<?> findObjectConverterReader() {
            return null;
        }
    }

    public static class MethodReader extends MethodDecoratedType implements Reader {
        public MethodReader(final Method method, final Type type) {
            super(method, type);
        }

        @Override
        public Object read(final Object instance) {
            try {
                return method.invoke(instance);
            } catch (final Exception e) {
                throw new MapperException(e);
            }
        }

        @Override
        public ObjectConverter.Writer<?> findObjectConverterWriter() {
            return null;
        }
    }

    private class MethodGetterAsWriter extends MethodReader implements Writer {
        public MethodGetterAsWriter(final Method readMethod, final Type type) {
            super(readMethod, type);
        }

        @Override
        public void write(final Object instance, final Object value) {
            if (value != null) {
                try {
                    final Collection<?> collection = Collection.class.cast(method.invoke(instance));
                    if (collection != null) {
                        collection.addAll(Collection.class.cast(value));
                    }
                } catch (final Exception e) {
                    throw new MapperException(e);
                }
            }
        }

        @Override
        public ObjectConverter.Reader<?> findObjectConverterReader() {
            return null;
        }
    }
}
