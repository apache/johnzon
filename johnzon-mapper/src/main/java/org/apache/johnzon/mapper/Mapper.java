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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.xml.bind.DatatypeConverter;

import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.converter.EnumConverter;
import org.apache.johnzon.mapper.reflection.JohnzonCollectionType;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;
import org.apache.johnzon.mapper.reflection.Mappings;

public class Mapper {
    private static final Converter<Object> FALLBACK_CONVERTER = new FallbackConverter();
    private static final JohnzonParameterizedType ANY_LIST = new JohnzonParameterizedType(List.class, Object.class);

    protected final Mappings mappings;
    protected final JsonReaderFactory readerFactory;
    protected final JsonGeneratorFactory generatorFactory;
    protected final boolean close;
    protected final ConcurrentMap<Type, Converter<?>> converters;
    protected final int version;
    protected final boolean skipNull;
    protected final boolean skipEmptyArray;
    protected final boolean treatByteArrayAsBase64;
    protected final Charset encoding;

    // CHECKSTYLE:OFF
    public Mapper(final JsonReaderFactory readerFactory, final JsonGeneratorFactory generatorFactory,
                  final boolean doClose, final Map<Class<?>, Converter<?>> converters,
                  final int version, final Comparator<String> attributeOrder, final boolean skipNull, final boolean skipEmptyArray,
                  final AccessMode accessMode, final boolean hiddenConstructorSupported, final boolean useConstructors,
                  final boolean treatByteArrayAsBase64,
                  final Charset encoding) {
    // CHECKSTYLE:ON
        this.readerFactory = readerFactory;
        this.generatorFactory = generatorFactory;
        this.close = doClose;
        this.converters = new ConcurrentHashMap<Type, Converter<?>>(converters);
        this.version = version;
        this.mappings = new Mappings(attributeOrder, accessMode, hiddenConstructorSupported, useConstructors, version);
        this.skipNull = skipNull;
        this.skipEmptyArray = skipEmptyArray;
        this.treatByteArrayAsBase64 = treatByteArrayAsBase64;
        this.encoding = encoding;
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
        final Converter<T> converter = (Converter<T>) findConverter(aClass);
        return doConverFrom(value, converter);
    }*/

    private static <T> String doConverFrom(final T value, final Converter<T> converter) {
        if (converter == null) {
            throw new MapperException("can't convert " + value + " to String");
        }
        return converter.toString(value);
    }

    private <T> Converter<T> findConverter(final Type aClass) {
        final Converter<T> converter = (Converter<T>) converters.get(aClass);
        if (converter != null) {
            return converter;
        }
        if (Class.class.isInstance(aClass)) {
            final Class<?> clazz = Class.class.cast(aClass);
            if (clazz.isEnum()) {
                final Converter<T> enumConverter = new EnumConverter(clazz);
                converters.putIfAbsent(clazz, enumConverter);
                return enumConverter;
            }
        }
        return null;
    }

    private Object convertTo(final Type aClass, final String text) {
        if (Object.class == aClass) {
            return text;
        }
        final Converter<?> converter = findConverter(aClass);
        if (converter == null) {
            converters.putIfAbsent(aClass, FALLBACK_CONVERTER);
            return FALLBACK_CONVERTER;
        }
        return converter.fromString(text);
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
                generator = writeItem(generator, t);
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
        final JsonGenerator generator = generatorFactory.createGenerator(stream);
        doWriteHandlingNullObject(object, generator);
    }

