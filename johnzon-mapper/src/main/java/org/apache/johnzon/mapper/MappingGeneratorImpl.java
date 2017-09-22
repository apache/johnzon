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

import org.apache.johnzon.core.JsonPointerUtil;
import org.apache.johnzon.mapper.internal.AdapterKey;

import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MappingGeneratorImpl implements MappingGenerator {
    private final MapperConfig config;
    private final JsonGenerator generator;
    private final Mappings mappings;
    private Map<Object, String> jsonPointers = new HashMap<>();


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
    public MappingGenerator writeObject(Object object, JsonGenerator generator) {
        if (object == null) {
            return this;
        } else if (object instanceof JsonValue) {
            generator.write((JsonValue) object);
        } else {
            doWriteObject(object, generator, false, null);
        }
        return this;
    }

    public void doWriteObject(Object object, JsonGenerator generator, boolean writeBody, final Collection<String> ignoredProperties) {

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

            if(writePrimitives(object)) {
                return;
            }

            final Class<?> objectClass = object.getClass();
            if (objectClass.isEnum()) {
                final Adapter adapter = config.findAdapter(objectClass);
                final String adaptedValue = adapter.from(object).toString(); // we know it ends as String for enums
                generator.write(adaptedValue);
                return;
            }

            if (object instanceof Iterable) {
                doWriteIterable((Iterable) object, ignoredProperties);
                return;
            }

            if (writeBody) {
                generator.writeStartObject();
            }

            ObjectConverter.Writer objectConverter = config.findObjectConverterWriter(objectClass);
            if (writeBody && objectConverter != null) {
                objectConverter.writeJson(object, this);
            } else {
                doWriteObjectBody(object, ignoredProperties, "/");
            }

            if (writeBody) {
                generator.writeEnd();
            }
        } catch (final InvocationTargetException e) {
            throw new MapperException(e);
        } catch (final IllegalAccessException e) {
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
            final boolean primitive = Mappings.isPrimitive(valueClass);
            final boolean clazz = mappings.getClassMapping(valueClass) != null;
            final boolean array = clazz || primitive ? false : valueClass.isArray();
            final boolean collection = clazz || primitive || array ? false : Collection.class.isAssignableFrom(valueClass);
            final boolean map = clazz || primitive || array || collection ? false : Map.class.isAssignableFrom(valueClass);
            writeValue(valueClass,
                    primitive, array, collection, map, itemConverter,
                    key == null ? "null" : key.toString(), value, null, null, null);
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
        } else if (type == long.class || type == Long.class) {
            generator.write(Long.class.cast(value).longValue());
            handled = true;
        } else if (isInt(type)) {
            generator.write(Number.class.cast(value).intValue());
            handled = true;
        } else if (isFloat(type)) {
            final double doubleValue = Number.class.cast(value).doubleValue();
            if (!Double.isNaN(doubleValue)) {
                generator.write(doubleValue);
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

    private boolean writePrimitives(final String key, final Class<?> type, final Object value) {
        boolean handled = false;
        if (type == String.class) {
            generator.write(key, value.toString());
            handled = true;
        } else if (type == long.class || type == Long.class) {
            generator.write(key, Long.class.cast(value).longValue());
            handled = true;
        } else if (isInt(type)) {
            generator.write(key, Number.class.cast(value).intValue());
            handled = true;
        } else if (isFloat(type)) {
            final double doubleValue = Number.class.cast(value).doubleValue();
            if (!Double.isNaN(doubleValue)) {
                generator.write(key, doubleValue);
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


    private void doWriteObjectBody(final Object object, final Collection<String> ignored, String jsonPointer)
            throws IllegalAccessException, InvocationTargetException {

        if (jsonPointer != null) {
            jsonPointers.put(object, jsonPointer);
        }

        final Class<?> objectClass = object.getClass();
        final Mappings.ClassMapping classMapping = mappings.findOrCreateClassMapping(objectClass);
        if (classMapping == null) {
            throw new MapperException("No mapping for " + objectClass.getName());
        }

        if (classMapping.writer != null) {
            classMapping.writer.writeJson(object, this);
            return;
        }
        if (classMapping.adapter != null) {
            doWriteObjectBody(classMapping.adapter.to(object), ignored, null);
            return;
        }

        for (final Map.Entry<String, Mappings.Getter> getterEntry : classMapping.getters.entrySet()) {
            final Mappings.Getter getter = getterEntry.getValue();
            if (ignored != null && ignored.contains(getterEntry.getKey())) {
                continue;
            }
            if (getter.version >= 0 && config.getVersion() >= getter.version) {
                continue;
            }

            final Object value = getter.reader.read(object);
            if (JsonValue.class.isInstance(value)) {
                generator.write(getterEntry.getKey(), JsonValue.class.cast(value));
                continue;
            }

            if (value == null) {
                if (config.isSkipNull() && !getter.reader.isNillable()) {
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
                        getter.primitive,
                        getter.array,
                        getter.collection,
                        getter.map,
                        getter.itemConverter,
                        getterEntry.getKey(),
                        val,
                        getter.objectConverter,
                        getter.ignoreNested,
                        nestJsonPointer(jsonPointer, getterEntry.getKey()));
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
    }

    private String nestJsonPointer(String jsonPointer, String attribName) {
        if (jsonPointer == null) {
            return null;
        }
        if (jsonPointer.length() == 1) {
            // the root element
            return jsonPointer + attribName;
        } else {
            return jsonPointer + "/" + JsonPointerUtil.encode(attribName);
        }
    }

    private void writeValue(final Class<?> type,
                            final boolean primitive, final boolean array,
                            final boolean collection, final boolean map,
                            final Adapter itemConverter,
                            final String key, final Object value,
                            final ObjectConverter.Writer objectConverter,
                            final Collection<String> ignoredProperties,
                            final String jsonPointer)
            throws InvocationTargetException, IllegalAccessException {
        if (config.getSerializeValueFilter().shouldIgnore(key, value)) {
            return;
        }
        if (array) {
            final int length = Array.getLength(value);
            if (length == 0 && config.isSkipEmptyArray()) {
                return;
            }

            if(config.isTreatByteArrayAsBase64() && (type == byte[].class /*|| type == Byte[].class*/)) {
                String base64EncodedByteArray = DatatypeConverter.printBase64Binary((byte[]) value);
                generator.write(key, base64EncodedByteArray);
                return;
            }
            if(config.isTreatByteArrayAsBase64URL() && (type == byte[].class /*|| type == Byte[].class*/)) {
                generator.write(key, String.valueOf(Adapter.class.cast(config.getAdapters().get(new AdapterKey(byte[].class, String.class))).to(value)));
                return;
            }

            generator.writeStartArray(key);
            for (int i = 0; i < length; i++) {
                final Object o = Array.get(value, i);
                writeItem(itemConverter != null ? itemConverter.from(o) : o, ignoredProperties);
            }
            generator.writeEnd();
            return;
        } else if (collection) {
            generator.writeStartArray(key);
            for (final Object o : Collection.class.cast(value)) {
                writeItem(itemConverter != null ? itemConverter.from(o) : o, ignoredProperties);
            }
            generator.writeEnd();
            return;
        } else if (map) {
            generator.writeStartObject(key);
            writeMapBody((Map<?, ?>) value, itemConverter);
            generator.writeEnd();
            return;
        } else if (primitive) {
            writePrimitives(key, type, value);
            return;
        } else {
            final Adapter converter = config.findAdapter(type);
            if (converter != null) {
                final Object adapted = doConvertFrom(value, converter);
                if (writePrimitives(key, adapted.getClass(), adapted)) {
                    return;
                }
                writeValue(String.class, true, false, false, false, null, key, adapted, null, ignoredProperties, null);
                return;
            } else {

                ObjectConverter.Writer objectConverterToUse = objectConverter;
                if (objectConverterToUse == null) {
                    objectConverterToUse = config.findObjectConverterWriter(type);
                }

                if (objectConverterToUse != null) {
                    generator.writeStartObject(key);
                    objectConverterToUse.writeJson(value, this);
                    generator.writeEnd();
                    return;
                }
            }
            if (writePrimitives(key, type, value)) {
                return;
            }
            generator.writeStartObject(key);
            doWriteObjectBody(value, ignoredProperties, jsonPointer);
            generator.writeEnd();
        }
    }

    private void writeItem(final Object o, final Collection<String> ignoredProperties) {
        if (!writePrimitives(o)) {
            if (Collection.class.isInstance(o)) {
                doWriteIterable(Collection.class.cast(o), ignoredProperties);
            } else if (o != null && o.getClass().isArray()) {
                final int length = Array.getLength(o);
                if (length > 0 || !config.isSkipEmptyArray()) {
                    generator.writeStartArray();
                    for (int i = 0; i < length; i++) {
                        Object t = Array.get(o, i);
                        if (t == null) {
                            generator.writeNull();
                        } else {
                            writeItem(t, ignoredProperties);
                        }
                    }
                    generator.writeEnd();
                }
            } else if (o == null) {
                generator.writeNull();
            } else {
                doWriteObject(o, generator, true, ignoredProperties);
            }
        }
    }

    private <T> void doWriteIterable(final Iterable<T> object, final Collection<String> ignoredProperties) {
        if (object == null) {
            generator.writeStartArray().writeEnd();
        } else {
            generator.writeStartArray();
            for (final T t : object) {
                if (JsonValue.class.isInstance(t)) {
                    generator.write(JsonValue.class.cast(t));
                } else {
                    if (t == null) {
                        generator.writeNull();
                    } else {
                        writeItem(t, ignoredProperties);
                    }
                }
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

}
