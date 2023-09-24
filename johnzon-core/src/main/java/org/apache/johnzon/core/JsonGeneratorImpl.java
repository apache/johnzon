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
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerationException;
import jakarta.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;

class JsonGeneratorImpl implements JsonGenerator, JsonChars, Serializable {
    private final transient Writer writer;
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;
    private final char[] buffer;
    private int bufferPos = 0;
    private final boolean prettyPrint;
    private static final String INDENT = "  ";
    private int depth = 0;
    private boolean closed;

    private final HStack<GeneratorState> state = new HStack<GeneratorState>();

    private enum GeneratorState {
        INITIAL(false, true, false), // nothing created yet
        START_OBJECT(true, false, true), IN_OBJECT(true, false, true), AFTER_KEY(false, true, false), // object
        START_ARRAY(false, true, true), IN_ARRAY(false, true, true), // array
        END(false, false, false), // end of context
        ROOT_VALUE(false, false, true); // direct primitive (added in jsonp 1.1, was not supported in 1.0)

        private final boolean acceptsKey;
        private final boolean acceptsValue;
        private final boolean endable;

        GeneratorState(final boolean acceptsKey, final boolean acceptsValue, final boolean endable) {
            this.acceptsKey = acceptsKey;
            this.acceptsValue = acceptsValue;
            this.endable = endable;
        }
    }

    JsonGeneratorImpl(final Writer writer, final BufferStrategy.BufferProvider<char[]> bufferProvider,
                      final boolean prettyPrint) {
        this.writer = writer;
        this.buffer = bufferProvider.newBuffer();
        this.bufferProvider = bufferProvider;
        this.prettyPrint = prettyPrint;
        state.push(GeneratorState.INITIAL);
    }

    private void writeEol() {
        if (prettyPrint) {
            justWrite(EOL);
        }
    }

