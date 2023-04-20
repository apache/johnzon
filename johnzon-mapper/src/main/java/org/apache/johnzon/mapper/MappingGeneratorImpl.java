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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import org.apache.johnzon.mapper.internal.JsonPointerTracker;
import org.apache.johnzon.mapper.util.ArrayUtil;

import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerator;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.BaseStream;
import java.util.stream.StreamSupport;

public class MappingGeneratorImpl implements MappingGenerator {
    private final MapperConfig config;
    private final JsonGenerator generator;
    private final Mappings mappings;
    private Map<Object, String> jsonPointers;

    MappingGeneratorImpl(MapperConfig config, JsonGenerator jsonGenerator, final Mappings mappings) {
        this.config = config;
        this.generator = jsonGenerator;
        this.mappings = mappings;
    }

    @Override
    public JsonGenerator getJsonGenerator() {
        return generator;
    }

    @Override
    public MappingGenerator writeObject(final String key, final Object object, final JsonGenerator generator) {
        if (object == null) {
            return this;
        } else if (object instanceof JsonValue) {
            generator.write(key, JsonValue.class.cast(object));
        } else {
            final Class<?> objectClass = object.getClass();
            try {
                if (Map.class.isInstance(object)) {
                    writeValue(Map.class, false, false, false, false, true, null, key, object,
                            null, emptyList(), isDedup() ? JsonPointerTracker.ROOT : null, generator);
                } else if(writePrimitives(key, objectClass, object, generator)) {
                    // no-op
                } else if (Enum.class.isAssignableFrom(objectClass)) {
                    final Adapter adapter = config.findAdapter(objectClass);
                    final String adaptedValue = adapter.from(object).toString(); // we know it ends as String for enums
                    generator.write(key, adaptedValue);
                } else if (objectClass.isArray()) {
                    writeValue(Map.class, false, false, true, false, false, null, key, object,
                            null, emptyList(), isDedup() ? JsonPointerTracker.ROOT : null, generator);
                } else if (Iterable.class.isInstance(object)) {
                    writeValue(Map.class, false, false, false, true, false, null, key, object,
                            null, emptyList(), isDedup() ? JsonPointerTracker.ROOT : null, generator);
                } else {
                    final ObjectConverter.Writer objectConverter = config.findObjectConverterWriter(objectClass);
                    if (objectConverter != null) {
                        writeWithObjectConverter(new DynamicMappingGenerator(this,
                                generator::writeStartObject, generator::writeEnd, null), objectConverter, object);
                    } else {
                        writeValue(objectClass, false, false, false, false, false, null, key, object,
                                null, emptyList(), isDedup() ? JsonPointerTracker.ROOT : null, generator);
                    }
                }
            } catch (final InvocationTargetException | IllegalAccessException e) {
                throw new MapperException(e);
            }
        }
        return this;
    }

    @Override
    public MappingGenerator writeObject(final Object object, final JsonGenerator generator) {
        if (object == null) {
            return this;
        } else if (object instanceof JsonValue) {
            generator.write((JsonValue) object);
        } else {
            doWriteObject(object, generator, false, null, isDedup() ? JsonPointerTracker.ROOT : null);
        }
        return this;
    }

    private boolean isDedup() {
        return config.isDeduplicateObjects() || (jsonPointers != null && jsonPointers != Collections.<Object, String>emptyMap());
    }

