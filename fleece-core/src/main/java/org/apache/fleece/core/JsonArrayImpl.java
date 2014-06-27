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
package org.apache.fleece.core;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class JsonArrayImpl extends LinkedList<JsonValue> implements JsonArray {
    private Integer hashCode = null;

    private <T> T value(final int idx, final Class<T> type) {
        if (idx > size()) {
            throw new IndexOutOfBoundsException(idx + "/" + size());
        }
        return type.cast(get(idx));
    }

    @Override
    public JsonObject getJsonObject(final int index) {
        return value(index, JsonObject.class);
    }

    @Override
    public JsonArray getJsonArray(final int index) {
        return value(index, JsonArray.class);
    }

    @Override
    public JsonNumber getJsonNumber(final int index) {
        return value(index, JsonNumber.class);
    }

    @Override
    public JsonString getJsonString(final int index) {
        return value(index, JsonString.class);
    }

    @Override
    public <T extends JsonValue> List<T> getValuesAs(final Class<T> clazz) {
        return (List<T>) this;
    }

    @Override
    public String getString(final int index) {
        return value(index, JsonString.class).getString();
    }

    @Override
    public String getString(final int index, final String defaultValue) {
        try {
            return getString(index);
        } catch (final IndexOutOfBoundsException ioobe) {
            return defaultValue;
        }
    }

    @Override
    public int getInt(final int index) {
        return value(index, JsonNumber.class).intValue();
    }

    @Override
    public int getInt(final int index, final int defaultValue) {
        try {
            return getInt(index);
        } catch (final IndexOutOfBoundsException ioobe) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(final int index) {
        return value(index, JsonValue.class) == JsonValue.TRUE;
    }

    @Override
    public boolean getBoolean(final int index, final boolean defaultValue) {
        try {
            return getBoolean(index);
        } catch (final IndexOutOfBoundsException ioobe) {
            return defaultValue;
        }
    }

    @Override
    public boolean isNull(final int index) {
        return value(index, JsonValue.class) == JsonValue.NULL;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.ARRAY;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("[");
        final Iterator<JsonValue> it = iterator();
        boolean hasNext = it.hasNext();
        while (hasNext) {
            final JsonValue jsonValue = it.next();
            if (JsonString.class.isInstance(jsonValue)) {
                builder.append(JsonChars.QUOTE).append(jsonValue.toString()).append(JsonChars.QUOTE);
            } else {
                builder.append(jsonValue != JsonValue.NULL ? jsonValue.toString() : JsonChars.NULL);
            }
            hasNext = it.hasNext();
            if (hasNext) {
                builder.append(",");
            }
        }
        return builder.append(']').toString();
    }

    @Override
    public boolean equals(final Object obj) {
        return JsonArray.class.isInstance(obj) && super.equals(obj);
    }

    //make protected if class is supposed to be subclassed
    //make package private otherwise
    protected void addInternal(final JsonValue value) {
        super.add(value);
    }

    @Override
    public boolean add(final JsonValue element) {
        throw immutable();
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends JsonValue> c) {
        throw immutable();
    }

    @Override
    public boolean remove(final Object o) {
        throw immutable();
    }

    @Override
    public JsonValue remove(final int index) {
        throw immutable();
    }

    @Override
    public void add(final int index, final JsonValue element) {
        throw immutable();
    }

    @Override
    public void clear() {
        throw immutable();
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw immutable();
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw immutable();
    }

    @Override
    public JsonValue set(final int index, final JsonValue element) {
        throw immutable();
    }

    private static UnsupportedOperationException immutable() {
        throw new UnsupportedOperationException("JsonArray is immutable. You can create another one thanks to JsonArrayBuilder");
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = super.hashCode();
        }
        return hashCode;
    }
}
