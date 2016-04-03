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

import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.converter.EnumConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;
import org.apache.johnzon.mapper.reflection.JohnzonCollectionType;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.xml.bind.DatatypeConverter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Arrays.asList;
import static org.apache.johnzon.mapper.internal.Streams.noClose;

public class Mapper implements Closeable {
    private static final Adapter<Object, String> FALLBACK_CONVERTER = new ConverterAdapter<Object>(new FallbackConverter());
    private static final JohnzonParameterizedType ANY_LIST = new JohnzonParameterizedType(List.class, Object.class);

    protected final MapperConfig config;
    protected final Mappings mappings;
    protected final JsonReaderFactory readerFactory;
    protected final JsonGeneratorFactory generatorFactory;
    protected final ConcurrentMap<Adapter<?, ?>, AdapterKey> reverseAdaptersRegistry = new ConcurrentHashMap<Adapter<?, ?>, AdapterKey>();
    protected final ReaderHandler readerHandler;
    protected final Collection<Closeable> closeables;

    Mapper(final JsonReaderFactory readerFactory, final JsonGeneratorFactory generatorFactory, MapperConfig config,
                  final Comparator<String> attributeOrder,
                  final Collection<Closeable> closeables) {
        this.readerFactory = readerFactory;
        this.generatorFactory = generatorFactory;
        this.config = config;
        this.mappings = new Mappings(attributeOrder, config.getAccessMode(), config.getVersion(), config.getAdapters());
        this.readerHandler = ReaderHandler.create(readerFactory);
        this.closeables = closeables;
    }

    /**
     * @deprecated see MapperConfig
     */
    private Adapter findAdapter(final Type aClass) {
        final Adapter<?, ?> converter = config.getAdapters().get(new AdapterKey(aClass, String.class));
        if (converter != null) {
            return converter;
        }
        if (Class.class.isInstance(aClass)) {
            final Class<?> clazz = Class.class.cast(aClass);
            if (clazz.isEnum()) {
                final Adapter<?, ?> enumConverter = new ConverterAdapter(new EnumConverter(clazz));
                config.getAdapters().putIfAbsent(new AdapterKey(String.class, aClass), enumConverter);
                return enumConverter;
            }
        }
        return null;
    }

    /**
     * @deprecated see MapperConfig
     */
    private Object convertTo(final Type aClass, final String text) {
        if (Object.class == aClass || String.class == aClass) {
            return text;
        }
        final Adapter converter = findAdapter(aClass);
        if (converter == null) {
            config.getAdapters().putIfAbsent(new AdapterKey(String.class, aClass), FALLBACK_CONVERTER);
            return FALLBACK_CONVERTER.to(text);
        }
        return converter.to(text);
    }

    public <T> void writeArray(final Object object, final OutputStream stream) {
        writeObject(asList((T[]) object), stream);
    }

    public <T> void writeArray(final T[] object, final OutputStream stream) {
        writeObject(asList(object), stream);
    }

    public <T> void writeArray(final T[] object, final Writer stream) {
        writeObject(asList(object), stream);
    }

    public <T> void writeArray(final Collection<T> object, final OutputStream stream) {
        writeArray(object, new OutputStreamWriter(stream, config.getEncoding()));
    }

    public <T> void writeArray(final Collection<T> object, final Writer stream) {
        JsonGenerator generator = generatorFactory.createGenerator(stream(stream));
        try {
            writeObject(object, generator);
        } finally {
            generator.close();
        }
    }

    public <T> void writeIterable(final Iterable<T> object, final OutputStream stream) {
        writeIterable(object, new OutputStreamWriter(stream, config.getEncoding()));
    }

    public <T> void writeIterable(final Iterable<T> object, final Writer stream) {
        JsonGenerator generator = generatorFactory.createGenerator(stream(stream));
        try {
            writeObject(object, generator);
        } finally {
            generator.close();
        }
    }

