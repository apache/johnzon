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

import javax.json.JsonException;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;

import static org.apache.fleece.core.Strings.asEscapedChar;

public class JsonStreamParser implements JsonChars, EscapedStringAwareJsonParser {
    private static final BufferCache<char[]> BUFFER_CACHE = new BufferCache<char[]>(Integer.getInteger("org.apache.fleece.default-char-buffer", 8192) /*BufferedReader.defaultCharBufferSize*/) {
        @Override
        protected char[] newValue(final int defaultSize) {
            return new char[defaultSize];
        }
    };

    private final Reader reader;
    private final int maxStringSize;

    // lexer state
    private final char[] loadedChars;
    private int availableLength = -1; // to trigger loading at first read() call
    private int currentBufferIdx;

    // current state
    private Event event = null;
    private Event lastEvent = null;
    private String currentValue = null;
    private String escapedValue = null;
    // location
    private int line = 1;
    private int column = 1;
    private int offset = 0;

    private final ValueBuilder valueBuilder = new ValueBuilder();

    public JsonStreamParser(final Reader reader, final int maxStringLength) {
        this.reader = reader;
        this.loadedChars = BUFFER_CACHE.getCache();
        this.maxStringSize = maxStringLength < 0 ? loadedChars.length : maxStringLength;
    }

    public JsonStreamParser(final InputStream stream, final int maxStringLength) {
        this(new InputStreamReader(stream), maxStringLength);
    }

    public JsonStreamParser(final InputStream in, final Charset charset, final int maxStringLength) {
        this(new InputStreamReader(in, charset), maxStringLength);
    }

