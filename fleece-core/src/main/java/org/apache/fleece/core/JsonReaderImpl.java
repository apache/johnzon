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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;

public class JsonReaderImpl implements JsonReader {
    private final EscapedStringAwareJsonParser parser;
    private final JsonReaderListenerFactory listenerFactory;
    
    public JsonReaderImpl(final InputStream in) {
        this((EscapedStringAwareJsonParser)Json.createParser(in), new JsonListenerFactory());
    }

    public JsonReaderImpl(final Reader in) {
        this((EscapedStringAwareJsonParser)Json.createParser(in), new JsonListenerFactory());
    }

    public JsonReaderImpl(final EscapedStringAwareJsonParser parser, final JsonReaderListenerFactory listenerFactory) {
        this.parser = parser;
        this.listenerFactory = listenerFactory;
    }

    @Override
    public JsonStructure read() {
        if (!parser.hasNext()) {
            throw new JsonParsingException("Nothing to read", new JsonLocationImpl(1, 1, 0));
        }
        switch (parser.next()) {
            case START_OBJECT:
                final JsonReaderListener subObject = listenerFactory.subObject();
                parseObject(subObject);
                return JsonObject.class.cast(subObject.getObject());
            case START_ARRAY:
                final JsonReaderListener subArray = listenerFactory.subArray();
                parseArray(subArray);
                return JsonArray.class.cast(subArray.getObject());
            default:
                throw new JsonParsingException("Unknown structure: " + parser.next(), parser.getLocation());
        }
    }

    @Override
    public JsonObject readObject() {
        return JsonObject.class.cast(read());
    }

    @Override
    public JsonArray readArray() {
        return JsonArray.class.cast(read());
    }

    @Override
    public void close() {
        parser.close();
    }

    private static class JsonListenerFactory implements JsonReaderListenerFactory {
        @Override
        public JsonReaderListener subObject() {
            return new JsonObjectListener();
        }

        @Override
        public JsonReaderListener subArray() {
            return new JsonArrayListener();
        }
    }

    private static class JsonObjectListener implements JsonReaderListener {
        private JsonObjectImpl object = new JsonObjectImpl();
        private String key = null;

        @Override
        public Object getObject() {
            return object;
        }

        @Override
        public void onKey(final String string) {
            key = string;
        }

        @Override
        public void onValue(final String string, final String escaped) {
            final JsonStringImpl value = new JsonStringImpl(string, escaped);
            object.putInternal(key, value);
        }

        @Override
        public void onLong(final long aLong) {
            final JsonLongImpl value = new JsonLongImpl(aLong);
            object.putInternal(key, value);
        }

        @Override
        public void onBigDecimal(final BigDecimal bigDecimal) {
            final JsonNumberImpl value = new JsonNumberImpl(bigDecimal);
            object.putInternal(key, value);
        }

        @Override
        public void onNull() {
            final JsonValue value = JsonValue.NULL;
            object.putInternal(key, value);
        }

        @Override
        public void onTrue() {
            final JsonValue value = JsonValue.TRUE;
            object.putInternal(key, value);
        }

        @Override
        public void onFalse() {
            final JsonValue value = JsonValue.FALSE;
            object.putInternal(key, value);
        }

        @Override
        public void onObject(final Object obj) {
            final JsonObject jsonObject = JsonObject.class.cast(obj);
            object.putInternal(key, jsonObject);
        }

        @Override
        public void onArray(final Object arr) {
            final JsonArray jsonArry = JsonArray.class.cast(arr);
            object.putInternal(key, jsonArry);
        }
    }

    private static class JsonArrayListener implements JsonReaderListener {
        private JsonArrayImpl array = new JsonArrayImpl();

        @Override
        public Object getObject() {
            return array;
        }

        @Override
        public void onKey(final String string) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onValue(final String string, final String escaped) {
            final JsonStringImpl value = new JsonStringImpl(string, escaped);
            array.addInternal(value);
        }

        @Override
        public void onLong(final long aLong) {
            final JsonLongImpl value = new JsonLongImpl(aLong);
            array.addInternal(value);
        }

        @Override
        public void onBigDecimal(final BigDecimal bigDecimal) {
            final JsonNumberImpl value = new JsonNumberImpl(bigDecimal);
            array.addInternal(value);
        }

        @Override
        public void onNull() {
            final JsonValue value = JsonValue.NULL;
            array.addInternal(value);
        }

        @Override
        public void onTrue() {
            final JsonValue value = JsonValue.TRUE;
            array.addInternal(value);
        }

        @Override
        public void onFalse() {
            final JsonValue value = JsonValue.FALSE;
            array.addInternal(value);
        }

        @Override
        public void onObject(final Object obj) {
            final JsonObject jsonObject = JsonObject.class.cast(obj);
            array.addInternal(jsonObject);
        }

        @Override
        public void onArray(final Object arr) {
            final JsonArray jsonArry = JsonArray.class.cast(arr);
            array.addInternal(jsonArry);
        }
    }

    private void parseObject(final JsonReaderListener listener) {
        while (parser.hasNext()) {
            final JsonParser.Event next = parser.next();
            switch (next) {
                case KEY_NAME:
                    listener.onKey(parser.getString());
                    break;

                case VALUE_STRING:
                    listener.onValue(parser.getString(), parser.getEscapedString());
                    break;

                case START_OBJECT:
                    final JsonReaderListener subListenerObject = listenerFactory.subObject();
                    parseObject(subListenerObject);
                    listener.onObject(subListenerObject.getObject());
                    break;

                case START_ARRAY:
                    final JsonReaderListener subListenerArray = listenerFactory.subArray();
                    parseArray(subListenerArray);
                    listener.onArray(subListenerArray.getObject());
                    break;

                case VALUE_NUMBER:
                    if (parser.isIntegralNumber()) {
                        listener.onLong(parser.getLong());
                    } else {
                        listener.onBigDecimal(parser.getBigDecimal());
                    }
                    break;

                case VALUE_NULL:
                    listener.onNull();
                    break;

                case VALUE_TRUE:
                    listener.onTrue();
                    break;

                case VALUE_FALSE:
                    listener.onFalse();
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

    private void parseArray(final JsonReaderListener listener) {
        while (parser.hasNext()) {
            final JsonParser.Event next = parser.next();
            switch (next) {
                case VALUE_STRING:
                    listener.onValue(parser.getString(), parser.getEscapedString());
                    break;

                case VALUE_NUMBER:
                    if (parser.isIntegralNumber()) {
                        listener.onLong(parser.getLong());
                    } else {
                        listener.onBigDecimal(parser.getBigDecimal());
                    }
                    break;

                case START_OBJECT:
                    final JsonReaderListener subListenerObject = listenerFactory.subObject();
                    parseObject(subListenerObject);
                    listener.onObject(subListenerObject.getObject());
                    break;

                case START_ARRAY:
                    final JsonReaderListener subListenerArray = listenerFactory.subArray();
                    parseArray(subListenerArray);
                    listener.onArray(subListenerArray.getObject());
                    break;

                case END_ARRAY:
                    return;

                case VALUE_NULL:
                    listener.onNull();
                    break;

                case VALUE_TRUE:
                    listener.onTrue();
                    break;

                case VALUE_FALSE:
                    listener.onFalse();
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
}