    public void writeObject(final Object object, final Writer stream) {
        if (JsonValue.class.isInstance(object) || String.class.isInstance(object) || Number.class.isInstance(object) || object == null) {
            try {
                stream.write(String.valueOf(object));
            } catch (final IOException e) {
                throw new MapperException(e);
            } finally {
                if (config.isClose()) {
                    try {
                        stream.close();
                    } catch (final IOException e) {
                        // no-op
                    }
                } else {
                    try {
                        stream.flush();
                    } catch (final IOException e) {
                        // no-op
                    }
                }
            }
            return;
        }

        final JsonGenerator generator = generatorFactory.createGenerator(stream(stream));
        writeObject(object, generator);
    }

    public void writeObject(final Object object, final OutputStream stream) {
        final JsonGenerator generator = generatorFactory.createGenerator(stream(stream), config.getEncoding());
        writeObject(object, generator);
    }

    private void writeObject(Object object, JsonGenerator generator) {
        MappingGeneratorImpl mappingGenerator = new MappingGeneratorImpl(config, generator, mappings);
        mappingGenerator.doWriteObject(object, true);
        generator.close();
    }

    public String writeArrayAsString(final Collection<?> instance) {
        final StringWriter writer = new StringWriter();
        writeArray(instance, writer);
        return writer.toString();
    }

    public <T> String writeArrayAsString(final T[] instance) {
        final StringWriter writer = new StringWriter();
        writeArray(instance, writer);
        return writer.toString();
    }

    public String writeObjectAsString(final Object instance) {
        final StringWriter writer = new StringWriter();
        writeObject(instance, writer);
        return writer.toString();
    }


    public <T> T readObject(final String string, final Type clazz) {
        return readObject(new StringReader(string), clazz);
    }

    public <T> T readObject(final Reader stream, final Type clazz) {
        return mapObject(clazz, readerFactory.createReader(stream(stream)));
    }

    public <T> T readObject(final InputStream stream, final Type clazz) {
        return mapObject(clazz, readerFactory.createReader(stream(stream)));
    }

    private <T> T mapObject(final Type clazz, final JsonReader reader) {
        try {
            final JsonValue object = readerHandler.read(reader);
            if (JsonStructure.class == clazz || JsonObject.class == clazz || JsonValue.class == clazz) {
                return (T) object;
            }
            if (JsonObject.class.isInstance(object)) {
                return (T) buildObject(clazz, JsonObject.class.cast(object));
            }
            if (JsonString.class.isInstance(object) && clazz == String.class) {
                return (T) JsonString.class.cast(object).getString();
            }
            if (JsonNumber.class.isInstance(object)) {
                final JsonNumber number = JsonNumber.class.cast(object);
                if (clazz == int.class || clazz == Integer.class) {
                    return (T) Integer.valueOf(number.intValue());
                }
                if (clazz == long.class || clazz == Long.class) {
                    return (T) Long.valueOf(number.longValue());
                }
                if (clazz == double.class || clazz == Double.class) {
                    return (T) Double.valueOf(number.doubleValue());
                }
                if (clazz == BigDecimal.class) {
                    return (T) number.bigDecimalValue();
                }
                if (clazz == BigInteger.class) {
                    return (T) number.bigIntegerValue();
                }
            }
            if (JsonValue.NULL == object) {
                return null;
            }
            if (JsonValue.TRUE == object && (Boolean.class == clazz || boolean.class == clazz)) {
                return (T) Boolean.TRUE;
            }
            if (JsonValue.FALSE == object && (Boolean.class == clazz || boolean.class == clazz)) {
                return (T) Boolean.FALSE;
            }
            throw new IllegalArgumentException("Unsupported " + object + " for type " + clazz);
        } catch (final Exception e) {
            throw new MapperException(e);
        } finally {
            if (config.isClose()) {
                reader.close();
            }
        }
    }

    public <T> Collection<T> readCollection(final InputStream stream, final ParameterizedType genericType) {
        final JsonReader reader = readerFactory.createReader(stream(stream));
        final Mappings.CollectionMapping mapping = mappings.findCollectionMapping(genericType);
        if (mapping == null) {
            throw new UnsupportedOperationException("type " + genericType + " not supported");
        }
        try {
            return mapCollection(mapping, reader.readArray(), null);
        } catch (final Exception e) {
            throw new MapperException(e);
        } finally {
            if (config.isClose()) {
                reader.close();
            }
        }
    }

