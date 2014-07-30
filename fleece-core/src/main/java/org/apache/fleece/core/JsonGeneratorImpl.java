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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;

class JsonGeneratorImpl implements JsonGenerator, JsonChars, Serializable {
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private final Writer writer;
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;
    private final char[] buffer;
    private int bufferPos = 0;
    //private final ConcurrentMap<String, String> cache;
    protected boolean needComma = false;

    private StructureElement currentStructureElement = null;
    private boolean valid = false;
    protected int depth = 0;

    //minimal stack implementation
    private static final class StructureElement {
        final StructureElement previous;
        final boolean isArray;

        StructureElement(final StructureElement previous, final boolean isArray) {
            super();
            this.previous = previous;
            this.isArray = isArray;
        }
    }

    JsonGeneratorImpl(final Writer writer, final BufferStrategy.BufferProvider<char[]> bufferProvider,
            final ConcurrentMap<String, String> cache) {
        this.writer = writer;
        //this.cache = cache;
        this.buffer = bufferProvider.newBuffer();
        this.bufferProvider = bufferProvider;
    }

    JsonGeneratorImpl(final OutputStream out, final BufferStrategy.BufferProvider<char[]> bufferProvider,
            final ConcurrentMap<String, String> cache) {
        this(new OutputStreamWriter(out, UTF8_CHARSET), bufferProvider, cache);
    }

    JsonGeneratorImpl(final OutputStream out, final Charset encoding, final BufferStrategy.BufferProvider<char[]> bufferProvider,
            final ConcurrentMap<String, String> cache) {
        this(new OutputStreamWriter(out, encoding), bufferProvider, cache);
    }

    protected void addCommaIfNeeded() {
        if (needComma) {
            justWrite(COMMA_CHAR);
            needComma = false;
        }

    }

