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
package org.apache.johnzon.mapper.reflection;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.JohnzonConverter;
import org.apache.johnzon.mapper.JohnzonIgnore;
import org.apache.johnzon.mapper.MapperException;

public class Mappings {
    public static class ClassMapping {
        public final Class<?> clazz;
        public final Map<String, Getter> getters;
        public final Map<String, Setter> setters;

        protected ClassMapping(final Class<?> clazz,
                               final Map<String, Getter> getters, final Map<String, Setter> setters) {
            this.clazz = clazz;
            this.getters = getters;
            this.setters = setters;
        }
    }

    public static class CollectionMapping {
        public final Class<?> raw;
        public final Type arg;
        public final boolean primitive;

        public CollectionMapping(final boolean primitive, final Class<?> collectionType, final Type fieldArgType) {
            this.raw = collectionType;
            this.arg = fieldArgType;
            this.primitive = primitive;
        }
    }

    public static class Getter {
        public final Method method;
        public final int version;
        public final Converter<Object> converter;
        public final boolean primitive;
        public final boolean array;
        public final boolean map;
        public final boolean collection;

        public Getter(final Method method,
                      final boolean primitive, final boolean array,
                      final boolean collection, final boolean map,
                      final Converter<Object> converter, final int version) {
            this.method = method;
            this.converter = converter;
            this.version = version;
            this.array = array;
            this.map = map && converter == null;
            this.collection = collection;
            this.primitive = primitive;
        }
    }

    public static class Setter {
        public final Method method;
        public final int version;
        public final Type paramType;
        public final Converter<?> converter;
        public final boolean primitive;

        public Setter(final Method method, final boolean primitive, final Type paramType, final Converter<?> converter, final int version) {
            this.method = method;
            this.paramType = paramType;
            this.converter = converter;
            this.version = version;
            this.primitive = primitive;
        }
    }

    protected final ConcurrentMap<Type, ClassMapping> classes = new ConcurrentHashMap<Type, ClassMapping>();
    protected final ConcurrentMap<Type, CollectionMapping> collections = new ConcurrentHashMap<Type, CollectionMapping>();
    protected final Comparator<String> fieldOrdering;

    public Mappings(final Comparator<String> attributeOrder) {
        this.fieldOrdering = attributeOrder;
    }

    public <T> CollectionMapping findCollectionMapping(final ParameterizedType genericType, final Class<T> raw) {
        CollectionMapping collectionMapping = collections.get(genericType);
        if (collectionMapping == null) {
            collectionMapping = createCollectionMapping(genericType, raw);
            if (collectionMapping == null) {
                return null;
            }
            final CollectionMapping existing = collections.putIfAbsent(genericType, collectionMapping);
            if (existing != null) {
                collectionMapping = existing;
            }
        }
        return collectionMapping;
    }

    private <T> CollectionMapping createCollectionMapping(final ParameterizedType aType, final Class<T> raw) {
        final Type[] fieldArgTypes = aType.getActualTypeArguments();
        if (fieldArgTypes.length == 1) {
            final Class<?> collectionType;
            if (List.class.isAssignableFrom(raw)) {
                collectionType = List.class;
            }else if (SortedSet.class.isAssignableFrom(raw)) {
                collectionType = SortedSet.class;
            } else if (Set.class.isAssignableFrom(raw)) {
                collectionType = Set.class;
            } else if (Queue.class.isAssignableFrom(raw)) {
                collectionType = Queue.class;
            } else if (Collection.class.isAssignableFrom(raw)) {
                collectionType = Collection.class;
            } else {
                return null;
            }

            final CollectionMapping mapping = new CollectionMapping(isPrimitive(fieldArgTypes[0]), collectionType, fieldArgTypes[0]);
            collections.putIfAbsent(aType, mapping);
            return mapping;
        }
        return null;
    }

    // has JSon API a method for this type
    public static boolean isPrimitive(final Type type) {
        if (type == String.class) {
            return true;
        } else if (type == char.class || type == Character.class) {
            return true;
        } else if (type == long.class || type == Long.class) {
            return true;
        } else if (type == int.class || type == Integer.class
                || type == byte.class || type == Byte.class
                || type == short.class || type == Short.class) {
            return true;
        } else if (type == double.class || type == Double.class
                || type == float.class || type == Float.class) {
            return true;
        } else if (type == boolean.class || type == Boolean.class) {
            return true;
        } else if (type == BigDecimal.class) {
            return true;
        } else if (type == BigInteger.class) {
            return true;
        }
        return false;
    }

    public ClassMapping getClassMapping(final Type clazz) {
        return classes.get(clazz);
    }

    public ClassMapping findOrCreateClassMapping(final Type clazz) {
        ClassMapping classMapping = classes.get(clazz);
        if (classMapping == null) {
            if (!Class.class.isInstance(clazz) || Map.class.isAssignableFrom(Class.class.cast(clazz))) {
                return null;
            }

            classMapping = createClassMapping(Class.class.cast(clazz));
            final ClassMapping existing = classes.putIfAbsent(clazz, classMapping);
            if (existing != null) {
                classMapping = existing;
            }
        }
        return classMapping;
    }

    private ClassMapping createClassMapping(final Class<?> clazz) {
        try {
            final Map<String, Getter> getters = fieldOrdering != null ?
                new TreeMap<String, Getter>(fieldOrdering) : new HashMap<String, Getter>();
            final Map<String, Setter> setters = fieldOrdering != null ?
                new TreeMap<String, Setter>(fieldOrdering) : new HashMap<String, Setter>();

            final PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
            for (final PropertyDescriptor descriptor : propertyDescriptors) {
                final Method writeMethod = descriptor.getWriteMethod();
                final JohnzonIgnore writeIgnore = writeMethod != null ? writeMethod.getAnnotation(JohnzonIgnore.class) : null;
                if (writeMethod != null && writeMethod.getDeclaringClass() != Object.class
                        && (writeIgnore == null || writeIgnore.minVersion() >= 0)) {
                    if (descriptor.getName().equals("metaClass")) {
                        continue;
                    }
                    final Type param = writeMethod.getGenericParameterTypes()[0];
                    setters.put(descriptor.getName(), new Setter(
                            writeMethod,
                            isPrimitive(param),
                            param,
                            findConverter(writeMethod),
                            writeIgnore != null ? writeIgnore.minVersion() : -1));
                }

                final Method readMethod = descriptor.getReadMethod();
                final JohnzonIgnore readIgnore = readMethod != null ? readMethod.getAnnotation(JohnzonIgnore.class) : null;
                if (readMethod != null && readMethod.getDeclaringClass() != Object.class
                        && (readIgnore == null || readIgnore.minVersion() >= 0)) {
                    if (descriptor.getName().equals("metaClass")) {
                        continue;
                    }

                    final Class<?> returnType = readMethod.getReturnType();
                    getters.put(descriptor.getName(), new Getter(
                            readMethod,
                            isPrimitive(returnType),
                            returnType.isArray(),
                            Collection.class.isAssignableFrom(returnType),
                            Map.class.isAssignableFrom(returnType),
                            findConverter(readMethod),
                            readIgnore != null ? readIgnore.minVersion() : -1));
                }
            }

            return new ClassMapping(clazz, getters, setters);
        } catch (final IntrospectionException e) {
            throw new MapperException(e);
        }
    }

    private static Converter findConverter(final Method method) {
        Converter converter = null;
        if (method.getAnnotation(JohnzonConverter.class) != null) {
            try {
                converter = method.getAnnotation(JohnzonConverter.class).value().newInstance();
            } catch (final Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        return converter;
    }
}
