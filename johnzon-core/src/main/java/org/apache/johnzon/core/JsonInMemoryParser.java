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

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.stream.JsonLocation;

class JsonInMemoryParser extends JohnzonJsonParserImpl {

    private final SimpleStack<Iterator<Event>> stack = new SimpleStack<Iterator<Event>>();
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;

    private Event currentEvent;
    private JsonValue currentValue;
    private int arrayDepth = 0;
    private int objectDepth = 0;

    private class ArrayIterator implements Iterator<Event> {

        private final Iterator<JsonValue> aentries;
        private Boolean end = null;

        public ArrayIterator(final JsonArray ja) {
            aentries = ja.iterator();

        }

        @Override
        public boolean hasNext() {
            return !Boolean.TRUE.equals(end);
        }

        @Override
        public Event next() {

            if (end == null) {
                end = Boolean.FALSE;
                return Event.START_ARRAY;
            } else if (!aentries.hasNext()) {

                if (!stack.isEmpty()) {
                    stack.pop();
                }

                end = Boolean.TRUE;

                return Event.END_ARRAY;
            } else {

                final JsonValue val = aentries.next();

                final ValueType vt = val.getValueType();

                if (vt == ValueType.OBJECT) {
                    stack.push(new ObjectIterator((JsonObject) val));
                    return stack.peek().next();

                } else if (vt == ValueType.ARRAY) {
                    stack.push(new ArrayIterator((JsonArray) val));
                    return stack.peek().next();

                } else {
                    currentValue = val;
                    return getEvent(vt);
                }
            }

        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();

        }

    }

    private class ObjectIterator implements Iterator<Event> {

        private final Iterator<Map.Entry<String, JsonValue>> oentries;
        private JsonValue jsonValue;
        private Boolean end = null;

        public ObjectIterator(final JsonObject jo) {
            oentries = jo.entrySet().iterator();

        }

        @Override
        public boolean hasNext() {
            return !Boolean.TRUE.equals(end);
        }

        @Override
        public Event next() {

            if (end == null) {
                end = Boolean.FALSE;
                return Event.START_OBJECT;
            } else if (jsonValue == null && !oentries.hasNext()) {

                if (!stack.isEmpty()) {
                    stack.pop();
                }

                end = Boolean.TRUE;

                return Event.END_OBJECT;
            } else if (jsonValue == null) {

                final Map.Entry<String, JsonValue> tmp = oentries.next();
                jsonValue = tmp.getValue();
                currentValue = new JsonStringImpl(tmp.getKey());
                return Event.KEY_NAME;

            } else {

                final ValueType vt = jsonValue.getValueType();

                if (vt == ValueType.OBJECT) {
                    stack.push(new ObjectIterator((JsonObject) jsonValue));
                    jsonValue = null;
                    return stack.peek().next();

                } else if (vt == ValueType.ARRAY) {
                    stack.push(new ArrayIterator((JsonArray) jsonValue));
                    jsonValue = null;
                    return stack.peek().next();

                } else {

                    final Event ret = getEvent(vt);
                    currentValue = jsonValue;
                    jsonValue = null;
                    return ret;
                }

            }

        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();

        }

    }

    @Override
    public Event currentEvent() {
        return currentEvent;
    }

    @Override
    public Event current() {
        if (currentEvent == null && hasNext()) {
            internalNext();
        }
        return currentEvent;
    }

    @Override
    protected boolean isInArray() {
        return arrayDepth > 0;
    }

    @Override
    protected boolean isInObject() {
        return objectDepth > 0;
    }

    @Override
    protected BufferStrategy.BufferProvider<char[]> getCharArrayProvider() {
        return bufferProvider;
    }

    private static Event getEvent(final ValueType value) {

        switch (value) {
        case NUMBER:
            return Event.VALUE_NUMBER;
        case STRING:
            return Event.VALUE_STRING;
        case FALSE:
            return Event.VALUE_FALSE;
        case NULL:
            return Event.VALUE_NULL;
        case TRUE:
            return Event.VALUE_TRUE;
        default:
            throw new IllegalArgumentException(value + " not supported");

        }

    }

    JsonInMemoryParser(final JsonObject object, final BufferStrategy.BufferProvider<char[]> bufferProvider,
                      final JsonProviderImpl provider) {
        super(provider);
        stack.push(new ObjectIterator(object));
        this.bufferProvider = bufferProvider;
    }

    JsonInMemoryParser(final JsonArray array, final BufferStrategy.BufferProvider<char[]> bufferProvider,
                       final JsonProviderImpl provider) {
        super(provider);
        stack.push(new ArrayIterator(array));
        this.bufferProvider = bufferProvider;
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    protected Event internalNext() {

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        currentEvent = stack.peek().next();

        if (currentEvent == Event.START_ARRAY) {
            arrayDepth++;
        } else if (currentEvent == Event.END_ARRAY) {
            arrayDepth--;
        }
        if (currentEvent == Event.START_OBJECT) {
            objectDepth++;
        } else if (currentEvent == Event.END_OBJECT) {
            objectDepth--;
        }

        return currentEvent;
    }

    @Override
    public String getString() {
        if (currentEvent != Event.KEY_NAME && currentEvent != Event.VALUE_STRING) {
            throw new IllegalStateException("String is for numbers and strings");
        }
        return JsonString.class.cast(currentValue).getString();
    }

    @Override
    public boolean isIntegralNumber() {
        if (currentEvent != Event.VALUE_NUMBER) {
            throw new IllegalStateException("isIntegralNumber is for numbers");
        }
        return JsonNumber.class.cast(currentValue).isIntegral();
    }

    @Override
    public boolean isNotTooLong() {
        return true;
    }

    @Override
    public int getInt() {
        if (currentEvent != Event.VALUE_NUMBER) {
            throw new IllegalStateException("getInt is for numbers");
        }
        return JsonNumber.class.cast(currentValue).intValue();
    }

    @Override
    public long getLong() {
        if (currentEvent != Event.VALUE_NUMBER) {
            throw new IllegalStateException("getLong is for numbers");
        }
        return JsonNumber.class.cast(currentValue).longValue();
    }

    @Override
    public boolean isFitLong() {
        return JsonLongImpl.class.isInstance(currentValue);
    }

    @Override
    public BigDecimal getBigDecimal() {
        if (currentEvent != Event.VALUE_NUMBER) {
            throw new IllegalStateException("getBigDecimal is for numbers");
        }
        return JsonNumber.class.cast(currentValue).bigDecimalValue();
    }

    @Override
    public JsonLocation getLocation() { // no location for in memory parsers
        return JsonLocationImpl.UNKNOWN_LOCATION;
    }

    @Override
    public void close() {
        // no-op
    }

}
