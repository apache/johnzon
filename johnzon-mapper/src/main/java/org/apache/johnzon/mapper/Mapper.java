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

import org.apache.johnzon.core.JsonLongImpl;
import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.converter.EnumConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;
import org.apache.johnzon.mapper.reflection.JohnzonCollectionType;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;
import org.apache.johnzon.mapper.reflection.Mappings;

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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
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

public class Mapper implements Closeable {
    private static final Adapter<Object, String> FALLBACK_CONVERTER = new ConverterAdapter<Object>(new FallbackConverter());
    private static final JohnzonParameterizedType ANY_LIST = new JohnzonParameterizedType(List.class, Object.class);

    protected final Mappings mappings;
    protected final JsonReaderFactory readerFactory;
    protected final JsonGeneratorFactory generatorFactory;
    protected final boolean close;
    protected final ConcurrentMap<AdapterKey, Adapter<?, ?>> adapters;
    protected final ConcurrentMap<Adapter<?, ?>, AdapterKey> reverseAdaptersRegistry = new ConcurrentHashMap<Adapter<?, ?>, AdapterKey>();
    protected final int version;
    protected final boolean skipNull;
    protected final boolean skipEmptyArray;
    protected final boolean treatByteArrayAsBase64;
    protected final boolean treatByteArrayAsBase64URL;
    protected final boolean readAttributeBeforeWrite;
    protected final Charset encoding;
    protected final ReaderHandler readerHandler;
    protected final Collection<Closeable> closeables;

    // CHECKSTYLE:OFF
    public Mapper(final JsonReaderFactory readerFactory, final JsonGeneratorFactory generatorFactory,
                  final boolean doClose, final Map<AdapterKey, Adapter<?, ?>> adapters,
                  final int version, final Comparator<String> attributeOrder, final boolean skipNull, final boolean skipEmptyArray,
                  final AccessMode accessMode, final boolean treatByteArrayAsBase64, final boolean treatByteArrayAsBase64URL, final Charset encoding,
                  final Collection<Closeable> closeables, final boolean readAttributeBeforeWrite) {
    // CHECKSTYLE:ON
        this.readerFactory = readerFactory;
        this.generatorFactory = generatorFactory;
        this.close = doClose;
        this.adapters = new ConcurrentHashMap<AdapterKey, Adapter<?, ?>>(adapters);
        this.version = version;
        this.mappings = new Mappings(attributeOrder, accessMode, version, this.adapters);
        this.skipNull = skipNull;
        this.skipEmptyArray = skipEmptyArray;
        this.treatByteArrayAsBase64 = treatByteArrayAsBase64;
        this.treatByteArrayAsBase64URL = treatByteArrayAsBase64URL;
        this.encoding = encoding;
        this.readerHandler = ReaderHandler.create(readerFactory);
        this.closeables = closeables;
        this.readAttributeBeforeWrite = readAttributeBeforeWrite;
    }

    private static JsonGenerator writePrimitives(final JsonGenerator generator, final Object value) {
        if (value == null) {
            return null; // fake a write
        }

        final Class<?> type = value.getClass();
        if (type == String.class) {
            return generator.write(value.toString());
        } else if (type == long.class || type == Long.class) {
            return generator.write(Long.class.cast(value).longValue());
        } else if (isInt(type)) {
            return generator.write(Number.class.cast(value).intValue());
        } else if (isFloat(type)) {
            final double doubleValue = Number.class.cast(value).doubleValue();
            if (Double.isNaN(doubleValue)) {
                return generator;
            }
            return generator.write(doubleValue);
        } else if (type == boolean.class || type == Boolean.class) {
            return generator.write(Boolean.class.cast(value));
        } else if (type == BigDecimal.class) {
            return generator.write(BigDecimal.class.cast(value));
        } else if (type == BigInteger.class) {
            return generator.write(BigInteger.class.cast(value));
        } else if (type == char.class || type == Character.class) {
            return generator.write(Character.class.cast(value).toString());
        }
        return null;
    }

