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
package org.apache.johnzon.core;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.johnzon.core.util.ArrayUtil;

class JsonArrayBuilderImpl implements JsonArrayBuilder, Serializable {
    private RejectDuplicateKeysMode rejectDuplicateKeysMode;
    private JsonProviderImpl jsonProvider;
    private List<JsonValue> tmpList;
    private BufferStrategy.BufferProvider<char[]> bufferProvider;

    protected JsonArrayBuilderImpl() {
        // no-op: serialization
    }

    public JsonArrayBuilderImpl(final JsonArray initialData,
                                final BufferStrategy.BufferProvider<char[]> provider,
                                final RejectDuplicateKeysMode rejectDuplicateKeysMode,
                                final JsonProviderImpl jsonProvider) {
        this.tmpList = new ArrayList<>(initialData);
        this.bufferProvider = provider;
        this.rejectDuplicateKeysMode = rejectDuplicateKeysMode;
        this.jsonProvider = jsonProvider;
    }

    public JsonArrayBuilderImpl(final Collection<?> initialData, final BufferStrategy.BufferProvider<char[]> provider,
                                final RejectDuplicateKeysMode rejectDuplicateKeysMode, final JsonProviderImpl jsonProvider) {
        this.bufferProvider = provider;
        this.rejectDuplicateKeysMode = rejectDuplicateKeysMode;
        this.tmpList = new ArrayList<>();
        if (!initialData.isEmpty()) {
            for (Object initialValue : initialData) {
                add(initialValue);
            }
        }
        this.jsonProvider = jsonProvider;
    }

    @Override
    public JsonArrayBuilder addAll(final JsonArrayBuilder builder) {
        builder.build().forEach(this::add);
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int index, final JsonValue value) {
        addValue(index, value);
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int index, final String value) {
        addValue(index, new JsonStringImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int index, final BigDecimal value) {
        addValue(index, new JsonNumberImpl(value, jsonProvider::checkBigDecimalScale));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int index, final BigInteger value) {
        addValue(index, new JsonNumberImpl(new BigDecimal(value), jsonProvider::checkBigDecimalScale));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int index, final int value) {
        addValue(index, new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int index, final long value) {
        addValue(index, new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int index, final double value) {
        addValue(index, new JsonDoubleImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int index, final boolean value) {
        addValue(index, value ? JsonValue.TRUE : JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonArrayBuilder addNull(final int index) {
        addValue(index, JsonValue.NULL);
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int index, final JsonObjectBuilder builder) {
        addValue(index, builder.build());
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int index, final JsonArrayBuilder builder) {
        addValue(index, builder.build());
        return this;
    }

    @Override
    public JsonArrayBuilder set(final int index, final JsonValue value) {
        setValue(index, value);
        return this;
    }

    @Override
    public JsonArrayBuilder set(final int index, final String value) {
        setValue(index, new JsonStringImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder set(final int index, final BigDecimal value) {
        setValue(index, new JsonNumberImpl(value, jsonProvider::checkBigDecimalScale));
        return this;
    }

    @Override
    public JsonArrayBuilder set(final int index, final BigInteger value) {
        setValue(index, new JsonNumberImpl(new BigDecimal(value), jsonProvider::checkBigDecimalScale));
        return this;
    }

    @Override
    public JsonArrayBuilder set(final int index, final int value) {
        setValue(index, new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder set(final int index, final long value) {
        setValue(index, new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder set(final int index, final double value) {
        setValue(index, new JsonDoubleImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder set(final int index, final boolean value) {
        setValue(index, value ? JsonValue.TRUE : JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonArrayBuilder setNull(final int index) {
        setValue(index, JsonValue.NULL);
        return this;
    }

    @Override
    public JsonArrayBuilder set(final int index, final JsonObjectBuilder builder) {
        setValue(index, builder.build());
        return this;
    }

    @Override
    public JsonArrayBuilder set(final int index, final JsonArrayBuilder builder) {
        setValue(index, builder.build());
        return this;
    }

    @Override
    public JsonArrayBuilder remove(final int index) {
        tmpList.remove(index);
        return this;
    }

    public JsonArrayBuilder add(final Object value) {
        if (value instanceof JsonValue) {
            add((JsonValue) value);
        } else if (value instanceof BigDecimal) {
            add((BigDecimal) value);
        } else if (value instanceof BigInteger) {
            add((BigInteger) value);
        } else if (value instanceof Boolean) {
            add((boolean) value);
        } else if (value instanceof Double) {
            add((double) value);
        } else if (value instanceof Integer) {
            add((int) value);
        } else if (value instanceof Long) {
            add((long) value);
        } else if (value instanceof String) {
            add((String) value);
        } else if (value instanceof Map) {
            add(new JsonObjectBuilderImpl(Map.class.cast(value), bufferProvider, rejectDuplicateKeysMode, jsonProvider).build());
        } else if (value instanceof Collection) {
            add(new JsonArrayBuilderImpl(Collection.class.cast(value), bufferProvider, rejectDuplicateKeysMode, jsonProvider).build());
        } else if (value.getClass().isArray()) {
            final Collection<Object> collection = ArrayUtil.newCollection(value);
            add(new JsonArrayBuilderImpl(collection, bufferProvider, rejectDuplicateKeysMode, jsonProvider).build());
        } else {
            throw new JsonException("Illegal JSON type! type=" + value.getClass());
        }

        return this;
    }

        @Override
    public JsonArrayBuilder add(final JsonValue value) {
        addValue(value);
        return this;
    }

    @Override
    public JsonArrayBuilder add(final String value) {
        addValue(new JsonStringImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final BigDecimal value) {
        addValue(new JsonNumberImpl(value, jsonProvider::checkBigDecimalScale));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final BigInteger value) {
        addValue(new JsonNumberImpl(new BigDecimal(value), jsonProvider::checkBigDecimalScale));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int value) {
        addValue(new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final long value) {
        addValue(new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final double value) {
        addValue(new JsonDoubleImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final boolean value) {
        addValue(value ? JsonValue.TRUE : JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonArrayBuilder addNull() {
        addValue(JsonValue.NULL);
        return this;
    }

    @Override
    public JsonArrayBuilder add(final JsonObjectBuilder builder) {
        addValue(builder.build());
        return this;
    }

    @Override
    public JsonArrayBuilder add(final JsonArrayBuilder builder) {
        addValue(builder.build());
        return this;
    }

    private void setValue(int idx, JsonValue value) {
        if (value == null || tmpList == null) {
            throw npe();
        }
        tmpList.set(idx, value);
    }

    private void addValue(JsonValue value) {
        if (value == null) {
            throw npe();
        }

        if(tmpList==null){
            tmpList=new ArrayList<>();
        }

        tmpList.add(value);
    }

    private void addValue(int idx, JsonValue value) {
        if (value == null) {
            throw npe();
        }

        if(tmpList==null){
            tmpList=new ArrayList<>();
        }

        tmpList.add(idx, value);
    }

    @Override
    public JsonArray build() {
        if(tmpList == null) {
            return JsonValue.EMPTY_JSON_ARRAY;
        }
        return new JsonArrayImpl(Collections.unmodifiableList(tmpList), bufferProvider);
    }

    private static NullPointerException npe() {
        throw new NullPointerException("value/builder must not be null");
    }
}