    public void doWriteObject(Object object, JsonGenerator generator, boolean writeBody, final Collection<String> ignoredProperties,
                              JsonPointerTracker jsonPointer) {

        try {
            if (object instanceof Map) {
                if (writeBody) {
                    generator.writeStartObject();
                }
                writeMapBody((Map<?, ?>) object, null);
                if (writeBody) {
                    generator.writeEnd();
                }
                return;
            }

            if (writePrimitives(object)) {
                return;
            }

            final Class<?> objectClass = object.getClass();
            if (Enum.class.isAssignableFrom(objectClass)) {
                final Adapter adapter = config.findAdapter(objectClass);
                final String adaptedValue = adapter.from(object).toString(); // we know it ends as String for enums
                generator.write(adaptedValue);
                return;
            }

            if (objectClass.isArray()) {
                final Adapter adapter = config.findAdapter(objectClass);
                writeArray(objectClass, adapter, null, object, ignoredProperties, jsonPointer);
                return;
            }

            if (object instanceof Iterable) {
                doWriteIterable((Iterable) object, ignoredProperties, jsonPointer);
                return;
            }

            Mappings.ClassMapping classMapping = mappings.getClassMapping(objectClass); // don't create here!
            if (classMapping != null) {
                if (classMapping.adapter != null) {
                    final Object result = classMapping.adapter.from(object);
                    doWriteObject(result, generator, writeBody, ignoredProperties, jsonPointer);
                    return;
                }
            } else {
                final Adapter adapter = config.findAdapter(objectClass);
                if (adapter != null) {
                    doWriteObject(adapter.from(object), generator, writeBody, ignoredProperties, jsonPointer);
                    return;
                }
            }

            ObjectConverter.Writer objectConverter = config.findObjectConverterWriter(objectClass);
            if (writeBody && objectConverter != null) {
                if (!writeBody) {
                    objectConverter.writeJson(object, this);
                } else {
                    writeWithObjectConverter(new DynamicMappingGenerator(this,
                            generator::writeStartObject, generator::writeEnd, null), objectConverter, object);
                }
            } else {
                if (classMapping == null) { // will be created anyway now so force it and if it has an adapter respect it
                    classMapping = mappings.findOrCreateClassMapping(objectClass);
                }

                if (classMapping.adapter != null) {
                    final Object result = classMapping.adapter.from(object);
                    doWriteObject(result, generator, writeBody, ignoredProperties, jsonPointer);
                    return;
                }

                if (writeBody) {
                    generator.writeStartObject();
                }

                if (classMapping.serializedPolymorphicProperties != null) {
                    for (Map.Entry<String, String> polymorphicProperty : classMapping.serializedPolymorphicProperties) {
                        generator.write(polymorphicProperty.getKey(), polymorphicProperty.getValue());
                    }
                }

                final boolean writeEnd = doWriteObjectBody(object, ignoredProperties, jsonPointer, generator);
                if (writeEnd && writeBody) {
                    generator.writeEnd();
                }
            }
        } catch (final InvocationTargetException | IllegalAccessException e) {
            throw new MapperException(e);
        }
    }

    private JsonGenerator writeMapBody(final Map<?, ?> object, final Adapter itemConverter) throws InvocationTargetException, IllegalAccessException {
        for (final Map.Entry<?, ?> entry : ((Map<?, ?>) object).entrySet()) {
            final Object value = entry.getValue();
            final Object key = entry.getKey();

            if (value == null) {
                if (config.isSkipNull()) {
                    continue;
                } else {
                    generator.writeNull(key == null ? "null" : key.toString());
                    continue;
                }
            }

            final Class<?> valueClass = value.getClass();
            writeValue(valueClass, true,
                    false, false, false, false, itemConverter,
                    key == null ? "null" : key.toString(), value, null, null, null,
                    generator);
        }
        return generator;
    }

    /**
     * @return {@code true} if it was a primitive, {@code false} if the value did not get handled
     */
    private boolean writePrimitives(final Object value) {
        boolean handled = false;
        if (value == null) {
            return true; // fake a write
        }

        final Class<?> type = value.getClass();
        if (type == String.class) {
            generator.write(value.toString());
            handled = true;
        } else if (JsonValue.class.isAssignableFrom(type)) {
            generator.write(JsonValue.class.cast(value));
            handled = true;
        } else if (type == long.class || type == Long.class) {
            final long longValue = Long.class.cast(value).longValue();
            if (isInJsRange(longValue)) {
                generator.write(longValue);
            } else {
                generator.write(value.toString());
            }
            handled = true;
        } else if (isInt(type)) {
            generator.write(Number.class.cast(value).intValue());
            handled = true;
        } else if (isFloat(type)) {
            if (type == Float.class || type == float.class) {
                if (!Float.isNaN(Float.class.cast(value))) {
                    generator.write(new BigDecimal(value.toString()));
                }
            } else {
                final double doubleValue = Number.class.cast(value).doubleValue();
                if (!Double.isNaN(doubleValue)) {
                    generator.write(doubleValue);
                }
            }
            handled = true;
        } else if (type == boolean.class || type == Boolean.class) {
            generator.write(Boolean.class.cast(value));
            return true;
        } else if (type == BigDecimal.class) {
            generator.write(BigDecimal.class.cast(value));
            handled = true;
        } else if (type == BigInteger.class) {
            generator.write(BigInteger.class.cast(value));
            handled = true;
        } else if (type == char.class || type == Character.class) {
            generator.write(Character.class.cast(value).toString());
            handled = true;
        }
        return handled;
    }