    public <T> T readJohnzonCollection(final InputStream stream, final JohnzonCollectionType<T> genericType) {
        return (T) readCollection(stream, genericType);
    }

    public <T> T readJohnzonCollection(final Reader stream, final JohnzonCollectionType<T> genericType) {
        return (T) readCollection(stream, genericType);
    }

    public <T> Collection<T> readCollection(final Reader stream, final ParameterizedType genericType) {
        final JsonReader reader = readerFactory.createReader(stream(stream));
        final Mappings.CollectionMapping mapping = mappings.findCollectionMapping(genericType);
        if (mapping == null) {
            throw new UnsupportedOperationException("type " + genericType + " not supported");
        }
        try {
            return mapCollection(mapping, reader.readArray(), null);
        } catch (final Exception e) {
            throw new MapperException(e);
        } finally {
            if (config.isClose()) {
                reader.close();
            }
        }
    }

    public <T> T[] readArray(final Reader stream, final Class<T> clazz) {
        final JsonReader reader = readerFactory.createReader(stream(stream));
        return (T[]) mapArray(clazz, reader);
    }

    public <T> T readTypedArray(final InputStream stream, final Class<?> elementType, final Class<T> arrayType) {
        final JsonReader reader = readerFactory.createReader(stream(stream));
        return arrayType.cast(mapArray(elementType, reader));
    }

    public <T> T readTypedArray(final Reader stream, final Class<?> elementType, final Class<T> arrayType) {
        final JsonReader reader = readerFactory.createReader(stream(stream));
        return arrayType.cast(mapArray(elementType, reader));
    }

    public <T> T[] readArray(final InputStream stream, final Class<T> clazz) {
        final JsonReader reader = readerFactory.createReader(stream(stream));
        return (T[]) mapArray(clazz, reader);
    }

    private Object mapArray(final Class<?> clazz, final JsonReader reader) {
        try {
            return buildArrayWithComponentType(reader.readArray(), clazz, null);
        } catch (final Exception e) {
            throw new MapperException(e);
        } finally {
            if (config.isClose()) {
                reader.close();
            }
        }
    }