    @Override
    public boolean hasNext() {
        if (event != null) {
            return loadedChars[currentBufferIdx] != EOF;
        }

        try {
            do {
                readUntilEvent();
                if (loadedChars[currentBufferIdx] == QUOTE) {
                    valueBuilder.reset(0); // actually offset = 1 but reset increments idx
                    boolean escape = false;

                    while (nextChar() != EOF && loadedChars[currentBufferIdx] != QUOTE && currentBufferIdx < valueBuilder.maxEnd) {
                        if (loadedChars[currentBufferIdx] == ESCAPE_CHAR) {
                            read();
                            escape = true;
                        }
                        valueBuilder.next();
                    }
                    currentValue = valueBuilder.readValue();

                    if (escape) { // this induces an overhead but that's not that often normally
                        final StringBuilder builder = new StringBuilder(currentValue.length());
                        boolean escaped = false;
                        for (final char current : currentValue.toCharArray()) {
                            if (current == ESCAPE_CHAR) {
                                escaped = true;
                                continue;
                            }
                            if (!escape) {
                                builder.append(current);
                            } else {
                                builder.append(asEscapedChar(current));
                            }
                        }
                        escapedValue = currentValue;
                        currentValue = builder.toString();
                    } else {
                        escapedValue = null;
                    }

                    readUntilEvent(); // we need to check if next char is a ':' to know it is a key
                    if (loadedChars[currentBufferIdx] == KEY_SEPARATOR) {
                        event = Event.KEY_NAME;
                    } else {
                        if (loadedChars[currentBufferIdx] != COMMA && loadedChars[currentBufferIdx] != END_OBJECT_CHAR && loadedChars[currentBufferIdx] != END_ARRAY_CHAR) {
                            throw new JsonParsingException("expecting end of structure or comma but got " + loadedChars[currentBufferIdx], createLocation());
                        }
                        currentBufferIdx--; // we are alredy in place so to avoid offset when calling readUntilEvent() going back
                        event = Event.VALUE_STRING;
                    }
                    return true;
                } else if (loadedChars[currentBufferIdx] == START_OBJECT_CHAR) {
                    event = Event.START_OBJECT;
                    return true;
                } else if (loadedChars[currentBufferIdx] == END_OBJECT_CHAR) {
                    event = Event.END_OBJECT;
                    return true;
                } else if (loadedChars[currentBufferIdx] == START_ARRAY_CHAR) {
                    event = Event.START_ARRAY;
                    return true;
                } else if (loadedChars[currentBufferIdx] == END_ARRAY_CHAR) {
                    event = Event.END_ARRAY;
                    return true;
                } else if (isNumber()) {
                    valueBuilder.reset(-1); // reset will increment to check overflow
                    while (nextChar() != EOF && isNumber() && currentBufferIdx < valueBuilder.maxEnd) {
                        valueBuilder.next();
                    }
                    currentValue = valueBuilder.readValue();
                    currentBufferIdx--; // we are alredy in place so to avoid offset when calling readUntilEvent() going back
                    event = Event.VALUE_NUMBER;
                    return true;
                } else if (loadedChars[currentBufferIdx] == TRUE_T) {
                    if (read() != TRUE_R || read() != TRUE_U || read() != TRUE_E) {
                        throw new JsonParsingException("true expected", createLocation());
                    }
                    event = Event.VALUE_TRUE;
                    return true;
                } else if (loadedChars[currentBufferIdx] == FALSE_F) {
                    if (read() != FALSE_A || read() != FALSE_L || read() != FALSE_S || read() != FALSE_E) {
                        throw new JsonParsingException("false expected", createLocation());
                    }
                    event = Event.VALUE_FALSE;
                    return true;
                } else if (loadedChars[currentBufferIdx] == NULL_N) {
                    if (read() != NULL_U || read() != NULL_L || read() != NULL_L) {
                        throw new JsonParsingException("null expected", createLocation());
                    }
                    event = Event.VALUE_NULL;
                    return true;
                } else if (loadedChars[currentBufferIdx] == EOF) {
                    return false;
                } else if (loadedChars[currentBufferIdx] == COMMA) {
                    if (event != null && event != Event.KEY_NAME && event != Event.VALUE_STRING && event != Event.VALUE_NUMBER && event != Event.VALUE_TRUE && event != Event.VALUE_FALSE && event != Event.VALUE_NULL) {
                        throw new JsonParsingException("unexpected comma", createLocation());
                    }
                } else {
                    throw new JsonParsingException("unexpected character: '" + loadedChars[currentBufferIdx] + "'", createLocation());
                }
            } while (true);
        } catch (final IOException e) {
            throw new JsonParsingException("unknown state", createLocation());
        }
    }

    private StringBuilder savePreviousStringBeforeOverflow(int start, StringBuilder previousParts) {
        final int length = currentBufferIdx - start;
        previousParts = (previousParts == null ? new StringBuilder(length * 2) : previousParts).append(loadedChars, start, length);
        return previousParts;
    }

    private boolean isNumber() {
        return isNumber(loadedChars[currentBufferIdx]) || loadedChars[currentBufferIdx] == DOT || loadedChars[currentBufferIdx] == MINUS || loadedChars[currentBufferIdx] == PLUS || loadedChars[currentBufferIdx] == EXP_LOWERCASE || loadedChars[currentBufferIdx] == EXP_UPPERCASE;
    }

    private static boolean isNumber(final char value) {
        return value >= ZERO && value <= NINE;
    }

    private void readUntilEvent() throws IOException {
        read();
        skipNotEventChars();
    }

    private void skipNotEventChars() throws IOException {
        int read = 0;
        do {
            final int current = currentBufferIdx;
            while (currentBufferIdx < availableLength) {
                if (loadedChars[currentBufferIdx] > SPACE) {
                    final int diff = currentBufferIdx - current;
                    offset += diff;
                    column += diff;
                    return;
                } else if (loadedChars[currentBufferIdx] == EOL) {
                    line++;
                    column = 0;
                }
                currentBufferIdx++;
            }
            read();
            read++;
        }
        while (loadedChars[currentBufferIdx] != EOF && read < loadedChars.length); // don't accept more space than buffer size to avoid DoS
        if (read == loadedChars.length) {
            throw new JsonParsingException("Too much spaces (>" + loadedChars.length + ")", createLocation());
        }
    }