    //caching currently disabled
    //two problems:
    // 1) not easy to get the escaped value efficiently wen its streamed and the buffer is full and needs to be flushed
    // 2) we have to use a kind of bounded threadsafe map to let the cache not grow indefinitely
    private void writeCachedOrEscape(final String name) {
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

        if (currentStructureElement == null && valid) {
            throw new JsonGenerationException("Method must not be called more than once in no context");
        }

        if (currentStructureElement != null && !currentStructureElement.isArray) {
            throw new JsonGenerationException("Method must not be called within an object context");
        }

        //push upon the stack
        if (currentStructureElement == null) {
            currentStructureElement = new StructureElement(null, false);
        } else {
            final StructureElement localStructureElement = new StructureElement(currentStructureElement, false);
            currentStructureElement = localStructureElement;
        }

        if (!valid) {
            valid = true;
        }

        noCheckWrite(START_OBJECT_CHAR);
        depth++;
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(final String name) {
        if (currentStructureElement == null || currentStructureElement.isArray) {
            throw new JsonGenerationException("Method must not be called within an array context");
        }

        //push upon the stack
        if (currentStructureElement == null) {
            currentStructureElement = new StructureElement(null, false);
        } else {
            final StructureElement localStructureElement = new StructureElement(currentStructureElement, false);
            currentStructureElement = localStructureElement;
        }

        addCommaIfNeeded();
        writeCachedOrEscape(name);
        noCheckWrite(START_OBJECT_CHAR);
        depth++;
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        if (currentStructureElement == null && valid) {
            throw new JsonGenerationException("Method must not be called more than once in no context");
        }

        if (currentStructureElement != null && !currentStructureElement.isArray) {
            throw new JsonGenerationException("Method must not be called within an object context");
        }

        //push upon the stack
        if (currentStructureElement == null) {
            currentStructureElement = new StructureElement(null, true);
        } else {
            final StructureElement localStructureElement = new StructureElement(currentStructureElement, true);
            currentStructureElement = localStructureElement;
        }

        if (!valid) {
            valid = true;
        }

        noCheckWrite(START_ARRAY_CHAR);
        depth++;
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(final String name) {
        if (currentStructureElement == null || currentStructureElement.isArray) {
            throw new JsonGenerationException("Method must not be called within an array context");
        }

        //push upon the stack
        if (currentStructureElement == null) {
            currentStructureElement = new StructureElement(null, true);
        } else {
            final StructureElement localStructureElement = new StructureElement(currentStructureElement, true);
            currentStructureElement = localStructureElement;
        }

        addCommaIfNeeded();
        writeCachedOrEscape(name);
        noCheckWrite(START_ARRAY_CHAR);
        depth++;
        return this;
    }

    private void writeJsonValue(final String name, final JsonValue value) {
        if (currentStructureElement != null) {
            checkObject();
        }
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
        if (currentStructureElement != null) {
            checkArray();
        }
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
                //TODO optimize
                final JsonNumber number = JsonNumber.class.cast(value);
                if (number.isIntegral()) {
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
        writeJsonValue(name, value);
        return this;
    }


    @Override
    public JsonGenerator write(final String name, final String value) {
        checkObject();

        addCommaIfNeeded();
        writeCachedOrEscape(name);

        addCommaIfNeeded();
        justWrite(QUOTE_CHAR);
        writeEscaped0(value);
        justWrite(QUOTE_CHAR);
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigInteger value) {
        checkObject();
        addCommaIfNeeded();
        writeCachedOrEscape(name);
        noCheckWriteAndForceComma(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigDecimal value) {
        checkObject();
        addCommaIfNeeded();
        writeCachedOrEscape(name);
        noCheckWriteAndForceComma(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final int value) {
        checkObject();
        addCommaIfNeeded();
        writeCachedOrEscape(name);
        addCommaIfNeeded();
        writeInt0(value);
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final long value) {
        checkObject();
        addCommaIfNeeded();
        writeCachedOrEscape(name);
        addCommaIfNeeded();
        writeLong0(value);
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final double value) {
        checkObject();
        checkDoubleRange(value);
        addCommaIfNeeded();
        writeCachedOrEscape(name);
        noCheckWriteAndForceComma(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final boolean value) {
        checkObject();
        addCommaIfNeeded();
        writeCachedOrEscape(name);
        noCheckWriteAndForceComma(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator writeNull(final String name) {
        checkObject();
        addCommaIfNeeded();
        writeCachedOrEscape(name);
        noCheckWriteAndForceComma(NULL);
        return this;
    }

    @Override
    public JsonGenerator writeEnd() {
        if (currentStructureElement == null) {
            throw new JsonGenerationException("Method must not be called in no context");
        }

        writeEnd(currentStructureElement.isArray ? END_ARRAY_CHAR : END_OBJECT_CHAR);

        //pop from stack
        currentStructureElement = currentStructureElement.previous;
        depth--;

        return this;
    }

    @Override
    public JsonGenerator write(final JsonValue value) {
        writeJsonValue(value);

        if (JsonStructure.class.isInstance(value)) {
            valid = true;
        }
        return this;
    }

    @Override
    public JsonGenerator write(final String value) {
        checkArray();
        addCommaIfNeeded();
        justWrite(QUOTE_CHAR);
        writeEscaped0(value);
        justWrite(QUOTE_CHAR);
        needComma = true;
        return this;
    }


    @Override
    public JsonGenerator write(final BigDecimal value) {
        checkArray();
        noCheckWrite(String.valueOf(value));
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final BigInteger value) {
        checkArray();
        noCheckWrite(String.valueOf(value));
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final int value) {
        checkArray();
        addCommaIfNeeded();
        writeInt0(value);
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final long value) {
        checkArray();
        addCommaIfNeeded();
        writeLong0(value);
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final double value) {
        checkArray();
        checkDoubleRange(value);
        noCheckWrite(Double.toString(value));
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final boolean value) {
        checkArray();
        noCheckWrite(Boolean.toString(value));
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        checkArray();
        noCheckWriteAndForceComma(NULL);
        needComma = true;
        return this;
    }

    @Override
    public void close() {

        try {
            if (currentStructureElement != null || !valid) {

                throw new JsonGenerationException("Invalid json " + currentStructureElement + " " + valid);
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

    private JsonGenerator noCheckWriteAndForceComma(final String value) {
        noCheckWrite(value);
        needComma = true;
        return this;
    }

    protected JsonGenerator writeEnd(final char value) {
        justWrite(value);
        needComma = true;
        return this;
    }

    protected void noCheckWrite(final String value) {
        addCommaIfNeeded();
        justWrite(value);
    }

    protected void noCheckWrite(final char value) {
        addCommaIfNeeded();
        justWrite(value);
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

        for (int i = 0; i < value.length(); i++) {
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

    protected void justWrite(final char[] chars) {

        if (bufferPos + chars.length >= buffer.length) {

            int start = 0;
            int len = buffer.length - bufferPos;

            while (true) {
                int end = start + len;
                if (end > chars.length) {
                    end = chars.length;
                }

                System.arraycopy(chars, start, buffer, bufferPos, end - start);

                bufferPos += (end - start);
                start += (len);

                if (start >= chars.length) {
                    return;
                }

                if (bufferPos >= buffer.length) {
                    flushBuffer();
                    len = buffer.length;
                }

            }

        } else {
            //fits completely into the buffer
            System.arraycopy(chars, 0, buffer, bufferPos, chars.length);
            bufferPos += chars.length;
        }

    }

    protected void justWrite(final String value) {
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

    protected void justWrite(final char value) {

        if (bufferPos >= buffer.length) {
            flushBuffer();
        }

        buffer[bufferPos++] = value;

    }
    
    private void checkObject() {
        if (currentStructureElement == null || currentStructureElement.isArray) {
            throw new JsonGenerationException("write(name, param) is only valid in objects");
        }
    }

    private void checkArray() {
        if (currentStructureElement == null || !currentStructureElement.isArray) {
            throw new JsonGenerationException("write(param) is only valid in arrays");
        }
    }

    private static void checkDoubleRange(final double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new NumberFormatException("double can't be infinite or NaN");
        }
    }
    
    
    //unopitimized, see below
    private void writeLong0(final long i) {

        justWrite(String.valueOf(i));
    }

    //unopitimized, see below
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
