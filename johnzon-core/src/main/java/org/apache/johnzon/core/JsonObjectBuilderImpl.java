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

import static java.util.Objects.requireNonNull;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.johnzon.core.util.ArrayUtil;

class JsonObjectBuilderImpl implements JsonObjectBuilder, Serializable {
    private RejectDuplicateKeysMode rejectDuplicateKeysMode;
    private JsonProviderImpl provider;
    private BufferStrategy.BufferProvider<char[]> bufferProvider;
    private Map<String, JsonValue> attributeMap = new LinkedHashMap<>();

    protected JsonObjectBuilderImpl() {
        // no-op: serialization
    }

    public JsonObjectBuilderImpl(final JsonObject initialData,
                                 final BufferStrategy.BufferProvider<char[]> bufferProvider,
                                 final RejectDuplicateKeysMode rejectDuplicateKeysMode,
                                 final JsonProviderImpl provider) {
        this.bufferProvider = bufferProvider;
        this.rejectDuplicateKeysMode = rejectDuplicateKeysMode;
        this.provider = provider;
        this.attributeMap = new LinkedHashMap<>(initialData);
    }

    public JsonObjectBuilderImpl(final Map<String, Object> initialValues,
                                 final BufferStrategy.BufferProvider<char[]> bufferProvider,
                                 final RejectDuplicateKeysMode rejectDuplicateKeysMode,
                                 final JsonProviderImpl provider) {
        this.bufferProvider = bufferProvider;
        this.rejectDuplicateKeysMode = rejectDuplicateKeysMode;
        this.provider = provider;
        if (!initialValues.isEmpty()) {
            for (Map.Entry<String, Object> entry : initialValues.entrySet()) {
                add(entry.getKey(), entry.getValue());
            }
        }
    }


    /**
     * Internal method to add a value where we do not yet know the type at compile time.
     */
    public void add(final String name, final Object value) {
        if (value instanceof JsonValue) {
            add(name, (JsonValue) value);
        } else if (value instanceof BigDecimal) {
            add(name, (BigDecimal) value);
        } else if (value instanceof BigInteger) {
            add(name, (BigInteger) value);
        } else if (value instanceof Boolean) {
            add(name, (boolean) value);
        } else if (value instanceof Double) {
            add(name, (double) value);
        } else if (value instanceof Integer) {
            add(name, (int) value);
        } else if (value instanceof Long) {
            add(name, (long) value);
        } else if (value instanceof String) {
            add(name, (String) value);
        } else if (value == null) {
            addNull(name);
        } else if (value instanceof Map) {
            add(name, new JsonObjectBuilderImpl(Map.class.cast(value), bufferProvider, rejectDuplicateKeysMode, provider).build());
        } else if (value instanceof Collection) {
            add(name, new JsonArrayBuilderImpl(Collection.class.cast(value), bufferProvider, rejectDuplicateKeysMode, provider).build());
        } else if (value.getClass().isArray()) {
            final Collection<Object> collection = ArrayUtil.newCollection(value);
            add(name, new JsonArrayBuilderImpl(collection, bufferProvider, rejectDuplicateKeysMode, provider).build());
        } else {
            throw new JsonException("Illegal JSON type! name=" + name + " type=" + value.getClass());
        }
    }

    @Override
    public JsonObjectBuilder add(final String name, final JsonValue value) {
        putValue(name, value);
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final String value) {
        putValue(name, new JsonStringImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final BigInteger value) {
        putValue(name, new JsonNumberImpl(new BigDecimal(value), provider::checkBigDecimalScale));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final BigDecimal value) {
        putValue(name, new JsonNumberImpl(value, provider::checkBigDecimalScale));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final int value) {
        putValue(name, new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final long value) {
        putValue(name, new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final double value) {
        putValue(name, new JsonDoubleImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final boolean value) {
        putValue(name, value ? JsonValue.TRUE : JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonObjectBuilder addNull(final String name) {
        putValue(name, JsonValue.NULL);
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final JsonObjectBuilder builder) {
        putValue(name, builder.build());
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final JsonArrayBuilder builder) {
        putValue(name, builder.build());
        return this;
    }

    @Override
    public JsonObjectBuilder addAll(JsonObjectBuilder builder) {
        attributeMap.putAll(builder.build());
        return this;
    }

    @Override
    public JsonObjectBuilder remove(String name) {
        attributeMap.remove(requireNonNull(name));
        return this;
    }

    private void putValue(final String name, final JsonValue value) {
        if(name == null) {
            throw new NullPointerException("name must not be null");
        }
        if(value == null) {
            throw new NullPointerException("value/builder must not be null for name: " + name);
        }
        rejectDuplicateKeysMode.put().put(attributeMap, name, value);
    }


    @Override
    public JsonObject build() {

        if(attributeMap == null || attributeMap.isEmpty()) {
            return JsonValue.EMPTY_JSON_OBJECT;
        } else {
            Map<String, JsonValue> dump = Collections.unmodifiableMap(new LinkedHashMap<>(attributeMap));

            attributeMap.clear();
            return new JsonObjectImpl(dump, bufferProvider);
        }
    }
}
