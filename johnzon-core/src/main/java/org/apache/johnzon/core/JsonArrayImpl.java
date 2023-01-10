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
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerator;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class JsonArrayImpl extends AbstractList<JsonValue> implements JsonArray, Serializable {
    private final BufferStrategy.BufferProvider<char[]> provider;
    private Integer hashCode = null;
    private final List<JsonValue> unmodifieableBackingList;
    private int size = -1;

    JsonArrayImpl(final List<JsonValue> backingList, final BufferStrategy.BufferProvider<char[]> provider) {
        this.unmodifieableBackingList = backingList;
        this.provider = provider;
    }

    private <T> T value(final int idx, final Class<T> type) {
        if (idx > unmodifieableBackingList.size()) {
            throw new IndexOutOfBoundsException(idx + "/" + unmodifieableBackingList.size());
        }
        return type.cast(unmodifieableBackingList.get(idx));
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
        return (List<T>) unmodifieableBackingList;
    }

    @Override
    public String getString(final int index) {
        return value(index, JsonString.class).getString();
    }

    @Override
    public String getString(final int index, final String defaultValue) {
        JsonValue val = null;
        int s = size;

        if (s == -1) {
            s = unmodifieableBackingList.size();
            size = s;
        }

        if (index > s - 1 || !((val = get(index)) instanceof JsonString)) {
            return defaultValue;
        } else {
            return JsonString.class.cast(val).getString();
        }
    }

    @Override
    public int getInt(final int index) {
        return value(index, JsonNumber.class).intValue();
    }

    @Override
    public int getInt(final int index, final int defaultValue) {
        JsonValue val = null;
        int s = size;

        if (s == -1) {
            s = unmodifieableBackingList.size();
            size = s;
        }

        if (index > s - 1 || !((val = get(index)) instanceof JsonNumber)) {
            return defaultValue;
        } else {
            return JsonNumber.class.cast(val).intValue();
        }
    }

    @Override
    public boolean getBoolean(final int index) {
        final JsonValue val = value(index, JsonValue.class);

        if (JsonValue.ValueType.TRUE == val.getValueType()) {
            return true;
        } else if (JsonValue.ValueType.FALSE == val.getValueType()) {
            return false;
        } else {
            throw new ClassCastException();
        }

    }

    @Override
    public boolean getBoolean(final int index, final boolean defaultValue) {

        int s = size;

        if (s == -1) {
            s = unmodifieableBackingList.size();
            size = s;
        }

        if (index > s - 1) {
            return defaultValue;
        }

        ValueType valueType = get(index).getValueType();
        return JsonValue.ValueType.TRUE == valueType || JsonValue.ValueType.FALSE != valueType && defaultValue;
    }

    @Override
    public boolean isNull(final int index) {
        return JsonValue.ValueType.NULL == value(index, JsonValue.class).getValueType();
    }

    @Override
    public ValueType getValueType() {
        return ValueType.ARRAY;
    }

    @Override
    public String toString() {
        if (unmodifieableBackingList.isEmpty()) {
            return "[]";
        }
        final StringWriter writer = new StringWriter(2048);
        try (final JsonGenerator generator = new JsonGeneratorImpl(writer, provider, false)) {
            generator.writeStartArray();
            unmodifieableBackingList.forEach(generator::write);
            generator.writeEnd();
        }
        return writer.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        return List.class.isInstance(obj) && super.equals(obj);
    }

    @Override
    public int hashCode() {
        Integer h = hashCode;
        if (h == null) {
            h = unmodifieableBackingList.hashCode();
            hashCode = h;
        }
        return h;
    }

    @Override
    public JsonValue get(final int index) {
        return unmodifieableBackingList.get(index);
    }

    @Override
    public int size() {
        return unmodifieableBackingList.size();
    }

    private Object writeReplace() throws ObjectStreamException {
        return new SerializableValue(toString());
    }

    @Override
    public Iterator<JsonValue> iterator() {
        return new JsonArrayIterator();
    }

    /**
     * We don't need any range check, so we can simplify the Iterator logic.
     * get() on an ArrayList works even faster than ArrayList#iterator!
     */
    private class JsonArrayIterator implements Iterator<JsonValue> {
        private int cursor = 0;

        @Override
        public boolean hasNext() {
            return cursor != unmodifieableBackingList.size();
        }

        @Override
        public JsonValue next() {
            try {
                int i = cursor;
                JsonValue next = unmodifieableBackingList.get(i);
                cursor = i + 1;
                return next;
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }
    }
}
