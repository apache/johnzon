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

import static java.util.stream.Collectors.toMap;
import static org.apache.johnzon.mapper.reflection.Records.isRecord;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.JohnzonAny;
import org.apache.johnzon.mapper.JohnzonIgnore;
import org.apache.johnzon.mapper.JohnzonProperty;
import org.apache.johnzon.mapper.JohnzonRecord;
import org.apache.johnzon.mapper.MapperException;
import org.apache.johnzon.mapper.ObjectConverter;
import org.apache.johnzon.mapper.reflection.Records;

public class MethodAccessMode extends BaseAccessMode {
    private final boolean supportGetterAsWritter;
    private Set<String> excludedMethods = Set.of("toString", "hashCode");
    private boolean supportAllRecordAttributes;
    private int version = -1;

    public MethodAccessMode(final boolean useConstructor, final boolean acceptHiddenConstructor, final boolean supportGetterAsWritter) {
        super(useConstructor, acceptHiddenConstructor);
        this.supportGetterAsWritter = supportGetterAsWritter;
    }

    public Set<String> getExcludedMethods() {
        return excludedMethods;
    }

    public void setExcludedMethods(final Set<String> excludedMethods) {
        this.excludedMethods = excludedMethods == null || excludedMethods.isEmpty()
                ? Set.of("toString", "hashCode")
                : new HashSet<>(excludedMethods);
    }

    public boolean isSupportAllGetters() {
        return supportAllRecordAttributes;
    }

    public void setSupportAllRecordAttributes(final boolean supportAllRecordAttributes) {
        this.supportAllRecordAttributes = supportAllRecordAttributes;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }

    @Override
    public Map<String, Reader> doFindReaders(final Class<?> clazz) {
        final Map<String, Reader> readers = new HashMap<>();
        if (isRecord(clazz) || Meta.getAnnotation(clazz, JohnzonRecord.class) != null) {
            // real records expose exactly their components, only @JohnzonRecord classes
            // fall back on "any no-arg instance method" since they carry no component metadata
            final Set<String> components = Records.componentNames(clazz);
            readers.putAll(Stream.of(clazz.getMethods())
                .filter(it -> it.getDeclaringClass() != Object.class && it.getParameterCount() == 0)
                .filter(it -> !Modifier.isStatic(it.getModifiers()))
                .filter(it -> supportAllRecordAttributes || components == null || components.contains(it.getName()))
                .filter(it -> !excludedMethods.contains(it.getName()))
                .filter(it -> !isIgnored(it.getName()) && Meta.getAnnotation(it, JohnzonAny.class) == null)
                .filter(it -> !isAlwaysIgnored(it))
                .collect(toMap(m -> extractKey(m.getName(), m, null), it -> new MethodReader(it, it.getGenericReturnType()))));
        } else {
            final PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(clazz);
            for (final PropertyDescriptor descriptor : propertyDescriptors) {
                final Method readMethod = descriptor.getReadMethod();
                final String name = descriptor.getName();
                if (readMethod != null && readMethod.getDeclaringClass() != Object.class) {
                    if (isIgnored(name) || Meta.getAnnotation(readMethod, JohnzonAny.class) != null
                            || isAlwaysIgnored(readMethod)) {
                        continue;
                    }
                    readers.put(extractKey(name, readMethod, null), new MethodReader(readMethod, readMethod.getGenericReturnType()));
                } else if (readMethod == null && descriptor.getWriteMethod() != null && // isXXX, not supported by javabeans
                        (descriptor.getPropertyType() == Boolean.class || descriptor.getPropertyType() == boolean.class)) {
                    try {
                        final Method method = clazz.getMethod(
                                "is" + Character.toUpperCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : ""));
                        if (!isAlwaysIgnored(method)) {
                            readers.put(extractKey(name, method, null), new MethodReader(method, method.getGenericReturnType()));
                        }
                    } catch (final NoSuchMethodException e) {
                        // no-op
                    }
                }
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

    private boolean isAlwaysIgnored(final Method method) {
        final JohnzonIgnore ignore = Meta.getAnnotation(method, JohnzonIgnore.class);
        if (ignore == null) {
            return false;
        }
        final int minVersion = ignore.minVersion();
        if (minVersion < 0) {
            return true;
        }
        return version >= 0 && version < minVersion;
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
            return Meta.getClassOrPackageAnnotation(method, clazz);
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
        public boolean isNillable(final boolean global) {
            return global;
        }

        @Override
        public String toString() {
            return "MethodDecoratedType{" +
                   "method=" + method +
                   '}';
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
                throw new MapperException("Error calling " + method, e);
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
                throw new MapperException("Error calling " + method, e);
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
                    throw new MapperException("Error calling " + method, e);
                }
            }
        }

        @Override
        public ObjectConverter.Reader<?> findObjectConverterReader() {
            return null;
        }
    }
}
