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
package org.apache.johnzon.mapper;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.apache.johnzon.mapper.reflection.Converters.matches;
import static org.apache.johnzon.mapper.reflection.Generics.resolve;

import jakarta.json.JsonObject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.access.FieldAccessMode;
import org.apache.johnzon.mapper.access.MethodAccessMode;
import org.apache.johnzon.mapper.converter.DateWithCopyConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;
import org.apache.johnzon.mapper.reflection.Generics;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;

public class Mappings {
    public static class ClassMapping {
        public final Class<?> clazz;
        public final AccessMode.Factory factory;
        public final Map<String, Getter> getters;
        public final Map<String, Setter> setters;
        public final Adapter adapter;
        public final ObjectConverter.Reader reader;
        public final ObjectConverter.Writer writer;
        public final Getter anyGetter;
        public final Method anySetter;
        public final Field anyField;
        public final Method mapAdder;
        public final Class<?> mapAdderType;
        public final Map.Entry<String, String>[] serializedPolymorphicProperties;
        public final BiFunction<JsonObject, Class<?>, Class<?>> polymorphicDeserializedTypeResolver;

        public boolean deduplicateObjects;

        protected ClassMapping(final Class<?> clazz, final AccessMode.Factory factory,
                               final Map<String, Getter> getters, final Map<String, Setter> setters,
                               final Adapter<?, ?> adapter,
                               final ObjectConverter.Reader<?> reader, final ObjectConverter.Writer<?> writer,
                               final Getter anyGetter, final Method anySetter, final Field anyField,
                               final Method mapAdder) {
            this(clazz, factory, getters, setters, adapter, reader, writer, anyGetter, anySetter, anyField, mapAdder, null, null);
        }

        protected ClassMapping(final Class<?> clazz, final AccessMode.Factory factory,
                               final Map<String, Getter> getters, final Map<String, Setter> setters,
                               final Adapter<?, ?> adapter,
                               final ObjectConverter.Reader<?> reader, final ObjectConverter.Writer<?> writer,
                               final Getter anyGetter, final Method anySetter, final Field anyField,
                               final Method mapAdder,
                               final Map.Entry<String, String>[] serializedPolymorphicProperties,
                               final BiFunction<JsonObject, Class<?>, Class<?>> polymorphicDeserializedTypeResolver) {
            this.clazz = clazz;
            this.factory = factory;
            this.getters = getters;
            this.setters = setters;
            this.adapter = adapter;
            this.writer = writer;
            this.reader = reader;
            this.anyGetter = anyGetter;
            this.anySetter = anySetter;
            this.anyField = anyField;
            this.mapAdder = mapAdder;
            this.mapAdderType = mapAdder == null ? null : mapAdder.getParameterTypes()[1];
            this.serializedPolymorphicProperties = serializedPolymorphicProperties;
            this.polymorphicDeserializedTypeResolver = polymorphicDeserializedTypeResolver;
            this.deduplicateObjects = isDeduplicateObjects();
        }

