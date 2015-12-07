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
import org.apache.johnzon.mapper.JohnzonVirtualObject;
import org.apache.johnzon.mapper.JohnzonVirtualObjects;
import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.converter.DateWithCopyConverter;
import org.apache.johnzon.mapper.converter.EnumConverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Arrays.asList;
import static org.apache.johnzon.mapper.reflection.Converters.matches;

public class Mappings {
    public static class ClassMapping {
        public final Class<?> clazz;
        public final AccessMode.Factory factory;
        public final Map<String, Getter> getters;
        public final Map<String, Setter> setters;


        protected ClassMapping(final Class<?> clazz, final AccessMode.Factory factory,
                               final Map<String, Getter> getters, final Map<String, Setter> setters) {
            this.clazz = clazz;
            this.factory = factory;
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
        public final AccessMode.Reader reader;
        public final int version;
        public final Converter<Object> converter;
        public final Converter<Object> itemConverter;
        public final boolean primitive;
        public final boolean array;
        public final boolean map;
        public final boolean collection;

        public Getter(final AccessMode.Reader reader,
                      final boolean primitive, final boolean array,
                      final boolean collection, final boolean map,
                      final Converter<Object> converter,
                      final int version) {
            this.reader = reader;
            this.version = version;
            this.array = array;
            this.collection = collection;
            this.primitive = primitive;
            if (converter != null && matches(reader.getType(), converter)) {
                this.converter = converter;
                this.itemConverter = null;
            } else if (converter != null) {
                this.converter = null;
                this.itemConverter = converter;
            } else {
                this.converter = null;
                this.itemConverter = null;
            }
            this.map = map && this.converter == null;
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
        public final Converter<?> converter;
        public final Converter<?> itemConverter;
        public final boolean primitive;
        public final boolean array;

        public Setter(final AccessMode.Writer writer, final boolean primitive, final boolean array,
                      final Type paramType, final Converter<?> converter,
                      final int version) {
            this.writer = writer;
            this.paramType = paramType;
            this.version = version;
            this.primitive = primitive;
            this.array = array;
            if (converter != null && matches(writer.getType(), converter)) {
                this.converter = converter;
                this.itemConverter = null;
            } else if (converter != null) {
                this.converter = null;
                this.itemConverter = converter;
            } else {
                this.converter = null;
                this.itemConverter = null;
            }
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
    protected final Comparator<String> fieldOrdering;
    protected final ConcurrentMap<Type, Converter<?>> converters;
    private final AccessMode accessMode;
    private final int version;

    public Mappings(final Comparator<String> attributeOrder, final AccessMode accessMode,
                    final int version, final ConcurrentMap<Type, Converter<?>> converters) {
        this.fieldOrdering = attributeOrder;
        this.accessMode = accessMode;
        this.version = version;
        this.converters = converters;
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
            } else if (SortedSet.class.isAssignableFrom(r)) {
                collectionType = SortedSet.class;
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

    protected ClassMapping createClassMapping(final Class<?> inClazz) {
        boolean copyDate = false;
        for (final Class<?> itf : inClazz.getInterfaces()) {
            if ("org.apache.openjpa.enhance.PersistenceCapable".equals(itf.getName())) {
                copyDate = true;
                break;
            }
        }
        final Class<?> clazz = findModelClass(inClazz);

        Comparator<String> fieldComparator = accessMode.fieldComparator(inClazz);
        fieldComparator = fieldComparator == null ? fieldOrdering : fieldComparator;

        final Map<String, Getter> getters = fieldComparator == null ? newOrderedMap(Getter.class) : new TreeMap<String, Getter>(fieldComparator);
        final Map<String, Setter> setters = fieldComparator == null ? newOrderedMap(Setter.class) : new TreeMap<String, Setter>(fieldComparator);

        final Map<String, AccessMode.Reader> readers = accessMode.findReaders(clazz);
        final Map<String, AccessMode.Writer> writers = accessMode.findWriters(clazz);

        final Collection<String> virtualFields = new HashSet<String>();
        {
            final JohnzonVirtualObjects virtualObjects = clazz.getAnnotation(JohnzonVirtualObjects.class);
            if (virtualObjects != null) {
                for (final JohnzonVirtualObject virtualObject : virtualObjects.value()) {
                    handleVirtualObject(virtualFields, virtualObject, getters, setters, readers, writers, copyDate);
                }
            }

            final JohnzonVirtualObject virtualObject = clazz.getAnnotation(JohnzonVirtualObject.class);
            if (virtualObject != null) {
                handleVirtualObject(virtualFields, virtualObject, getters, setters, readers, writers, copyDate);
            }
        }

        for (final Map.Entry<String, AccessMode.Reader> reader : readers.entrySet()) {
            final String key = reader.getKey();
            if (virtualFields.contains(key)) {
                continue;
            }
            addGetterIfNeeded(getters, key, reader.getValue(), copyDate);
        }

        for (final Map.Entry<String, AccessMode.Writer> writer : writers.entrySet()) {
            final String key = writer.getKey();
            if (virtualFields.contains(key)) {
                continue;
            }
            addSetterIfNeeded(setters, key, writer.getValue(), copyDate);
        }
        return new ClassMapping(clazz, accessMode.findFactory(clazz), getters, setters);
    }

    protected Class<?> findModelClass(final Class<?> inClazz) {
        Class<?> clazz = inClazz;
        // unproxy to get a clean model
        while (clazz != null && clazz != Object.class
                && (clazz.getName().contains("$$") || clazz.getName().contains("$proxy")
                || clazz.getName().startsWith("org.apache.openjpa.enhance.") /* subclassing mode, not the default */ )) {
            clazz = clazz.getSuperclass();
        }
        if (clazz == null || clazz == Object.class) { // shouldn't occur but a NPE protection
            clazz = inClazz;
        }
        return clazz;
    }

    private <T> Map<String, T> newOrderedMap(final Class<T> value) {
        return fieldOrdering != null ? new TreeMap<String, T>(fieldOrdering) : new HashMap<String, T>();
    }

    private void addSetterIfNeeded(final Map<String, Setter> setters,
                                   final String key,
                                   final AccessMode.Writer value,
                                   final boolean copyDate) {
        final JohnzonIgnore writeIgnore = value.getAnnotation(JohnzonIgnore.class);
        if (writeIgnore == null || writeIgnore.minVersion() >= 0) {
            if (key.equals("metaClass")) {
                return;
            }
            final Type param = value.getType();
            final Class<?> returnType = Class.class.isInstance(param) ? Class.class.cast(param) : null;
            final Setter setter = new Setter(
                    value, isPrimitive(param), returnType != null && returnType.isArray(), param,
                    findConverter(copyDate, value), writeIgnore != null ? writeIgnore.minVersion() : -1);
            setters.put(key, setter);
        }
    }

    private void addGetterIfNeeded(final Map<String, Getter> getters,
                                   final String key,
                                   final AccessMode.Reader value,
                                   final boolean copyDate) {
        final JohnzonIgnore readIgnore = value.getAnnotation(JohnzonIgnore.class);
        if (readIgnore == null || readIgnore.minVersion() >= 0) {
            final Class<?> returnType = Class.class.isInstance(value.getType()) ? Class.class.cast(value.getType()) : null;
            final ParameterizedType pt = ParameterizedType.class.isInstance(value.getType()) ? ParameterizedType.class.cast(value.getType()) : null;
            final Getter getter = new Getter(value, isPrimitive(returnType),
                    returnType != null && returnType.isArray(),
                    (pt != null && Collection.class.isAssignableFrom(Class.class.cast(pt.getRawType())))
                            || (returnType != null && Collection.class.isAssignableFrom(returnType)),
                    (pt != null && Map.class.isAssignableFrom(Class.class.cast(pt.getRawType())))
                            || (returnType != null && Map.class.isAssignableFrom(returnType)),
                    findConverter(copyDate, value),
                    readIgnore != null ? readIgnore.minVersion() : -1);
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
                                     final boolean copyDate) {
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
                    addGetterIfNeeded(objectGetters, name, reader,copyDate);
                }
            }
            if (f.write()) {
                final AccessMode.Writer writer = writers.get(name);
                if (writer != null) {
                    addSetterIfNeeded(objectSetters, name, writer, copyDate);
                }
            }
        }

        final String key = path[0];

        final Getter getter = getters.get(key);
        final MapBuilderReader newReader = new MapBuilderReader(objectGetters, path, version);
        getters.put(key, new Getter(getter == null ? newReader : new CompositeReader(getter.reader, newReader), false, false, false, true, null, -1));

        final Setter newSetter = setters.get(key);
        final MapUnwrapperWriter newWriter = new MapUnwrapperWriter(objectSetters, path);
        setters.put(key, new Setter(newSetter == null ? newWriter : new CompositeWriter(newSetter.writer, newWriter), false, false, VIRTUAL_TYPE, null, -1));
    }

    private Converter findConverter(final boolean copyDate, final AccessMode.DecoratedType decoratedType) {
        Converter converter = decoratedType.findConverter();
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
            if (Date.class.isAssignableFrom(type) && copyDate) {
                converter = new DateWithCopyConverter(Converter.class.cast(converters.get(Date.class)));
            } else if (type.isEnum()) {
                converter = converters.get(type); // first ensure user didnt override it
                if (converter == null) {
                    converter = new EnumConverter(type);
                    converters.put(type, converter);
                }
            }
        }
        return converter;
    }

    private static class MapBuilderReader implements AccessMode.Reader {
        private final Map<String, Getter> getters;
        private final Map<String, Object> template;
        private final String[] paths;
        private final int version;

        public MapBuilderReader(final Map<String, Getter> objectGetters, final String[] paths, final int version) {
            this.getters = objectGetters;
            this.paths = paths;
            this.template = new LinkedHashMap<String, Object>();
            this.version = version;

            Map<String, Object> last = this.template;
            for (int i = 1; i < paths.length; i++) {
                final Map<String, Object> newLast = new LinkedHashMap<String, Object>();
                last.put(paths[i], newLast);
                last = newLast;
            }
        }

        @Override
        public Object read(final Object instance) {
            final Map<String, Object> map = new LinkedHashMap<String, Object>(template);
            Map<String, Object> nested = map;
            for (int i = 1; i < paths.length; i++) {
                nested = Map.class.cast(nested.get(paths[i]));
            }
            for (final Map.Entry<String, Getter> g : getters.entrySet()) {
                final Mappings.Getter getter = g.getValue();
                final Object value = getter.reader.read(instance);
                final Object val = value == null || getter.converter == null ? value : getter.converter.toString(value);
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
        public Converter<?> findConverter() {
            return null;
        }

        @Override
        public boolean isNillable() {
            return false;
        }
    }

    private static class MapUnwrapperWriter implements AccessMode.Writer {
        private final Map<String, Setter> writers;
        private final Map<String, Class<?>> componentTypes;
        private final String[] paths;

        public MapUnwrapperWriter(final Map<String, Setter> writers, final String[] paths) {
            this.writers = writers;
            this.paths = paths;
            this.componentTypes = new HashMap<String, Class<?>>();

            for (final Map.Entry<String, Setter> setter : writers.entrySet()) {
                if (setter.getValue().array) {
                    componentTypes.put(setter.getKey(), Class.class.cast(setter.getValue().paramType).getComponentType());
                }
            }
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
        public Converter<?> findConverter() {
            return null;
        }

        @Override
        public boolean isNillable() {
            return false;
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
                for (final Map.Entry<String, Object> entry :readerMap.entrySet()) {
                    final Object o = map.get(entry.getKey());
                    if (o == null) {
                        map.put(entry.getKey(), entry.getValue());
                    } else  if (Map.class.isInstance(o)) {
                        // TODO
                    } else {
                        throw new IllegalStateException(entry.getKey() + " is ambiguous");
                    }
                }
            }
            return map;
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
        public Converter<?> findConverter() {
            for (final AccessMode.Reader r : delegates) {
                final Converter<?> converter = r.findConverter();
                if (converter != null) {
                    return converter;
                }
            }
            return null;
        }

        @Override
        public boolean isNillable() {
            for (final AccessMode.Reader r : delegates) {
                if (r.isNillable()) {
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
        public Converter<?> findConverter() {
            for (final AccessMode.Writer r : delegates) {
                final Converter<?> converter = r.findConverter();
                if (converter != null) {
                    return converter;
                }
            }
            return null;
        }

        @Override
        public boolean isNillable() {
            for (final AccessMode.Writer r : delegates) {
                if (r.isNillable()) {
                    return true;
                }
            }
            return false;
        }
    }
}
