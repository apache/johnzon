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

import org.apache.johnzon.core.JsonReaderImpl;
import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.converter.CharacterConverter;
import org.apache.johnzon.mapper.converter.EnumConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;
import org.apache.johnzon.mapper.internal.JsonPointerTracker;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.xml.bind.DatatypeConverter;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import static javax.json.JsonValue.ValueType.FALSE;
import static javax.json.JsonValue.ValueType.NULL;
import static javax.json.JsonValue.ValueType.NUMBER;
import static javax.json.JsonValue.ValueType.TRUE;

/**
 * This class is not concurrently usable as it contains state.
 */
public class MappingParserImpl implements MappingParser {

    private static final JohnzonParameterizedType ANY_LIST = new JohnzonParameterizedType(List.class, Object.class);
    private static final CharacterConverter CHARACTER_CONVERTER = new CharacterConverter(); // this one is particular, share the logic

    protected final ConcurrentMap<Adapter<?, ?>, AdapterKey> reverseAdaptersRegistry;
    protected final ConcurrentMap<Class<?>, Method> valueOfs = new ConcurrentHashMap<Class<?>, Method>();

    private final MapperConfig config;
    private final Mappings mappings;
    private final boolean isDeduplicateObjects;

    private final JsonReader jsonReader;

    /**
     * Used for de-referencing JsonPointers during deserialisation.
     * key: JsonPointer
     * value: already deserialised Object
     */
    private Map<String, Object> jsonPointers;


    public MappingParserImpl(MapperConfig config, Mappings mappings, JsonReader jsonReader, boolean isDeduplicateObjects) {
        this.config = config;
        this.mappings = mappings;

        this.jsonReader = jsonReader;

        reverseAdaptersRegistry = new ConcurrentHashMap<Adapter<?, ?>, AdapterKey>(config.getAdapters().size());


        this.isDeduplicateObjects = isDeduplicateObjects;

        if (isDeduplicateObjects) {
            jsonPointers = new HashMap<String, Object>();
        } else {
            jsonPointers = Collections.emptyMap();
        }
    }


    @Override
    public <T> T readObject(Type targetType) {

        try {
            if (jsonReader.getClass().getName().equals("org.apache.johnzon.core.JsonReaderImpl")) {
                // later in JSON-P 1.1 we can remove this hack again
                return readObject(((JsonReaderImpl) jsonReader).readValue(), targetType);
            }

            return readObject(jsonReader.read(), targetType);
        } finally {
            if (config.isClose()) {
                jsonReader.close();
            }
        }
    }

    @Override
    public <T> T readObject(JsonValue jsonValue, Type targetType) {
        return readObject(jsonValue, targetType, targetType instanceof Class || targetType instanceof ParameterizedType);
    }

    private <T> T readObject(JsonValue jsonValue, Type targetType, boolean applyObjectConverter) {
        if (JsonStructure.class == targetType || JsonObject.class == targetType || JsonValue.class == targetType) {
            return (T) jsonValue;
        }
        if (JsonObject.class.isInstance(jsonValue)) {
            return (T) buildObject(targetType, JsonObject.class.cast(jsonValue), applyObjectConverter, isDeduplicateObjects ? new JsonPointerTracker(null, "/") : null);
        }
        if (JsonString.class.isInstance(jsonValue) && (targetType == String.class || targetType == Object.class)) {
            return (T) JsonString.class.cast(jsonValue).getString();
        }
        if (JsonNumber.class.isInstance(jsonValue)) {
            final JsonNumber number = JsonNumber.class.cast(jsonValue);
            if (targetType == int.class || targetType == Integer.class) {
                return (T) Integer.valueOf(number.intValue());
            }
            if (targetType == long.class || targetType == Long.class) {
                return (T) Long.valueOf(number.longValue());
            }
            if (targetType == double.class || targetType == Double.class || targetType == Object.class) {
                return (T) Double.valueOf(number.doubleValue());
            }
            if (targetType == BigDecimal.class) {
                return (T) number.bigDecimalValue();
            }
            if (targetType == BigInteger.class) {
                return (T) number.bigIntegerValue();
            }
        }
        if (JsonArray.class.isInstance(jsonValue)) {

            JsonArray jsonArray = (JsonArray) jsonValue;

            if (Class.class.isInstance(targetType) && ((Class) targetType).isArray()) {
                final Class componentType = ((Class) targetType).getComponentType();
                return (T) buildArrayWithComponentType(jsonArray, componentType, config.findAdapter(componentType),
                        isDeduplicateObjects ? new JsonPointerTracker(null, "/") : null, Object.class);
            }
            if (ParameterizedType.class.isInstance(targetType)) {

                final ParameterizedType pt = (ParameterizedType) targetType;
                final Mappings.CollectionMapping mapping = mappings.findCollectionMapping(pt, Object.class);
                if (mapping == null) {
                    throw new UnsupportedOperationException("type " + targetType + " not supported");
                }

                final Type arg = pt.getActualTypeArguments()[0];
                return (T) mapCollection(mapping, jsonArray, Class.class.isInstance(arg) ? config.findAdapter(Class.class.cast(arg)) : null,
                        null, isDeduplicateObjects ? new JsonPointerTracker(null, "/") : null, Object.class);
            }
            if (Object.class == targetType) {
                return (T) new ArrayList(asList(Object[].class.cast(buildArrayWithComponentType(jsonArray, Object.class, null,
                        isDeduplicateObjects ? new JsonPointerTracker(null, "/") : null, Object.class))));
            }
        }
        if (JsonValue.NULL.equals(jsonValue)) {
            return null;
        }
        if (jsonValue.equals(JsonValue.TRUE) && (Boolean.class == targetType || boolean.class == targetType || Object.class == targetType)) {
            return (T) Boolean.TRUE;
        }
        if (jsonValue.equals(JsonValue.FALSE) && (Boolean.class == targetType || boolean.class == targetType || Object.class == targetType)) {
            return (T) Boolean.FALSE;
        }
        throw new IllegalArgumentException("Unsupported " + jsonValue + " for type " + targetType);
    }


