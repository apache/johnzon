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

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;

// we don't use visitor pattern to ensure we work with other impl of JsonObject and JsonArray
class JsonInMemoryParser implements JsonParser {
    private final Iterator<Entry> iterator;

    private Entry next = null;

    JsonInMemoryParser(final JsonObject object) {
        final List<Entry> events = new LinkedList<Entry>();
        generateObjectEvents(events, object);
        iterator = events.iterator();
    }

    JsonInMemoryParser(final JsonArray array) {
        final List<Entry> events = new LinkedList<Entry>();
        generateArrayEvents(events, array);
        iterator = events.iterator();
    }

    private static void generateObjectEvents(final List<Entry> events, final JsonObject object) {
        events.add(new Entry(Event.START_OBJECT, null));
        for (final Map.Entry<String, JsonValue> entry : object.entrySet()) {
            events.add(new Entry(Event.KEY_NAME, new JsonStringImpl(entry.getKey())));
            final JsonValue value = entry.getValue();
            addValueEvents(events, value);
        }
        events.add(new Entry(Event.END_OBJECT, null));
    }

    private static void generateArrayEvents(final List<Entry> events, final JsonArray array) {
        events.add(new Entry(Event.START_ARRAY, null));
        for (final JsonValue value : array) {
            addValueEvents(events, value);
        }
        events.add(new Entry(Event.END_ARRAY, null));
    }

    private static void addValueEvents(final List<Entry> events, final JsonValue value) {
        
        switch(value.getValueType()) {
            case ARRAY:
                generateArrayEvents(events, JsonArray.class.cast(value));
                break;
            case OBJECT:
                generateObjectEvents(events, JsonObject.class.cast(value));
                break;
            case NUMBER:
                events.add(new Entry(Event.VALUE_NUMBER, value));
                break;
            case STRING:
                events.add(new Entry(Event.VALUE_STRING, value));
                break;
            case FALSE:
                events.add(new Entry(Event.VALUE_FALSE, null));
                break;
            case NULL:
                events.add(new Entry(Event.VALUE_NULL, null));
                break;
            case TRUE:
                events.add(new Entry(Event.VALUE_TRUE, null));
                break;
            default: throw new IllegalArgumentException(value + " not supported");
                
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
        if (next.event != Event.KEY_NAME && next.event != Event.VALUE_STRING) {
            throw new IllegalStateException("String is for numbers and strings");
        }
        return JsonString.class.cast(next.value).getString();
    }

    @Override
    public boolean isIntegralNumber() {
        if (next.event != Event.VALUE_NUMBER) {
            throw new IllegalStateException("isIntegralNumber is for numbers");
        }
        return JsonNumber.class.cast(next.value).isIntegral();
    }

    @Override
    public int getInt() {
        if (next.event != Event.VALUE_NUMBER) {
            throw new IllegalStateException("getInt is for numbers");
        }
        return JsonNumber.class.cast(next.value).intValue();
    }

    @Override
    public long getLong() {
        if (next.event != Event.VALUE_NUMBER) {
            throw new IllegalStateException("getLong is for numbers");
        }
        return JsonNumber.class.cast(next.value).longValue();
    }

    @Override
    public BigDecimal getBigDecimal() {
        if (next.event != Event.VALUE_NUMBER) {
            throw new IllegalStateException("getBigDecimal is for numbers");
        }
        return JsonNumber.class.cast(next.value).bigDecimalValue();
    }

    @Override
    public JsonLocation getLocation() { // no location for in memory parsers
        return JsonLocationImpl.UNKNOW_LOCATION;
    }

    @Override
    public void close() {
        // no-op
    }

    private static class Entry {
        final Event event;
        final JsonValue value;

        Entry(final Event event, final JsonValue value) {
            this.event = event;
            this.value = value;
        }
    }
}
