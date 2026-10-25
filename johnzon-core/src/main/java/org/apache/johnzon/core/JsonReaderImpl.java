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
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParsingException;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.util.Arrays;

public class JsonReaderImpl implements JsonReader {
    private final JohnzonJsonParser parser;
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;
    private final JsonProviderImpl provider;
    private final RejectDuplicateKeysMode rejectDuplicateKeysMode;
    private boolean closed = false;

    private final boolean subStreamReader;

    public JsonReaderImpl(final JsonParser parser, final BufferStrategy.BufferProvider<char[]> bufferProvider,
                          final RejectDuplicateKeysMode rejectDuplicateKeysMode, final JsonProviderImpl provider) {
        this(parser, false, bufferProvider, rejectDuplicateKeysMode, provider);
    }

    /**
     * @param parser json parser.
     * @param subStreamReader {@code true} if the Stream already got started and the first
     *           operation should not be next() but {@link JohnzonJsonParser#current()} instead.
     * @param bufferProvider buffer provider for toString of created instances.
     */
    public JsonReaderImpl(final JsonParser parser, boolean subStreamReader,
                          final BufferStrategy.BufferProvider<char[]> bufferProvider,
                          final RejectDuplicateKeysMode rejectDuplicateKeys, final JsonProviderImpl provider) {
        this.bufferProvider = bufferProvider;
        this.provider = provider;
        if (parser instanceof JohnzonJsonParser) {
            this.parser = (JohnzonJsonParser) parser;
        } else {
            this.parser = new JohnzonJsonParser.JohnzonJsonParserWrapper(parser);
        }

        this.subStreamReader = subStreamReader;
        this.rejectDuplicateKeysMode = rejectDuplicateKeys;
    }

    @Override
    public JsonStructure read() {
        return JsonStructure.class.cast(readValue());
    }