    private Object buildObject(final Type inType, final JsonObject object, final boolean applyObjectConverter, JsonPointerTracker jsonPointer) {
        Type type = inType;
        if (inType == Object.class) {
            type = new JohnzonParameterizedType(Map.class, String.class, Object.class);
        }

        if (applyObjectConverter && !(type instanceof ParameterizedType)) {

            if (!(type instanceof Class)) {
                throw new MapperException("ObjectConverters are only supported for Classes not Types");
            }

            ObjectConverter.Reader objectConverter = config.findObjectConverterReader((Class) type);
            if (objectConverter != null) {
                return objectConverter.fromJson(object, type, new SuppressConversionMappingParser(this, object));
            }
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
                        map = config.getAttributeOrder() == null ? new TreeMap() : new TreeMap(config.getAttributeOrder());
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
                                map.put(value.getKey(), toNumberValue(JsonNumber.class.cast(jsonValue)));
                            } else if (JsonString.class.isInstance(jsonValue) && any) {
                                map.put(value.getKey(), JsonString.class.cast(jsonValue).getString());
                            } else {
                                map.put(convertTo(keyType, value.getKey()), toObject(null, jsonValue, fieldArgTypes[1], null, jsonPointer, Object.class));
                            }
                        }
                        return map;
                    }
                }
            } else if (Map.class == type || HashMap.class == type || LinkedHashMap.class == type) {
                final LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
                for (final Map.Entry<String, JsonValue> value : object.entrySet()) {
                    map.put(value.getKey(), toObject(null, value.getValue(), Object.class, null, jsonPointer, Object.class));
                }
                return map;
            }
        }
        if (classMapping == null) {
            throw new MapperException("Can't map " + type);
        }

        if (applyObjectConverter && classMapping.reader != null) {
            return classMapping.reader.fromJson(object, type, new SuppressConversionMappingParser(this, object));
        }
        /* doesn't work yet
        if (classMapping.adapter != null) {
            return classMapping.adapter.from(t);
        }
        */

        if (classMapping.factory == null) {
            throw new MapperException(classMapping.clazz + " not instantiable");
        }

        if (config.isFailOnUnknown()) {
            if (!classMapping.setters.keySet().containsAll(object.keySet())) {
                throw new MapperException("(fail on unknown properties): " + new HashSet<String>(object.keySet()) {{
                    removeAll(classMapping.setters.keySet());
                }});
            }
        }

        Object t;
        if (classMapping.factory.getParameterTypes().length == 0) {
            t = classMapping.factory.create(null);
        } else {
            t = classMapping.factory.create(createParameters(classMapping, object, jsonPointer));
        }
        // store the new object under it's jsonPointer in case it gets referenced later
        if (isDeduplicateObjects) {
            jsonPointers.put(jsonPointer.toString(), t);
        }

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
            if (JsonValue.NULL.equals(jsonValue)) { // forced
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
                final Object convertedValue = toValue(existingInstance, jsonValue, value.converter, value.itemConverter, value.paramType, value.objectConverter,
                        new JsonPointerTracker(jsonPointer, setter.getKey()), inType);
                if (convertedValue != null) {
                    setterMethod.write(t, convertedValue);
                }
            }
        }
        if (classMapping.anySetter != null) {
            for (final Map.Entry<String, JsonValue> entry : object.entrySet()) {
                final String key = entry.getKey();
                if (!classMapping.setters.containsKey(key)) {
                    try {
                        classMapping.anySetter.invoke(t, key, toValue(null, entry.getValue(), null, null, Object.class, null,
                                isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, entry.getKey()) : null, type));
                    } catch (final IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    } catch (final InvocationTargetException e) {
                        throw new MapperException(e.getCause());
                    }
                }
            }
        }

        return t;
    }

    private Number toNumberValue(JsonNumber jsonNumber) {
        if (jsonNumber.isIntegral()) {
            final int intValue = jsonNumber.intValue();
            final long longValue = jsonNumber.longValue();
            if (intValue == longValue) {
                return intValue;
            } else {
                return longValue;
            }
        } else {
            if (config.isUseBigDecimalForFloats()) {
                return jsonNumber.bigDecimalValue();
            } else {
                return jsonNumber.doubleValue();
            }
        }
    }

    private Object convertTo(final Adapter converter, final JsonValue jsonValue, JsonPointerTracker jsonPointer) {
        if (jsonValue.getValueType() == JsonValue.ValueType.OBJECT) {

            //X TODO maybe we can put this into MapperConfig?
            //X      config.getAdapter(AdapterKey)
            //X      config.getAdapterKey(Adapter)
            final AdapterKey adapterKey = getAdapterKey(converter);

            final Object param;
            try {
                Type to = adapterKey.getTo();
                param = buildObject(to, JsonObject.class.cast(jsonValue), to instanceof Class, jsonPointer);
            } catch (final Exception e) {
                throw new MapperException(e);
            }
            return converter.to(param);
        }

        final AdapterKey key = getAdapterKey(converter);
        final JsonValue.ValueType valueType = jsonValue.getValueType();
        if (NULL.equals(valueType)) {
            return null;
        }
        if (TRUE.equals(valueType) || FALSE.equals(valueType)) {
            if (key != null) {
                if (boolean.class == key.getTo() || Boolean.class == key.getTo()) {
                    return converter.to(Boolean.parseBoolean(jsonValue.toString()));
                }
            }
        }
        if (NUMBER.equals(valueType)) {
            if (key != null) {
                if (Long.class == key.getTo() || long.class == key.getTo()) {
                    return converter.to(JsonNumber.class.cast(jsonValue).longValue());
                } else if (Integer.class == key.getTo() || int.class == key.getTo()) {
                    return converter.to(JsonNumber.class.cast(jsonValue).intValue());
                } else if (Double.class == key.getTo() || double.class == key.getTo()) {
                    return converter.to(JsonNumber.class.cast(jsonValue).doubleValue());
                } else if (Float.class == key.getTo() || float.class == key.getTo()) {
                    return converter.to(JsonNumber.class.cast(jsonValue).doubleValue());
                } else if (BigInteger.class == key.getTo()) {
                    return converter.to(JsonNumber.class.cast(jsonValue).bigIntegerValue());
                } else if (BigDecimal.class == key.getTo()) {
                    return converter.to(JsonNumber.class.cast(jsonValue).bigDecimalValue());
                }
            }
        }
        return converter.to(jsonValue.toString());

    }

    private AdapterKey getAdapterKey(final Adapter converter) {
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
        if (adapterKey == null) {
            final Type[] types = converter.getClass().getGenericInterfaces();
            for (final Type t : types) {
                if (!ParameterizedType.class.isInstance(t)) {
                    continue;
                }
                final ParameterizedType pt = ParameterizedType.class.cast(t);
                if (Adapter.class == pt.getRawType()) {
                    final Type[] actualTypeArguments = pt.getActualTypeArguments();
                    adapterKey = new AdapterKey(actualTypeArguments[0], actualTypeArguments[1]);
                    reverseAdaptersRegistry.putIfAbsent(converter, adapterKey);
                    break;
                }
            }
        }
        return adapterKey;
    }


    private Object toObject(final Object baseInstance, final JsonValue jsonValue,
                            final Type type, final Adapter itemConverter, final JsonPointerTracker jsonPointer,
                            final Type rootType) {
        if (jsonValue == null || JsonValue.NULL.equals(jsonValue)) {
            return null;
        }

        if (type == Boolean.class || type == boolean.class) {
            if (jsonValue.equals(JsonValue.TRUE)) {
                return true;
            }
            if (jsonValue.equals(JsonValue.FALSE)) {
                return false;
            }
            throw new MapperException("Unable to parse " + jsonValue + " to boolean");
        }

        if (config.isTreatByteArrayAsBase64() && jsonValue.getValueType() == JsonValue.ValueType.STRING && (type == byte[].class /*|| type == Byte[].class*/)) {
            return DatatypeConverter.parseBase64Binary(((JsonString)jsonValue).getString());
        }
        if (config.isTreatByteArrayAsBase64URL() && jsonValue.getValueType() == JsonValue.ValueType.STRING && (type == byte[].class /*|| type == Byte[].class*/)) {
            return DatatypeConverter.parseBase64Binary(((JsonString)jsonValue).getString());
        }

        if (Object.class == type) { // handling specific types here to keep exception in standard handling
            if (jsonValue.equals(JsonValue.TRUE)) {
                return true;
            }
            if (jsonValue.equals(JsonValue.FALSE)) {
                return false;
            }
            if (JsonNumber.class.isInstance(jsonValue)) {
                return toNumberValue(JsonNumber.class.cast(jsonValue));
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
                    JsonObject.class.cast(jsonValue), type instanceof Class,
                    jsonPointer);
            return typedAdapter ? itemConverter.to(object) : object;
        } else if (JsonArray.class.isInstance(jsonValue)) {
            if (JsonArray.class == type || JsonStructure.class == type) {
                return jsonValue;
            }
            return buildArray(type, JsonArray.class.cast(jsonValue), itemConverter, null, jsonPointer, rootType);
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
                // check whether we have a jsonPointer to a previously deserialised object
                if (!String.class.equals(type)) {
                    Object o = jsonPointers.get(string);
                    if (o != null) {
                        return o;
                    }
                }
                return convertTo(Class.class.cast(type), string);
            } else {
                return itemConverter.to(string);
            }
        }

        throw new MapperException("Unable to parse " + jsonValue + " to " + type);
    }

    private Object buildArray(final Type type, final JsonArray jsonArray, final Adapter itemConverter,
                              final ObjectConverter.Reader objectConverter,
                              final JsonPointerTracker jsonPointer, final Type rootType) {
        if (Class.class.isInstance(type)) {
            final Class clazz = Class.class.cast(type);
            if (clazz.isArray()) {
                final Class<?> componentType = clazz.getComponentType();
                return buildArrayWithComponentType(jsonArray, componentType, itemConverter, jsonPointer, rootType);
            }
        }

        if (ParameterizedType.class.isInstance(type)) {
            final Mappings.CollectionMapping mapping = mappings.findCollectionMapping(ParameterizedType.class.cast(type), rootType);
            if (mapping != null) {
                return mapCollection(mapping, jsonArray, itemConverter, objectConverter, jsonPointer, rootType);
            }
        }

        if (Object.class == type) {
            return buildArray(ANY_LIST, jsonArray, null, null, jsonPointer, rootType);
        }

        throw new UnsupportedOperationException("type " + type + " not supported");
    }

    private Object buildArrayWithComponentType(final JsonArray jsonArray, final Class<?> componentType, final Adapter itemConverter,
                                               final JsonPointerTracker jsonPointer, final Type rootType) {
        final Object array = Array.newInstance(componentType, jsonArray.size());
        int i = 0;
        for (final JsonValue value : jsonArray) {
            Array.set(array, i, toObject(null, value, componentType, itemConverter,
                    isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType));
            i++;
        }
        return array;
    }

    private <T> Collection<T> mapCollection(final Mappings.CollectionMapping mapping, final JsonArray jsonArray,
                                            final Adapter itemConverter, ObjectConverter.Reader objectConverter,
                                            final JsonPointerTracker jsonPointer, final Type rootType) {
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

        int i = 0;
        for (final JsonValue value : jsonArray) {
            collection.add(JsonValue.NULL.equals(value)
                    ? null
                    : toValue(null, value, null, itemConverter, mapping.arg, objectConverter,
                    isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType));
            i++;
        }

        if (EnumSet.class == mapping.raw) {
            if (collection.isEmpty()) {
                return EnumSet.noneOf(Class.class.cast(mapping.arg));
            } else if (collection.size() == 1) {
                return Collection.class.cast(EnumSet.of(Enum.class.cast(collection.iterator().next())));
            } else {
                final List<Enum> list = List.class.cast(collection);
                return Collection.class.cast(EnumSet.of(list.get(0), list.subList(1, list.size()).toArray(new Enum[list.size() - 1])));
            }
        }

        return collection;
    }


    private Object[] createParameters(final Mappings.ClassMapping mapping, final JsonObject object, JsonPointerTracker jsonPointer) {
        final int length = mapping.factory.getParameterTypes().length;
        final Object[] objects = new Object[length];

        for (int i = 0; i < length; i++) {

            String paramName = mapping.factory.getParameterNames()[i];
            objects[i] = toValue(null,
                    object.get(paramName),
                    mapping.factory.getParameterConverter()[i],
                    mapping.factory.getParameterItemConverter()[i],
                    mapping.factory.getParameterTypes()[i],
                    mapping.factory.getObjectConverter()[i],
                    isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, paramName) : null,
                    mapping.clazz); //X TODO ObjectConverter in @JohnzonConverter with Constructors!
        }

        return objects;
    }

    private Object toValue(final Object baseInstance, final JsonValue jsonValue, final Adapter converter,
                           final Adapter itemConverter, final Type type, final ObjectConverter.Reader objectConverter,
                           final JsonPointerTracker jsonPointer, final Type rootType) {

        if (objectConverter != null) {

            if (jsonValue instanceof JsonObject) {
                return objectConverter.fromJson((JsonObject) jsonValue, type, this);
            } else if (jsonValue instanceof JsonArray) {
                return buildArray(type, (JsonArray) jsonValue, itemConverter, objectConverter, jsonPointer, rootType);
            } else {
                throw new UnsupportedOperationException("Array handling with ObjectConverter currently not implemented");
            }
        }

        try {
            return converter == null ? toObject(baseInstance, jsonValue, type, itemConverter, jsonPointer, rootType)
                    : jsonValue.getValueType() == JsonValue.ValueType.STRING ? converter.to(JsonString.class.cast(jsonValue).getString())
                    : convertTo(converter, jsonValue, jsonPointer);
        } catch (Exception e) {
            if (e instanceof MapperException) {
                throw (MapperException) e;
            }
            throw new MapperException(e);
        }
    }


    /**
     * @deprecated see MapperConfig
     */
    private Object convertTo(final Type aClass, final String text) {
        if (Object.class == aClass || String.class == aClass) {
            return text;
        }
        final Adapter converter = findAdapter(aClass);
        Method method = valueOfs.get(aClass);
        if (method == null && Class.class.isInstance(aClass)) { // handle primitives
            final Class cast = Class.class.cast(aClass);
            try {
                method = cast.getMethod("valueOf", String.class);
                if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
                    valueOfs.putIfAbsent(cast, method);
                } else {
                    method = null;
                }
            } catch (final NoSuchMethodException e) {
                // if a real primitive (very unlikely) try the wrapper
                if (char.class == aClass) {
                    return CHARACTER_CONVERTER.fromString(text);
                }
                try {
                    return convertTo(Class.class.cast(cast.getField("TYPE").get(null)), text);
                } catch (final Exception e1) {
                    // no-op
                }
                // no-op
            }
        }
        if (method != null) {
            try {
                return method.invoke(null, text);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new MapperException(e.getCause());
            }
        }
        if (converter == null) {
            throw new MapperException("Missing a Converter for type " + aClass + " to convert the JSON String '" +
                    text + "' . Please register a custom converter for it.");
        }
        return converter.to(text);
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
     * Internal class to suppress {@link ObjectConverter} lookup if and only if
     * the {@link JsonValue} is the same refernece than the lookup was done before.
     */
    private static class SuppressConversionMappingParser implements MappingParser {
        private final MappingParserImpl delegate;
        private final JsonObject suppressConversionFor;

        public SuppressConversionMappingParser(MappingParserImpl delegate, JsonObject suppressConversionFor) {
            this.delegate = delegate;
            this.suppressConversionFor = suppressConversionFor;
        }

        @Override
        public <T> T readObject(Type targetType) {
            return delegate.readObject(targetType);
        }

        @Override
        public <T> T readObject(JsonValue jsonValue, Type targetType) {
            if (suppressConversionFor == jsonValue) {
                return delegate.readObject(jsonValue, targetType, false);
            }
            return delegate.readObject(jsonValue, targetType);
        }
    }

}
