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
package org.apache.johnzon.jsonb;

import org.apache.johnzon.mapper.util.ArrayUtil;
import org.apache.johnzon.jsonb.api.experimental.JsonbExtension;
import org.apache.johnzon.mapper.JsonObjectGenerator;
import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperException;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class JohnzonJsonb implements Jsonb, AutoCloseable, JsonbExtension {
    private final Mapper delegate;
    private final boolean ijson;
    private final Consumer<JohnzonJsonb> onClose;
    private final Map<Class<?>, Boolean> structureAwareIo = new ConcurrentHashMap<>();

    public JohnzonJsonb(final Mapper build, final boolean ijson, final Consumer<JohnzonJsonb> onClose) {
        this.delegate = build;
        this.ijson = ijson;
        this.onClose = onClose;
    }

    @Override
    public <T> T fromJson(final String str, final Class<T> type) throws JsonbException {
        try {
            if (isArray(type)) {
                return delegate.readTypedArray(new StringReader(str), type.getComponentType(), type);
            } else if (JsonArray.class == type) {
                return (T) delegate.readJsonArray(new StringReader(str));
            } else if (Collection.class.isAssignableFrom(type)) {
                return (T) delegate.readCollection(new StringReader(str), new JohnzonParameterizedType(type, Object.class));
            }
            final Type mappingType = unwrapPrimitiveOptional(type);
            final Object object = delegate.readObject(str, mappingType);
            if (mappingType != type) {
                return wrapPrimitiveOptional(object, type);
            }
            return (T) object;
        } catch (final MapperException me) {
            throw new JsonbException(me.getMessage(), me);
        }
    }

    private <T> T wrapPrimitiveOptional(final Object object, final Type type) {
        if (OptionalDouble.class == type) {
            if (object == null) {
                return (T) OptionalDouble.empty();
            }
            return (T) OptionalDouble.of(Number.class.cast(object).doubleValue());
        } else if (OptionalInt.class == type) {
            if (object == null) {
                return (T) OptionalInt.empty();
            }
            return (T) OptionalInt.of(Number.class.cast(object).intValue());
        } else if (OptionalLong.class == type) {
            if (object == null) {
                return (T) OptionalLong.empty();
            }
            return (T) OptionalLong.of(Number.class.cast(object).longValue());
        }
        // Optional
        return (T) Optional.ofNullable(object);
    }

    private Type unwrapPrimitiveOptional(final Type type) {
        if (OptionalDouble.class == type) {
            return double.class;
        } else if (OptionalInt.class == type) {
            return int.class;
        } else if (OptionalLong.class == type) {
            return long.class;
        } else if (ParameterizedType.class.isInstance(type)) {
            final ParameterizedType pt = ParameterizedType.class.cast(type);
            if (Optional.class == pt.getRawType()) {
                return pt.getActualTypeArguments()[0];
            }
        }
        return type;
    }

    @Override
    public <T> T fromJson(final String str, final Type runtimeType) throws JsonbException {
        try {
            if (isArray(runtimeType)) {
                final Class cast = Class.class.cast(runtimeType);
                return (T) delegate.readTypedArray(new StringReader(str), cast.getComponentType(), cast);
            } else if (JsonArray.class == runtimeType) {
                return (T) delegate.readJsonArray(new StringReader(str));
            } else if (isCollection(runtimeType)) {
                return (T) delegate.readCollection(new StringReader(str), toCollectionType(runtimeType));
            }
            final Type mappingType = unwrapPrimitiveOptional(runtimeType);
            final Object object = delegate.readObject(str, mappingType);
            if (mappingType != runtimeType) {
                return wrapPrimitiveOptional(object, runtimeType);
            }
            return (T) object;
        } catch (final MapperException me) {
            throw new JsonbException(me.getMessage(), me);
        }
    }

    @Override
    public <T> T fromJson(final Reader reader, final Class<T> type) throws JsonbException {
        final boolean valueProvider = isValueProvider(reader);
        try {
            if (isArray(type)) {
                if (valueProvider) {
                    return delegate.readObject(((Supplier<JsonValue>) reader).get(), type);
                }
                return delegate.readTypedArray(reader, type.getComponentType(), type);
            } else if (JsonArray.class == type) {
                if (valueProvider) {
                    return delegate.readObject(((Supplier<JsonValue>) reader).get(), type);
                }
                return (T) delegate.readJsonArray(reader);
            } else if (Collection.class.isAssignableFrom(type)) {
                if (valueProvider) {
                    return delegate.readObject(((Supplier<JsonValue>) reader).get(), type);
                }
                return (T) delegate.readCollection(reader, new JohnzonParameterizedType(type, Object.class));
            }
            if (valueProvider) {
                return delegate.readObject(((Supplier<JsonValue>) reader).get(), type);
            }

            final Type mappingType = unwrapPrimitiveOptional(type);
            final Object object = delegate.readObject(reader, mappingType);
            if (mappingType != type) {
                return wrapPrimitiveOptional(object, type);
            }
            return (T) object;
        } catch (final MapperException me) {
            throw new JsonbException(me.getMessage(), me);
        }
    }

    @Override
    public <T> T fromJson(final Reader reader, final Type runtimeType) throws JsonbException {
        if (isValueProvider(reader)) {
            return delegate.readObject(((Supplier<JsonStructure>) reader).get(), runtimeType);
        }

        try {
            if (isArray(runtimeType)) {
                final Class<T> type = Class.class.cast(runtimeType);
                return delegate.readTypedArray(reader, type.getComponentType(), type);
            } else if (JsonArray.class == runtimeType) {
                return (T) delegate.readJsonArray(reader);
            } else if (isCollection(runtimeType)) {
                return (T) delegate.readCollection(reader, toCollectionType(runtimeType));
            }
            final Type mappingType = unwrapPrimitiveOptional(runtimeType);
            final Object object = delegate.readObject(reader, mappingType);
            if (mappingType != runtimeType) {
                return wrapPrimitiveOptional(object, runtimeType);
            }
            return (T) object;
        } catch (final MapperException me) {
            throw new JsonbException(me.getMessage(), me);
        }
    }

    @Override
    public <T> T fromJson(final InputStream stream, final Class<T> type) throws JsonbException {
        try {
            if (isArray(type)) {
                return delegate.readTypedArray(stream, type.getComponentType(), type);
            } else if (JsonArray.class == type) {
                return (T) delegate.readJsonArray(stream);
            } else if (Collection.class.isAssignableFrom(type)) {
                return (T) delegate.readCollection(stream, new JohnzonParameterizedType(type, Object.class));
            }
            final Type mappingType = unwrapPrimitiveOptional(type);
            final Object object = delegate.readObject(stream, mappingType);
            if (mappingType != type) {
                return wrapPrimitiveOptional(object, type);
            }
            return (T) object;
        } catch (final MapperException me) {
            throw new JsonbException(me.getMessage(), me);
        }
    }

    @Override
    public <T> T fromJson(final InputStream stream, final Type runtimeType) throws JsonbException {
        try {
            if (isArray(runtimeType)) {
                final Class<T> type = Class.class.cast(runtimeType);
                return delegate.readTypedArray(stream, type.getComponentType(), type);
            } else if (JsonArray.class == runtimeType) {
                return (T) delegate.readJsonArray(stream);
            } else if (isCollection(runtimeType)) {
                return (T) delegate.readCollection(stream, toCollectionType(runtimeType));
            }

            final Type mappingType = unwrapPrimitiveOptional(runtimeType);
            final Object object = delegate.readObject(stream, mappingType);
            if (mappingType != runtimeType) {
                return wrapPrimitiveOptional(object, runtimeType);
            }
            return (T) object;
        } catch (final MapperException me) {
            throw new JsonbException(me.getMessage(), me);
        }
    }

    @Override
    public String toJson(final Object inObject) throws JsonbException {
        try {
            final Object object = unwrapOptional(inObject);
            if (object == null) {
                return "null";
            }
            if (isArray(object.getClass())) {
                return delegate.writeArrayAsString(toArray(object));
            } else if (Collection.class.isInstance(object)) {
                return delegate.writeArrayAsString(Collection.class.cast(object));
            } else if (ijson && isNotObjectOrArray(object)) {
                throw new JsonbException("I-JSON mode only accepts arrays and objects as root instances");
            }
            return delegate.writeObjectAsString(object);

        } catch (final MapperException me) {
            throw new JsonbException(me.getMessage(), me);
        }
    }

    private Object[] toArray(final Object object) {
        final Class<?> componentType = object.getClass().getComponentType();
        Object[] array;
        if (int.class == componentType) {
            final int length = ArrayUtil.getArrayLength(object);
            array = new Integer[length];
            for (int i = 0; i < length; i++) {
                array[i] = ((int[])object)[i];
            }
        } else if (double.class == componentType) {
            final int length = ArrayUtil.getArrayLength(object);
            array = new Double[length];
            for (int i = 0; i < length; i++) {
                array[i] = ((double[])object)[i];
            }
        } else if (byte.class == componentType) {
            final int length = ArrayUtil.getArrayLength(object);
            array = new Byte[length];
            for (int i = 0; i < length; i++) {
                array[i] = ((byte[])object)[i];
            }
        } else if (char.class == componentType) {
            final int length = ArrayUtil.getArrayLength(object);
            array = new Character[length];
            for (int i = 0; i < length; i++) {
                array[i] = ((char[])object)[i];
            }
        } else if (float.class == componentType) {
            final int length = ArrayUtil.getArrayLength(object);
            array = new Float[length];
            for (int i = 0; i < length; i++) {
                array[i] = ((float[])object)[i];
            }
        } else if (long.class == componentType) {
            final int length = ArrayUtil.getArrayLength(object);
            array = new Long[length];
            for (int i = 0; i < length; i++) {
                array[i] = ((long[])object)[i];
            }
        } else if (short.class == componentType) {
            final int length = ArrayUtil.getArrayLength(object);
            array = new Short[length];
            for (int i = 0; i < length; i++) {
                array[i] = ((short[])object)[i];
            }
        } else if (boolean.class == componentType) {
            final int length = ArrayUtil.getArrayLength(object);
            array = new Boolean[length];
            for (int i = 0; i < length; i++) {
                array[i] = ((boolean[])object)[i];
            }
        } else {
            array = (Object[]) object;
        }
        return array;
    }

    @Override
    public String toJson(final Object inObject, final Type runtimeType) throws JsonbException {
        final Object object = unwrapOptional(inObject);
        if (object != null && isArray(runtimeType)) {
            return delegate.writeArrayAsString(toArray(object));
        } else if (isCollection(runtimeType)) {
            return delegate.writeArrayAsString(Collection.class.cast(object));
        } else if (ijson && isNotObjectOrArray(object)) {
            throw new JsonbException("I-JSON mode only accepts arrays and objects as root instances");
        }
        return delegate.writeObjectAsString(object);
    }

    @Override
    public void toJson(final Object inObject, final Writer writer) throws JsonbException {
        if (isValueConsumer(writer)) {
            Consumer.class.cast(writer).accept(delegate.toStructure(inObject));
            return;
        }

        final Object object = unwrapOptional(inObject);
        if (object != null && isArray(object.getClass())) {
            delegate.writeArray((Object[]) object, writer);
        } else if (Collection.class.isInstance(object)) {
            delegate.writeArray(Collection.class.cast(object), writer);
        } else if (ijson && isNotObjectOrArray(object)) {
            throw new JsonbException("I-JSON mode only accepts arrays and objects as root instances");
        } else {
            delegate.writeObject(object, writer);
        }
    }

    @Override
    public void toJson(final Object inObject, final Type runtimeType, final Writer writer) throws JsonbException {
        if (isValueConsumer(writer)) {
            Consumer.class.cast(writer).accept(delegate.toStructure(inObject));
            return;
        }

        final Object object = unwrapOptional(inObject);
        if (object != null && isArray(runtimeType)) {
            delegate.writeArray((Object[]) object, writer);
        } else if (isCollection(runtimeType)) {
            delegate.writeArray(Collection.class.cast(object), writer);
        } else if (ijson && isNotObjectOrArray(object)) {
            throw new JsonbException("I-JSON mode only accepts arrays and objects as root instances");
        } else {
            delegate.writeObject(object, writer);
        }
    }

    @Override
    public void toJson(final Object inObject, final OutputStream stream) throws JsonbException {
        final Object object = unwrapOptional(inObject);
        if (object != null && isArray(object.getClass())) {
            delegate.writeArray(toArray(object), stream);
        } else if (Collection.class.isInstance(object)) {
            delegate.writeArray(Collection.class.cast(object), stream);
        } else if (ijson && isNotObjectOrArray(object)) {
            throw new JsonbException("I-JSON mode only accepts arrays and objects as root instances");
        } else {
            delegate.writeObject(object, stream);
        }
    }

    @Override
    public void toJson(final Object inObject, final Type runtimeType, final OutputStream stream) throws JsonbException {
        final Object object = unwrapOptional(inObject);
        if (object != null && isArray(runtimeType)) {
            delegate.writeArray((Object[]) object, stream);
        } else if (isCollection(runtimeType)) {
            delegate.writeArray(Collection.class.cast(object), stream);
        } else if (ijson && isNotObjectOrArray(object)) {
            throw new JsonbException("I-JSON mode only accepts arrays and objects as root instances");
        } else {
            delegate.writeObject(object, stream);
        }
    }

    private boolean isNotObjectOrArray(final Object object) {
        if (String.class.isInstance(object) || Number.class.isInstance(object) || Boolean.class.isInstance(object)) {
            return true;
        }
        if (JsonValue.class.isInstance(object)) {
            switch (JsonValue.class.cast(object).getValueType()) {
                case ARRAY:
                case OBJECT:
                    return false;
                default:
                    return true;
            }
        }
        return false;
    }

    private Object unwrapOptional(final Object inObject) {
        if (Optional.class.isInstance(inObject)) {
            return Optional.class.cast(inObject).orElse(null);
        }
        if (OptionalInt.class.isInstance(inObject)) {
            final OptionalInt optionalInt = OptionalInt.class.cast(inObject);
            return optionalInt.isPresent() ? optionalInt.getAsInt() : null;
        }
        if (OptionalLong.class.isInstance(inObject)) {
            final OptionalLong optionalLong = OptionalLong.class.cast(inObject);
            return optionalLong.isPresent() ? optionalLong.getAsLong() : null;
        }
        if (OptionalDouble.class.isInstance(inObject)) {
            final OptionalDouble optionalDouble = OptionalDouble.class.cast(inObject);
            return optionalDouble.isPresent() ? optionalDouble.getAsDouble() : null;
        }
        return inObject;
    }

    private boolean isArray(final Type runtimeType) {
        return Class.class.isInstance(runtimeType) && Class.class.cast(runtimeType).isArray();
    }

    private boolean isCollection(final Type runtimeType) {
        if (!ParameterizedType.class.isInstance(runtimeType)) {
            return runtimeType == List.class || runtimeType == Set.class ||
                    runtimeType == SortedSet.class || runtimeType == Collection.class;
        }
        final Type rawType = ParameterizedType.class.cast(runtimeType).getRawType();
        return Class.class.isInstance(rawType) && Collection.class.isAssignableFrom(Class.class.cast(rawType));
    }

    @Override
    public void close() {
        try {
            delegate.close();
        } finally {
            if (onClose != null) {
                onClose.accept(this);
            }
        }
    }

    @Override
    public <T> T fromJsonValue(final JsonValue json, final Class<T> type) {
        return fromJsonValue(json, Type.class.cast(type));
    }

    @Override
    public JsonValue toJsonValue(final Object object) {
        return toJsonValue(object, object.getClass());
    }

    @Override
    public <T> T fromJsonValue(final JsonValue json, final Type type) {
        switch (json.getValueType()) {
            case NULL:
                if (Class.class.isInstance(type) && Class.class.cast(type).isPrimitive()) {
                    throw new JsonbException("can't map a primritive to null");
                }
                return null;
            case STRING:
                if (String.class != type) {
                    throw new JsonbException("STRING json can't be casted to " + type);
                }
                return (T) JsonString.class.cast(json).getString();
            case TRUE:
            case FALSE:
                if (Boolean.class != type && boolean.class != type) {
                    throw new JsonbException("TRUE and FALSE json can't be casted to " + type);
                }
                return (T) Boolean.valueOf(JsonValue.ValueType.TRUE == json.getValueType());
            case NUMBER:
                if (!Class.class.isInstance(type) || !Number.class.isAssignableFrom(Class.class.cast(type))) {
                    throw new JsonbException("NUMBER json can't be casted to " + type);
                }
                final JsonNumber jsonNumber = JsonNumber.class.cast(json);
                if (int.class == type || Integer.class == type) {
                    return (T) Integer.valueOf(jsonNumber.intValue());
                }
                if (long.class == type || Long.class == type) {
                    return (T) Long.valueOf(jsonNumber.longValue());
                }
                if (double.class == type || Double.class == type) {
                    return (T) Double.valueOf(jsonNumber.doubleValue());
                }
                if (float.class == type || Float.class == type) {
                    return (T) Float.valueOf((float) jsonNumber.doubleValue());
                }
                if (byte.class == type || Byte.class == type) {
                    return (T) Byte.valueOf((byte) jsonNumber.intValue());
                }
                if (short.class == type || Short.class == type) {
                    return (T) Short.valueOf((short) jsonNumber.intValue());
                }
                if (BigInteger.class == type) {
                    return (T) jsonNumber.bigIntegerValue();
                }
                return (T) jsonNumber.bigDecimalValue();
            case OBJECT:
            case ARRAY:
                return delegate.readObject(JsonStructure.class.cast(json), type);
            default:
                throw new JsonbException("Unsupported type: " + json.getValueType());
        }
    }

    @Override
    public JsonValue toJsonValue(final Object rawObject, final Type runtimeType) {
        if (JsonValue.class.isInstance(rawObject)) {
            return JsonValue.class.cast(rawObject);
        }
        try (final JsonObjectGenerator jsonObjectGenerator = new JsonObjectGenerator(delegate.getBuilderFactory())) {
            delegate.writeObjectWithGenerator(unwrapOptional(rawObject), jsonObjectGenerator);
            jsonObjectGenerator.flush();
            return jsonObjectGenerator.getResult();
        }
    }

    @Override
    public <T> T fromJson(final JsonParser json, final Class<T> type) {
        return type.cast(fromJson(json, Type.class.cast(type)));
    }

    @Override
    public <T> T fromJson(final JsonParser parser, final Type runtimeType) {
        try {
            if (isArray(runtimeType)) {
                final Class<T> type = Class.class.cast(runtimeType);
                return delegate.readTypedArray(parser, type.getComponentType(), type);
            } else if (JsonArray.class == runtimeType) {
                return (T) delegate.readJsonArray(parser);
            } else if (isCollection(runtimeType)) {
                return (T) delegate.readCollection(parser, ParameterizedType.class.cast(runtimeType));
            }
            final Type mappingType = unwrapPrimitiveOptional(runtimeType);
            final Object object = delegate.readObject(parser, mappingType);
            if (mappingType != runtimeType) {
                return wrapPrimitiveOptional(object, runtimeType);
            }
            return (T) object;
        } catch (final MapperException me) {
            throw new JsonbException(me.getMessage(), me);
        }
    }

    @Override
    public void toJson(final Object object, final JsonGenerator jsonGenerator) {
        delegate.writeObjectWithGenerator(unwrapOptional(object), jsonGenerator);
    }

    @Override
    public void toJson(final Object object, final Type runtimeType, final JsonGenerator jsonGenerator) {
        toJson(object, jsonGenerator);
    }

    private boolean isValueProvider(final Reader reader) {
        final Class<? extends Reader> key = reader.getClass();
        Boolean exists = structureAwareIo.get(key);
        if (exists == null) {
            exists = matchesType(key, Supplier.class);
            structureAwareIo.putIfAbsent(key, exists);
        }
        return exists;
    }

    private boolean isValueConsumer(final Writer writer) {
        final Class<? extends Writer> key = writer.getClass();
        Boolean exists = structureAwareIo.get(key);
        if (exists == null) {
            exists = matchesType(writer.getClass(), Consumer.class);
            structureAwareIo.putIfAbsent(key, exists);
        }
        return exists;
    }

    private boolean matchesType(final Class<?> type, final Class<?> rawType) {
        return rawType.isAssignableFrom(type) &&
                Stream.of(type.getGenericInterfaces())
                        .filter(ParameterizedType.class::isInstance)
                        .map(ParameterizedType.class::cast)
                        .anyMatch(pt -> pt.getRawType() == rawType &&
                                pt.getActualTypeArguments().length == 1 &&
                                Class.class.isInstance(pt.getActualTypeArguments()[0]) &&
                                JsonValue.class.isAssignableFrom(Class.class.cast(pt.getActualTypeArguments()[0])));
    }

    private ParameterizedType toCollectionType(final Type runtimeType) {
        if (ParameterizedType.class.isInstance(runtimeType)) {
            return ParameterizedType.class.cast(runtimeType);
        }
        return new JohnzonParameterizedType(runtimeType, Object.class);
    }
}