    private Object buildObject(final Type inType, final JsonObject object) throws Exception {
        Type type = inType;
        if (inType == Object.class) {
            type = new JohnzonParameterizedType(Map.class, String.class, Object.class);
        }

        final Mappings.ClassMapping classMapping = mappings.findOrCreateClassMapping(type);

        if (classMapping == null) {
            if (ParameterizedType.class.isInstance(type)) {
                final ParameterizedType aType = ParameterizedType.class.cast(type);
                final Type[] fieldArgTypes = aType.getActualTypeArguments();
                if (fieldArgTypes.length >= 2) {
                    final Class<?> raw = Class.class.cast(aType.getRawType());

                    final Map map;
                    if (LinkedHashMap.class == raw) {
                        map = new LinkedHashMap();
                    } else if (SortedMap.class.isAssignableFrom(raw) || NavigableMap.class == raw || TreeMap.class == raw) {
                        map = new TreeMap();
                    } else if (ConcurrentMap.class.isAssignableFrom(raw)) {
                        map = new ConcurrentHashMap(object.size());
                    } else if (EnumMap.class.isAssignableFrom(raw)) {
                        map = new EnumMap(Class.class.cast(fieldArgTypes[0]));
                    } else if (Map.class.isAssignableFrom(raw)) {
                        map = new HashMap(object.size());
                    } else {
                        map = null;
                    }

                    if (map != null) {

                        Type keyType;
                        if (ParameterizedType.class.isInstance(fieldArgTypes[0])) {
                            keyType = fieldArgTypes[0];
                        } else {
                            keyType = fieldArgTypes[0];
                        }

                        final boolean any = fieldArgTypes.length < 2 || fieldArgTypes[1] == Object.class;
                        for (final Map.Entry<String, JsonValue> value : object.entrySet()) {
                            final JsonValue jsonValue = value.getValue();
                            if (JsonNumber.class.isInstance(jsonValue) && any) {
                                final JsonNumber number = JsonNumber.class.cast(jsonValue);
                                if (readerHandler.isJsonLong(number)) {
                                    final int integer = number.intValue();
                                    final long asLong = number.longValue();
                                    if (integer == asLong) {
                                        map.put(value.getKey(), integer);
                                    } else {
                                        map.put(value.getKey(), asLong);
                                    }
                                } else {
                                    map.put(value.getKey(), !number.isIntegral() ? number.bigDecimalValue() : number.intValue());
                                }
                            } else if (JsonString.class.isInstance(jsonValue) && any) {
                                map.put(value.getKey(), JsonString.class.cast(jsonValue).getString());
                            } else {
                                map.put(convertTo(keyType, value.getKey()), toObject(null, jsonValue, fieldArgTypes[1], null));
                            }
                        }
                        return map;
                    }
                }
            } else if (Map.class == type || HashMap.class == type || LinkedHashMap.class == type) {
                final LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
                for (final Map.Entry<String, JsonValue> value : object.entrySet()) {
                    map.put(value.getKey(), toObject(null, value.getValue(), Object.class, null));
                }
                return map;
            }
        }
        if (classMapping == null) {
            throw new MapperException("Can't map " + type);
        }

        if (classMapping.factory == null) {
            throw new IllegalArgumentException(classMapping.clazz + " not instantiable");
        }

        final Object t = classMapping.factory.getParameterTypes().length == 0 ?
                classMapping.factory.create(null) : classMapping.factory.create(createParameters(classMapping, object));
        for (final Map.Entry<String, Mappings.Setter> setter : classMapping.setters.entrySet()) {
            final JsonValue jsonValue = object.get(setter.getKey());
            final Mappings.Setter value = setter.getValue();
            if (JsonValue.class == value.paramType) {
                setter.getValue().writer.write(t, jsonValue);
                continue;
            }
            if (jsonValue == null) {
                continue;
            }

            final AccessMode.Writer setterMethod = value.writer;
            if (jsonValue == JsonValue.NULL) { // forced
                setterMethod.write(t, null);
            } else {
                Object existingInstance = null;
                if (config.isReadAttributeBeforeWrite()) {
                    final Mappings.Getter getter = classMapping.getters.get(setter.getKey());
                    if (getter != null) {
                        try {
                            existingInstance = getter.reader.read(t);
                        } catch (final RuntimeException re) {
                            // backward compatibility
                        }
                    }
                }
                final Object convertedValue = toValue(existingInstance, jsonValue, value.converter, value.itemConverter, value.paramType);
                if (convertedValue != null) {
                    setterMethod.write(t, convertedValue);
                }
            }
        }

        return t;
    }

    private Object toValue(final Object baseInstance, final JsonValue jsonValue, final Adapter converter,
                           final Adapter itemConverter, final Type type) throws Exception {
        return converter == null ?
                toObject(baseInstance, jsonValue, type, itemConverter) : jsonValue.getValueType() == ValueType.STRING ?
                converter.to(JsonString.class.cast(jsonValue).getString()) :
                convertTo(converter, jsonValue);
    }

    private Object convertTo(final Adapter converter, final JsonValue jsonValue) {
        if (jsonValue.getValueType() == ValueType.OBJECT) {
            AdapterKey adapterKey = reverseAdaptersRegistry.get(converter);
            if (adapterKey == null) {
                for (final Map.Entry<AdapterKey, Adapter<?, ?>> entry : config.getAdapters().entrySet()) {
                    if (entry.getValue() == converter) {
                        adapterKey = entry.getKey();
                        reverseAdaptersRegistry.put(converter, adapterKey);
                        break;
                    }
                }
            }
            final Object param;
            try {
                param = buildObject(adapterKey.getTo(), JsonObject.class.cast(jsonValue));
            } catch (final Exception e) {
                throw new MapperException(e);
            }
            return converter.to(param);
        }
        return converter.to(jsonValue.toString());
    }

    private Object[] createParameters(final Mappings.ClassMapping mapping, final JsonObject object) throws Exception {
        final int length = mapping.factory.getParameterTypes().length;
        final Object[] objects = new Object[length];
        for (int i = 0; i < length; i++) {
            objects[i] = toValue(
                    null,
                    object.get(mapping.factory.getParameterNames()[i]), mapping.factory.getParameterConverter()[i],
                    mapping.factory.getParameterItemConverter()[i], mapping.factory.getParameterTypes()[i]);
        }
        return objects;
    }

