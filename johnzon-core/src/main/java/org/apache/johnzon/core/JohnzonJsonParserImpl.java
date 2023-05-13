/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.core;


import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * Base parser which handles higher level operations which are
 * mixtures of Reader and Parsers like {@code getObject(), getValue(), getArray()}
 */
public abstract class JohnzonJsonParserImpl implements JohnzonJsonParser {

    /**
     * @return {@code true} if we are currently inside an array
     */
    protected abstract boolean isInArray();
    /**
     * @return {@code true} if we are currently inside an object
     */
    protected abstract boolean isInObject();

    protected abstract BufferStrategy.BufferProvider<char[]> getCharArrayProvider();

    private boolean manualNext = false;

    private final JsonProviderImpl provider;

    protected JohnzonJsonParserImpl(final JsonProviderImpl provider) {
        this.provider = provider;
    }


    @Override
    public Event next() {
        manualNext = true;
        return internalNext();
    }

    protected abstract Event internalNext();

    @Override
    public JsonObject getObject() {
        Event current = current();
        if (current != Event.START_OBJECT) {
            throw new IllegalStateException(current + " doesn't support getObject()");
        }

        JsonReaderImpl jsonReader = new JsonReaderImpl(this, true, getCharArrayProvider(), RejectDuplicateKeysMode.DEFAULT, provider);
        return jsonReader.readObject();
    }


    @Override
    public JsonArray getArray() {
        Event current = current();
        if (current != Event.START_ARRAY) {
            throw new IllegalStateException(current + " doesn't support getArray()");
        }

        JsonReaderImpl jsonReader = new JsonReaderImpl(this, true, getCharArrayProvider(), RejectDuplicateKeysMode.DEFAULT, provider);
        return jsonReader.readArray();
    }

    @Override
    public JsonValue getValue() {
        Event current = current();
        switch (current) {
            case START_ARRAY:
            case START_OBJECT:
                JsonReaderImpl jsonReader = new JsonReaderImpl(this, true, getCharArrayProvider(), RejectDuplicateKeysMode.DEFAULT, provider);
                return jsonReader.readValue();
            case VALUE_TRUE:
                return JsonValue.TRUE;
            case VALUE_FALSE:
                return JsonValue.FALSE;
            case VALUE_NULL:
                return JsonValue.NULL;
            case VALUE_STRING:
            case KEY_NAME:
                return new JsonStringImpl(getString());
            case VALUE_NUMBER:
                if (isFitLong()) {
                    return new JsonLongImpl(getLong());
                }
                return new JsonNumberImpl(getBigDecimal(), provider::checkBigDecimalScale);
            default:
                throw new IllegalStateException(current + " doesn't support getValue()");
        }
    }

    @Override
    public void skipObject() {
        if (isInObject()) {
            int level = 1;
            do {
                Event event = internalNext();
                if (event == Event.START_OBJECT) {
                    level++;
                } else if (event == Event.END_OBJECT) {
                    level--;
                }
            } while (level > 0 && hasNext());
        }
    }

    @Override
    public void skipArray() {
        if (isInArray()) {
            int level = 1;
            do {
                Event event = internalNext();
                if (event == Event.START_ARRAY) {
                    level++;
                } else if (event == Event.END_ARRAY) {
                    level--;
                }
            } while (level > 0 && hasNext());
        }
    }

    private static class ArrayStreamSpliterator extends Spliterators.AbstractSpliterator<JsonValue> {

        private final JohnzonJsonParserImpl parser;

        ArrayStreamSpliterator(JohnzonJsonParserImpl parser) {
            super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED);
            this.parser = parser;
        }

        @Override
        public boolean tryAdvance(Consumer<? super JsonValue> action) {
            Event next = parser.next();

            if (next == Event.END_ARRAY) {
                return false;
            }

            action.accept(parser.getValue());
            return true;
        }
    }

    @Override
    public Stream<JsonValue> getArrayStream() {
        Event current = current();
        if (current != Event.START_ARRAY) {
            throw new IllegalStateException(current + " doesn't support getArrayStream()");
        }

        return StreamSupport.stream(new ArrayStreamSpliterator(this), false);
    }

    private static class ObjectStreamSpliterator extends Spliterators.AbstractSpliterator<Map.Entry<String,JsonValue>> {
        
        private final JohnzonJsonParserImpl parser;

        ObjectStreamSpliterator(JohnzonJsonParserImpl parser) {
            super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED);
            this.parser = parser;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Entry<String, JsonValue>> action) {
            Event next = parser.next();

            if (next == Event.END_OBJECT) {
                return false;
            }

            if (next != Event.KEY_NAME) {
                throw new IllegalStateException("Expected key name event but got " + next + " instead.");
            }

            String key = parser.getString();
            parser.next();
            JsonValue value = parser.getValue();
            action.accept(new AbstractMap.SimpleImmutableEntry<>(key, value));
            return true;
        }

    }

    @Override
    public Stream<Map.Entry<String, JsonValue>> getObjectStream() {
        Event current = current();
        if (current != Event.START_OBJECT) {
            throw new IllegalStateException(current + " doesn't support getObjectStream()");
        }

        return StreamSupport.stream(new ObjectStreamSpliterator(this), false);
    }

    @Override
    public Stream<JsonValue> getValueStream() {
        if (manualNext) {
            throw new IllegalStateException("JsonStream already got propagated manually");
        }

        Event event = internalNext();
        switch (event) {
            case START_ARRAY:
            case START_OBJECT:
            case VALUE_STRING:
            case VALUE_NUMBER:
            case VALUE_TRUE:
            case VALUE_FALSE:
            case VALUE_NULL:
                    return Collections.singletonList(getValue()).stream();
            default:
                throw new IllegalStateException(event + " doesn't support getValueStream");
        }
    }
}
