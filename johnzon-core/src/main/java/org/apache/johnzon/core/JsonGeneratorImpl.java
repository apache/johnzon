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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;

class JsonGeneratorImpl implements JsonGenerator, JsonChars, Serializable {
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private final transient Writer writer;
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;
    private final char[] buffer;
    private int bufferPos = 0;
    //private final ConcurrentMap<String, String> cache;

    private final ArrayDeque<GeneratorState> state = new ArrayDeque<JsonGeneratorImpl.GeneratorState>(100);

    private enum GeneratorState {
        INITIAL(false, true), START_OBJECT(true, false), IN_OBJECT(true, false), AFTER_KEY(false, true), START_ARRAY(false, true), IN_ARRAY(
                false, true), END(false, false);

        private final boolean acceptsKey;
        private final boolean acceptsValue;

        private GeneratorState(final boolean acceptsKey, final boolean acceptsValue) {
            this.acceptsKey = acceptsKey;
            this.acceptsValue = acceptsValue;
        }
    }

    JsonGeneratorImpl(final Writer writer, final BufferStrategy.BufferProvider<char[]> bufferProvider,
            final ConcurrentMap<String, String> cache) {
        this.writer = writer;
        //this.cache = cache;
        this.buffer = bufferProvider.newBuffer();
        this.bufferProvider = bufferProvider;
        state.push(GeneratorState.INITIAL);
    }

    JsonGeneratorImpl(final OutputStream out, final BufferStrategy.BufferProvider<char[]> bufferProvider,
            final ConcurrentMap<String, String> cache) {
        this(new OutputStreamWriter(out, UTF8_CHARSET), bufferProvider, cache);
    }

    JsonGeneratorImpl(final OutputStream out, final Charset encoding, final BufferStrategy.BufferProvider<char[]> bufferProvider,
            final ConcurrentMap<String, String> cache) {
        this(new OutputStreamWriter(out, encoding), bufferProvider, cache);
    }

    protected void incrementDepth() {

    }

    protected void decrementDepth() {

    }

    protected void writeEol() {

    }

    protected void writeIndent() {

    }

    //caching currently disabled
    //two problems:
    // 1) not easy to get the escaped value efficiently when its streamed and the buffer is full and needs to be flushed
    // 2) we have to use a kind of bounded threadsafe map to let the cache not grow indefinitely
    private void writeCachedKey(final String name) {
        /*  String k = cache.get(name);

          if (k == null) {

                  justWrite(QUOTE_CHAR);
                  int start = bufferPos;
                  writeEscaped0(name);
                  int end = bufferPos;
                  String escaped= get from buffer
                  ---
                  //FIXME if buffer is flushed this will not work here
                  cache.putIfAbsent(name, escaped);
                  justWrite(QUOTE_CHAR);
                  justWrite(KEY_SEPARATOR);
          }else*/
        {
            justWrite(QUOTE_CHAR);
            writeEscaped0(name);
            justWrite(QUOTE_CHAR);
            justWrite(KEY_SEPARATOR);
        }

    }

