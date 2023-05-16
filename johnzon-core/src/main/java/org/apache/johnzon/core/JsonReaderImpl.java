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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParsingException;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class JsonReaderImpl implements JsonReader {
    private final JohnzonJsonParser parser;
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;
    private JsonProviderImpl provider;
    private final RejectDuplicateKeysMode rejectDuplicateKeysMode;
    private boolean closed = false;

    private boolean subStreamReader;

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
                final JsonObjectBuilder objectBuilder = new JsonObjectBuilderImpl(emptyMap(), bufferProvider, rejectDuplicateKeysMode, provider);
                parseObject(objectBuilder);
                if (!subStreamReader && parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return objectBuilder.build();
            case START_ARRAY:
                final JsonArrayBuilder arrayBuilder = new JsonArrayBuilderImpl(emptyList(), bufferProvider, rejectDuplicateKeysMode, provider);
                parseArray(arrayBuilder);
                if (!subStreamReader && parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return arrayBuilder.build();
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

    private void parseObject(final JsonObjectBuilder builder) {
        String key = null;
        while (parser.hasNext()) {
            final JsonParser.Event next = parser.next();
            switch (next) {
                case KEY_NAME:
                    key = parser.getString();
                    break;

                case VALUE_STRING:
                    builder.add(key, new JsonStringImpl(parser.getString()));
                    break;

                case START_OBJECT:
                    JsonObjectBuilder subObject = new JsonObjectBuilderImpl(emptyMap(), bufferProvider, rejectDuplicateKeysMode, provider);
                    parseObject(subObject);
                    builder.add(key, subObject);
                    break;

                case START_ARRAY:
                    JsonArrayBuilder subArray = new JsonArrayBuilderImpl(emptyList(), bufferProvider, rejectDuplicateKeysMode, provider);
                    parseArray(subArray);
                    builder.add(key, subArray);
                    break;

                case VALUE_NUMBER:
                    if (parser.isIntegralNumber() && parser.isNotTooLong()) {
                        builder.add(key, new JsonLongImpl(parser.getLong()));
                    } else {
                        builder.add(key, new JsonNumberImpl(parser.getBigDecimal(), provider::checkBigDecimalScale));
                    }
                    break;

                case VALUE_NULL:
                    builder.addNull(key);
                    break;

                case VALUE_TRUE:
                    builder.add(key, true);
                    break;

                case VALUE_FALSE:
                    builder.add(key, false);
                    break;

                case END_OBJECT:
                    return;

                case END_ARRAY:
                    throw new JsonParsingException("']', shouldn't occur", parser.getLocation());

                default:
                    throw new JsonParsingException(next.name() + ", shouldn't occur", parser.getLocation());
            }
        }
    }

    private void parseArray(final JsonArrayBuilder builder) {
        while (parser.hasNext()) {
            final JsonParser.Event next = parser.next();
            switch (next) {
                case VALUE_STRING:
                    builder.add(new JsonStringImpl(parser.getString()));
                    break;

                case VALUE_NUMBER:
                    if (parser.isFitLong()) {
                        builder.add(new JsonLongImpl(parser.getLong()));
                    } else {
                        builder.add(new JsonNumberImpl(parser.getBigDecimal(), provider::checkBigDecimalScale));
                    }
                    break;

                case START_OBJECT:
                    JsonObjectBuilder subObject = new JsonObjectBuilderImpl(emptyMap(), bufferProvider, rejectDuplicateKeysMode, provider);
                    parseObject(subObject);
                    builder.add(subObject);
                    break;

                case START_ARRAY:
                    JsonArrayBuilder subArray = null;
                    parseArray(subArray = new JsonArrayBuilderImpl(emptyList(), bufferProvider, rejectDuplicateKeysMode, provider));
                    builder.add(subArray);
                    break;

                case END_ARRAY:
                    return;

                case VALUE_NULL:
                    builder.addNull();
                    break;

                case VALUE_TRUE:
                    builder.add(true);
                    break;

                case VALUE_FALSE:
                    builder.add(false);
                    break;

                case KEY_NAME:
                    throw new JsonParsingException("array doesn't have keys", parser.getLocation());

                case END_OBJECT:
                    throw new JsonParsingException("'}', shouldn't occur", parser.getLocation());

                default:
                    throw new JsonParsingException(next.name() + ", shouldn't occur", parser.getLocation());
            }
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