    private void writeIndent() {
        if (prettyPrint && depth > 0) {
            for (int i = 0; i < depth; i++) {
                justWrite(INDENT);
            }
        }
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
        depth++;
        justWrite(START_OBJECT_CHAR);

        writeEol();
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(final String name) {
        checkObject();
        writeKey(name);
        justWrite(START_OBJECT_CHAR);
        writeEol();
        state.push(GeneratorState.START_OBJECT);
        depth++;
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        prepareValue();
        writeIndent();
        state.push(GeneratorState.START_ARRAY);
        justWrite(START_ARRAY_CHAR);
        depth++;
        writeEol();
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(final String name) {
        checkObject();
        writeKey(name);
        justWrite(START_ARRAY_CHAR);
        writeEol();
        state.push(GeneratorState.START_ARRAY);
        depth++;
        return this;
    }

    private void writeJsonValue(final String name, final JsonValue value) {
        checkObject();
        //TODO check null handling
        switch (value.getValueType()) {
            case ARRAY:
                writeStartArray(name);
                final JsonArray array = value.asJsonArray();
                for (final JsonValue jsonValue : array) {
                    write(jsonValue);
                }
                writeEnd();

                break;
            case OBJECT:
                writeStartObject(name);
                final JsonObject object = value.asJsonObject();
                for (final Map.Entry<String, JsonValue> keyval : object.entrySet()) {
                    write(keyval.getKey(), keyval.getValue());
                }
                writeEnd();

                break;
            case STRING:
                write(name, JsonString.class.cast(value).getString());
                break;
            case NUMBER:
                //TODO optimize
                final JsonNumber number = JsonNumber.class.cast(value);
                if (number.isIntegral()) {
                    write(name, number.longValueExact());
                } else {
                    write(name, number.bigDecimalValue());
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
        checkArrayOrValue();
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
                final JsonNumber number = JsonNumber.class.cast(value);
                if (number instanceof JsonLongImpl) {
                    write(number.longValueExact());
                } else {
                    write(number.bigDecimalValue());
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
        checkObject();
        writeJsonValue(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final String value) {
        checkObject();
        writeKey(name);
        writeValueAsJsonString(value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigInteger value) {
        checkObject();
        writeKey(name);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigDecimal value) {
        checkObject();
        writeKey(name);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final int value) {
        checkObject();
        writeKey(name);
        writeValue(value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final long value) {
        checkObject();
        writeKey(name);
        writeValue(value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final double value) {
        checkObject();
        checkDoubleRange(value);
        writeKey(name);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final boolean value) {
        checkObject();
        writeKey(name);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator writeNull(final String name) {
        checkObject();
        writeKey(name);
        writeValue(NULL);
        return this;
    }

    @Override
    public JsonGenerator writeEnd() {
        final GeneratorState last = state.pop();
        if (last == null || !last.endable || last == GeneratorState.ROOT_VALUE) {
            throw new JsonGenerationException("Can't end current context: " + last);
        }
        depth--;
        if (last != GeneratorState.START_ARRAY) {
            writeEol();
        }
        writeIndent();
        if (last == GeneratorState.IN_ARRAY || last == GeneratorState.START_ARRAY) {
            justWrite(END_ARRAY_CHAR);
        } else if (last != GeneratorState.ROOT_VALUE) {
            justWrite(END_OBJECT_CHAR);
        }
        alignState();
        return this;
    }

    @Override
    public JsonGenerator write(final JsonValue value) {
        checkArrayOrValue();
        writeJsonValue(value);
        return this;
    }

    @Override
    public JsonGenerator write(final String value) {
        checkArrayOrValue();
        writeValueAsJsonString(value);
        return this;
    }

    @Override
    public JsonGenerator write(final BigDecimal value) {
        checkArrayOrValue();
        writeValue(value.toString());
        return this;
    }

    @Override
    public JsonGenerator write(final BigInteger value) {
        checkArrayOrValue();
        writeValue(value.toString());
        return this;
    }

    @Override
    public JsonGenerator write(final int value) {
        checkArrayOrValue();
        writeValue(value);
        return this;
    }

    @Override
    public JsonGenerator write(final long value) {
        checkArrayOrValue();
        writeValue(value);
        return this;
    }

    @Override
    public JsonGenerator write(final double value) {
        checkArrayOrValue();
        checkDoubleRange(value);
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final boolean value) {
        checkArrayOrValue();
        writeValue(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        checkArrayOrValue();
        writeValue(NULL);
        return this;
    }

    @Override
    public JsonGenerator writeKey(final String key) {
        final GeneratorState currentState = currentState();
        if (!currentState.acceptsKey) {
            throw new JsonGenerationException("state " + currentState + " does not accept a key");
        }
        if (currentState == GeneratorState.IN_OBJECT) {
            justWrite(COMMA_CHAR);
            writeEol();
        }

        writeIndent();

        writeCachedKey(key);
        state.push(GeneratorState.AFTER_KEY);
        return this;
    }



    @Override
    public void close() {
        if (closed) {
            return;
        }
        JsonGenerationException ex = null;
        final GeneratorState state = currentState();
        if (state != GeneratorState.END && state != GeneratorState.ROOT_VALUE) {
            ex = new JsonGenerationException("Invalid json, state=" + state);
        }
        try {
            if (ex == null) {
                flushBuffer();
                writer.close();
            }
        } catch (final IOException e) {
            if (ex != null) {
                throw ex;
            }
            throw new JsonException(e.getMessage(), e);
        } finally {
            closed = true;
            bufferProvider.release(buffer);
        }
        if (ex != null) {
            throw ex;
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

    private void writeEscaped0(final String value) {
        int len = 0;
        if (value == null || (len = value.length()) == 0) {
            return;
        }

        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);

            while (c != ESCAPE_CHAR && c != QUOTE_CHAR && c >= SPACE) {

                //read fast
                justWrite(c);

                if (i >= len - 1) {
                    return;
                }

                i++;
                c = value.charAt(i);
            }

            switch (c) {
                case QUOTE_CHAR:
                case ESCAPE_CHAR:
                    justWrite(ESCAPE_CHAR);
                    justWrite(c);
                    break;
                default:
                    if (c < SPACE) {
                        switch (c) {
                            case EOL:
                                justWrite("\\n");
                                break;
                            case '\r':
                                justWrite("\\r");
                                break;
                            case '\t':
                                justWrite("\\t");
                                break;
                            case '\b':
                                justWrite("\\b");
                                break;
                            case '\f':
                                justWrite("\\f");
                                break;
                            default:
                                justWrite(toUnicode(c));
                        }
                    } else if ((c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
                        justWrite(toUnicode(c));
                    } else {
                        justWrite(c);
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

    private void justWrite(final String value) {
        final int valueLength = value.length();

        if (bufferPos + valueLength >= buffer.length) {

            int start = 0;
            int len = buffer.length - bufferPos;

            while (true) {
                int end = start + len;
                if (end > valueLength) {
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
            value.getChars(0, valueLength, buffer, bufferPos);
            bufferPos += valueLength;
        }
    }

    private void justWrite(final char value) {
        if (bufferPos >= buffer.length) {
            flushBuffer();
        }
        buffer[bufferPos++] = value;
    }

    private void checkObject() {
        final GeneratorState currentState = currentState();
        if (currentState != GeneratorState.IN_OBJECT && currentState != GeneratorState.START_OBJECT) {
            throw new JsonGenerationException("write(name, param) is only valid in objects");
        }
    }

    private void checkArrayOrValue() {
        final GeneratorState currentState = currentState();
        if (currentState != GeneratorState.IN_ARRAY && currentState != GeneratorState.START_ARRAY &
                currentState != GeneratorState.AFTER_KEY) {
            if (currentState != GeneratorState.INITIAL) {
                throw new JsonGenerationException("write(param) is only valid in arrays");
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
                state.push(GeneratorState.ROOT_VALUE);
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

    private void writeValueAsJsonString(final String value) {
        prepareValue();
        final GeneratorState peek = state.peek();
        if (peek == GeneratorState.START_ARRAY || peek == GeneratorState.IN_ARRAY) {
            writeIndent();
        }
        justWrite(QUOTE_CHAR);
        writeEscaped0(value);
        justWrite(QUOTE_CHAR);
        alignState();
    }

    private void writeValue(final String value) {
        prepareValue();
        final GeneratorState peek = state.peek();
        if (peek == GeneratorState.START_ARRAY || peek == GeneratorState.IN_ARRAY) {
            writeIndent();
        }
        justWrite(value);
        alignState();
    }

    private void writeValue(final int value) {
        prepareValue();
        final GeneratorState peek = state.peek();
        if (peek == GeneratorState.START_ARRAY || peek == GeneratorState.IN_ARRAY) {
            writeIndent();
        }
        writeInt0(value);
        alignState();
    }

    private void writeValue(final long value) {
        prepareValue();
        final GeneratorState peek = state.peek();
        if (peek == GeneratorState.START_ARRAY || peek == GeneratorState.IN_ARRAY) {
            writeIndent();
        }
        writeLong0(value);
        alignState();
    }

    //unoptimized, see below
    private void writeLong0(final long i) {
        justWrite(String.valueOf(i));
    }

    //unoptimized, see below
    private void writeInt0(final int i) {
        justWrite(String.valueOf(i));
    }

    //optimized number optimizations
    /*
        private void writeLong0(final long i) {
            if (i == Long.MIN_VALUE) {
                justWrite("-9223372036854775808");
                return;
            }
            final int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
            final char[] buf = new char[size];
            getChars(i, size, buf);
            justWrite(buf);
        }

        private void writeInt0(final int i) {
            if (i == Integer.MIN_VALUE) {
                justWrite("-2147483648");
                return;
            }
            final int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
            final char[] buf = new char[size];
            getChars(i, size, buf);
            justWrite(buf);
        }

        private final static char[] DIGIT_TENS = { '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1', '1', '1', '1', '1', '1', '1', '1',
                '1', '1', '2', '2', '2', '2', '2', '2', '2', '2', '2', '2', '3', '3', '3', '3', '3', '3', '3', '3', '3', '3', '4', '4', '4',
                '4', '4', '4', '4', '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '6', '6', '6', '6', '6', '6', '6', '6',
                '6', '6', '7', '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8', '9', '9', '9',
                '9', '9', '9', '9', '9', '9', '9', };

        private final static char[] DIGIT_ONES = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2',
                '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2',
                '3', '4', '5', '6', '7', '8', '9', };

        private final static char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
                'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

        // Requires positive x
        private static int stringSize(final long x) {
            long p = 10;
            for (int i = 1; i < 19; i++) {
                if (x < p) {
                    return i;
                }
                p = 10 * p;
            }
            return 19;
        }

        private static void getChars(long i, final int index, final char[] buf) {
            long q;
            int r;
            int charPos = index;
            char sign = 0;

            if (i < 0) {
                sign = '-';
                i = -i;
            }

            // Get 2 digits/iteration using longs until quotient fits into an int
            while (i > Integer.MAX_VALUE) {
                q = i / 100;
                // really: r = i - (q * 100);
                r = (int) (i - ((q << 6) + (q << 5) + (q << 2)));
                i = q;
                buf[--charPos] = DIGIT_ONES[r];
                buf[--charPos] = DIGIT_TENS[r];
            }

            // Get 2 digits/iteration using ints
            int q2;
            int i2 = (int) i;
            while (i2 >= 65536) {
                q2 = i2 / 100;
                // really: r = i2 - (q * 100);
                r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
                i2 = q2;
                buf[--charPos] = DIGIT_ONES[r];
                buf[--charPos] = DIGIT_TENS[r];
            }

            // Fall thru to fast mode for smaller numbers
            // assert(i2 <= 65536, i2);
            for (;;) {
                q2 = (i2 * 52429) >>> (16 + 3);
                r = i2 - ((q2 << 3) + (q2 << 1)); // r = i2-(q2*10) ...
                buf[--charPos] = DIGITS[r];
                i2 = q2;
                if (i2 == 0) {
                    break;
                }
            }
            if (sign != 0) {
                buf[--charPos] = sign;
            }
        }
     */
}
