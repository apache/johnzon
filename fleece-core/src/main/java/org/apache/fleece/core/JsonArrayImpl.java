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

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

class JsonArrayImpl extends AbstractList<JsonValue> implements JsonArray, Serializable {
    private Integer hashCode = null;
    private final List<JsonValue> unmodifieableBackingList;

    JsonArrayImpl(List<JsonValue> backingList) {
        super();
        this.unmodifieableBackingList = backingList;
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
        final Iterator<JsonValue> it = unmodifieableBackingList.iterator();
        boolean hasNext = it.hasNext();
        while (hasNext) {
            final JsonValue jsonValue = it.next();
            if (JsonString.class.isInstance(jsonValue)) {
                builder.append(jsonValue.toString());
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
        return JsonArrayImpl.class.isInstance(obj) && unmodifieableBackingList.equals(JsonArrayImpl.class.cast(obj).unmodifieableBackingList);
    }

    
    @Override
    public int hashCode() {
        Integer h=hashCode;
        if (h == null) {
            h = unmodifieableBackingList.hashCode();
            h=hashCode;
        }
        return h;
    }

    @Override
    public JsonValue get(int index) {
        return unmodifieableBackingList.get(index);
    }

    @Override
    public int size() {
        return unmodifieableBackingList.size();
    }
}