    public void writeObject(final Object object, final OutputStream stream) {
        final JsonGenerator generator = generatorFactory.createGenerator(stream, encoding);
        doWriteHandlingNullObject(object, generator);
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
                gen = writeMapBody((Map<?, ?>) object, gen);
                gen = gen.writeEnd();
                return gen;
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
            final Object value = getter.reader.read(object);
            if (getter.version >= 0 && version >= getter.version) {
                continue;
            }

            if (value == null) {
                if (skipNull) {
                    continue;
                } else {
                    gen.writeNull(getterEntry.getKey());
                    continue;
                }
            }

            final Object val = getter.converter == null ? value : getter.converter.toString(value);

            generator = writeValue(generator, value.getClass(),
                    getter.primitive, getter.array,
                    getter.collection, getter.map,
                    getterEntry.getKey(),
                    val);
        }
        return generator;
    }

    private JsonGenerator writeMapBody(final Map<?, ?> object, final JsonGenerator gen) throws InvocationTargetException, IllegalAccessException {
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
                    primitive, array, collection, map,
                    key == null ? "null" : key.toString(), value);
        }
        return generator;
    }

    private JsonGenerator writeValue(final JsonGenerator generator, final Class<?> type,
                                     final boolean primitive, final boolean array,
                                     final boolean collection, final boolean map,
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

            JsonGenerator gen = generator.writeStartArray(key);
            for (int i = 0; i < length; i++) {
                gen = writeItem(gen, Array.get(value, i));
            }
            return gen.writeEnd();
        } else if (collection) {
            JsonGenerator gen = generator.writeStartArray(key);
            for (final Object o : Collection.class.cast(value)) {
                gen = writeItem(gen, o);
            }
            return gen.writeEnd();
        } else if (map) {
            JsonGenerator gen = generator.writeStartObject(key);
            gen = writeMapBody((Map<?, ?>) value, gen);
            return gen.writeEnd();
        } else if (primitive) {
            return writePrimitives(generator, key, type, value);
        } else {
            final Converter<?> converter = findConverter(type);
            if (converter != null) {
                return writeValue(generator, String.class, true, false, false, false, key,
                        doConverFrom(value, (Converter<Object>) converter));
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
            return (T) buildObject(clazz, reader.readObject());
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
            return mapCollection(mapping, reader.readArray());
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
            return mapCollection(mapping, reader.readArray());
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
        return mapArray(clazz, reader);
    }

    public <T> T[] readArray(final InputStream stream, final Class<T> clazz) {
        final JsonReader reader = readerFactory.createReader(stream);
        return mapArray(clazz, reader);
    }

    private <T> T[] mapArray(final Class<T> clazz, final JsonReader reader) {
        try {
            return (T[]) buildArrayWithComponentType(reader.readArray(), clazz);
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
                    if (SortedMap.class.isAssignableFrom(raw)) {
                        map = new TreeMap();
                    } else if (ConcurrentMap.class.isAssignableFrom(raw)) {
                        map = new ConcurrentHashMap(object.size());
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

                        for (final Map.Entry<String, JsonValue> value : object.entrySet()) {
                            map.put(convertTo(keyType, value.getKey()), toObject(value.getValue(), fieldArgTypes[1]));
                        }
                        return map;
                    }
                }
            }
        }
        if (classMapping == null) {
            throw new MapperException("Can't map " + type);
        }

        if (classMapping.constructor == null) {
            throw new IllegalArgumentException(classMapping.clazz.getName() + " can't be instantiated by Johnzon, this is a write only class");
        }

        final Object t = !classMapping.constructorHasArguments ?
                classMapping.constructor.newInstance() : classMapping.constructor.newInstance(createParameters(classMapping, object));
        for (final Map.Entry<String, Mappings.Setter> setter : classMapping.setters.entrySet()) {
            final JsonValue jsonValue = object.get(setter.getKey());
            final Mappings.Setter value = setter.getValue();
            final AccessMode.Writer setterMethod = value.writer;
            final Object convertedValue = toValue(jsonValue, value.converter, value.paramType);

            if (convertedValue != null) {
                setterMethod.write(t, convertedValue);
            }
        }

        return t;
    }

    private Object toValue(final JsonValue jsonValue, final Converter<?> converter, final Type type) throws Exception {
        return converter == null ?
                toObject(jsonValue, type) : jsonValue.getValueType() == ValueType.STRING ?
                converter.fromString(JsonString.class.cast(jsonValue).getString()) :
                converter.fromString(jsonValue.toString());
    }

    private Object[] createParameters(final Mappings.ClassMapping mapping, final JsonObject object) throws Exception {
        final Object[] objects = new Object[mapping.constructorParameters.length];
        for (int i = 0; i < mapping.constructorParameters.length; i++) {
            objects[i] = toValue(object.get(mapping.constructorParameters[i]), mapping.constructorParameterConverters[i], mapping.constructorParameterTypes[i]);
        }
        return objects;
    }

    private Object toObject(final JsonValue jsonValue, final Type type) throws Exception {
        if (jsonValue == null || jsonValue == JsonValue.NULL) {
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
            return buildObject(type, JsonObject.class.cast(jsonValue));
        } else if (JsonArray.class.isInstance(jsonValue)) {
            return buildArray(type, JsonArray.class.cast(jsonValue));
        } else if (JsonNumber.class.isInstance(jsonValue)) {

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
        } else if (JsonString.class.isInstance(jsonValue) || Object.class == type) {
            return convertTo(Class.class.cast(type), JsonString.class.cast(jsonValue).getString());
        }

        throw new MapperException("Unable to parse " + jsonValue + " to " + type);
    }

    private Object buildArray(final Type type, final JsonArray jsonArray) throws Exception {
        if (Class.class.isInstance(type)) {
            final Class clazz = Class.class.cast(type);
            if (clazz.isArray()) {
                final Class<?> componentType = clazz.getComponentType();
                return buildArrayWithComponentType(jsonArray, componentType);
            }
        }

        if (ParameterizedType.class.isInstance(type)) {
            final Mappings.CollectionMapping mapping = mappings.findCollectionMapping(ParameterizedType.class.cast(type));
            if (mapping != null) {
                return mapCollection(mapping, jsonArray);
            }
        }

        if (Object.class == type) {
            return buildArray(ANY_LIST, jsonArray);
        }

        throw new UnsupportedOperationException("type " + type + " not supported");
    }

    private <T> Collection<T> mapCollection(final Mappings.CollectionMapping mapping, final JsonArray jsonArray) throws Exception {
        final Collection collection;

        if (SortedSet.class == mapping.raw) {
            collection = new TreeSet<T>();
        } else if (Set.class == mapping.raw) {
            collection = new HashSet<T>(jsonArray.size());
        } else if (Queue.class == mapping.raw) {
            collection = new ArrayBlockingQueue<T>(jsonArray.size());
        } else if (List.class == mapping.raw || Collection.class == mapping.raw) {
            collection = new ArrayList<T>(jsonArray.size());
        } else {
            throw new IllegalStateException("not supported collection type: " + mapping.raw.getName());
        }

        for (final JsonValue value : jsonArray) {
            final Object element = toObject(value, mapping.arg);
            collection.add(element);
        }
        return collection;
    }

    private Object buildArrayWithComponentType(final JsonArray jsonArray, final Class<?> componentType) throws Exception {
        final Object array = Array.newInstance(componentType, jsonArray.size());
        int i = 0;
        for (final JsonValue value : jsonArray) {
            Array.set(array, i++, toObject(value, componentType));
        }
        return array;
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