        private Boolean isDeduplicateObjects() {
            final JohnzonDeduplicateObjects jdo = clazz.getAnnotation(JohnzonDeduplicateObjects.class);
            if (jdo != null){
                return jdo.value();
            }
            return false;
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
        public final Adapter converter;
        public final Adapter itemConverter;
        public final ObjectConverter.Writer objectConverter;
        public final boolean dynamic;
        public final boolean primitive;
        public final boolean array;
        public final boolean map;
        public final boolean collection;
        public final Collection<String> ignoreNested;

        public Getter(final AccessMode.Reader reader, final boolean dynamic,
                      final boolean primitive, final boolean array,
                      final boolean collection, final boolean map,
                      final MapperConverter converter,
                      final ObjectConverter.Writer providedObjectConverter,
                      final int version, final String[] ignoreNested) {
            this.reader = reader;
            this.version = version;

            Adapter theConverter = null;
            Adapter theItemConverter = null;
            ObjectConverter.Writer theObjectConverter = providedObjectConverter;

            if (converter != null) {

                if (converter instanceof ObjectConverter.Writer) {
                    theObjectConverter = (ObjectConverter.Writer) converter;
                }
                if (theObjectConverter == null) {
                    Adapter adapter;
                    final Type readerType = reader.getType();
                    if (converter instanceof Converter) {
                        adapter = new ConverterAdapter((Converter) converter, readerType);
                    } else {
                        adapter = (Adapter) converter;
                    }

                    if (matches(readerType, adapter)) {
                        theConverter = adapter;
                    } else {
                        if (converter instanceof Converter) {
                            if (ParameterizedType.class.isInstance(readerType) && ParameterizedType.class.cast(readerType).getActualTypeArguments().length > 0) {
                                final Type[] args = ParameterizedType.class.cast(readerType).getActualTypeArguments();
                                // List<A> or Map<String, A> lead to read the last arg in all cases
                                theItemConverter = new ConverterAdapter((Converter) converter, args[args.length - 1]);
                            } else {
                                theItemConverter = adapter;
                            }
                        } else {
                            theItemConverter = adapter;
                        }
                    }
                }
            }

            this.converter = theConverter;
            this.itemConverter = theItemConverter;
            this.objectConverter = theObjectConverter;
            this.ignoreNested = ignoreNested == null || ignoreNested.length == 0 ? null : new HashSet<>(asList(ignoreNested));

            if (converter == null) {
                this.dynamic = dynamic;
                this.array = array;
                this.collection = collection;
                this.primitive = primitive;
                this.map = map;
            } else { // todo: likely find from/to types from the adapter and adjust these meta accordingly
                this.dynamic = true;
                this.array = array;
                this.collection = collection;
                this.primitive = primitive;
                this.map = false;
            }
        }

        @Override
        public String toString() {
            return "Getter{" +
                    "reader=" + reader +
                    ", version=" + version +
                    ", converter=" + converter +
                    ", itemConverter=" + itemConverter +
                    ", primitive=" + primitive +
                    ", array=" + array +
                    ", map=" + map +
                    ", collection=" + collection +
                    '}';
        }
    }

    public static class Setter {
        public final AccessMode.Writer writer;
        public final int version;
        public final Type paramType;
        public final Adapter converter;
        public final Adapter itemConverter;
        public final ObjectConverter.Reader objectConverter;
        public final boolean primitive;
        public final boolean array;

        public Setter(final AccessMode.Writer writer, final boolean primitive, final boolean array,
                      final Type paramType, final MapperConverter converter, final ObjectConverter.Reader providedObjectConverter,
                      final int version) {
            this.writer = writer;
            this.paramType = paramType;
            this.version = version;
            this.primitive = primitive;
            this.array = array;

            Adapter theConverter = null;
            Adapter theItemConverter = null;
            ObjectConverter.Reader theObjectConverter = providedObjectConverter;

            if (converter != null) {

                if (converter instanceof ObjectConverter.Reader) {
                    theObjectConverter = (ObjectConverter.Reader) converter;
                }
                final Type writerType = writer.getType();
                if (theObjectConverter == null) {
                    Adapter adapter;
                    if (converter instanceof Converter) {
                        adapter = new ConverterAdapter((Converter) converter, writerType);
                    } else {
                        adapter = (Adapter) converter;
                    }

                    if (matches(writerType, adapter)) {
                        theConverter = adapter;
                    } else {
                        if (converter instanceof Converter) {
                            if (ParameterizedType.class.isInstance(writerType) && ParameterizedType.class.cast(writerType).getActualTypeArguments().length > 0) {
                                final Type[] args = ParameterizedType.class.cast(writerType).getActualTypeArguments();
                                // List<A> or Map<String, A> lead to read the last arg in all cases
                                theItemConverter = new ConverterAdapter((Converter) converter, args[args.length - 1]);
                            } else {
                                theItemConverter = adapter;
                            }
                        } else {
                            theItemConverter = adapter;
                        }
                    }
                }
            }

            this.converter = theConverter;
            this.itemConverter = theItemConverter;
            this.objectConverter = theObjectConverter;
        }

        @Override
        public String toString() {
            return "Setter{" +
                    "writer=" + writer +
                    ", version=" + version +
                    ", paramType=" + paramType +
                    ", converter=" + converter +
                    ", itemConverter=" + itemConverter +
                    ", primitive=" + primitive +
                    ", array=" + array +
                    '}';
        }
    }

    private static final JohnzonParameterizedType VIRTUAL_TYPE = new JohnzonParameterizedType(Map.class, String.class, Object.class);

