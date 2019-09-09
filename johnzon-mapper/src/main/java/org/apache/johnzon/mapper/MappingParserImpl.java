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
import org.apache.johnzon.mapper.converter.CharacterConverter;
import org.apache.johnzon.mapper.converter.EnumConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;
import org.apache.johnzon.mapper.internal.JsonPointerTracker;
import org.apache.johnzon.mapper.number.Validator;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static javax.json.JsonValue.ValueType.ARRAY;
import static javax.json.JsonValue.ValueType.FALSE;
import static javax.json.JsonValue.ValueType.NULL;
import static javax.json.JsonValue.ValueType.NUMBER;
import static javax.json.JsonValue.ValueType.STRING;
import static javax.json.JsonValue.ValueType.TRUE;

/**
 * This class is not concurrently usable as it contains state.
 */
public class MappingParserImpl implements MappingParser {

    private static final JohnzonParameterizedType ANY_LIST = new JohnzonParameterizedType(List.class, Object.class);
    private static final CharacterConverter CHARACTER_CONVERTER = new CharacterConverter(); // this one is particular, share the logic

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

        this.isDeduplicateObjects = isDeduplicateObjects;

        if (isDeduplicateObjects) {
            jsonPointers = new HashMap<>();
        } else {
            jsonPointers = Collections.emptyMap();
        }
    }


    @Override
    public <T> T readObject(Type targetType) {
        try {
            return readObject(jsonReader.readValue(), targetType);
        } catch (final NoSuchMethodError noSuchMethodError) { // jsonp 1.0 fallback - mainly for tests
            return readObject(jsonReader.read(), targetType);
        }
    }

    @Override
    public <T> T readObject(JsonValue jsonValue, Type targetType) {
        return readObject(jsonValue, targetType, targetType instanceof Class || targetType instanceof ParameterizedType);
    }

    private <T> T readObject(JsonValue jsonValue, Type targetType, boolean applyObjectConverter) {
        final JsonValue.ValueType valueType = jsonValue != null ? jsonValue.getValueType() : null;

        if (JsonStructure.class == targetType || JsonObject.class == targetType || JsonValue.class == targetType) {
            return (T) jsonValue;
        }
        if (JsonObject.class.isInstance(jsonValue)) {
            return (T) buildObject(targetType, JsonObject.class.cast(jsonValue), applyObjectConverter, isDeduplicateObjects ? new JsonPointerTracker(null, "/") : null);
        }
        if (JsonString.class.isInstance(jsonValue)) {
            if ((targetType == String.class || targetType == Object.class)) {
                return (T) JsonString.class.cast(jsonValue).getString();
            }
            if (targetType == Character.class || targetType == char.class) {
                final CharSequence string = JsonString.class.cast(jsonValue).getChars();
                if (string.length() == 1) {
                    return (T) Character.valueOf(string.charAt(0));
                }
                throw new IllegalArgumentException("Invalid Character binding"); // don't log the value (pwd case)
            }

            final Mappings.ClassMapping classMapping = mappings.getClassMapping(targetType);
            if (classMapping != null && classMapping.adapter != null) {
                return (T) classMapping.adapter.to(JsonString.class.cast(jsonValue).getString());
            }
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
            if (targetType == float.class || targetType == Float.class) {
                return (T) Float.valueOf((float) number.doubleValue());
            }
            if (targetType == byte.class || targetType == Byte.class) {
                final int intValue = number.intValue();
                Validator.validateByte(intValue);
                return (T) Byte.valueOf((byte) intValue);
            }
            if (targetType == short.class || targetType == Short.class) {
                return (T) Short.valueOf((short) number.intValue());
            }
            if (targetType == BigDecimal.class || Number.class == targetType) {
                return (T) number.bigDecimalValue();
            }
            if (targetType == BigInteger.class) {
                return (T) number.bigIntegerValue();
            }
        }
        if (JsonArray.class.isInstance(jsonValue)) {

            JsonArray jsonArray = (JsonArray) jsonValue;

            if (Class.class.isInstance(targetType)) {
                final Class<?> asClass = (Class) targetType;
                if (asClass.isArray()) {
                    final Class componentType = asClass.getComponentType();
                    return (T) buildArrayWithComponentType(jsonArray, componentType, config.findAdapter(componentType),
                            isDeduplicateObjects ? new JsonPointerTracker(null, "/") : null, Object.class);
                }
                if (Collection.class.isAssignableFrom(asClass)) {
                    return readObject(jsonValue, new JohnzonParameterizedType(asClass, Object.class), applyObjectConverter);
                }
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
        if (NULL == valueType) {
            return null;
        }
        if (TRUE == valueType && (Boolean.class == targetType || boolean.class == targetType || Object.class == targetType)) {
            return (T) Boolean.TRUE;
        }
        if (FALSE == valueType && (Boolean.class == targetType || boolean.class == targetType || Object.class == targetType)) {
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
                    if (SortedMap.class.isAssignableFrom(raw) || NavigableMap.class == raw || TreeMap.class == raw) {
                        map = config.getAttributeOrder() == null ? new TreeMap() : new TreeMap(config.getAttributeOrder());
                    } else if (ConcurrentMap.class.isAssignableFrom(raw)) {
                        map = new ConcurrentHashMap(object.size());
                    } else if (EnumMap.class.isAssignableFrom(raw)) {
                        if (!config.isSupportEnumContainerDeserialization()) {
                            throw new MapperException("JSON-B forbids EnumMap deserialization, " +
                                    "set supportEnumMapDeserialization=true to disable that arbitrary limitation");
                        }
                        map = new EnumMap(Class.class.cast(fieldArgTypes[0]));
                    } else if (Map.class.isAssignableFrom(raw)) {
                        map = new LinkedHashMap(object.size()); // todo: configurable from config.getNewDefaultMap()?
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
                                map.put(value.getKey(), config.isUseBigDecimalForObjectNumbers() ?
                                        JsonNumber.class.cast(jsonValue).bigDecimalValue() : toNumberValue(JsonNumber.class.cast(jsonValue)));
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
                final Map<String, Object> map = new LinkedHashMap<String, Object>();
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
        if (classMapping.factory.getParameterTypes() == null || classMapping.factory.getParameterTypes().length == 0) {
            t = classMapping.factory.create(null);
        } else {
            t = classMapping.factory.create(createParameters(classMapping, object, jsonPointer));
        }
        // store the new object under it's jsonPointer in case it gets referenced later
        if (isDeduplicateObjects) {
            jsonPointers.put(jsonPointer.toString(), t);
        }

        for (final Map.Entry<String, JsonValue> jsonEntry : object.entrySet()) {
            final Mappings.Setter value = classMapping.setters.get(jsonEntry.getKey());
            if (value == null) {
                continue;
            }

            final JsonValue jsonValue = jsonEntry.getValue();
            final JsonValue.ValueType valueType = jsonValue != null ? jsonValue.getValueType() : null;

            if (JsonValue.class == value.paramType) {
                value.writer.write(t, jsonValue);
                continue;
            }
            if (jsonValue == null) {
                continue;
            }

            final AccessMode.Writer setterMethod = value.writer;
            if (NULL == valueType) { // forced
                setterMethod.write(t, null);
            } else {
                Object existingInstance = null;
                if (config.isReadAttributeBeforeWrite()) {
                    final Mappings.Getter getter = classMapping.getters.get(jsonEntry.getKey());
                    if (getter != null) {
                        try {
                            existingInstance = getter.reader.read(t);
                        } catch (final RuntimeException re) {
                            // backward compatibility
                        }
                    }
                }
                final Object convertedValue = toValue(
                        existingInstance, jsonValue, value.converter, value.itemConverter,
                        value.paramType, value.objectConverter,
                        new JsonPointerTracker(jsonPointer, jsonEntry.getKey()), inType);
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
        if (classMapping.mapAdder != null) {
            object.entrySet().stream()
                .filter(it -> !classMapping.setters.containsKey(it.getKey()))
                .filter(it -> it.getValue().getValueType() != NULL)
                .forEach(e -> {
                    final Object convertedValue = toValue(
                            null, e.getValue(), null, null,
                            classMapping.mapAdderType, null,
                            new JsonPointerTracker(jsonPointer, e.getKey()), inType);
                    if (convertedValue != null) {
                        try {
                            classMapping.mapAdder.invoke(t, e.getKey(), convertedValue);
                        } catch (final IllegalAccessException ex) {
                            throw new IllegalStateException(ex);
                        } catch (final InvocationTargetException ex) {
                            throw new MapperException(ex.getCause());
                        }
                    }
                });
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

    private Object convertTo(final Adapter converter, final JsonValue jsonValue, final JsonPointerTracker jsonPointer,
                             final Type targetType) {
        final JsonValue.ValueType valueType = jsonValue != null ? jsonValue.getValueType() : null;

        final AdapterKey key = getAdapterKey(converter);
        if (key != null && JsonValue.class == key.getTo()) {
            return converter.to(jsonValue);
        }

        if (JsonValue.ValueType.OBJECT == valueType) {
            if (JsonObject.class == key.getTo() || JsonStructure.class == key.getTo()) {
                return converter.to(jsonValue.asJsonObject());
            }

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

        if (NULL.equals(valueType)) {
            return null;
        }
        if (STRING.equals(valueType)) {
            if (key.getTo() == JsonString.class) {
                return converter.to(JsonString.class.cast(jsonValue));
            }
            return converter.to(JsonString.class.cast(jsonValue).getString());
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
                } else if (BigDecimal.class == key.getTo() ||  Number.class == key.getTo()) {
                    return converter.to(JsonNumber.class.cast(jsonValue).bigDecimalValue());
                } else if (JsonNumber.class == key.getTo()) {
                    return converter.to(JsonNumber.class.cast(jsonValue));
                }
            }
        }
        if (ARRAY.equals(valueType)) {
            if (JsonArray.class == key.getTo() || JsonStructure.class == key.getTo()) {
                return converter.to(jsonValue.asJsonObject());
            }
            if (TypeAwareAdapter.class.isInstance(converter)) {
                final TypeAwareAdapter adapter = TypeAwareAdapter.class.cast(converter);
                if (adapter.getFrom().equals(targetType)) {
                    return converter.to(readObject(jsonValue, adapter.getTo()));
                }
            }
            return buildArray(key.getTo(), jsonValue.asJsonArray(), null, null, jsonPointer, null);
        }
        return converter.to(jsonValue.toString());

    }

    private AdapterKey getAdapterKey(final Adapter converter) {
        AdapterKey adapterKey = config.getReverseAdapters().get(converter);

        if (adapterKey == null) {
            if (converter instanceof TypeAwareAdapter) {
                TypeAwareAdapter typeAwareAdapter = (TypeAwareAdapter) converter;
                adapterKey = new AdapterKey(typeAwareAdapter.getFrom(), typeAwareAdapter.getTo());
                config.getReverseAdapters().putIfAbsent(converter, adapterKey);

            } else {
                Class<?> current = converter.getClass();
                while (current != null && current != Object.class) {
                    final Type[] types = current.getGenericInterfaces();
                    for (final Type t : types) {
                        if (!ParameterizedType.class.isInstance(t)) {
                            continue;
                        }
                        final ParameterizedType pt = ParameterizedType.class.cast(t);
                        if (Adapter.class == pt.getRawType()) {
                            final Type[] actualTypeArguments = pt.getActualTypeArguments();
                            adapterKey = new AdapterKey(actualTypeArguments[0], actualTypeArguments[1]);
                            config.getReverseAdapters().putIfAbsent(converter, adapterKey);
                            return adapterKey;
                        }
                    }
                    current = current.getSuperclass();
                }
            }
        }
        return adapterKey;
    }


    private Object toObject(final Object baseInstance, final JsonValue jsonValue,
                            final Type type, final Adapter itemConverter, final JsonPointerTracker jsonPointer,
                            final Type rootType) {
        if (jsonValue == null) {
            return null;
        }

        JsonValue.ValueType valueType = jsonValue.getValueType();
        if (JsonValue.ValueType.NULL == valueType) {
            return null;
        }

        if (type == Boolean.class || type == boolean.class) {
            if (JsonValue.ValueType.TRUE == valueType) {
                return true;
            }
            if (JsonValue.ValueType.FALSE == valueType) {
                return false;
            }
            throw new MapperException("Unable to parse " + jsonValue + " to boolean");
        }

        if (config.isTreatByteArrayAsBase64() && jsonValue.getValueType() == JsonValue.ValueType.STRING && (type == byte[].class /*|| type == Byte[].class*/)) {
            return Base64.getDecoder().decode(((JsonString) jsonValue).getString());
        }
        if (config.isTreatByteArrayAsBase64URL() && jsonValue.getValueType() == JsonValue.ValueType.STRING && (type == byte[].class /*|| type == Byte[].class*/)) {
            return Base64.getUrlDecoder().decode(((JsonString) jsonValue).getString());
        }

        if (Object.class == type) { // handling specific types here to keep exception in standard handling
            if (JsonValue.ValueType.TRUE == valueType) {
                return true;
            }
            if (JsonValue.ValueType.FALSE == valueType) {
                return false;
            }
            if (JsonNumber.class.isInstance(jsonValue)) {
                return toNumberValue(JsonNumber.class.cast(jsonValue));
            }
            if (JsonString.class.isInstance(jsonValue)) {
                return JsonString.class.cast(jsonValue).getString();
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

            if (type == Long.class || type == long.class) {
                return number.longValueExact();
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

            if (type == BigDecimal.class ||  Number.class == type) {
                return number.bigDecimalValue();
            }

            int intValue = number.intValueExact();
            if (type == Integer.class || type == int.class) {
                return intValue;
            }

            if (type == Short.class || type == short.class) {
                short shortVal = (short) intValue;
                if (intValue != shortVal) {
                    throw new java.lang.ArithmeticException("Overflow");
                }
                return shortVal;
            }

            if (type == Byte.class || type == byte.class) {
                Validator.validateByte(intValue);
                return (byte) intValue;
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
                return convertTo(type, string);
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
            if (Collection.class.isAssignableFrom(clazz)) {
                final Mappings.CollectionMapping mapping = mappings.findCollectionMapping(
                        new JohnzonParameterizedType(clazz, Object.class), rootType);
                if (mapping != null) {
                    return mapCollection(mapping, jsonArray, itemConverter, objectConverter, jsonPointer, rootType);
                }
            }
        }

        if (ParameterizedType.class.isInstance(type)) {
            final ParameterizedType genericType = ParameterizedType.class.cast(type);
            if (Stream.class == genericType.getRawType()) {
                return Stream.of(1).flatMap(seed -> Collection.class.cast(buildArray(
                        new JohnzonParameterizedType(List.class, genericType.getActualTypeArguments()),
                        jsonArray, itemConverter, objectConverter, jsonPointer, rootType)).stream());
            }

            final Mappings.CollectionMapping mapping = mappings.findCollectionMapping(genericType, rootType);
            if (mapping != null) {
                return mapCollection(mapping, jsonArray, itemConverter, objectConverter, jsonPointer, rootType);
            }
        }

        if (GenericArrayType.class.isInstance(type)) {
            Type genericComponentType = GenericArrayType.class.cast(type).getGenericComponentType();
            while (ParameterizedType.class.isInstance(genericComponentType)) {
                genericComponentType = ParameterizedType.class.cast(genericComponentType).getRawType();
            }
            if (Class.class.isInstance(genericComponentType)) {
                return buildArrayWithComponentType(jsonArray, Class.class.cast(genericComponentType), itemConverter, jsonPointer, rootType);
            } // else: fail for now
        }

        if (Object.class == type) {
            return buildArray(ANY_LIST, jsonArray, null, null, jsonPointer, rootType);
        }

        // guess we don't want to map stream impls - keep it lazy since it is the only advantage to have streams there
        if (IntStream.class == type) {
            return Stream.of(1).flatMapToInt(seed -> IntStream.of(int[].class.cast(
                    buildArray(int[].class, jsonArray, null, null, jsonPointer, rootType))));
        }
        if (LongStream.class == type) {
            return Stream.of(1).flatMapToLong(seed -> LongStream.of(long[].class.cast(
                    buildArray(long[].class, jsonArray, null, null, jsonPointer, rootType))));
        }
        if (DoubleStream.class == type) {
            return Stream.of(1).flatMapToDouble(seed -> DoubleStream.of(double[].class.cast(
                    buildArray(double[].class, jsonArray, null, null, jsonPointer, rootType))));
        }

        throw new UnsupportedOperationException("type " + type + " not supported");
    }

    private Object buildArrayWithComponentType(final JsonArray jsonArray, final Class<?> componentType, final Adapter itemConverter,
                                               final JsonPointerTracker jsonPointer, final Type rootType) {

        if (boolean.class == componentType) {
            boolean[] array = new boolean[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (boolean) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (byte.class == componentType) {
            byte[] array = new byte[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (byte) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (char.class == componentType) {
            char[] array = new char[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (char) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (short.class == componentType) {
            short[] array = new short[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (short) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (int.class == componentType) {
            int[] array = new int[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (int) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (long.class == componentType) {
            long[] array = new long[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (long) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (float.class == componentType) {
            float[] array = new float[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (float) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (double.class == componentType) {
            double[] array = new double[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (double) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }

        // wrapper types
        if (Boolean.class == componentType) {
            Boolean[] array = new Boolean[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (Boolean) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (Byte.class == componentType) {
            Byte[] array = new Byte[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (Byte) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (Character.class == componentType) {
            Character[] array = new Character[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (Character) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (Short.class == componentType) {
            Short[] array = new Short[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (Short) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (Integer.class == componentType) {
            Integer[] array = new Integer[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (Integer) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (Long.class == componentType) {
            Long[] array = new Long[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (Long) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (Float.class == componentType) {
            Float[] array = new Float[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (Float) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }
        if (Double.class == componentType) {
            Double[] array = new Double[jsonArray.size()];
            int i = 0;
            for (final JsonValue value : jsonArray) {
                array[i] = (Double) toObject(null, value, componentType, itemConverter,
                        isDeduplicateObjects ? new JsonPointerTracker(jsonPointer, i) : null, rootType);
                i++;
            }
            return array;
        }

        // for all the rest we have to rely on reflection :(
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
        } else if (LinkedList.class == mapping.raw) {
            collection = new LinkedList<T>();
        } else if (Deque.class == mapping.raw || ArrayDeque.class == mapping.raw) {
            collection = new ArrayDeque(jsonArray.size());
        } else if (PriorityQueue.class == mapping.raw) {
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
            if (!config.isSupportEnumContainerDeserialization()) {
                throw new MapperException("Enum container deserialization disabled, " +
                        "set supportEnumContainerDeserialization=true to enable it");
            }
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
            return objectConverter.fromJson(jsonValue, type, this);
        }

        try {
            return converter == null ?
                    toObject(baseInstance, jsonValue, type, itemConverter, jsonPointer, rootType) :
                    convertTo(converter, jsonValue, jsonPointer, type);
        } catch (Exception e) {
            if (e instanceof MapperException) {
                throw e;
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
            if (ParameterizedType.class.isInstance(aClass)) {
                return convertTo(ParameterizedType.class.cast(aClass).getRawType(), text);
            }
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