    private Object toObject(final Object baseInstance, final JsonValue jsonValue,
                            final Type type, final Adapter itemConverter) throws Exception {
        if (jsonValue == null || JsonValue.NULL == jsonValue) {
            return null;
        }

        if (type == Boolean.class || type == boolean.class) {
            if (jsonValue == JsonValue.TRUE) {
                return true;
            }
            if (jsonValue == JsonValue.FALSE) {
                return false;
            }
            throw new MapperException("Unable to parse " + jsonValue + " to boolean");
        }

        if(config.isTreatByteArrayAsBase64() && jsonValue.getValueType() == ValueType.STRING && (type == byte[].class /*|| type == Byte[].class*/)) {
            return DatatypeConverter.parseBase64Binary(((JsonString)jsonValue).getString());
        }

        if (Object.class == type) { // handling specific types here to keep exception in standard handling
            if (jsonValue == JsonValue.TRUE) {
                return true;
            }
            if (jsonValue == JsonValue.FALSE) {
                return false;
            }
            if (JsonNumber.class.isInstance(jsonValue)) {
                final JsonNumber jsonNumber = JsonNumber.class.cast(jsonValue);
                if(jsonNumber.isIntegral()) {
                    return jsonNumber.intValue();
                }
                return jsonNumber.doubleValue();
            }
        }

        if (type == Character.class || type == char.class) {
            return convertTo(Class.class.cast(type), (JsonString.class.cast(jsonValue).getString()));
        }

        if (JsonObject.class.isInstance(jsonValue)) {
            if (JsonObject.class == type || JsonStructure.class == type) {
                return jsonValue;
            }
            final boolean typedAdapter = TypeAwareAdapter.class.isInstance(itemConverter);
            final Object object = buildObject(
                    baseInstance != null ? baseInstance.getClass() : (
                            typedAdapter ? TypeAwareAdapter.class.cast(itemConverter).getTo() : type),
                    JsonObject.class.cast(jsonValue));
            return typedAdapter ? itemConverter.to(object) : object;
        } else if (JsonArray.class.isInstance(jsonValue)) {
            if (JsonArray.class == type || JsonStructure.class == type) {
                return jsonValue;
            }
            return buildArray(type, JsonArray.class.cast(jsonValue), itemConverter);
        } else if (JsonNumber.class.isInstance(jsonValue)) {
            if (JsonNumber.class == type) {
                return jsonValue;
            }

            final JsonNumber number = JsonNumber.class.cast(jsonValue);

            if (type == Integer.class || type == int.class) {
                return number.intValue();
            }

            if (type == Long.class || type == long.class) {
                return number.longValue();
            }

            if (type == Short.class || type == short.class) {
                return (short) number.intValue();
            }

            if (type == Byte.class || type == byte.class) {
                return (byte) number.intValue();
            }

            if (type == Float.class || type == float.class) {
                return (float) number.doubleValue();
            }

            if (type == Double.class || type == double.class) {
                return number.doubleValue();
            }

            if (type == BigInteger.class) {
                return number.bigIntegerValue();
            }

            if (type == BigDecimal.class) {
                return number.bigDecimalValue();
            }
        } else if (JsonString.class.isInstance(jsonValue)) {
            if (JsonString.class == type) {
                return jsonValue;
            }

            final String string = JsonString.class.cast(jsonValue).getString();
            if (itemConverter == null) {
                return convertTo(Class.class.cast(type), string);
            } else {
                return itemConverter.to(string);
            }
        }

        throw new MapperException("Unable to parse " + jsonValue + " to " + type);
    }