    public JsonLocationImpl createLocation() {
        return new JsonLocationImpl(line, column, offset);
    }

    private char read() throws IOException {
        incr();
        return nextChar();
    }

    private char nextChar() throws IOException {
        if (overflowIfNeeded()) {
            offset--;
            column--;
            return EOF;
        }
        return loadedChars[currentBufferIdx];
    }

    private int incr() {
        offset++;
        column++;
        currentBufferIdx++;
        return currentBufferIdx;
    }

    private boolean overflowIfNeeded() throws IOException {
        if (currentBufferIdx >= availableLength) {
            availableLength = reader.read(loadedChars, 0, loadedChars.length);
            currentBufferIdx = 0;
            if (availableLength <= 0) { // 0 or -1 typically
                loadedChars[0] = EOF;
                return true;
            }
        }
        return false;
    }

    @Override
    public Event next() {
        if (event == null) {
            hasNext();
        }
        lastEvent = event;
        event = null;
        return lastEvent;
    }

    @Override
    public String getString() {
        if (lastEvent == Event.KEY_NAME || lastEvent == Event.VALUE_STRING || lastEvent == Event.VALUE_NUMBER) {
            return currentValue;
        }
        throw new IllegalStateException(event + " doesnt support getString()");
    }

    @Override
    public boolean isIntegralNumber() {
        for (int i = 0; i < currentValue.length(); i++) {
            if (!isNumber(currentValue.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getInt() {
        if (lastEvent != Event.VALUE_NUMBER) {
            throw new IllegalStateException(event + " doesn't support getInt()");
        }
        return Integer.parseInt(currentValue);
    }

    @Override
    public long getLong() {
        if (lastEvent != Event.VALUE_NUMBER) {
            throw new IllegalStateException(event + " doesn't support getLong()");
        }
        return Long.parseLong(currentValue);
    }

    @Override
    public BigDecimal getBigDecimal() {
        if (lastEvent != Event.VALUE_NUMBER) {
            throw new IllegalStateException(event + " doesn't support getBigDecimal()");
        }
        return new BigDecimal(currentValue);
    }

    @Override
    public JsonLocation getLocation() {
        return createLocation();
    }

    @Override
    public void close() {
        BUFFER_CACHE.release(loadedChars);
        try {
            reader.close();
        } catch (final IOException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

    public static JsonLocation location(final JsonParser parser) {
        if (JsonStreamParser.class.isInstance(parser)) {
            return JsonStreamParser.class.cast(parser).createLocation();
        }
        return new JsonLocationImpl(-1, -1, -1);
    }

    @Override
    public String getEscapedString() {
        return escapedValue;
    }

    private class ValueBuilder {
        private int start;
        private int maxEnd;
        private StringBuilder previousParts = null;

        public void next() {
            if (incr() >= availableLength) { // overflow case
                previousParts = savePreviousStringBeforeOverflow(start, previousParts);
                start = 0;
                maxEnd = maxStringSize;
            }
        }

        public String readValue() {
            if (loadedChars[currentBufferIdx] == EOF) {
                throw new JsonParsingException("Can't read string", createLocation());
            }

            final int length = currentBufferIdx - start;
            if (length >= maxStringSize) {
                throw new JsonParsingException("String too long", createLocation());
            }

            final String currentValue = new String(loadedChars, start, length);
            if (previousParts != null && previousParts.length() > 0) {
                return previousParts.append(currentValue).toString();
            }
            return currentValue;
        }

        public void reset(final int offset) {
            if (incr() < availableLength) { // direct overflow case
                start = currentBufferIdx + offset;
                maxEnd = start + maxStringSize;
            } else {
                maxEnd = maxStringSize - (maxEnd - start);
                start = 0;
            }
            if (previousParts != null) {
                previousParts.setLength(0);
            }
        }
    }
}
