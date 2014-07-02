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
import javax.json.stream.JsonLocation;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// we don't use visitor pattern to ensure we work with other impl of JsonObject and JsonArray
public class JsonInMemoryParser implements EscapedStringAwareJsonParser {
    private final Iterator<Entry> iterator;

    private Entry next = null;

    public JsonInMemoryParser(final JsonObject object) {
        final List<Entry> events = new LinkedList<Entry>();
        generateObjectEvents(events, object);
        iterator = events.iterator();
    }

    public JsonInMemoryParser(final JsonArray array) {
        final List<Entry> events = new LinkedList<Entry>();
        generateArrayEvents(events, array);
        iterator = events.iterator();
    }

    private static void generateObjectEvents(final List<Entry> events, final JsonObject object) {
        events.add(new Entry(Event.START_OBJECT, object));
        for (final Map.Entry<String, JsonValue> entry : object.entrySet()) {
            events.add(new Entry(Event.KEY_NAME, new JsonStringImpl(entry.getKey())));
            final JsonValue value = entry.getValue();
            addValueEvents(events, value);
        }
        events.add(new Entry(Event.END_OBJECT, object));
    }

    private static void generateArrayEvents(final List<Entry> events, final JsonArray array) {
        events.add(new Entry(Event.START_ARRAY, array));
        for (final JsonValue value : array) {
            addValueEvents(events, value);
        }
        events.add(new Entry(Event.END_ARRAY, array));
    }

    private static void addValueEvents(final List<Entry> events, final JsonValue value) {
        if (JsonArray.class.isInstance(value)) {
            generateArrayEvents(events, JsonArray.class.cast(value));
        } else if (JsonObject.class.isInstance(value)) {
            generateObjectEvents(events, JsonObject.class.cast(value));
        } else if (JsonString.class.isInstance(value)) {
            events.add(new Entry(Event.VALUE_STRING, JsonValue.class.cast(value)));
        } else if (JsonNumber.class.isInstance(value)) {
            events.add(new Entry(Event.VALUE_NUMBER, JsonValue.class.cast(value)));
        } else if (value == JsonValue.TRUE) {
            events.add(new Entry(Event.VALUE_TRUE, value));
        } else if (value == JsonValue.FALSE) {
            events.add(new Entry(Event.VALUE_FALSE, value));
        } else if (value == JsonValue.NULL) {
            events.add(new Entry(Event.VALUE_NULL, value));
        } else {
            throw new IllegalArgumentException(value + " not supported");
        }
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Event next() {
        next = iterator.next();
        return next.event;
    }

    @Override
    public String getString() {
        if (JsonObject.class.isInstance(next.value) || JsonArray.class.isInstance(next.value)) {
            throw new IllegalStateException("String is for numbers and strings");
        }
        return getEscapedString();
    }

    @Override
    public boolean isIntegralNumber() {
        if (!JsonNumber.class.isInstance(next.value)) {
            throw new IllegalStateException("isIntegralNumber is for numbers");
        }
        return JsonNumber.class.cast(next.value).isIntegral();
    }

    @Override
    public int getInt() {
        if (!JsonNumber.class.isInstance(next.value)) {
            throw new IllegalStateException("getInt is for numbers");
        }
        return JsonNumber.class.cast(next.value).intValue();
    }

    @Override
    public long getLong() {
        if (!JsonNumber.class.isInstance(next.value)) {
            throw new IllegalStateException("getLong is for numbers");
        }
        return JsonNumber.class.cast(next.value).longValue();
    }

    @Override
    public BigDecimal getBigDecimal() {
        if (!JsonNumber.class.isInstance(next.value)) {
            throw new IllegalStateException("getBigDecimal is for numbers");
        }
        return JsonNumber.class.cast(next.value).bigDecimalValue();
    }

    @Override
    public JsonLocation getLocation() { // no location for in memory parsers
        return new JsonLocationImpl(-1, -1, -1);
    }

    @Override
    public String getEscapedString() {
        return JsonValue.class.cast(next.value).toString();
    }

    @Override
    public void close() {
        // no-op
    }

    private static class Entry {
        private final Event event;
        private final JsonValue value;

        private Entry(final Event event, final JsonValue value) {
            this.event = event;
            this.value = value;
        }
    }
}
