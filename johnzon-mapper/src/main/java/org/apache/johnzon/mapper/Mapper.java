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

import org.apache.johnzon.mapper.converter.EnumConverter;
import org.apache.johnzon.mapper.reflection.JohnzonCollectionType;
import org.apache.johnzon.mapper.reflection.Mappings;

import javax.json.Json;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
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

import static java.util.Arrays.asList;

public class Mapper {
    protected static final JsonObject EMPTY_OBJECT = Json.createObjectBuilder().build();
    private static final Converter<Object> FALLBACK_CONVERTER = new FallbackConverter();

    protected final Mappings mappings;
    protected final JsonReaderFactory readerFactory;
    protected final JsonGeneratorFactory generatorFactory;
    protected final boolean close;
    protected final ConcurrentMap<Type, Converter<?>> converters;
    protected final int version;
    protected boolean skipNull;
    protected boolean skipEmptyArray;

    public Mapper(final JsonReaderFactory readerFactory, final JsonGeneratorFactory generatorFactory,
                  final boolean doClose, final Map<Class<?>, Converter<?>> converters,
                  final int version, final Comparator<String> attributeOrder, boolean skipNull, boolean skipEmptyArray) {
        this.readerFactory = readerFactory;
        this.generatorFactory = generatorFactory;
        this.close = doClose;
        this.converters = new ConcurrentHashMap<Type, Converter<?>>(converters);
        this.version = version;
        this.mappings = new Mappings(attributeOrder);
        this.skipNull =  skipNull;
        this.skipEmptyArray = skipEmptyArray;
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
            return generator.write(Integer.class.cast(value).intValue());
        } else if (isFloat(type)) {
            return generator.write(Number.class.cast(value).doubleValue());
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
            return generator.write(key, Number.class.cast(value).doubleValue());
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
        writeArray(object, new OutputStreamWriter(stream));
    }

    public <T> void writeArray(final Collection<T> object, final Writer stream) {
        JsonGenerator generator = generatorFactory.createGenerator(stream);
        try {
            if (object == null) {
                generator = generator.writeStartArray().writeEnd();
            } else {
                generator = generator.writeStartArray();
                for (final T t : object) {
                    generator = writeItem(generator, t);
                }
                generator = generator.writeEnd();
            }
        } finally {
            doCloseOrFlush(generator);
        }
    }

    private void doCloseOrFlush(JsonGenerator generator) {
        if (close) {
            generator.close();
        } else {
            generator.flush();
        }
    }

    public <T> void writeIterable(final Iterable<T> object, final OutputStream stream) {
        writeIterable(object, new OutputStreamWriter(stream));
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
        final JsonGenerator generator = generatorFactory.createGenerator(stream);
        doWriteHandlingNullObject(object, generator);
    }

    private void doWriteHandlingNullObject(final Object object, final JsonGenerator generator) {
        if (object == null) {
            generator.writeStartObject().writeEnd().close();
            return;
        }

        //JsonGenerator gen = null;
        try {
            /*gen = */doWriteObject(generator, object);
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
            final Object value = getter.method.invoke(object);
            if (getter.version >= 0 && version >= getter.version) {
                continue;
            }

            if (value == null) {
                if(skipNull) {
                    continue;
                } else {
                    gen.writeNull(getterEntry.getKey());
                    continue;
                }
            }

            generator = writeValue(generator, value.getClass(),
                                    getter.primitive, getter.array,
                                    getter.collection, getter.map,
                                    getterEntry.getKey(),
                                    getter.converter == null ? value : getter.converter.toString(value));
        }
        return generator;
    }

