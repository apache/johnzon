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

import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.JohnzonConverter;
import org.apache.johnzon.mapper.JohnzonIgnore;
import org.apache.johnzon.mapper.access.AccessMode;

import java.beans.ConstructorProperties;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
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

public class Mappings {
    public static class ClassMapping {
        public final Class<?> clazz;
        public final Map<String, Getter> getters;
        public final Map<String, Setter> setters;
        public final Constructor<?> constructor;
        public final boolean constructorHasArguments;
        public final String[] constructorParameters;
        public final Converter<?>[] constructorParameterConverters;
        public final Type[] constructorParameterTypes;

        protected ClassMapping(final Class<?> clazz,
                               final Map<String, Getter> getters, final Map<String, Setter> setters,
                               final boolean acceptHiddenConstructor, final boolean useConstructor) {
            this.clazz = clazz;
            this.getters = getters;
            this.setters = setters;
            this.constructor = findConstructor(acceptHiddenConstructor, useConstructor);

            this.constructorHasArguments = this.constructor != null && this.constructor.getGenericParameterTypes().length > 0;
            if (this.constructorHasArguments) {
                this.constructorParameterTypes = this.constructor.getGenericParameterTypes();

                this.constructorParameters = new String[this.constructor.getGenericParameterTypes().length];
                final ConstructorProperties constructorProperties = this.constructor.getAnnotation(ConstructorProperties.class);
                System.arraycopy(constructorProperties.value(), 0, this.constructorParameters, 0, this.constructorParameters.length);

                this.constructorParameterConverters = new Converter<?>[this.constructor.getGenericParameterTypes().length];
                for (int i = 0; i < this.constructorParameters.length; i++) {
                    for (final Annotation a : this.constructor.getParameterAnnotations()[i]) {
                        if (a.annotationType() == JohnzonConverter.class) {
                            try {
                                this.constructorParameterConverters[i] = JohnzonConverter.class.cast(a).value().newInstance();
                            } catch (final Exception e) {
                                throw new IllegalArgumentException(e);
                            }
                        }
                    }
                }
            } else {
                this.constructorParameterTypes = null;
                this.constructorParameters = null;
                this.constructorParameterConverters = null;
            }
        }

        private Constructor<?> findConstructor(final boolean acceptHiddenConstructor, final boolean useConstructor) {
            Constructor<?> found = null;
            for (final Constructor<?> c : clazz.getDeclaredConstructors()) {
                if (c.getParameterTypes().length == 0) {
                    if (!Modifier.isPublic(c.getModifiers()) && acceptHiddenConstructor) {
                        c.setAccessible(true);
                    }
                    found = c;
                    if (!useConstructor) {
                        break;
                    }
                } else if (c.getAnnotation(ConstructorProperties.class) != null) {
                    found = c;
                    break;
                }
            }
            if (found != null) {
                return found;
            }
            try {
                return clazz.getConstructor();
            } catch (final NoSuchMethodException e) {
                return null; // readOnly class
            }
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
        public final AccessMode.Reader reader;
        public final int version;
        public final Converter<Object> converter;
        public final boolean primitive;
        public final boolean array;
        public final boolean map;
        public final boolean collection;

        public Getter(final AccessMode.Reader reader,
                      final boolean primitive, final boolean array,
                      final boolean collection, final boolean map,
                      final Converter<Object> converter, final int version) {
            this.reader = reader;
            this.converter = converter;
            this.version = version;
            this.array = array;
            this.map = map && converter == null;
            this.collection = collection;
            this.primitive = primitive;
        }
    }

    public static class Setter {
        public final AccessMode.Writer writer;
        public final int version;
        public final Type paramType;
        public final Converter<?> converter;
        public final boolean primitive;

        public Setter(final AccessMode.Writer writer, final boolean primitive, final Type paramType, final Converter<?> converter, final int version) {
            this.writer = writer;
            this.paramType = paramType;
            this.converter = converter;
            this.version = version;
            this.primitive = primitive;
        }
    }