    private static JsonGenerator writePrimitives(final JsonGenerator generator, final String key, final Class<?> type, final Object value) {
        if (type == String.class) {
            return generator.write(key, value.toString());
        } else if (type == long.class || type == Long.class) {
            return generator.write(key, Long.class.cast(value).longValue());
        } else if (isInt(type)) {
            return generator.write(key, Number.class.cast(value).intValue());
        } else if (isFloat(type)) {
            final double doubleValue = Number.class.cast(value).doubleValue();
            if (Double.isNaN(doubleValue)) {
                return generator;
            }
            return generator.write(key, doubleValue);
        } else if (type == boolean.class || type == Boolean.class) {
            return generator.write(key, Boolean.class.cast(value));
        } else if (type == BigDecimal.class) {
            return generator.write(key, BigDecimal.class.cast(value));
        } else if (type == BigInteger.class) {
            return generator.write(key, BigInteger.class.cast(value));
        } else if (type == char.class || type == Character.class) {
            return generator.write(key, Character.class.cast(value).toString());
        }
        return generator;
    }

    private static boolean isInt(final Class<?> type) {
        return type == int.class || type == Integer.class
                || type == byte.class || type == Byte.class
                || type == short.class || type == Short.class;
    }

    private static boolean isFloat(final Class<?> type) {
        return type == double.class || type == Double.class
                || type == float.class || type == Float.class;
    }

    /*private <T> String convertFrom(final Class<T> aClass, final T value) {
        final Converter<T> converter = (Converter<T>) findAdapter(aClass);
        return doConvertFrom(value, converter);
    }*/

    private static <T> Object doConvertFrom(final T value, final Adapter<T, Object> converter) {
        if (converter == null) {
            throw new MapperException("can't convert " + value + " to String");
        }
        return converter.from(value);
    }

    private Adapter findAdapter(final Type aClass) {
        final Adapter<?, ?> converter = adapters.get(new AdapterKey(aClass, String.class));
        if (converter != null) {
            return converter;
        }
        if (Class.class.isInstance(aClass)) {
            final Class<?> clazz = Class.class.cast(aClass);
            if (clazz.isEnum()) {
                final Adapter<?, ?> enumConverter = new ConverterAdapter(new EnumConverter(clazz));
                adapters.putIfAbsent(new AdapterKey(String.class, aClass), enumConverter);
                return enumConverter;
            }
        }
        return null;
    }

    private Object convertTo(final Type aClass, final String text) {
        if (Object.class == aClass || String.class == aClass) {
            return text;
        }
        final Adapter converter = findAdapter(aClass);
        if (converter == null) {
            adapters.putIfAbsent(new AdapterKey(String.class, aClass), FALLBACK_CONVERTER);
            return FALLBACK_CONVERTER.to(text);
        }
        return converter.to(text);
    }

    public <T> void writeArray(final Object object, final OutputStream stream) {
        writeArray(asList((T[]) object), stream);
    }

    public <T> void writeArray(final T[] object, final OutputStream stream) {
        writeArray(asList(object), stream);
    }

    public <T> void writeArray(final T[] object, final Writer stream) {
        writeArray(asList(object), stream);
    }

    public <T> void writeArray(final Collection<T> object, final OutputStream stream) {
        writeArray(object, new OutputStreamWriter(stream, encoding));
    }

    public <T> void writeArray(final Collection<T> object, final Writer stream) {
        JsonGenerator generator = generatorFactory.createGenerator(stream);
        try {
            generator = doWriteArray(object, generator);
        } finally {
            doCloseOrFlush(generator);
        }
    }

    private <T> JsonGenerator doWriteArray(final Collection<T> object, final JsonGenerator inGenerator) {
        JsonGenerator generator = inGenerator;
        if (object == null) {
            generator = generator.writeStartArray().writeEnd();
        } else {
            generator = generator.writeStartArray();
            for (final T t : object) {
                if (JsonValue.class.isInstance(t)) {
                    generator = generator.write(JsonValue.class.cast(t));
                } else {
                    generator = writeItem(generator, t);
                }
            }
            generator = generator.writeEnd();
        }
        return generator;
    }