    private boolean writePrimitives(final String key, final Class<?> type, final Object value,
                                    final JsonGenerator generator) {
        boolean handled = false;
        if (type == String.class) {
            generator.write(key, value.toString());
            handled = true;
        } else if (JsonValue.class.isAssignableFrom(type)) {
            generator.write(key, JsonValue.class.cast(value));
            handled = true;
        } else if (type == long.class || type == Long.class) {
            final long longValue = Long.class.cast(value).longValue();
            if (isInJsRange(longValue)) {
                generator.write(key, longValue);
            } else {
                generator.write(key, value.toString());
            }
            handled = true;
        } else if (isInt(type)) {
            generator.write(key, Number.class.cast(value).intValue());
            handled = true;
        } else if (isFloat(type)) {
            if (type == Float.class || type == float.class) {
                if (!Float.isNaN(Float.class.cast(value))) {
                    generator.write(key, new BigDecimal(value.toString()));
                }
            } else {
                final double doubleValue = Number.class.cast(value).doubleValue();
                if (!Double.isNaN(doubleValue)) {
                    generator.write(key, doubleValue);
                }
            }
            handled = true;
        } else if (type == boolean.class || type == Boolean.class) {
            generator.write(key, Boolean.class.cast(value));
            handled = true;
        } else if (type == BigDecimal.class) {
            generator.write(key, BigDecimal.class.cast(value));
            handled = true;
        } else if (type == BigInteger.class) {
            generator.write(key, BigInteger.class.cast(value));
            handled = true;
        } else if (type == char.class || type == Character.class) {
            generator.write(key, Character.class.cast(value).toString());
            handled = true;
        }
        return handled;
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


    private boolean doWriteObjectBody(final Object object, final Collection<String> ignored,
                                      final JsonPointerTracker jsonPointer, final JsonGenerator generator)
            throws IllegalAccessException, InvocationTargetException {
        final Class<?> objectClass = object.getClass();
        final Mappings.ClassMapping classMapping = mappings.findOrCreateClassMapping(objectClass);
        if (classMapping == null) {
            throw new MapperException("No mapping for " + objectClass.getName());
        }

        if (jsonPointers == null) {
            if (classMapping.deduplicateObjects || config.isDeduplicateObjects()) {
                jsonPointers = new HashMap<>();
                jsonPointers.putIfAbsent(object, jsonPointer == null ? "/" : jsonPointer.toString());
            } else {
                jsonPointers = emptyMap();
            }
        } else if (isDedup()) {
            jsonPointers.putIfAbsent(object, jsonPointer == null ? "/" : jsonPointer.toString());
        }

        if (classMapping.writer != null) {
            writeWithObjectConverter(new DynamicMappingGenerator.SkipEnclosingWriteEnd(this, null, generator), classMapping.writer, object);
            return false;
        }
        if (classMapping.adapter != null) {
            doWriteObjectBody(classMapping.adapter.from(object), ignored, jsonPointer, generator);
            return true;
        }

        for (final Map.Entry<String, Mappings.Getter> getterEntry : classMapping.getters.entrySet()) {
            final Mappings.Getter getter = getterEntry.getValue();
            if (ignored != null && ignored.contains(getterEntry.getKey())) {
                continue;
            }
            if (getter.version >= 0 && config.getVersion() >= 0 && config.getVersion() < getter.version) {
                continue;
            }

            final Object value = getter.reader.read(object);
            if (JsonValue.class.isInstance(value)) {
                generator.write(getterEntry.getKey(), JsonValue.class.cast(value));
                continue;
            }

            if (value == null) {
                if (!getter.reader.isNillable(!config.isSkipNull())) {
                    continue;
                } else {
                    generator.writeNull(getterEntry.getKey());
                    continue;
                }
            }

            final Object val = getter.converter == null ? value : getter.converter.from(value);

            String valJsonPointer = jsonPointers.get(val);
            if (valJsonPointer != null) {
                // write the JsonPointer instead
                generator.write(getterEntry.getKey(), valJsonPointer);
            } else {
                writeValue(val.getClass(),
                        getter.dynamic,
                        getter.primitive,
                        getter.array,
                        getter.collection,
                        getter.map,
                        getter.itemConverter,
                        getterEntry.getKey(),
                        val,
                        getter.objectConverter,
                        getter.ignoreNested,
                        isDedup() ? new JsonPointerTracker(jsonPointer, getterEntry.getKey()) : null,
                        generator);
            }
        }

        // @JohnzonAny doesn't respect comparator since it is a map and not purely in the model we append it after and
        // sorting is up to the user for this part (TreeMap if desired)
        if (classMapping.anyGetter != null) {
            final Map<String, Object> any = Map.class.cast(classMapping.anyGetter.reader.read(object));
            if (any != null) {
                writeMapBody(any, null);
            }
        }

        return true;
    }

    //CHECKSTYLE:OFF
    private void writeValue(final Class<?> type, final boolean dynamic,
                            final boolean primitive, final boolean array,
                            final boolean collection, final boolean map,
                            final Adapter itemConverter,
                            final String key, final Object value,
                            final ObjectConverter.Writer objectConverter,
                            final Collection<String> ignoredProperties,
                            final JsonPointerTracker jsonPointer,
                            final JsonGenerator generator)
            throws InvocationTargetException, IllegalAccessException {
        //CHECKSTYLE:ON
        if (config.getSerializeValueFilter().shouldIgnore(key, value)) {
            return;
        }
        if ((!dynamic && array) || (dynamic && type.isArray())) {
            writeArray(type, itemConverter, key, value, ignoredProperties, jsonPointer);
        } else if ((!dynamic && collection) || (dynamic && Iterable.class.isAssignableFrom(type))) {
            writeIterator(itemConverter, key, objectConverter, ignoredProperties, jsonPointer, generator,
                    Iterable.class.cast(value).iterator(), value);
        } else if ((!dynamic && map) || (dynamic && Map.class.isAssignableFrom(type))) {
            generator.writeStartObject(key);
            if (objectConverter != null) {
                writeWithObjectConverter(new DynamicMappingGenerator(this,
                        () -> this.generator.writeStartObject(key), this.generator::writeEnd, key), objectConverter, value);
            } else {
                writeMapBody((Map<?, ?>) value, itemConverter);
            }
            generator.writeEnd();
        } else if ((!dynamic && primitive) || (dynamic && Mappings.isPrimitive(type))) {
            if (objectConverter != null) {
                writeWithObjectConverter(new DynamicMappingGenerator(this,
                        () -> this.generator.writeStartObject(key), this.generator::writeEnd, key), objectConverter, value);
            } else {
                writePrimitives(key, type, value, generator);
            }
        } else if (BaseStream.class.isAssignableFrom(type)) {
            writeIterator(itemConverter, key, objectConverter, ignoredProperties, jsonPointer, generator,
                    BaseStream.class.cast(value).iterator(), value);
        } else if (Iterator.class.isAssignableFrom(type)) {
            if (objectConverter != null) {
                generator.writeStartObject(key);
                writeWithObjectConverter(new DynamicMappingGenerator(this,
                        () -> this.generator.writeStartObject(key), this.generator::writeEnd, key), objectConverter, value);
                generator.writeEnd();
            } else {
                writeIterator(itemConverter, key, objectConverter, ignoredProperties, jsonPointer, generator,
                        Iterator.class.cast(value), value);
            }
        } else {
            if (objectConverter != null) {
                writeWithObjectConverter(new DynamicMappingGenerator(this,
                        () -> this.generator.writeStartObject(key), this.generator::writeEnd, key), objectConverter, value);
                return;
            }

            final Adapter converter = config.findAdapter(type);
            if (converter != null) {
                final Object adapted = doConvertFrom(value, converter);
                if (writePrimitives(key, adapted.getClass(), adapted, generator)) {
                    return;
                }
                writeValue(String.class, true, true, false, false, false, null, key, adapted, null, ignoredProperties, jsonPointer, generator);
                return;
            } else {
                ObjectConverter.Writer objectConverterToUse = objectConverter;
                if (objectConverterToUse == null) {
                    objectConverterToUse = config.findObjectConverterWriter(type);
                }

                if (objectConverterToUse != null) {
                    writeWithObjectConverter(new DynamicMappingGenerator(this,
                            () -> this.generator.writeStartObject(key), this.generator::writeEnd, key), objectConverterToUse, value);
                    return;
                }
            }
            if (writePrimitives(key, type, value, generator)) {
                return;
            }
            generator.writeStartObject(key);
            if (doWriteObjectBody(value, ignoredProperties, jsonPointer, generator)) {
                generator.writeEnd();
            }
        }
    }

    private void writeWithObjectConverter(final DynamicMappingGenerator generator,
                                          final ObjectConverter.Writer objectConverter,
                                          final Object value) {
        final DynamicMappingGenerator dynamicMappingGenerator = generator;
        objectConverter.writeJson(value, dynamicMappingGenerator);
        dynamicMappingGenerator.flushIfNeeded();
    }

    private void writeIterator(final Adapter itemConverter, final String key,
                               final ObjectConverter.Writer objectConverter,
                               final Collection<String> ignoredProperties,
                               final JsonPointerTracker jsonPointer,
                               final JsonGenerator generator,
                               final Iterator<?> iterator,
                               final Object originalValue) {
        if (objectConverter != null && objectConverter.isGlobal()) {
            final List<Object> list = List.class.isInstance(originalValue) ?
                    List.class.cast(originalValue) :
                    StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE), false)
                        .collect(toList());
            objectConverter.writeJson(list, new DynamicMappingGenerator(
                    this, generator::writeStartArray, generator::writeEnd, key));
            return;
        }