    private Object buildArray(final Type type, final JsonArray jsonArray, final Adapter itemConverter) throws Exception {
        if (Class.class.isInstance(type)) {
            final Class clazz = Class.class.cast(type);
            if (clazz.isArray()) {
                final Class<?> componentType = clazz.getComponentType();
                return buildArrayWithComponentType(jsonArray, componentType, itemConverter);
            }
        }

        if (ParameterizedType.class.isInstance(type)) {
            final Mappings.CollectionMapping mapping = mappings.findCollectionMapping(ParameterizedType.class.cast(type));
            if (mapping != null) {
                return mapCollection(mapping, jsonArray, itemConverter);
            }
        }

        if (Object.class == type) {
            return buildArray(ANY_LIST, jsonArray, null);
        }

        throw new UnsupportedOperationException("type " + type + " not supported");
    }

    private <T> Collection<T> mapCollection(final Mappings.CollectionMapping mapping, final JsonArray jsonArray,
                                            final Adapter itemConverter) throws Exception {
        final Collection collection;

        if (SortedSet.class == mapping.raw || NavigableSet.class == mapping.raw || TreeSet.class == mapping.raw) {
            collection = new TreeSet<T>();
        } else if (Set.class == mapping.raw || HashSet.class == mapping.raw) {
            collection = new HashSet<T>(jsonArray.size());
        } else if (Queue.class == mapping.raw || ArrayBlockingQueue.class == mapping.raw) {
            collection = new ArrayBlockingQueue<T>(jsonArray.size());
        } else if (List.class == mapping.raw || Collection.class == mapping.raw || ArrayList.class == mapping.raw || EnumSet.class == mapping.raw) {
            collection = new ArrayList<T>(jsonArray.size());
        } else if (LinkedHashSet.class == mapping.raw) {
            collection = new LinkedHashSet<T>(jsonArray.size());
        } else if (Deque.class == mapping.raw || ArrayDeque.class == mapping.raw) {
            collection = new ArrayDeque(jsonArray.size());
        } else if (Queue.class == mapping.raw || PriorityQueue.class == mapping.raw) {
            collection = new PriorityQueue(jsonArray.size());
        } else {
            throw new IllegalStateException("not supported collection type: " + mapping.raw.getName());
        }

        for (final JsonValue value : jsonArray) {
            collection.add(value == JsonValue.NULL ? null : toObject(null, value, mapping.arg, itemConverter));
        }

        if (EnumSet.class == mapping.raw) {
            if (collection.isEmpty()) {
                return EnumSet.noneOf(Class.class.cast(mapping.arg));
            } else if (collection.size() == 1) {
                return Collection.class.cast(EnumSet.of(Enum.class.cast(collection.iterator().next())));
            } else {
                final List<Enum<?>> list = List.class.cast(collection);
                return Collection.class.cast(EnumSet.of(list.get(0), list.subList(1, list.size()).toArray(new Enum[list.size() - 1])));
            }
        }

        return collection;
    }

    private Object buildArrayWithComponentType(final JsonArray jsonArray, final Class<?> componentType, final Adapter itemConverter) throws Exception {
        final Object array = Array.newInstance(componentType, jsonArray.size());
        int i = 0;
        for (final JsonValue value : jsonArray) {
            Array.set(array, i++, toObject(null, value, componentType, itemConverter));
        }
        return array;
    }

    private Reader stream(final Reader stream) {
        return !config.isClose() ? noClose(stream) : stream;
    }

    private Writer stream(final Writer stream) {
        return !config.isClose() ? noClose(stream) : stream;
    }

    private OutputStream stream(final OutputStream stream) {
        return !config.isClose() ? noClose(stream) : stream;
    }

    private InputStream stream(final InputStream stream) {
        return !config.isClose() ? noClose(stream) : stream;
    }

    @Override
    public synchronized void close() {
        Collection<Exception> errors = null;
        for (final Closeable c : closeables) {
            try {
                c.close();
            } catch (final IOException e) {
                if (errors == null) {
                    errors = new ArrayList<Exception>();
                }
                errors.add(e);
            }
        }
        closeables.clear();
        if (errors != null) {
            throw new IllegalStateException(errors.toString());
        }
    }

    private static class FallbackConverter implements Converter<Object> {
        @Override
        public String toString(final Object instance) {
            return instance.toString();
        }

        @Override
        public Object fromString(final String text) {
            throw new UnsupportedOperationException("Using fallback converter, " +
                    "this only works in write mode but not in read. Please register a custom converter to do so.");
        }
    }
}