    private void doCloseOrFlush(final JsonGenerator generator) {
        if (close) {
            generator.close();
        } else {
            generator.flush();
        }
    }

    public <T> void writeIterable(final Iterable<T> object, final OutputStream stream) {
        writeIterable(object, new OutputStreamWriter(stream, encoding));
    }

    public <T> void writeIterable(final Iterable<T> object, final Writer stream) {
        JsonGenerator generator = generatorFactory.createGenerator(stream);
        try {
            if (object == null) {
                generator = generator.writeStartArray().writeEnd();
            } else {
                generator.writeStartArray();
                for (final T t : object) {
                    generator = writeItem(generator, t);
                }
                generator.writeEnd();
            }
        } finally {
            doCloseOrFlush(generator);
        }
    }

    public void writeObject(final Object object, final Writer stream) {
        if (JsonValue.class.isInstance(object) || String.class.isInstance(object) || Number.class.isInstance(object) || object == null) {
            try {
                stream.write(String.valueOf(object));
            } catch (final IOException e) {
                throw new MapperException(e);
            } finally {
                if (close) {
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
        final JsonGenerator generator = generatorFactory.createGenerator(stream);
        doWriteHandlingNullObject(object, generator);
    }

    public void writeObject(final Object object, final OutputStream stream) {
        final JsonGenerator generator = generatorFactory.createGenerator(stream, encoding);
        doWriteHandlingNullObject(object, generator);
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

    private void doWriteHandlingNullObject(final Object object, final JsonGenerator generator) {
        if (object == null) {
            generator.writeStartObject().writeEnd().close();
            return;
        }
        if (JsonObject.class.isInstance(object)) {
            final JsonObject jsonObject = JsonObject.class.cast(object);
            generator.writeStartObject();
            for (final Map.Entry<String, JsonValue> value  : jsonObject.entrySet()) {
                generator.write(value.getKey(), value.getValue());
            }
            generator.writeEnd().close();
            return;
        }

        //JsonGenerator gen = null;
        try {
            /*gen = */
            doWriteObject(generator, object);
        } finally {
            doCloseOrFlush(generator);
        }
    }

    private JsonGenerator doWriteObject(final JsonGenerator generator, final Object object) {
        try {
            JsonGenerator gen = generator;
            if (object == null) {
                return generator;
            }

            if (Map.class.isInstance(object)) {
                gen = gen.writeStartObject();
                gen = writeMapBody((Map<?, ?>) object, gen, null);
                gen = gen.writeEnd();
                return gen;
            }

            final JsonGenerator jsonGenerator = writePrimitives(generator, object);
            if (jsonGenerator != null) {
                return jsonGenerator;
            }

            final Class<?> objectClass = object.getClass();
            if (objectClass.isEnum()) {
                final Adapter adapter = findAdapter(objectClass);
                final String adaptedValue = adapter.from(object).toString(); // we know it ends as String for enums
                return gen.write(adaptedValue);
            }

            gen = gen.writeStartObject();
            gen = doWriteObjectBody(gen, object);
            return gen.writeEnd();
        } catch (final InvocationTargetException e) {
            throw new MapperException(e);
        } catch (final IllegalAccessException e) {
            throw new MapperException(e);
        }
    }

    private JsonGenerator doWriteObjectBody(final JsonGenerator gen, final Object object) throws IllegalAccessException, InvocationTargetException {
        final Class<?> objectClass = object.getClass();
        final Mappings.ClassMapping classMapping = mappings.findOrCreateClassMapping(objectClass);
        if (classMapping == null) {
            throw new MapperException("No mapping for " + objectClass.getName());
        }

        JsonGenerator generator = gen;
        for (final Map.Entry<String, Mappings.Getter> getterEntry : classMapping.getters.entrySet()) {
            final Mappings.Getter getter = getterEntry.getValue();
            if (getter.version >= 0 && version >= getter.version) {
                continue;
            }

            final Object value = getter.reader.read(object);
            if (JsonValue.class.isInstance(value)) {
                generator = generator.write(getterEntry.getKey(), JsonValue.class.cast(value));
                continue;
            }

            if (value == null) {
                if (skipNull && !getter.reader.isNillable()) {
                    continue;
                } else {
                    gen.writeNull(getterEntry.getKey());
                    continue;
                }
            }

            final Object val = getter.converter == null ? value : getter.converter.from(value);

            generator = writeValue(generator, val.getClass(),
                    getter.primitive, getter.array,
                    getter.collection, getter.map,
                    getter.itemConverter,
                    getterEntry.getKey(),
                    val);
        }
        return generator;
    }

    private JsonGenerator writeMapBody(final Map<?, ?> object, final JsonGenerator gen, final Adapter itemConverter) throws InvocationTargetException, IllegalAccessException {
        JsonGenerator generator = gen;
        for (final Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
            final Object value = entry.getValue();
            final Object key = entry.getKey();

            if (value == null) {
                if (skipNull) {
                    continue;
                } else {
                    gen.writeNull(key == null ? "null" : key.toString());
                    continue;
                }
            }

            final Class<?> valueClass = value.getClass();
            final boolean primitive = Mappings.isPrimitive(valueClass);
            final boolean clazz = mappings.getClassMapping(valueClass) != null;
            final boolean array = clazz || primitive ? false : valueClass.isArray();
            final boolean collection = clazz || primitive || array ? false : Collection.class.isAssignableFrom(valueClass);
            final boolean map = clazz || primitive || array || collection ? false : Map.class.isAssignableFrom(valueClass);
            generator = writeValue(generator, valueClass,
                    primitive, array, collection, map, itemConverter,
                    key == null ? "null" : key.toString(), value);
        }
        return generator;
    }

    private JsonGenerator writeValue(final JsonGenerator generator, final Class<?> type,
                                     final boolean primitive, final boolean array,
                                     final boolean collection, final boolean map,
                                     final Adapter itemConverter,
                                     final String key, final Object value) throws InvocationTargetException, IllegalAccessException {
        if (array) {
            final int length = Array.getLength(value);
            if (length == 0 && skipEmptyArray) {
                return generator;
            }
            
            if(treatByteArrayAsBase64 && (type == byte[].class /*|| type == Byte[].class*/)) {
                String base64EncodedByteArray = DatatypeConverter.printBase64Binary((byte[]) value);
                generator.write(key, base64EncodedByteArray);
                return generator;
            }
            if(treatByteArrayAsBase64URL && (type == byte[].class /*|| type == Byte[].class*/)) {
                return generator.write(key, String.valueOf(Adapter.class.cast(adapters.get(new AdapterKey(byte[].class, String.class))).to(value)));
            }

            JsonGenerator gen = generator.writeStartArray(key);
            for (int i = 0; i < length; i++) {
                final Object o = Array.get(value, i);
                gen = writeItem(gen, itemConverter != null ? itemConverter.from(o) : o);
            }
            return gen.writeEnd();
        } else if (collection) {
            JsonGenerator gen = generator.writeStartArray(key);
            for (final Object o : Collection.class.cast(value)) {
                gen = writeItem(gen, itemConverter != null ? itemConverter.from(o) : o);
            }
            return gen.writeEnd();
        } else if (map) {
            JsonGenerator gen = generator.writeStartObject(key);
            gen = writeMapBody((Map<?, ?>) value, gen, itemConverter);
            return gen.writeEnd();
        } else if (primitive) {
            return writePrimitives(generator, key, type, value);
        } else {
            final Adapter converter = findAdapter(type);
            if (converter != null) {
                final Object adapted = doConvertFrom(value, converter);
                final JsonGenerator written = writePrimitives(generator, key, adapted.getClass(), adapted);
                if (written != null) {
                    return written;
                }
                return writeValue(generator, String.class, true, false, false, false, null, key, adapted);
            }
            return doWriteObjectBody(generator.writeStartObject(key), value).writeEnd();
        }
    }

    private JsonGenerator writeItem(final JsonGenerator generator, final Object o) {
        JsonGenerator newGen = writePrimitives(generator, o);
        if (newGen == null) {
            if (Collection.class.isInstance(o)) {
                newGen = doWriteArray(Collection.class.cast(o), generator);
            } else if (o != null && o.getClass().isArray()) {
                final int length = Array.getLength(o);
                if (length > 0 || !skipEmptyArray) {
                    newGen = generator.writeStartArray();
                    for (int i = 0; i < length; i++) {
                        newGen = writeItem(newGen, Array.get(o, i));
                    }
                    newGen = newGen.writeEnd();
                }
            } else if (o == null) {
                newGen = generator.writeNull();
            } else {
                newGen = doWriteObject(generator, o);
            }
        }
        return newGen;
    }

    public <T> T readObject(final String string, final Type clazz) {
        return readObject(new StringReader(string), clazz);
    }

    public <T> T readObject(final Reader stream, final Type clazz) {
        return mapObject(clazz, readerFactory.createReader(stream));
    }

    public <T> T readObject(final InputStream stream, final Type clazz) {
        return mapObject(clazz, readerFactory.createReader(stream));
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
            if (close) {
                reader.close();
            }
        }
    }

    public <T> Collection<T> readCollection(final InputStream stream, final ParameterizedType genericType) {
        final JsonReader reader = readerFactory.createReader(stream);
        final Mappings.CollectionMapping mapping = mappings.findCollectionMapping(genericType);
        if (mapping == null) {
            throw new UnsupportedOperationException("type " + genericType + " not supported");
        }
        try {
            return mapCollection(mapping, reader.readArray(), null);
        } catch (final Exception e) {
            throw new MapperException(e);
        } finally {
            if (close) {
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
        final JsonReader reader = readerFactory.createReader(stream);
        final Mappings.CollectionMapping mapping = mappings.findCollectionMapping(genericType);
        if (mapping == null) {
            throw new UnsupportedOperationException("type " + genericType + " not supported");
        }
        try {
            return mapCollection(mapping, reader.readArray(), null);
        } catch (final Exception e) {
            throw new MapperException(e);
        } finally {
            if (close) {
                reader.close();
            }
        }
    }

    public <T> T[] readArray(final Reader stream, final Class<T> clazz) {
        final JsonReader reader = readerFactory.createReader(stream);
        return (T[]) mapArray(clazz, reader);
    }

    public <T> T readTypedArray(final InputStream stream, final Class<?> elementType, final Class<T> arrayType) {
        final JsonReader reader = readerFactory.createReader(stream);
        return arrayType.cast(mapArray(elementType, reader));
    }

    public <T> T readTypedArray(final Reader stream, final Class<?> elementType, final Class<T> arrayType) {
        final JsonReader reader = readerFactory.createReader(stream);
        return arrayType.cast(mapArray(elementType, reader));
    }

    public <T> T[] readArray(final InputStream stream, final Class<T> clazz) {
        final JsonReader reader = readerFactory.createReader(stream);
        return (T[]) mapArray(clazz, reader);
    }

    private Object mapArray(final Class<?> clazz, final JsonReader reader) {
        try {
            return buildArrayWithComponentType(reader.readArray(), clazz, null);
        } catch (final Exception e) {
            throw new MapperException(e);
        } finally {
            if (close) {
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
                            if (JsonLongImpl.class.isInstance(jsonValue) && any) {
                                final JsonNumber number = JsonNumber.class.cast(jsonValue);
                                final int integer = number.intValue();
                                final long asLong = number.longValue();
                                if (integer == asLong) {
                                    map.put(value.getKey(), integer);
                                } else {
                                    map.put(value.getKey(), asLong);
                                }
                            } else if (JsonNumber.class.isInstance(jsonValue) && any) {
                                final JsonNumber number = JsonNumber.class.cast(jsonValue);
                                map.put(value.getKey(), !number.isIntegral() ? number.bigDecimalValue() : number.intValue());
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
                if (readAttributeBeforeWrite) {
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
                for (final Map.Entry<AdapterKey, Adapter<?, ?>> entry : adapters.entrySet()) {
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

        if(treatByteArrayAsBase64 && jsonValue.getValueType() == ValueType.STRING && (type == byte[].class /*|| type == Byte[].class*/)) {
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