    protected final ConcurrentMap<Type, ClassMapping> classes = new ConcurrentHashMap<Type, ClassMapping>();
    protected final ConcurrentMap<Type, CollectionMapping> collections = new ConcurrentHashMap<Type, CollectionMapping>();

    protected final MapperConfig config;

    public Mappings(final MapperConfig config) {
        this.config = config;
    }

    public CollectionMapping findCollectionMapping(final ParameterizedType genericType, final Type enclosingType) {
        CollectionMapping collectionMapping = collections.get(genericType);
        if (collectionMapping == null) {
            collectionMapping = createCollectionMapping(genericType, enclosingType);
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

    private <T> CollectionMapping createCollectionMapping(final ParameterizedType aType, final Type root) {
        final Type[] fieldArgTypes = aType.getActualTypeArguments();
        final Type raw = aType.getRawType();
        if (fieldArgTypes.length == 1 && Class.class.isInstance(raw)) {
            final Class<?> r = Class.class.cast(raw);
            final Class<?> collectionType;
            if (PriorityQueue.class.isAssignableFrom(r)) {
                collectionType = PriorityQueue.class;
            } else if (LinkedHashSet.class.isAssignableFrom(r)) {
                collectionType = LinkedHashSet.class;
            } else if (LinkedList.class.isAssignableFrom(r)) {
                collectionType = LinkedList.class;
            } else if (TreeSet.class.isAssignableFrom(r)) {
                collectionType = TreeSet.class;
            } else if (List.class.isAssignableFrom(r)) {
                collectionType = List.class;
            } else if (SortedSet.class.isAssignableFrom(r)) {
                collectionType = SortedSet.class;
            } else if (EnumSet.class.isAssignableFrom(r)) {
                collectionType = EnumSet.class;
            } else if (Set.class.isAssignableFrom(r)) {
                collectionType = Set.class;
            } else if (Deque.class.isAssignableFrom(r)) {
                collectionType = Deque.class;
            } else if (Queue.class.isAssignableFrom(r)) {
                collectionType = Queue.class;
            } else if (Collection.class.isAssignableFrom(r)) {
                collectionType = Collection.class;
            } else {
                return null;
            }

            final CollectionMapping mapping = new CollectionMapping(isPrimitive(fieldArgTypes[0]), collectionType,
                    Generics.resolve(fieldArgTypes[0], root, emptyMap()));
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

    public static Object getPrimitiveDefault(final Type type) {
        if (type == char.class) {
            return '\u0000';
        } else if (type == long.class) {
            return 0L;
        } else if (type == int.class) {
            return 0;
        } else if (type == short.class) {
            return (short) 0;
        } else if (type == byte.class) {
            return (byte) 0;
        } else if (type == double.class) {
            return 0.;
        } else if (type == float.class) {
            return 0f;
        } else if (type == boolean.class) {
            return false;
        }
        return null;
    }

    public ClassMapping getClassMapping(final Type clazz) {
        return classes.get(clazz);
    }

    public ClassMapping findOrCreateClassMapping(final Type clazz) {
        if (isPrimitive(clazz)) {
            return null;
        }
        return doFindOrCreateClassMapping(clazz, emptyMap(), false);
    }

    private ClassMapping doFindOrCreateClassMapping(final Type clazz, final Map<Type, Type> args,
                                                    final boolean noPutForClass) {
        ClassMapping classMapping = classes.get(clazz);
        if (classMapping == null) {
            if (ParameterizedType.class.isInstance(clazz)) {
                final ParameterizedType pt = ParameterizedType.class.cast(clazz);
                final ClassMapping mapping = doFindOrCreateClassMapping(pt.getRawType(), Generics.toResolvedTypes(pt), true);
                return putOrGetClassMapping(clazz, mapping);
            }
            if (!Class.class.isInstance(clazz)) {
                return null;
            }
            final Class asClass = Class.class.cast(clazz);
            if (Map.class.isAssignableFrom(asClass) || asClass.isInterface()) {
                final Class<?> mapping = config.getInterfaceImplementationMapping().get(clazz);
                if (mapping != null) {
                    classMapping = createClassMapping(mapping, args);
                } else if (asClass.getName().startsWith("java.")) { // we'll not be able to map it with pojo rules
                    return null;
                } else { // assume that it can be written with pojo rules but not deserialized
                    classMapping = createClassMapping(asClass, args);
                }
            } else {
                classMapping = createClassMapping(asClass, args);
            }
            if (!noPutForClass) {
                classMapping = putOrGetClassMapping(clazz, classMapping);
            }
        }
        return classMapping;
    }

    private ClassMapping putOrGetClassMapping(final Type clazz, final ClassMapping classMapping) {
        if (classMapping == null) {
            return null;
        }
        final ClassMapping existing = classes.putIfAbsent(clazz, classMapping);
        if (existing != null) {
            return existing;
        }
        return classMapping;
    }

    protected ClassMapping createClassMapping(final Class<?> inClazz, final Map<Type, Type> resolvedTypes) {
        boolean copyDate = false;
        for (final Class<?> itf : inClazz.getInterfaces()) {
            if ("org.apache.openjpa.enhance.PersistenceCapable".equals(itf.getName())) {
                copyDate = true;
                break;
            }
        }
        final Class<?> clazz = findModelClass(inClazz);

        AccessMode accessMode = config.getAccessMode();

        Comparator<String> fieldComparator = accessMode.fieldComparator(inClazz);
        fieldComparator = fieldComparator == null ? config.getAttributeOrder() : fieldComparator;

        final Map<String, Getter> getters = fieldComparator == null ? newOrderedMap(Getter.class) : new TreeMap<>(fieldComparator);
        final Map<String, Setter> setters = fieldComparator == null ? newOrderedMap(Setter.class) : new TreeMap<>(fieldComparator);

        try {
            final Map<String, AccessMode.Reader> readers = accessMode.findReaders(clazz);
            final Map<String, AccessMode.Writer> writers = accessMode.findWriters(clazz);

            final Collection<String> virtualFields = new HashSet<String>();
            {
                final JohnzonVirtualObjects virtualObjects = clazz.getAnnotation(JohnzonVirtualObjects.class);
                if (virtualObjects != null) {
                    for (final JohnzonVirtualObject virtualObject : virtualObjects.value()) {
                        handleVirtualObject(virtualFields, virtualObject, getters, setters, readers, writers, copyDate, clazz);
                    }
                }

                final JohnzonVirtualObject virtualObject = clazz.getAnnotation(JohnzonVirtualObject.class);
                if (virtualObject != null) {
                    handleVirtualObject(virtualFields, virtualObject, getters, setters, readers, writers, copyDate, clazz);
                }
            }

            for (final Map.Entry<String, AccessMode.Reader> reader : readers.entrySet()) {
                final String key = reader.getKey();
                if (virtualFields.contains(key)) {
                    continue;
                }
                addGetterIfNeeded(getters, key, reader.getValue(), copyDate, resolvedTypes);
            }

            for (final Map.Entry<String, AccessMode.Writer> writer : writers.entrySet()) {
                final String key = writer.getKey();
                if (virtualFields.contains(key)) {
                    continue;
                }
                addSetterIfNeeded(setters, key, writer.getValue(), copyDate, clazz, resolvedTypes);
            }

            final Method anyGetter = accessMode.findAnyGetter(clazz);
            final Field anyField = accessMode.findAnyField(clazz);
            final ClassMapping mapping = new ClassMapping(
                    clazz, accessMode.findFactory(clazz), getters, setters,
                    accessMode.findAdapter(clazz),
                    accessMode.findReader(clazz),
                    accessMode.findWriter(clazz),
                    anyGetter != null ? new Getter(
                            new MethodAccessMode.MethodReader(anyGetter, anyGetter.getReturnType()),
                            false,false, false, false, true, null, null, -1, null) :
                            (anyField != null ? new Getter(new FieldAccessMode.FieldReader(anyField, anyField.getGenericType()),
                            false,false, false, false, true, null, null, -1, null) : null),
                    accessMode.findAnySetter(clazz),
                    anyField,
                    Map.class.isAssignableFrom(clazz) ? accessMode.findMapAdder(clazz) : null,
                    config.getSerializationPredicate() != null && config.getSerializationPredicate().test(clazz)
                            ? new Map.Entry[] { Map.entry(config.getDiscriminator(), config.getDiscriminatorMapper().apply(clazz)) } : null,
                    config.getDeserializationPredicate() != null && config.getDeserializationPredicate().test(clazz)
                            ? (jsonObject, type) -> config.getTypeLoader().apply(jsonObject.getString(config.getDiscriminator())) : null);

            accessMode.afterParsed(clazz);

            return mapping;
        } finally {
            if (Cleanable.class.isInstance(accessMode)) {
                ((Cleanable<Class<?>>) accessMode).clean(clazz);
            }
        }
    }

    protected Class<?> findModelClass(final Class<?> inClazz) {
        Class<?> clazz = inClazz;
        // unproxy to get a clean model
        while (clazz != null && clazz != Object.class
                && (clazz.getName().contains("$$") || clazz.getName().contains("$proxy")
                || clazz.getName().startsWith("org.apache.openjpa.enhance.") /* subclassing mode, not the default */)) {
            clazz = clazz.getSuperclass();
        }
        if (clazz == null || clazz == Object.class) { // shouldn't occur but a NPE protection
            clazz = inClazz;
        }
        return clazz;
    }

    private <T> Map<String, T> newOrderedMap(final Class<T> value) {
        return config.getAttributeOrder() != null ? new TreeMap<>(config.getAttributeOrder()) : new HashMap<>();
    }

    private void addSetterIfNeeded(final Map<String, Setter> setters,
                                   final String key,
                                   final AccessMode.Writer value,
                                   final boolean copyDate,
                                   final Class<?> rootClass,
                                   final Map<Type, Type> resolvedTypes) {
        final JohnzonIgnore writeIgnore = value.getAnnotation(JohnzonIgnore.class);
        if (writeIgnore == null || writeIgnore.minVersion() >= 0) {
            if (key.equals("metaClass")) {
                return;
            }
            final Type param = lookupType(value, resolvedTypes);
            final Class<?> returnType = Class.class.isInstance(param) ? Class.class.cast(param) : null;
            final Setter setter = new Setter(
                    value, isPrimitive(param),
                    (returnType != null && returnType.isArray()) || GenericArrayType.class.isInstance(value.getType()),
                    resolve(param, rootClass, resolvedTypes),
                    findConverter(copyDate, value), value.findObjectConverterReader(),
                    writeIgnore != null ? writeIgnore.minVersion() : -1);
            setters.put(key, setter);
        }
    }

    private Type lookupType(final AccessMode.DecoratedType value, final Map<Type, Type> resolvedTypes) {
        return resolvedTypes.getOrDefault(value.getType(), value.getType());
    }

    private void addGetterIfNeeded(final Map<String, Getter> getters,
                                   final String key,
                                   final AccessMode.Reader value,
                                   final boolean copyDate,
                                   final Map<Type, Type> resolvedTypes) {
        final JohnzonIgnore readIgnore = value.getAnnotation(JohnzonIgnore.class);
        final JohnzonIgnoreNested ignoreNested = value.getAnnotation(JohnzonIgnoreNested.class);
        if (readIgnore == null || readIgnore.minVersion() >= 0) {
            final Type type = lookupType(value, resolvedTypes);
            final Class<?> returnType = Class.class.isInstance(type) ? Class.class.cast(type) : null;
            final ParameterizedType pt = ParameterizedType.class.isInstance(type) ? ParameterizedType.class.cast(type) : null;
            final Getter getter = new Getter(value, returnType == Object.class, isPrimitive(returnType),
                    (returnType != null && returnType.isArray()) || GenericArrayType.class.isInstance(type),
                    (pt != null && Collection.class.isAssignableFrom(Class.class.cast(pt.getRawType())))
                            || (returnType != null && Collection.class.isAssignableFrom(returnType)),
                    (pt != null && Map.class.isAssignableFrom(Class.class.cast(pt.getRawType())))
                            || (returnType != null && Map.class.isAssignableFrom(returnType)),
                    findConverter(copyDate, value), value.findObjectConverterWriter(),
                    readIgnore != null ? readIgnore.minVersion() : -1,
                    ignoreNested != null ? ignoreNested.properties() : null);
            getters.put(key, getter);
        }
    }

    // idea is quite trivial, simulate an object with a Map<String, Object>
    private void handleVirtualObject(final Collection<String> virtualFields,
                                     final JohnzonVirtualObject o,
                                     final Map<String, Getter> getters,
                                     final Map<String, Setter> setters,
                                     final Map<String, AccessMode.Reader> readers,
                                     final Map<String, AccessMode.Writer> writers,
                                     final boolean copyDate,
                                     final Class<?> rootClazz) {
        final String[] path = o.path();
        if (path.length < 1) {
            throw new IllegalArgumentException("@JohnzonVirtualObject need a path");
        }

        // add them to ignored fields
        for (final JohnzonVirtualObject.Field f : o.fields()) {
            virtualFields.add(f.value());
        }

        // build "this" model
        final Map<String, Getter> objectGetters = newOrderedMap(Getter.class);
        final Map<String, Setter> objectSetters = newOrderedMap(Setter.class);

        for (final JohnzonVirtualObject.Field f : o.fields()) {
            final String name = f.value();
            if (f.read()) {
                final AccessMode.Reader reader = readers.get(name);
                if (reader != null) {
                    addGetterIfNeeded(objectGetters, name, reader, copyDate, emptyMap());
                }
            }
            if (f.write()) {
                final AccessMode.Writer writer = writers.get(name);
                if (writer != null) {
                    addSetterIfNeeded(objectSetters, name, writer, copyDate, rootClazz, emptyMap());
                }
            }
        }

        final String key = path[0];

        final Getter getter = getters.get(key);
        final MapBuilderReader newReader = new MapBuilderReader(objectGetters, path, config.getVersion());
        getters.put(key, new Getter(getter == null ? newReader :
                new CompositeReader(getter.reader, newReader), false, false, false, false, true, null, null, -1, null));

        final Setter newSetter = setters.get(key);
        final MapUnwrapperWriter newWriter = new MapUnwrapperWriter(objectSetters, path);
        setters.put(key, new Setter(newSetter == null ? newWriter : new CompositeWriter(newSetter.writer, newWriter), false, false, VIRTUAL_TYPE, null, null, -1));
    }

    private MapperConverter findConverter(final boolean copyDate, final AccessMode.DecoratedType decoratedType) {
        MapperConverter converter = decoratedType.findConverter();
        if (converter != null) {
            return converter;
        }

        final JohnzonConverter annotation = decoratedType.getAnnotation(JohnzonConverter.class);

        Type typeToTest = decoratedType.getType();
        if (annotation != null) {
            try {
                converter = annotation.value().newInstance();
            } catch (final Exception e) {
                throw new IllegalArgumentException(e);
            }
        } else if (ParameterizedType.class.isInstance(decoratedType.getType())) {
            final ParameterizedType type = ParameterizedType.class.cast(decoratedType.getType());
            final Type rawType = type.getRawType();
            if (Class.class.isInstance(rawType)
                    && Collection.class.isAssignableFrom(Class.class.cast(rawType))
                    && type.getActualTypeArguments().length >= 1) {
                typeToTest = type.getActualTypeArguments()[0];
            } // TODO: map
        }
        if (converter == null && Class.class.isInstance(typeToTest)) {
            final Class type = Class.class.cast(typeToTest);
            ConcurrentMap<AdapterKey, Adapter<?, ?>> adapters = config.getAdapters();

            if (Date.class.isAssignableFrom(type) && copyDate) {
                converter = new DateWithCopyConverter(Adapter.class.cast(adapters.get(new AdapterKey(Date.class, String.class))));
            } else {
                for (final Map.Entry<AdapterKey, Adapter<?, ?>> adapterEntry : adapters.entrySet()) {
                    if (adapterEntry.getKey().getFrom() == adapterEntry.getKey().getTo()) { // String -> String
                        continue;
                    }
                    if (adapterEntry.getKey().getFrom() == type && !(
                            // ignore internal converters to let primitives be correctly handled
                            ConverterAdapter.class.isInstance(adapterEntry.getValue()) &&
                                    isBuiltInJohnzonConverter(ConverterAdapter.class.cast(adapterEntry.getValue()).getConverter()))) {

                        if (converter != null) {
                            throw new IllegalArgumentException("Ambiguous adapter for " + decoratedType);
                        }
                        converter = adapterEntry.getValue();
                    }
                }
            }
            if (converter == null && Enum.class.isAssignableFrom(type)) {
                final AdapterKey key = new AdapterKey(String.class, type);
                converter = adapters.get(key); // first ensure user didnt override it
                if (converter == null) {
                    converter = new ConverterAdapter(config.getEnumConverterFactory().apply(type), type);
                    adapters.put(key, (Adapter<?, ?>) converter);
                }
            }
        }
        return converter;
    }

    private boolean isBuiltInJohnzonConverter(final Converter<?> converter) {
        final Class<?> clazz = converter.getClass();
        // don't reduce too much this package filter, we really want to filter the mapper module ones
        return clazz.getName().startsWith("org.apache.johnzon.mapper.") ||
                // jsonb adds a custom converter for enums so we must filter this one too
                (MapperConfig.CustomEnumConverter.class.isAssignableFrom(clazz) && clazz.getName().startsWith("org.apache.johnzon."));
    }

    private static class MapBuilderReader implements AccessMode.Reader {
        private final Map<String, Getter> getters;
        private final Map<String, Object> template;
        private final String[] paths;
        private final int version;

        public MapBuilderReader(final Map<String, Getter> objectGetters, final String[] paths, final int version) {
            this.getters = objectGetters;
            this.paths = paths;
            this.template = new LinkedHashMap<>();
            this.version = version;

            Map<String, Object> last = this.template;
            for (int i = 1; i < paths.length; i++) {
                final Map<String, Object> newLast = new LinkedHashMap<>();
                last.put(paths[i], newLast);
                last = newLast;
            }
        }

        @Override
        public Object read(final Object instance) {
            final Map<String, Object> map = new LinkedHashMap<>(template);
            Map<String, Object> nested = map;
            for (int i = 1; i < paths.length; i++) {
                nested = Map.class.cast(nested.get(paths[i]));
            }
            for (final Map.Entry<String, Getter> g : getters.entrySet()) {
                final Mappings.Getter getter = g.getValue();
                final Object value = getter.reader.read(instance);
                final Object val = value == null || getter.converter == null ? value : getter.converter.from(value);
                if (val == null) {
                    continue;
                }
                if (getter.version >= 0 && version >= getter.version) {
                    continue;
                }

                nested.put(g.getKey(), val);
            }
            return map;
        }

        @Override
        public ObjectConverter.Writer<?> findObjectConverterWriter() {
            return null;
        }

        @Override
        public Type getType() {
            return VIRTUAL_TYPE;
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
            throw new UnsupportedOperationException("getAnnotation shouldn't get called for virtual fields");
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
            return globalConfig;
        }
    }

    private static class MapUnwrapperWriter implements AccessMode.Writer {
        private final Map<String, Setter> writers;
        private final Map<String, Class<?>> componentTypes;
        private final String[] paths;

        public MapUnwrapperWriter(final Map<String, Setter> writers, final String[] paths) {
            this.writers = writers;
            this.paths = paths;
            this.componentTypes = new HashMap<>();

            for (final Map.Entry<String, Setter> setter : writers.entrySet()) {
                if (setter.getValue().array) {
                    componentTypes.put(setter.getKey(),
                            Class.class.isInstance(setter.getValue().paramType) ?
                                    Class.class.cast(setter.getValue().paramType).getComponentType() :
                                    cast(GenericArrayType.class.cast(setter.getValue().paramType).getGenericComponentType()));
                }
            }
        }

        private Class<?> cast(final Type genericComponentType) {
            if (Class.class.isInstance(genericComponentType)) {
                return Class.class.cast(genericComponentType);
            }
            if (ParameterizedType.class.isInstance(genericComponentType)) {
                return cast(ParameterizedType.class.cast(genericComponentType).getRawType());
            }
            throw new UnsupportedOperationException("Unsupported type: " + genericComponentType);
        }

        @Override
        public void write(final Object instance, final Object value) {
            Map<String, Object> nested = null;
            for (final String path : paths) {
                nested = Map.class.cast(nested == null ? value : nested.get(path));
                if (nested == null) {
                    return;
                }
            }

            for (final Map.Entry<String, Setter> setter : writers.entrySet()) {
                final Setter setterValue = setter.getValue();
                final String key = setter.getKey();
                final Object rawValue = nested.get(key);
                Object val = value == null || setterValue.converter == null ?
                        rawValue : Converter.class.cast(setterValue.converter).toString(rawValue);
                if (val == null) {
                    continue;
                }

                if (setterValue.array && Collection.class.isInstance(val)) {
                    final Collection<?> collection = Collection.class.cast(val);
                    final Object[] array = (Object[]) Array.newInstance(componentTypes.get(key), collection.size());
                    val = collection.toArray(array);
                }

                final AccessMode.Writer setterMethod = setterValue.writer;
                setterMethod.write(instance, val);
            }
        }

        @Override
        public ObjectConverter.Reader<?> findObjectConverterReader() {
            return null;
        }

        @Override
        public Type getType() {
            return VIRTUAL_TYPE;
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
            throw new UnsupportedOperationException("getAnnotation shouldn't get called for virtual fields");
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
            return globalConfig;
        }
    }

    private static class CompositeReader implements AccessMode.Reader {
        private final AccessMode.Reader[] delegates;

        public CompositeReader(final AccessMode.Reader... delegates) {
            final Collection<AccessMode.Reader> all = new LinkedList<AccessMode.Reader>();
            for (final AccessMode.Reader r : delegates) {
                if (CompositeReader.class.isInstance(r)) {
                    all.addAll(asList(CompositeReader.class.cast(r).delegates));
                } else {
                    all.add(r);
                }
            }
            this.delegates = all.toArray(new AccessMode.Reader[all.size()]);
        }

        @Override
        public Object read(final Object instance) {
            final Map<String, Object> map = new LinkedHashMap<String, Object>();
            for (final AccessMode.Reader reader : delegates) {
                final Map<String, Object> readerMap = (Map<String, Object>) reader.read(instance);
                for (final Map.Entry<String, Object> entry : readerMap.entrySet()) {
                    final Object o = map.get(entry.getKey());
                    if (o == null) {
                        map.put(entry.getKey(), entry.getValue());
                    } else if (Map.class.isInstance(o)) {
                        // TODO
                    } else {
                        throw new IllegalStateException(entry.getKey() + " is ambiguous");
                    }
                }
            }
            return map;
        }

        @Override
        public ObjectConverter.Writer<?> findObjectConverterWriter() {
            for (final AccessMode.Reader w : delegates) {
                final ObjectConverter.Writer<?> objectConverter = w.findObjectConverterWriter();
                if (objectConverter != null) {
                    return objectConverter;
                }
            }
            return null;
        }

        @Override
        public Type getType() {
            return VIRTUAL_TYPE;
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
            throw new UnsupportedOperationException("getAnnotation shouldn't get called for virtual fields");
        }

        @Override
        public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
            return null;
        }

        @Override
        public Adapter<?, ?> findConverter() {
            for (final AccessMode.Reader r : delegates) {
                final Adapter<?, ?> converter = r.findConverter();
                if (converter != null) {
                    return converter;
                }
            }
            return null;
        }

        @Override
        public boolean isNillable(final boolean global) {
            for (final AccessMode.Reader r : delegates) {
                if (r.isNillable(global)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class CompositeWriter implements AccessMode.Writer {
        private final AccessMode.Writer[] delegates;

        public CompositeWriter(final AccessMode.Writer... writers) {
            final Collection<AccessMode.Writer> all = new LinkedList<AccessMode.Writer>();
            for (final AccessMode.Writer r : writers) {
                if (CompositeWriter.class.isInstance(r)) {
                    all.addAll(asList(CompositeWriter.class.cast(r).delegates));
                } else {
                    all.add(r);
                }
            }
            this.delegates = all.toArray(new AccessMode.Writer[all.size()]);
        }

        @Override
        public void write(final Object instance, final Object value) {
            for (final AccessMode.Writer w : delegates) {
                w.write(instance, value);
            }
        }

        @Override
        public ObjectConverter.Reader<?> findObjectConverterReader() {
            for (final AccessMode.Writer w : delegates) {
                final ObjectConverter.Reader<?> objectConverter = w.findObjectConverterReader();
                if (objectConverter != null) {
                    return objectConverter;
                }
            }
            return null;
        }

        @Override
        public Type getType() {
            return VIRTUAL_TYPE;
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
            throw new UnsupportedOperationException("getAnnotation shouldn't get called for virtual fields");
        }

        @Override
        public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
            return null;
        }

        @Override
        public Adapter<?, ?> findConverter() {
            for (final AccessMode.Writer r : delegates) {
                final Adapter<?, ?> converter = r.findConverter();
                if (converter != null) {
                    return converter;
                }
            }
            return null;
        }

        @Override
        public boolean isNillable(final boolean global) {
            for (final AccessMode.Writer r : delegates) {
                if (r.isNillable(global)) {
                    return true;
                }
            }
            return false;
        }
    }
}