    @Override
    public JsonGenerator writeStartObject() {
        prepareValue();
        state.push(GeneratorState.START_OBJECT);

        writeIndent();
        incrementDepth();
        justWrite(START_OBJECT_CHAR);

        writeEol();
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(final String name) {
        checkObject(false);
        writeKey(name);
        justWrite(START_OBJECT_CHAR);
        writeEol();
        state.push(GeneratorState.START_OBJECT);
        incrementDepth();
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        prepareValue();
        state.push(GeneratorState.START_ARRAY);
        incrementDepth();
        writeIndent();
        justWrite(START_ARRAY_CHAR);
        writeEol();
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(final String name) {
        checkObject(false);
        writeKey(name);
        justWrite(START_ARRAY_CHAR);
        writeEol();
        state.push(GeneratorState.START_ARRAY);
        incrementDepth();
        return this;
    }

    private void writeJsonValue(final String name, final JsonValue value) {
        checkObject(false);
        //TODO check null handling
        switch (value.getValueType()) {
            case ARRAY:
                writeStartArray(name);
                final JsonArray array = JsonArray.class.cast(value);
                final Iterator<JsonValue> ait = array.iterator();
                while (ait.hasNext()) {
                    write(ait.next());
                }
                writeEnd();

                break;
            case OBJECT:
                writeStartObject(name);
                final JsonObject object = JsonObject.class.cast(value);
                final Iterator<Map.Entry<String, JsonValue>> oit = object.entrySet().iterator();
                while (oit.hasNext()) {
                    final Map.Entry<String, JsonValue> keyval = oit.next();
                    write(keyval.getKey(), keyval.getValue());
                }
                writeEnd();

                break;
            case STRING:
                write(name, JsonString.class.cast(value).getString());
                break;
            case NUMBER:
                if (value instanceof JsonDoubleImpl) {
                    write(name, JsonDoubleImpl.class.cast(value).doubleValue());
                } else if (value instanceof JsonLongImpl) {
                    write(name, JsonLongImpl.class.cast(value).longValue());
                } else {
                    write(name, JsonNumber.class.cast(value).bigDecimalValue());
                }
                break;
            case TRUE:
                write(name, true);
                break;
            case FALSE:
                write(name, false);
                break;
            case NULL:
                writeNull(name);
                break;
            default:
                throw new JsonGenerationException("Unknown JsonValue type");
        }
    }

    private void writeJsonValue(final JsonValue value) {
        checkArray(true);
        //TODO check null handling
        switch (value.getValueType()) {
            case ARRAY:
                writeStartArray();
                final JsonArray array = JsonArray.class.cast(value);
                final Iterator<JsonValue> ait = array.iterator();
                while (ait.hasNext()) {
                    write(ait.next());
                }
                writeEnd();

                break;
            case OBJECT:
                writeStartObject();
                final JsonObject object = JsonObject.class.cast(value);
                final Iterator<Map.Entry<String, JsonValue>> oit = object.entrySet().iterator();
                while (oit.hasNext()) {
                    final Map.Entry<String, JsonValue> keyval = oit.next();
                    write(keyval.getKey(), keyval.getValue());
                }
                writeEnd();

                break;
            case STRING:
                write(JsonString.class.cast(value).getString());
                break;
            case NUMBER:
                if (value instanceof JsonDoubleImpl) {
                    write(JsonDoubleImpl.class.cast(value).doubleValue());
                } else if (value instanceof JsonLongImpl) {
                    write(JsonLongImpl.class.cast(value).longValue());
                } else {
                    write(JsonNumber.class.cast(value).bigDecimalValue());
                }
                break;
            case TRUE:
                write(true);
                break;
            case FALSE:
                write(false);
                break;
            case NULL:
                writeNull();
                break;
            default:
                throw new JsonGenerationException("Unknown JsonValue type");
        }
    }

    @Override
    public JsonGenerator write(final String name, final JsonValue value) {
        checkObject(false);
        writeJsonValue(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final String value) {
        checkObject(false);
        writeKey(name);
        writeValueAsJsonString(value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigInteger value) {
        checkObject(false);
        writeKey(name);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigDecimal value) {
        checkObject(false);
        writeKey(name);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final int value) {
        checkObject(false);
        writeKey(name);
        writeValue(value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final long value) {
        checkObject(false);
        writeKey(name);
        writeValue(value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final double value) {
        checkObject(false);
        checkDoubleRange(value);
        writeKey(name);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final boolean value) {
        checkObject(false);
        writeKey(name);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator writeNull(final String name) {
        checkObject(false);
        writeKey(name);
        writeValue(NULL);
        return this;
    }

    @Override
    public JsonGenerator writeEnd() {
        checkArrayOrObject(false);
        final GeneratorState last = state.pop();
        decrementDepth();
        writeEol();
        writeIndent();
        if (last == GeneratorState.IN_ARRAY || last == GeneratorState.START_ARRAY) {
            justWrite(END_ARRAY_CHAR);
        } else {
            justWrite(END_OBJECT_CHAR);
        }
        alignState();
        return this;
    }

    @Override
    public JsonGenerator write(final JsonValue value) {
        checkArray(true);
        writeJsonValue(value);
        return this;
    }

    @Override
    public JsonGenerator write(final String value) {
        checkArray(false);
        writeValueAsJsonString(value);
        return this;
    }

    @Override
    public JsonGenerator write(final BigDecimal value) {
        checkArray(false);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final BigInteger value) {
        checkArray(false);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final int value) {
        checkArray(false);
        writeValue(value);
        return this;
    }

    @Override
    public JsonGenerator write(final long value) {
        checkArray(false);
        writeValue(value);
        return this;
    }

    @Override
    public JsonGenerator write(final double value) {
        checkArray(false);
        checkDoubleRange(value);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final boolean value) {
        checkArray(false);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        checkArray(false);
        writeValue(NULL);
        return this;
    }

    @Override
    public void close() {
        try {
            if (currentState() != GeneratorState.END) {
                throw new JsonGenerationException("Invalid json");
            }
        } finally {
            flushBuffer();
            try {
                writer.close();
            } catch (final IOException e) {
                throw new JsonException(e.getMessage(), e);
            }
            bufferProvider.release(buffer);
        }
    }

    @Override
    public void flush() {
        flushBuffer();
        try {
            writer.flush();
        } catch (final IOException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    private void flushBuffer() {
        if (bufferPos > 0) {
            try {
                writer.write(buffer, 0, bufferPos);
                bufferPos = 0;
            } catch (final IOException e) {
                throw new JsonException(e.getMessage(), e);
            }
        }
    }

    private final static char[][] REPLACEMENT_CHARS;

    static {
        REPLACEMENT_CHARS = new char[32][];
        for (int i = 0; i < 32; i++) {
            REPLACEMENT_CHARS[i] = toUnicode((char) i).toCharArray();
        }
    }

    private void writeEscaped0(final String value) {
        int len = 0;
        if (value == null || (len = value.length()) == 0) {
            return;
        }

        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            final int y = i;
            while (c != ESCAPE_CHAR && c != QUOTE_CHAR && c >= SPACE) {

                if (i >= len - 1) {
                    justWrite(value, y, i - y + 1);
                    return;
                }

                i++;
                c = value.charAt(i);
            }

            justWrite(value, y, i - y);

            switch (c) {
                case QUOTE_CHAR:
                case ESCAPE_CHAR:
                    justWrite(ESCAPE_CHAR);
                    justWrite(c);
                    break;
                default:
                    switch (c) {
                        case EOL:
                            justWrite('\\');
                            justWrite('n');
                            break;
                        case '\r':
                            justWrite('\\');
                            justWrite('r');
                            break;
                        case '\t':
                            justWrite('\\');
                            justWrite('t');
                            break;
                        case '\b':
                            justWrite('\\');
                            justWrite('b');
                            break;
                        case '\f':
                            justWrite('\\');
                            justWrite('f');
                            break;
                        default:
                            justWrite(REPLACEMENT_CHARS[c]);
                    }
            }
        }
    }

    private static final String UNICODE_PREFIX = "\\u";
    private static final String UNICODE_PREFIX_HELPER = "000";

    private static String toUnicode(final char c) {
        final String hex = UNICODE_PREFIX_HELPER + Integer.toHexString(c);
        final String s = UNICODE_PREFIX + hex.substring(hex.length() - 4);
        return s;
    }

    protected void justWrite(final String value) {
        justWrite(value, 0, value.length());
    }

    protected void justWrite(final String value, final int offset, final int valueLength) {

        if (valueLength == 0) {
            return;
        }

        if (bufferPos + valueLength >= buffer.length) {

            int start = offset;
            int len = buffer.length - bufferPos;

            while (true) {
                int end = start + len;
                if (end > valueLength + offset) {
                    end = valueLength;
                }

                value.getChars(start, end, buffer, bufferPos);

                bufferPos += (end - start);
                start += (len);

                if (start >= valueLength) {
                    return;
                }

                if (bufferPos >= buffer.length) {
                    flushBuffer();
                    len = buffer.length;
                }
            }
        } else {
            //fits completely into the buffer
            value.getChars(offset, valueLength + offset, buffer, bufferPos);
            bufferPos += valueLength;
        }
    }

    protected void justWrite(final char[] value) {
        justWrite(value, 0, value.length);
    }

    protected void justWrite(final char[] value, final int offset, final int valueLength) {

        if (valueLength == 0) {
            return;
        }

        if (bufferPos + valueLength >= buffer.length) {

            int start = offset;
            int len = buffer.length - bufferPos;

            while (true) {
                int end = start + len;
                if (end > valueLength + offset) {
                    end = valueLength;
                }

                //value.getChars(start, end, buffer, bufferPos);
                System.arraycopy(value, start, buffer, bufferPos, end - start);

                bufferPos += (end - start);
                start += (len);

                if (start >= valueLength) {
                    return;
                }

                if (bufferPos >= buffer.length) {
                    flushBuffer();
                    len = buffer.length;
                }
            }
        } else {
            //fits completely into the buffer
            //value.getChars(offset, valueLength + offset, buffer, bufferPos);
            System.arraycopy(value, offset, buffer, bufferPos, valueLength);
            bufferPos += valueLength;
        }
    }

    protected void justWrite(final char value) {
        if (bufferPos >= buffer.length) {
            flushBuffer();
        }
        buffer[bufferPos++] = value;
    }

    private void checkObject(final boolean allowInitial) {
        final GeneratorState currentState = currentState();
        if (currentState != GeneratorState.IN_OBJECT && currentState != GeneratorState.START_OBJECT) {
            if (!allowInitial || currentState != GeneratorState.INITIAL) {
                throw new JsonGenerationException("write(name, param) is only valid in objects");
            }
        }
    }

    private void checkArray(final boolean allowInitial) {
        final GeneratorState currentState = currentState();
        if (currentState != GeneratorState.IN_ARRAY && currentState != GeneratorState.START_ARRAY) {
            if (!allowInitial || currentState != GeneratorState.INITIAL) {
                throw new JsonGenerationException("write(param) is only valid in arrays");
            }
        }
    }

    private void checkArrayOrObject(final boolean allowInitial) {
        final GeneratorState currentState = currentState();
        if (currentState != GeneratorState.IN_ARRAY && currentState != GeneratorState.START_ARRAY
                && currentState != GeneratorState.IN_OBJECT && currentState != GeneratorState.START_OBJECT) {
            if (!allowInitial || currentState != GeneratorState.INITIAL) {
                throw new JsonGenerationException("only valid within array or object");
            }
        }
    }

    private static void checkDoubleRange(final double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new NumberFormatException("double can't be infinite or NaN");
        }
    }

    private void prepareValue() {
        final GeneratorState currentState = currentState();
        if (!currentState.acceptsValue) {
            throw new JsonGenerationException("state " + currentState + " does not accept a value");
        }
        if (currentState == GeneratorState.IN_ARRAY) {
            justWrite(',');
            writeEol();
        }
    }

    private void alignState() {

        if (currentState() == GeneratorState.AFTER_KEY) {
            state.pop();
        }
        switch (currentState()) {
            case START_ARRAY:
                swapState(GeneratorState.IN_ARRAY);
                break;
            case START_OBJECT:
                swapState(GeneratorState.IN_OBJECT);
                break;
            case INITIAL:
                swapState(GeneratorState.END);
                break;
            default:
        }
    }

    private void swapState(final GeneratorState newState) {
        state.pop();
        state.push(newState);
    }

    private GeneratorState currentState() {
        return state.peek();
    }

    private void writeKey(final String key) {
        final GeneratorState currentState = currentState();
        if (!currentState.acceptsKey) {
            throw new IllegalStateException("state " + currentState + " does not accept a key");
        }
        if (currentState == GeneratorState.IN_OBJECT) {
            justWrite(COMMA_CHAR);
            writeEol();
        }

        writeIndent();

        writeCachedKey(key);
        state.push(GeneratorState.AFTER_KEY);
    }

    private void writeValueAsJsonString(final String value) {
        prepareValue();
        justWrite(QUOTE_CHAR);
        writeEscaped0(value);
        justWrite(QUOTE_CHAR);
        alignState();
    }

    private void writeValue(final String value) {
        prepareValue();
        justWrite(value);
        alignState();
    }

    private void writeValue(final int value) {
        prepareValue();
        justWrite(String.valueOf(value));
        alignState();
    }

    private void writeValue(final long value) {
        prepareValue();
        justWrite(String.valueOf(value));
        alignState();
    }
}
