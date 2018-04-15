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

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


final class JsonObjectImpl extends AbstractMap<String, JsonValue> implements JsonObject, Serializable {
    private transient Integer hashCode = null;
    private final Map<String, JsonValue> unmodifieableBackingMap;

    private <T> T value(final String name, final Class<T> clazz) {
        final JsonValue v = unmodifieableBackingMap.get(name);
        if (v != null) {
            return clazz.cast(v);
        }
        return null;
    }

    private <T> T valueOrExcpetion(final String name, final Class<T> clazz) {
        T value = value(name, clazz);
        if (value == null) {
            throw new NullPointerException("no mapping for " + name);
        }

        return value;
    }

    JsonObjectImpl(final Map<String, JsonValue> backingMap) {
        super();
        this.unmodifieableBackingMap = backingMap;
    }

    @Override
    public JsonArray getJsonArray(final String name) {
        return value(name, JsonArray.class);
    }

    @Override
    public JsonObject getJsonObject(final String name) {
        return value(name, JsonObject.class);
    }

    @Override
    public JsonNumber getJsonNumber(final String name) {
        return value(name, JsonNumber.class);
    }

    @Override
    public JsonString getJsonString(final String name) {
        return value(name, JsonString.class);
    }

    @Override
    public String getString(final String name) {
        return valueOrExcpetion(name, JsonString.class).getString();
    }

    @Override
    public String getString(final String name, final String defaultValue) {
        final Object v = value(name, JsonValue.class);
        if (v != null && v instanceof JsonString) {
            return JsonString.class.cast(v).getString();
        }

        return defaultValue;
    }

    @Override
    public int getInt(final String name) {
        return valueOrExcpetion(name, JsonNumber.class).intValue();
    }

    @Override
    public int getInt(final String name, final int defaultValue) {
        final Object v = value(name, JsonValue.class);
        if (v != null && v instanceof JsonNumber) {
            return JsonNumber.class.cast(v).intValue();
        }

        return defaultValue;
    }

    @Override
    public boolean getBoolean(final String name) {
        final JsonValue obj = valueOrExcpetion(name, JsonValue.class);
        if (JsonValue.TRUE == obj) {
            return true;
        }
        if (JsonValue.FALSE == obj) {
            return false;
        }
        throw new ClassCastException("Wrong value for a boolean: " + obj);
    }

    @Override
    public boolean getBoolean(final String name, final boolean defaultValue) {
        final Object v = value(name, JsonValue.class);
        if (v != null) {
            return JsonValue.TRUE.equals(v) || !JsonValue.FALSE.equals(v) && defaultValue;
        }
        return defaultValue;
    }

    @Override
    public boolean isNull(final String name) {
        return JsonValue.NULL.equals(valueOrExcpetion(name, JsonValue.class));
    }

    @Override
    public ValueType getValueType() {
        return ValueType.OBJECT;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("{");
        final Iterator<Map.Entry<String, JsonValue>> it = unmodifieableBackingMap.entrySet().iterator();
        boolean hasNext = it.hasNext();
        while (hasNext) {
            final Map.Entry<String, JsonValue> entry = it.next();

            builder.append('"').append(entry.getKey()).append("\":");

            final JsonValue value = entry.getValue();
            if (JsonString.class.isInstance(value)) {
                builder.append(value.toString());
            } else {
                builder.append(value != JsonValue.NULL ? value.toString() : JsonChars.NULL);
            }

            hasNext = it.hasNext();
            if (hasNext) {
                builder.append(",");
            }
        }
        return builder.append('}').toString();
    }

    @Override
    public boolean equals(final Object obj) {
        return JsonObjectImpl.class.isInstance(obj)
                && unmodifieableBackingMap.equals(JsonObjectImpl.class.cast(obj).unmodifieableBackingMap);
    }

    @Override
    public int hashCode() {
        Integer h = hashCode;
        if (h == null) {
            h = unmodifieableBackingMap.hashCode();
            hashCode = h;
        }
        return h;
    }

    @Override
    public Set<java.util.Map.Entry<String, JsonValue>> entrySet() {
        return unmodifieableBackingMap.entrySet();
    }

    private Object writeReplace() throws ObjectStreamException {
        return new SerializableValue(toString());
    }
}