        int i = 0;
        generator.writeStartArray(key);
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            String valJsonPointer = jsonPointers == null ? null : jsonPointers.get(o);
            if (valJsonPointer != null) {
                // write JsonPointer instead of the original object
                writePrimitives(valJsonPointer);
            } else {
                ObjectConverter.Writer objectConverterToUse = objectConverter;
                if (o != null && objectConverterToUse == null) {
                    objectConverterToUse = config.findObjectConverterWriter(o.getClass());
                }

                if (objectConverterToUse != null) {
                    writeWithObjectConverter(new DynamicMappingGenerator(this,
                            generator::writeStartObject, generator::writeEnd, null), objectConverterToUse, o);
                } else {
                    writeItem(itemConverter != null ? itemConverter.from(o) : o, ignoredProperties,
                            isDedup() ? new JsonPointerTracker(jsonPointer, i) : null);
                }
            }
            i++;
        }
        generator.writeEnd();
    }

    /**
     * Write a JSON Array with a given Array Value, like byte[], int[], Person[] etc.
     * @param key either the attribute key or {@code null} if the array should be rendered without key
     */
    private void writeArray(Class<?> type, Adapter itemConverter, String key, Object arrayValue, Collection<String> ignoredProperties, JsonPointerTracker jsonPointer) {
        final int length = ArrayUtil.getArrayLength(arrayValue);
        if (length == 0 && config.isSkipEmptyArray()) {
            return;
        }

        if(config.isTreatByteArrayAsBase64() && (type == byte[].class /*|| type == Byte[].class*/)) {
            String base64EncodedByteArray = Base64.getEncoder().encodeToString((byte[]) arrayValue);
            if (key != null) {
                generator.write(key, base64EncodedByteArray);
            } else {
                generator.write(base64EncodedByteArray);
            }
            return;
        }
        if(config.isTreatByteArrayAsBase64URL() && (type == byte[].class /*|| type == Byte[].class*/)) {
            if (key != null) {
                generator.write(key, Base64.getUrlEncoder().encodeToString((byte[]) arrayValue));
            } else {
                generator.write(Base64.getUrlEncoder().encodeToString((byte[]) arrayValue));
            }
            return;
        }

        if (key != null) {
            generator.writeStartArray(key);
        } else {
            generator.writeStartArray();
        }

        // some specialised arrays to speed up conversion.
        // Needed since Array.get is rather slow :(
        if (type == byte[].class) {
            byte[] tArrayValue = (byte[]) arrayValue;
            for (int i = 0; i < length; i++) {
                final byte o = tArrayValue[i];
                generator.write(o);
            }
        } else if (type == short[].class) {
            short[] tArrayValue = (short[]) arrayValue;
            for (int i = 0; i < length; i++) {
                final short o = tArrayValue[i];
                generator.write(o);
            }
        } else if (type == int[].class) {
            int[] tArrayValue = (int[]) arrayValue;
            for (int i = 0; i < length; i++) {
                final int o = tArrayValue[i];
                generator.write(o);
            }
        } else if (type == long[].class) {
            long[] tArrayValue = (long[]) arrayValue;
            for (int i = 0; i < length; i++) {
                final long o = tArrayValue[i];
                generator.write(o);
            }
        } else if (type == float[].class) {
            float[] tArrayValue = (float[]) arrayValue;
            for (int i = 0; i < length; i++) {
                final float o = tArrayValue[i];
                generator.write(o);
            }
        } else if (type == double[].class) {
            double[] tArrayValue = (double[]) arrayValue;
            for (int i = 0; i < length; i++) {
                final double o = tArrayValue[i];
                generator.write(o);
            }
        } else if (type == char[].class) {
            char[] tArrayValue = (char[]) arrayValue;
            for (int i = 0; i < length; i++) {
                final char o = tArrayValue[i];
                generator.write(String.valueOf(o));
            }
        } else if (type == boolean[].class) {
            boolean[] tArrayValue = (boolean[]) arrayValue;
            for (int i = 0; i < length; i++) {
                final boolean o = tArrayValue[i];
                generator.write(o);
            }
        } else if (type == Byte[].class ||
                   type == Short[].class ||
                   type == Integer[].class ||
                   type == Long[].class ||
                   type == Float[].class ||
                   type == Double[].class ||
                   type == Character[].class ||
                   type == Boolean[].class) {
            // Wrapper types do not not need deduplication
            Object[] oArrayValue = (Object[]) arrayValue;
            for (int i = 0; i < length; i++) {
                final Object o = oArrayValue[i];
                writeItem(itemConverter != null ? itemConverter.from(o) : o, ignoredProperties,
                        isDedup() ? new JsonPointerTracker(jsonPointer, i) : null);
            }
        } else {
            // must be object arrays
            for (int i = 0; i < length; i++) {
                Object[] oArrayValue = (Object[]) arrayValue;
                final Object o = oArrayValue[i];
                String valJsonPointer = jsonPointers == null ? null : jsonPointers.get(o);
                if (valJsonPointer != null) {
                    // write the JsonPointer as String natively
                    generator.write(valJsonPointer);
                } else if (o instanceof JsonValue) {
                    generator.write((JsonValue) o);
                } else {
                    writeItem(itemConverter != null ? itemConverter.from(o) : o, ignoredProperties,
                            isDedup() ? new JsonPointerTracker(jsonPointer, i) : null);
                }
            }
        }
        generator.writeEnd();
    }


    private void writeItem(final Object o, final Collection<String> ignoredProperties, JsonPointerTracker jsonPointer) {
        if (o == null) {
            generator.writeNull();
        } else if (!writePrimitives(o)) {
            if (Collection.class.isInstance(o)) {
                doWriteIterable(Collection.class.cast(o), ignoredProperties, jsonPointer);
            } else if (o.getClass().isArray()) {
                final int length = ArrayUtil.getArrayLength(o);
                if (length > 0 || !config.isSkipEmptyArray()) {
                    writeArray(o.getClass(), null, null, o, ignoredProperties, jsonPointer);
                }
            } else {
                String valJsonPointer = jsonPointers == null ? null : jsonPointers.get(o);
                if (valJsonPointer != null) {
                    // write the JsonPointer instead
                    generator.write(valJsonPointer);
                } else {
                    doWriteObject(o, generator, true, ignoredProperties, jsonPointer);
                }
            }
        }
    }

    private <T> void doWriteIterable(final Iterable<T> object, final Collection<String> ignoredProperties, JsonPointerTracker jsonPointer) {
        if (object == null) {
            generator.writeStartArray().writeEnd();
        } else {
            generator.writeStartArray();
            int i = 0;
            for (final T t : object) {
                if (JsonValue.class.isInstance(t)) {
                    generator.write(JsonValue.class.cast(t));
                } else {
                    if (t == null) {
                        generator.writeNull();
                    } else {
                        writeItem(t, ignoredProperties, isDedup() ? new JsonPointerTracker(jsonPointer, i) : null);
                    }
                }
                i++;
            }
            generator.writeEnd();
        }
    }


    private <T> Object doConvertFrom(final T value, final Adapter<T, Object> converter) {
        if (converter == null) {
            throw new MapperException("can't convert " + value + " to String");
        }
        return converter.from(value);
    }

    private boolean isInJsRange(final Number longValue) {
        return !config.isUseJsRange() ||
                (longValue.longValue() <= 9007199254740991L && longValue.longValue() >= -9007199254740991L);
    }
}