    @Override
    public JsonValue readValue() {
        checkClosed();

        if (!parser.hasNext()) {
            throw new NothingToRead();
        }


        final JsonParser.Event next;
        if (subStreamReader) {
            next = parser.current();
        } else {
            next = parser.next();
        }

        switch (next) {
            case START_OBJECT:
                final JsonObject object = JsonObject.class.cast(parseStructure(next));
                if (!subStreamReader && parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return object;
            case START_ARRAY:
                final JsonArray array = JsonArray.class.cast(parseStructure(next));
                if (!subStreamReader && parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return array;
            case VALUE_STRING:
                final JsonStringImpl string = new JsonStringImpl(parser.getString());
                if (!subStreamReader && parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return string;
            case VALUE_FALSE:
                if (!subStreamReader && parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return JsonValue.FALSE;
            case VALUE_TRUE:
                if (!subStreamReader && parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return JsonValue.TRUE;
            case VALUE_NULL:
                if (!subStreamReader && parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return JsonValue.NULL;
            case VALUE_NUMBER:
                final JsonNumber number;
                if (parser.isFitLong()) {
                    number = new JsonLongImpl(parser.getLong());
                } else {
                    number = new JsonNumberImpl(parser.getBigDecimal(), provider::checkBigDecimalScale);
                }
                if (!subStreamReader && parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return number;
            default:
                close();
                throw new JsonParsingException("Unknown structure: " + next, parser.getLocation());
        }
    }

    @Override
    public JsonObject readObject() {
        final JsonStructure read = read();
        checkType(JsonObject.class, read);
        return JsonObject.class.cast(read);
    }

    @Override
    public JsonArray readArray() {
        final JsonStructure read = read();
        checkType(JsonArray.class, read);
        return JsonArray.class.cast(read);
    }

    private void checkType(final Class<?> expected, final JsonStructure read) {
        if (!expected.isInstance(read)) {
            throw new JsonParsingException("Expecting " + expected + " but got " + read, parser.getLocation());
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            parser.close();
        }
    }

    // IMPORTANT: keep it iterative and not recursive to use the heap
    private JsonValue parseStructure(final JsonParser.Event first) {
        final StructureStack parents = new StructureStack();
        Object current = isObject(first) ?
                new JsonObjectBuilderImpl(emptyMap(), bufferProvider, rejectDuplicateKeysMode, provider) :
                new JsonArrayBuilderImpl(emptyList(), bufferProvider, rejectDuplicateKeysMode, provider);
        String currentKey = null;
        while (parser.hasNext()) {
            final JsonParser.Event next = parser.next();
            switch (next) {
                case START_OBJECT:
                case START_ARRAY:
                    parents.push(current, currentKey);
                    current = isObject(next) ?
                            new JsonObjectBuilderImpl(emptyMap(), bufferProvider, rejectDuplicateKeysMode, provider) :
                            new JsonArrayBuilderImpl(emptyList(), bufferProvider, rejectDuplicateKeysMode, provider);
                    currentKey = null;
                    break;

                case KEY_NAME:
                    if (!(current instanceof JsonObjectBuilderImpl)) {
                        throw new JsonParsingException("array doesn't have keys", parser.getLocation());
                    }
                    currentKey = parser.getString();
                    break;

                case VALUE_STRING:
                    add(current, currentKey, new JsonStringImpl(parser.getString()));
                    break;

                case VALUE_NUMBER:
                    if (parser.isFitLong()) {
                        add(current, currentKey, new JsonLongImpl(parser.getLong()));
                    } else {
                        add(current, currentKey, new JsonNumberImpl(parser.getBigDecimal(), provider::checkBigDecimalScale));
                    }
                    break;

                case VALUE_NULL:
                    if (current instanceof JsonObjectBuilderImpl) {
                        JsonObjectBuilderImpl.class.cast(current).addNull(currentKey);
                    } else {
                        JsonArrayBuilderImpl.class.cast(current).addNull();
                    }
                    break;

                case VALUE_TRUE:
                    add(current, currentKey, JsonValue.TRUE);
                    break;

                case VALUE_FALSE:
                    add(current, currentKey, JsonValue.FALSE);
                    break;

                case END_OBJECT:
                    if (!(current instanceof JsonObjectBuilderImpl)) {
                        throw new JsonParsingException("'}', shouldn't occur", parser.getLocation());
                    }
                    final JsonObject builtObject = JsonObjectBuilderImpl.class.cast(current).build();
                    if (parents.isEmpty()) {
                        return builtObject;
                    }
                    parents.pop();
                    current = parents.builder;
                    currentKey = parents.key;
                    if (current instanceof JsonObjectBuilderImpl) {
                        JsonObjectBuilderImpl.class.cast(current).add(currentKey, builtObject);
                    } else {
                        JsonArrayBuilderImpl.class.cast(current).add(builtObject);
                    }
                    break;

                case END_ARRAY:
                    if (!(current instanceof JsonArrayBuilderImpl)) {
                        throw new JsonParsingException("']', shouldn't occur", parser.getLocation());
                    }
                    final JsonArray builtArray = JsonArrayBuilderImpl.class.cast(current).build();
                    if (parents.isEmpty()) {
                        return builtArray;
                    }
                    parents.pop();
                    current = parents.builder;
                    currentKey = parents.key;
                    if (current instanceof JsonObjectBuilderImpl) {
                        JsonObjectBuilderImpl.class.cast(current).add(currentKey, builtArray);
                    } else {
                        JsonArrayBuilderImpl.class.cast(current).add(builtArray);
                    }
                    break;

                default:
                    throw new JsonParsingException(next.name() + ", shouldn't occur", parser.getLocation());
            }
        }
        throw new JsonParsingException("Unexpected end of structure", parser.getLocation());
    }

    private static boolean isObject(final JsonParser.Event event) {
        return event == JsonParser.Event.START_OBJECT;
    }

    private static void add(final Object builder, final String key, final JsonValue value) {
        if (builder instanceof JsonObjectBuilderImpl) {
            JsonObjectBuilderImpl.class.cast(builder).add(key, value);
        } else {
            JsonArrayBuilderImpl.class.cast(builder).add(value);
        }
    }

    // backing by arrays enables to limit the memory impact of this "wrapper"
    private static final class StructureStack {
        private Object[] builders = new Object[16];
        private String[] keys = new String[16];
        private int depth;
        private Object builder;
        private String key;

        private boolean isEmpty() {
            return depth == 0;
        }

        private void push(final Object builder, final String key) {
            if (depth == builders.length) {
                builders = Arrays.copyOf(builders, builders.length << 1);
                keys = Arrays.copyOf(keys, keys.length << 1);
            }
            builders[depth] = builder;
            keys[depth] = key;
            depth++;
        }

        private void pop() {
            depth--;
            builder = builders[depth];
            key = keys[depth];
            builders[depth] = null;
            keys[depth] = null;
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("read(), readObject(), readArray() or close() method was already called");
        }

    }

    public static class NothingToRead extends IllegalStateException {
        public NothingToRead() {
            super("Nothing to read");
        }
    }
}