    private JsonGenerator writeMapBody(final Map<?, ?> object, final JsonGenerator gen) throws InvocationTargetException, IllegalAccessException {
        JsonGenerator generator = gen;
        for (final Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
            final Object value = entry.getValue();
            final Object key = entry.getKey();
            
            if (value == null) {
                if(skipNull) {
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
            if(length == 0 && skipEmptyArray) {
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
        final JsonGenerator newGen = writePrimitives(generator, o);
        if (newGen == null) {
            return doWriteObject(generator, o);
        }
        return newGen;
    }

    public <T> T readObject(final Reader stream, final Type clazz) {
        final JsonReader reader = readerFactory.createReader(stream);
        return mapObject(clazz, reader);
    }

    public <T> T readObject(final InputStream stream, final Type clazz) {
        final JsonReader reader = readerFactory.createReader(stream);
        return mapObject(clazz, reader);
    }

    private <T> T mapObject(final Type clazz, final JsonReader reader) {
        try {
            return (T) buildObject(clazz, reader.readObject());
        } catch (final InstantiationException e) {
            throw new MapperException(e);
        } catch (final IllegalAccessException e) {
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
        } catch (final InstantiationException e) {
            throw new MapperException(e);
        } catch (final IllegalAccessException e) {
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
        } catch (final InstantiationException e) {
            throw new MapperException(e);
        } catch (final IllegalAccessException e) {
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
        } catch (final InstantiationException e) {
            throw new MapperException(e);
        } catch (final IllegalAccessException e) {
            throw new MapperException(e);
        } finally {
            if (close) {
                reader.close();
            }
        }
    }

    private Object buildObject(final Type type, final JsonObject object) throws InstantiationException, IllegalAccessException {
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
                } else {
                    throw new MapperException("Can't map " + type + ", not a map and no Mapping found");
                }
            } else {
                throw new MapperException("Can't map " + type);
            }
        }

        final Object t = classMapping.clazz.newInstance();
        for (final Map.Entry<String, Mappings.Setter> setter : classMapping.setters.entrySet()) {
            final JsonValue jsonValue = object.get(setter.getKey());
            final Mappings.Setter value = setter.getValue();
            final Method setterMethod = value.method;
            final Object convertedValue = value.converter == null?
                    toObject(jsonValue, value.paramType) : jsonValue.getValueType() == ValueType.STRING ?
                                                            value.converter.fromString(JsonString.class.cast(jsonValue).getString()):
                                                                value.converter.fromString(jsonValue.toString());
                
            if (convertedValue != null) {
                try {
                    setterMethod.invoke(t, convertedValue);
                } catch (final InvocationTargetException e) {
                    throw new MapperException(e.getCause());
                }
            }
        }

        return t;
    }

    private Object toObject(final JsonValue jsonValue, final Type type) throws InstantiationException, IllegalAccessException {
      
        if(jsonValue == null || jsonValue == JsonValue.NULL) {           
            return null;
        }
        
        if (type == Boolean.class || type == boolean.class) {
            
            //if this would be commented out than the json string value "true" would pe parsed to a bool literal
            //but this is according to json spec invalid
            /*if (JsonString.class.isInstance(jsonValue)) {
                return Boolean.valueOf(JsonString.class.cast(jsonValue).getString());
            }*/
            
            if(jsonValue == JsonValue.FALSE) {
                return Boolean.FALSE;
            }
            
            if(jsonValue == JsonValue.TRUE) {
                return Boolean.TRUE;
            }
            
            throw new MapperException("Unable to parse "+jsonValue+" to boolean");
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
                
                if (type == Long.class || type == long.class) {
                    return number.longValue();
                }
                
                if (type == Integer.class || type == int.class) {
                    return number.intValue();
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
            return convertTo(Class.class.cast(type), (JsonString.class.cast(jsonValue).getString()));
        }

        
        throw new MapperException("Unable to parse "+jsonValue+" to "+type);
    }

    private Object buildArray(final Type type, final JsonArray jsonArray) throws IllegalAccessException, InstantiationException {
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

        throw new UnsupportedOperationException("type " + type + " not supported");
    }

    private <T> Collection<T> mapCollection(final Mappings.CollectionMapping mapping, final JsonArray jsonArray) throws InstantiationException, IllegalAccessException {
        final Collection collection;
        
        if (SortedSet.class == mapping.raw) {
            collection = new TreeSet<T>();
        } else if (Set.class == mapping.raw) {
            collection = new HashSet<T>(jsonArray.size());
        } else if (Queue.class == mapping.raw) {
            collection = new ArrayBlockingQueue<T>(jsonArray.size());
          //fail fast if collection is not know, assume Collection.class to be compatible with ArrayList is wrong for almost all cases
        } else if (List.class == mapping.raw /*|| Collection.class == mapping.raw*/) {
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

    private Object buildArrayWithComponentType(final JsonArray jsonArray, final Class<?> componentType) throws InstantiationException, IllegalAccessException {
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
