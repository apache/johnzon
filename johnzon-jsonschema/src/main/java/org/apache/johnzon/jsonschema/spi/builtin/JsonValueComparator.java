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
package org.apache.johnzon.jsonschema.spi.builtin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

final class JsonValueComparator implements Comparator<JsonValue> {
    static final Comparator<JsonValue> INSTANCE = new JsonValueComparator();

    private JsonValueComparator() {
    }

    @Override
    public int compare(final JsonValue left, final JsonValue right) {
        final int type = left.getValueType().compareTo(right.getValueType());
        if (type != 0) {
            return type;
        }
        switch (left.getValueType()) {
            case STRING:
                return JsonString.class.cast(left).getString().compareTo(JsonString.class.cast(right).getString());
            case NUMBER:
                return JsonNumber.class.cast(left).bigDecimalValue()
                        .compareTo(JsonNumber.class.cast(right).bigDecimalValue());
            case ARRAY:
                return compare(JsonArray.class.cast(left), JsonArray.class.cast(right));
            case OBJECT:
                return compare(JsonObject.class.cast(left), JsonObject.class.cast(right));
            default:
                return 0;
        }
    }

    private int compare(final JsonArray left, final JsonArray right) {
        final int size = Integer.compare(left.size(), right.size());
        if (size != 0) {
            return size;
        }
        for (int i = 0; i < left.size(); i++) {
            final int element = compare(left.get(i), right.get(i));
            if (element != 0) {
                return element;
            }
        }
        return 0;
    }

    private int compare(final JsonObject left, final JsonObject right) {
        final int size = Integer.compare(left.size(), right.size());
        if (size != 0) {
            return size;
        }
        final List<Map.Entry<String, JsonValue>> leftEntries = entries(left);
        final List<Map.Entry<String, JsonValue>> rightEntries = entries(right);
        for (int i = 0; i < leftEntries.size(); i++) {
            final Map.Entry<String, JsonValue> leftEntry = leftEntries.get(i);
            final Map.Entry<String, JsonValue> rightEntry = rightEntries.get(i);
            final int name = leftEntry.getKey().compareTo(rightEntry.getKey());
            if (name != 0) {
                return name;
            }
            final int value = compare(leftEntry.getValue(), rightEntry.getValue());
            if (value != 0) {
                return value;
            }
        }
        return 0;
    }

    private List<Map.Entry<String, JsonValue>> entries(final JsonObject object) {
        final List<Map.Entry<String, JsonValue>> entries = new ArrayList<>(object.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        return entries;
    }
}