    protected final ConcurrentMap<Type, ClassMapping> classes = new ConcurrentHashMap<Type, ClassMapping>();
    protected final ConcurrentMap<Type, CollectionMapping> collections = new ConcurrentHashMap<Type, CollectionMapping>();
    protected final Comparator<String> fieldOrdering;
    private final boolean supportHiddenConstructors;
    private final boolean supportConstructors;
    private final AccessMode accessMode;

    public Mappings(final Comparator<String> attributeOrder, final AccessMode accessMode,
                    final boolean supportHiddenConstructors, final boolean supportConstructors) {
        this.fieldOrdering = attributeOrder;
        this.accessMode = accessMode;
        this.supportHiddenConstructors = supportHiddenConstructors;
        this.supportConstructors = supportConstructors;
    }

    public <T> CollectionMapping findCollectionMapping(final ParameterizedType genericType) {
        CollectionMapping collectionMapping = collections.get(genericType);
        if (collectionMapping == null) {
            collectionMapping = createCollectionMapping(genericType);
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

    private <T> CollectionMapping createCollectionMapping(final ParameterizedType aType) {
        final Type[] fieldArgTypes = aType.getActualTypeArguments();
        final Type raw = aType.getRawType();
        if (fieldArgTypes.length == 1 && Class.class.isInstance(raw)) {
            final Class<?> r = Class.class.cast(raw);
            final Class<?> collectionType;
            if (List.class.isAssignableFrom(r)) {
                collectionType = List.class;
            }else if (SortedSet.class.isAssignableFrom(r)) {
                collectionType = SortedSet.class;
            } else if (Set.class.isAssignableFrom(r)) {
                collectionType = Set.class;
            } else if (Queue.class.isAssignableFrom(r)) {
                collectionType = Queue.class;
            } else if (Collection.class.isAssignableFrom(r)) {
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
        final Map<String, Getter> getters = fieldOrdering != null ?
            new TreeMap<String, Getter>(fieldOrdering) : new HashMap<String, Getter>();
        final Map<String, Setter> setters = fieldOrdering != null ?
            new TreeMap<String, Setter>(fieldOrdering) : new HashMap<String, Setter>();

        for (final Map.Entry<String, AccessMode.Reader> reader : accessMode.findReaders(clazz).entrySet()) {
            final AccessMode.Reader value = reader.getValue();
            final JohnzonIgnore readIgnore = value.getAnnotation(JohnzonIgnore.class);
            if (readIgnore == null || readIgnore.minVersion() >= 0) {
                final Class<?> returnType = Class.class.isInstance(value.getType()) ? Class.class.cast(value.getType()) : null;
                final ParameterizedType pt = ParameterizedType.class.isInstance(value.getType()) ? ParameterizedType.class.cast(value.getType()) : null;
                getters.put(reader.getKey(), new Getter(value, isPrimitive(returnType),
                        returnType != null && returnType.isArray(),
                        (pt != null && Collection.class.isAssignableFrom(Class.class.cast(pt.getRawType())))
                                || (returnType != null && Collection.class.isAssignableFrom(returnType)),
                        (pt != null && Map.class.isAssignableFrom(Class.class.cast(pt.getRawType())))
                                || (returnType != null && Map.class.isAssignableFrom(returnType)),
                        findConverter(value),
                        readIgnore != null ? readIgnore.minVersion() : -1));
            }
        }
        for (final Map.Entry<String, AccessMode.Writer> writer : accessMode.findWriters(clazz).entrySet()) {
            final AccessMode.Writer value = writer.getValue();
            final JohnzonIgnore writeIgnore = value.getAnnotation(JohnzonIgnore.class);
            if (writeIgnore == null || writeIgnore.minVersion() >= 0) {
                final String key = writer.getKey();
                if (key.equals("metaClass")) {
                    continue;
                }
                final Type param = value.getType();
                setters.put(key, new Setter(value, isPrimitive(param), param, findConverter(value), writeIgnore != null ? writeIgnore.minVersion() : -1));
            }
        }
        return new ClassMapping(clazz, getters, setters, supportHiddenConstructors, supportConstructors);
    }

    private static Converter findConverter(final AccessMode.DecoratedType method) {
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
