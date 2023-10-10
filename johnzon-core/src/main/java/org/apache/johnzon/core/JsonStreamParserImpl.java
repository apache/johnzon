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

import jakarta.json.JsonException;
import jakarta.json.stream.JsonLocation;
import jakarta.json.stream.JsonParsingException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;

//This class represents either the Json tokenizer and the Json parser.
public class JsonStreamParserImpl extends JohnzonJsonParserImpl implements JsonChars {
    private final boolean autoAdjust;

    //the main buffer where the stream will be buffered
    private final char[] buffer;

    //current parser position within the buffer
    //Initial MIN_VALUE will trigger buffer refill, normally bufferPos is >= -1
    //-1 would cause a re-read of the first character in the buffer (which is at zero index)
    private int bufferPos = Integer.MIN_VALUE;

    // performance optimisation to avoid subtraction on readNextChar
    private int bufferLeft = 0;

    //available character in the buffer. It might be <= "buffer.length".
    private int availableCharsInBuffer;

    //start and end position of values in the buffer
    //may cross boundaries, then value is in fallBackCopyBuffer
    private int startOfValueInBuffer = -1;
    private int endOfValueInBuffer = -1;

    private final Reader in;

    //do we read from a character stream or a byte stream
    //not used at the moment but maybe relevant in future to calculate the JsonLocation offset
    @SuppressWarnings("unused")
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;
    private final BufferStrategy.BufferProvider<char[]> valueProvider;

    //max length for strings and numbers (max count of characters)
    private final int maxValueLength;

    //we use a byte here, because comparing bytes
    //is more efficient than comparing enums
    //Additionally we handle internally two more event: COMMA_EVENT and KEY_SEPARATOR_EVENT
    private byte previousEvent = -1;

    //this buffer is used to store current String or Number value in case that
    //within the value a buffer boundary is crossed or the string contains escaped characters
    private char[] fallBackCopyBuffer;
    private boolean releaseFallBackCopyBufferLength = true;
    private int fallBackCopyBufferLength;

    // location (line, column, offset)
    // We try to calculate this efficiently so we do not just increment the values per char read
    // Instead we calculate the column and offset relative to the pastBufferReadCount and/or lastLineBreakPosition.
    private long currentLine = 1;
    private long lastLineBreakPosition;
    private long pastBufferReadCount;

    //cache (if current value is a number) integral state and the number itself if its only one digit    
    private boolean isCurrentNumberIntegral = true;
    private int currentIntegralNumber = Integer.MIN_VALUE; //for number from 0 - 9

    //maybe we want also cache BigDecimals
    //private BigDecimal currentBigDecimalNumber = null;

    //We need a stack if we want detect bad formatted Json do determine if we are within an array or not
    //example
    //     Streamparser sees: ],1  <-- we look from here
    //the 1 is only allowed if we are within an array
    //This can only be determined by build up a stack which tracks the trail of Json objects and arrays
    //This stack here is only needed for validating the above mentioned case, if we want to be lenient we can skip suing the stack.
    //Stack can cause out of memory issues when the nesting depth of a Json stream is too deep.
    private StructureElement currentStructureElement = null;

    private int arrayDepth = 0;
    private int objectDepth = 0;

    private boolean closed;

    //minimal stack implementation
    private static final class StructureElement {
        private final StructureElement previous;
        private final boolean isArray;

        StructureElement(final StructureElement previous, final boolean isArray) {
            super();
            this.previous = previous;
            this.isArray = isArray;
        }
    }

    //detect charset according to RFC 4627
    public JsonStreamParserImpl(final InputStream inputStream, final int maxStringLength,
                                final BufferStrategy.BufferProvider<char[]> bufferProvider, final BufferStrategy.BufferProvider<char[]> valueBuffer,
                                final boolean autoAdjust, final JsonProviderImpl provider) {

        this(inputStream, null, null, maxStringLength, bufferProvider, valueBuffer, autoAdjust, provider);
    }

    //use charset provided
    public JsonStreamParserImpl(final InputStream inputStream, final Charset encoding, final int maxStringLength,
                                final BufferStrategy.BufferProvider<char[]> bufferProvider, final BufferStrategy.BufferProvider<char[]> valueBuffer,
                                final boolean autoAdjust, final JsonProviderImpl provider) {

        this(inputStream, null, encoding, maxStringLength, bufferProvider, valueBuffer, autoAdjust, provider);
    }

    public JsonStreamParserImpl(final Reader reader, final int maxStringLength, final BufferStrategy.BufferProvider<char[]> bufferProvider,
                                final BufferStrategy.BufferProvider<char[]> valueBuffer, final boolean autoAdjust, final JsonProviderImpl provider) {

        this(null, reader, null, maxStringLength, bufferProvider, valueBuffer, autoAdjust, provider);
    }

    private JsonStreamParserImpl(final InputStream inputStream, final Reader reader, final Charset encoding, final int maxStringLength,
                                 final BufferStrategy.BufferProvider<char[]> bufferProvider, final BufferStrategy.BufferProvider<char[]> valueBuffer,
                                 final boolean autoAdjust, final JsonProviderImpl provider) {

        super(provider);
        this.autoAdjust = autoAdjust;
        this.maxValueLength = maxStringLength <= 0 ? 8192 : maxStringLength;
        this.fallBackCopyBuffer = valueBuffer.newBuffer();
        this.buffer = bufferProvider.newBuffer();
        this.bufferProvider = bufferProvider;
        this.valueProvider = valueBuffer;

        if (fallBackCopyBuffer.length < maxStringLength) {
            throw cust("Size of value buffer cannot be smaller than maximum string length");
        }

        if (reader != null) {
            this.in = reader;
        } else if (encoding != null) { // always respect it
            this.in = new InputStreamReader(inputStream, encoding);
        } else { // should we log the usage is unexpected? (for perf we want to avoid the pushbackinputstream)
            this.in = new RFC4627AwareInputStreamReader(inputStream);
        }
    }

    //append a single char to the value buffer
    private void appendToCopyBuffer(final char c) {
        if (fallBackCopyBufferLength >= fallBackCopyBuffer.length - 1) {
            doAutoAdjust(1);
        }
        fallBackCopyBuffer[fallBackCopyBufferLength++] = c;
    }

    //copy content between "start" and "end" from buffer to value buffer 
    private void copyCurrentValue() {
        final int length = endOfValueInBuffer - startOfValueInBuffer;
        if (length > 0) {

            if (length > maxValueLength) {
                throw tmc();
            }

            if (fallBackCopyBufferLength >= fallBackCopyBuffer.length - length) { // not good at runtime but handled
                doAutoAdjust(length);
            } else {
                System.arraycopy(buffer, startOfValueInBuffer, fallBackCopyBuffer, fallBackCopyBufferLength, length);
            }
            fallBackCopyBufferLength += length;
        }

        startOfValueInBuffer = endOfValueInBuffer = -1;
    }

    private void doAutoAdjust(final int length) {
        if (!autoAdjust) {
            throw new ArrayIndexOutOfBoundsException("Buffer too small for such a long string");
        }

        final char[] newArray = new char[fallBackCopyBuffer.length + Math.max(getBufferExtends(fallBackCopyBuffer.length), length)];
        // TODO: log to adjust size once?
        System.arraycopy(fallBackCopyBuffer, 0, newArray, 0, fallBackCopyBufferLength);
        if (startOfValueInBuffer != -1) {
            System.arraycopy(buffer, startOfValueInBuffer, newArray, fallBackCopyBufferLength, length);
        }
        if (releaseFallBackCopyBufferLength) {
            bufferProvider.release(fallBackCopyBuffer);
            releaseFallBackCopyBufferLength = false;
        }
        fallBackCopyBuffer = newArray;
    }

    /**
     * @param currentLength length of the buffer
     * @return the amount of bytes the current buffer should get extended with
     */
    protected int getBufferExtends(int currentLength) {
        return currentLength / 4;
    }


    @Override
    public final boolean hasNext() {
        if (currentStructureElement != null || previousEvent == 0) {
            return true;
        }
        if (previousEvent != END_ARRAY && previousEvent != END_OBJECT &&
                previousEvent != VALUE_STRING && previousEvent != VALUE_FALSE && previousEvent != VALUE_TRUE &&
                previousEvent != VALUE_NULL && previousEvent != VALUE_NUMBER) {
            if (bufferPos < 0) { // check we don't have an empty string to parse
                final char c = readNextChar();
                unreadChar();
                return c != EOF;
            }
            return true;
        }

        //detect garbage at the end of the file after last object or array is closed
        if (bufferPos < availableCharsInBuffer) {

            final char c = readNextNonWhitespaceChar(readNextChar());

            if (c == EOF) {
                return false;
            }

            if (bufferPos < availableCharsInBuffer) {
                throw uexc("EOF expected");
            }

        }

        return false;

    }

    private static boolean isAsciiDigit(final char value) {
        return value <= NINE && value >= ZERO;
    }

    //check if value is a valid hex digit and return the numeric value
    private int parseHexDigit(final char value) {

        if (isAsciiDigit(value)) {
            return value - 48;
        } else if (value <= 'f' && value >= 'a') {
            return (value) - 87;
        } else if ((value <= 'F' && value >= 'A')) {
            return (value) - 55;
        } else {
            throw uexc("Invalid hex character");
        }
    }

    private JsonLocation createLocation() {

        //we start with column = 1, so column is always >= 1
        //APi is not clear in this, but starting column with 1 is convenient
        long column = 1;
        long charOffset = 0;

        if (bufferPos >= -1) {

            charOffset = pastBufferReadCount + bufferPos + 1;
            column = lastLineBreakPosition == 0 ? charOffset + 1 : charOffset - lastLineBreakPosition;
        }

        //For now its unclear how to calculate offset for (byte) inputsream.
        //API says count bytes but thats dependent on encoding and not efficient
        //skip this for now, count always bytes and defer this until the JSR TCK arrives.

        return new JsonLocationImpl(currentLine, column, charOffset);
    }

    //read the next char from the stream and set/increment the bufferPos
    //will also refill buffer if necessary
    //if we are currently processing a value (string or number) and buffer 
    //refill is necessary copy the already read value part into the value buffer
    protected final char readNextChar() {

        if (bufferLeft == 0) {
            //fillbuffer

            //copy content from old buffer to valuebuffer
            //correct start end mark
            if (startOfValueInBuffer > -1 && endOfValueInBuffer == -1) {
                endOfValueInBuffer = availableCharsInBuffer;
                copyCurrentValue();

                startOfValueInBuffer = 0;
            }

            if (bufferPos >= -1) {
                pastBufferReadCount += availableCharsInBuffer;
            }

            try {
                availableCharsInBuffer = in.read(buffer, 0, buffer.length);
                if (availableCharsInBuffer <= 0) {
                    return EOF;
                }

            } catch (final IOException e) {
                close();
                throw uexio(e);
            }

            bufferPos = 0;
            bufferLeft = availableCharsInBuffer - 1;
            //end fillbuffer
        } else {

            //since JOHNZON-18 not longer necessary
            //prevent "bufferoverflow
            //if(bufferPos + 1 >= availableCharsInBuffer) {
            //    return EOF;
            //}

            bufferPos++;
            bufferLeft--;
        }

        return buffer[bufferPos];
    }

    //skip whitespaces
    //tracks location informations (line, column)
    //returns the first non whitespace character
    protected final char readNextNonWhitespaceChar(char c) {

        int dosCount = 0;

        while (c == SPACE || c == TAB || c == CR || c == EOL) {

            if (c == EOL) {
                currentLine++;
                lastLineBreakPosition = pastBufferReadCount + bufferPos;
            }

            //prevent DOS (denial of service) attack
            if (dosCount >= maxValueLength) {
                throw tmc();
            }
            dosCount++;

            //read next character
            c = readNextChar();

        }

        return c;
    }

    @Override
    public Event currentEvent() {
        return previousEvent >= 0 && previousEvent < Event.values().length
                ? Event.values()[previousEvent]
                : null;
    }

    @Override
    public Event current() {
        if (previousEvent < 0 && hasNext()) {
            internalNext();
        }

        return currentEvent();
    }

    private void unreadChar() {
        bufferPos--;
        bufferLeft++;
    }

    @Override
    protected final Event internalNext() {
        //main entry, make decision how to handle the current character in the stream

        if (!hasNext()) {
            final char c = readNextChar();
            unreadChar();
            if (c != EOF) {
                throw uexc("No available event");
            }
            throw new NoSuchElementException();
        }

        if (previousEvent > 0 && currentStructureElement == null) {
            throw uexc("Unexpected end of structure");
        }

        final char c = readNextNonWhitespaceChar(readNextChar());

        if (c == COMMA_CHAR) {
            //last event must one of the following-> " ] } LITERAL
            if (previousEvent == KEY_SEPARATOR_EVENT || previousEvent == START_ARRAY
                    || previousEvent == START_OBJECT || previousEvent == COMMA_EVENT
                    || previousEvent == KEY_NAME) {
                throw uexc("Expected \" ] } LITERAL");
            }

            previousEvent = COMMA_EVENT;
            return internalNext();

        }

        if (c == KEY_SEPARATOR) {

            if (previousEvent != KEY_NAME) {
                throw uexc("A : can only follow a key name");
            }

            previousEvent = KEY_SEPARATOR_EVENT;
            return internalNext();

        }

        if (!isCurrentNumberIntegral) {
            isCurrentNumberIntegral = true;
        }
        //        if (currentBigDecimalNumber != null) {
        //            currentBigDecimalNumber = null;
        //        }
        if (currentIntegralNumber != Integer.MIN_VALUE) {
            currentIntegralNumber = Integer.MIN_VALUE;
        }

        if (fallBackCopyBufferLength != 0) {
            fallBackCopyBufferLength = 0;
        }

        startOfValueInBuffer = endOfValueInBuffer = -1;

        switch (c) {

            case START_OBJECT_CHAR:

                return handleStartObject();

            case END_OBJECT_CHAR:

                return handleEndObject();

            case START_ARRAY_CHAR:

                return handleStartArray();

            case END_ARRAY_CHAR:

                return handleEndArray();

            case QUOTE_CHAR:

                return handleQuote();

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case MINUS:
            case FALSE_F: // false
            case TRUE_T: // true
            case NULL_N: // null

                return handleLiteral();

            default:

                return defaultHandling(c);
        }
    }


    protected Event defaultHandling(char c) {
        if (c == EOF) {
            throw uexc("End of file hit too early");
        }
        throw uexc("Expected structural character or digit or 't' or 'n' or 'f' or '-'");
    }

    private Event handleStartObject() {

        //last event must one of the following-> : , [
        if (previousEvent > 0 && previousEvent != KEY_SEPARATOR_EVENT && previousEvent != START_ARRAY && previousEvent != COMMA_EVENT) {
            throw uexc("Expected : , [");
        }

        //push upon the stack
        if (currentStructureElement == null) {
            currentStructureElement = new StructureElement(null, false);
        } else {
            if (!currentStructureElement.isArray && previousEvent != KEY_SEPARATOR_EVENT) {
                throw uexc("Expected :");
            }
            final StructureElement localStructureElement = new StructureElement(currentStructureElement, false);
            currentStructureElement = localStructureElement;
        }

        objectDepth++;

        return EVT_MAP[previousEvent = START_OBJECT];

    }

    private Event handleEndObject() {

        //last event must one of the following-> " ] { } LITERAL
        if (previousEvent == START_ARRAY || previousEvent == COMMA_EVENT || previousEvent == KEY_NAME
                || previousEvent == KEY_SEPARATOR_EVENT || currentStructureElement == null) {
            throw uexc("Expected \" ] { } LITERAL");
        }

        if (currentStructureElement.isArray) {
            throw uexc("Expected : ]");
        }

        //pop from stack
        currentStructureElement = currentStructureElement.previous;

        objectDepth--;

        return EVT_MAP[previousEvent = END_OBJECT];
    }

    private Event handleStartArray() {

        //last event must one of the following-> : , [
        if (previousEvent > 0 && previousEvent != KEY_SEPARATOR_EVENT && previousEvent != START_ARRAY && previousEvent != COMMA_EVENT) {
            throw uexc("Expected : , [");
        }

        //push upon the stack
        if (currentStructureElement == null) {
            currentStructureElement = new StructureElement(null, true);
        } else {
            if (!currentStructureElement.isArray && previousEvent != KEY_SEPARATOR_EVENT) {
                throw uexc("Expected \"");
            }
            final StructureElement localStructureElement = new StructureElement(currentStructureElement, true);
            currentStructureElement = localStructureElement;
        }

        arrayDepth++;

        return EVT_MAP[previousEvent = START_ARRAY];
    }

    private Event handleEndArray() {

        //last event must one of the following-> [ ] } " LITERAL
        if (previousEvent == START_OBJECT || previousEvent == COMMA_EVENT || previousEvent == KEY_SEPARATOR_EVENT
                || currentStructureElement == null) {
            throw uexc("Expected [ ] } \" LITERAL");
        }

        if (!currentStructureElement.isArray) {
            throw uexc("Expected : }");
        }

        //pop from stack
        currentStructureElement = currentStructureElement.previous;

        arrayDepth--;

        return EVT_MAP[previousEvent = END_ARRAY];
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

    //read a string, gets called recursively
    //Handles escape/d characters
    //if string contains escape chars and/or cross buffer boundary then copy in the value buffer
    //if not then denote string start and end in startOfValueInBuffer and endOfValueInBuffer and read directly from buffer
    private void readString() {

        do {
            char n = readNextChar();
            //when first called n its first char after the starting quote
            //after that its the next character after the while loop below

            if (n == QUOTE_CHAR) {
                endOfValueInBuffer = startOfValueInBuffer = bufferPos; //->"" case
                return;
            } else if (n == EOL) {
                throw uexc("Unexpected linebreak");

            } else if (/* n >= '\u0000' && */ n <= '\u001F') {
                throw uexc("Unescaped control character");

            } else if (n == ESCAPE_CHAR) {

                n = readNextChar();

                //  \ u XXXX -> unicode char
                if (n == 'u') {
                    n = parseUnicodeHexChars();
                    appendToCopyBuffer(n);

                    // \\ -> \
                } else if (n == ESCAPE_CHAR) {
                    appendToCopyBuffer(n);

                    //another escape chars, for example \t
                } else {
                    appendToCopyBuffer(Strings.asEscapedChar(n));
                }

            } else {

                startOfValueInBuffer = bufferPos;
                endOfValueInBuffer = -1;

                while ((n = readNextChar()) > '\u001F' && n != ESCAPE_CHAR && n != EOL && n != QUOTE_CHAR) {
                    //read fast
                }

                endOfValueInBuffer = bufferPos;

                if (n == QUOTE_CHAR) {

                    if (fallBackCopyBufferLength > 0) {
                        copyCurrentValue();
                    } else {
                        if ((endOfValueInBuffer - startOfValueInBuffer) > maxValueLength) {
                            throw tmc();
                        }

                    }

                    return;
                } else if (n == EOL) {
                    throw uexc("Unexpected linebreak");

                } else if (n >= '\u0000' && n <= '\u001F') {
                    throw uexc("Unescaped control character");
                }

                copyCurrentValue();

                //current n is one of < '\u001F' -OR- ESCAPE_CHAR -OR- EOL -OR- QUOTE

                unreadChar(); //unread one char

            }
        } while (true);

        // before this do while(true) it was:
        //
        //recurse until string is terminated by a non escaped quote
        //readString();
        //
        //
        // but recursive = can't read big strings

    }


    //read the next four chars, check them and treat them as an single unicode char
    private char parseUnicodeHexChars() {
        // \u08Ac etc       
        return (char) (((parseHexDigit(readNextChar())) * 4096) + ((parseHexDigit(readNextChar())) * 256)
                + ((parseHexDigit(readNextChar())) * 16) + ((parseHexDigit(readNextChar()))));

    }

    private Event handleQuote() {

        //always the beginning quote of a key or value  

        //last event must one of the following-> : { [ ,
        if (previousEvent != -1 &&
                (previousEvent != KEY_SEPARATOR_EVENT &&
                 previousEvent != START_OBJECT &&
                 previousEvent != START_ARRAY  &&
                 previousEvent != COMMA_EVENT)) {
            throw uexc("Expected : { [ ,");
        }
        //starting quote already consumed
        readString();
        //end quote already consumed

        //make the decision if its an key or value
        if (previousEvent == KEY_SEPARATOR_EVENT) {
            //must be value

            if (currentStructureElement != null && currentStructureElement.isArray) {
                //not in array, only allowed within array
                throw uexc("Key value pair not allowed in an array");
            }

            return EVT_MAP[previousEvent = VALUE_STRING];

        } else { //Event is  START_OBJECT  OR START_ARRAY OR COMMA_EVENT
            //must be a key if we are in an object, if not its a value 

            if ((currentStructureElement != null && currentStructureElement.isArray) || currentStructureElement == null) {
                return EVT_MAP[previousEvent = VALUE_STRING];
            }

            return EVT_MAP[previousEvent = KEY_NAME];
        }

    }

    //read a number
    //if a number cross buffer boundary then copy in the value buffer
    //if not then denote string start and end in startOfValueInBuffer and endOfValueInBuffer and read directly from buffer
    private void readNumber() {

        char c = buffer[bufferPos];

        //start can change on any read() if we cross buffer boundary
        startOfValueInBuffer = bufferPos;
        endOfValueInBuffer = -1;

        char y = EOF;

        //sum up the digit values 
        int cumulatedDigitValue = 0;
        while (isAsciiDigit(y = readNextChar())) {

            if (c == ZERO) {
                throw uexc("Leading zeros not allowed");
            }

            if (c == MINUS && cumulatedDigitValue == 48) {
                throw uexc("Leading zeros after minus not allowed");
            }

            cumulatedDigitValue += y;

        }

        if (c == MINUS && cumulatedDigitValue == 0) {

            throw uexc("Unexpected premature end of number");
        }

        if (y == DOT) {
            isCurrentNumberIntegral = false;
            cumulatedDigitValue = 0;
            while (isAsciiDigit(y = readNextChar())) {
                cumulatedDigitValue++;
            }

            if (cumulatedDigitValue == 0) {

                throw uexc("Unexpected premature end of number");
            }

        }

        if (y == EXP_LOWERCASE || y == EXP_UPPERCASE) {
            isCurrentNumberIntegral = false;

            y = readNextChar(); //+ or - or digit

            if (!isAsciiDigit(y) && y != MINUS && y != PLUS) {
                throw uexc("Expected DIGIT or + or -");
            }

            if (y == MINUS || y == PLUS) {
                y = readNextChar();
                if (!isAsciiDigit(y)) {
                    throw uexc("Unexpected premature end of number");
                }

            }

            while (isAsciiDigit(y = readNextChar())) {
                //no-op
            }

        }

        endOfValueInBuffer = y == EOF && endOfValueInBuffer < 0 ? -1 : bufferPos;

        if (y == COMMA_CHAR || y == END_ARRAY_CHAR || y == END_OBJECT_CHAR || y == EOL || y == SPACE || y == TAB || y == CR || y == EOF) {

            unreadChar();//unread one char

            //['-', DIGIT]
            if (isCurrentNumberIntegral && c == MINUS && cumulatedDigitValue >= 48 && cumulatedDigitValue <= 57) {

                currentIntegralNumber = -(cumulatedDigitValue - 48); //optimize -0 till -9
                return;
            }

            //[DIGIT]
            if (isCurrentNumberIntegral && c != MINUS && cumulatedDigitValue == 0) {

                currentIntegralNumber = (c - 48); //optimize 0 till 9
                return;
            }

            if (fallBackCopyBufferLength > 0) {

                //we crossed a buffer boundary, use value buffer
                copyCurrentValue();

            } else {
                if ((endOfValueInBuffer - startOfValueInBuffer) >= maxValueLength) {
                    throw tmc();
                }
            }

            return;

        }

        throw uexc("Unexpected premature end of number");

    }

    //handles false, true, null and numbers
    private Event handleLiteral() {

        //last event must one of the following-> : , [
        if (previousEvent != -1 && previousEvent != KEY_SEPARATOR_EVENT && previousEvent != START_ARRAY && previousEvent != COMMA_EVENT) {
            throw uexc("Expected : , [");
        }

        if (previousEvent == COMMA_EVENT && !currentStructureElement.isArray) {
            //only allowed within array
            throw uexc("Not in an array context");
        }

        char c = buffer[bufferPos];

        // probe literals
        switch (c) {
            case TRUE_T:

                if (readNextChar() != TRUE_R || readNextChar() != TRUE_U || readNextChar() != TRUE_E) {
                    throw uexc("Expected LITERAL: true");
                }
                return EVT_MAP[previousEvent = VALUE_TRUE];
            case FALSE_F:

                if (readNextChar() != FALSE_A || readNextChar() != FALSE_L || readNextChar() != FALSE_S || readNextChar() != FALSE_E) {
                    throw uexc("Expected LITERAL: false");
                }

                return EVT_MAP[previousEvent = VALUE_FALSE];

            case NULL_N:

                if (readNextChar() != NULL_U || readNextChar() != NULL_L || readNextChar() != NULL_L) {
                    throw uexc("Expected LITERAL: null");
                }
                return EVT_MAP[previousEvent = VALUE_NULL];

            default:
                readNumber();
                return EVT_MAP[previousEvent = VALUE_NUMBER];
        }

    }

    @Override
    public String getString() {
        if (previousEvent == KEY_NAME || previousEvent == VALUE_STRING || previousEvent == VALUE_NUMBER) {

            //if there a content in the value buffer read from them, if not use main buffer
            return fallBackCopyBufferLength > 0 ? new String(fallBackCopyBuffer, 0, fallBackCopyBufferLength) : new String(buffer,
                    startOfValueInBuffer, endOfValueInBuffer - startOfValueInBuffer);
        } else {
            throw new IllegalStateException(EVT_MAP[previousEvent] + " doesn't support getString()");
        }
    }

    @Override
    public boolean isIntegralNumber() {

        if (previousEvent != VALUE_NUMBER) {
            throw new IllegalStateException(EVT_MAP[previousEvent] + " doesn't support isIntegralNumber()");
        } else {
            return isCurrentNumberIntegral;
        }
    }

    @Override
    public boolean isNotTooLong() {
        return (endOfValueInBuffer - startOfValueInBuffer) < 19;
    }

    @Override
    public int getInt() {
        if (previousEvent != VALUE_NUMBER) {
            throw new IllegalStateException(EVT_MAP[previousEvent] + " doesn't support getInt()");
        } else if (isCurrentNumberIntegral && currentIntegralNumber != Integer.MIN_VALUE) {
            return currentIntegralNumber;
        } else if (isCurrentNumberIntegral) {
            //if there a content in the value buffer read from them, if not use main buffer
            final Integer retVal = fallBackCopyBufferLength > 0 ? parseIntegerFromChars(fallBackCopyBuffer, 0, fallBackCopyBufferLength)
                    : parseIntegerFromChars(buffer, startOfValueInBuffer, endOfValueInBuffer);
            if (retVal == null) {
                return getBigDecimal().intValue();
            } else {
                return retVal.intValue();
            }
        } else {
            return getBigDecimal().intValue();
        }
    }

    @Override
    public long getLong() {
        if (previousEvent != VALUE_NUMBER) {
            throw new IllegalStateException(EVT_MAP[previousEvent] + " doesn't support getLong()");
        } else if (isCurrentNumberIntegral && currentIntegralNumber != Integer.MIN_VALUE) {
            return currentIntegralNumber;
        } else if (isCurrentNumberIntegral) {
            //if there a content in the value buffer read from them, if not use main buffer
            final Long retVal = fallBackCopyBufferLength > 0 ? parseLongFromChars(fallBackCopyBuffer, 0, fallBackCopyBufferLength)
                    : parseLongFromChars(buffer, startOfValueInBuffer, endOfValueInBuffer);
            if (retVal == null) {
                return getBigDecimal().longValue();
            } else {
                return retVal.longValue();
            }
        } else {
            return getBigDecimal().longValue();
        }

    }

    @Override
    public boolean isFitLong() { // not exact but good enough for most cases
        if (!isCurrentNumberIntegral) {
            return false;
        }

        // no buffer overflow - assumes a buffer can hold a long
        // + length <= since max long is 9223372036854775807 and min is -9223372036854775808
        final int len = endOfValueInBuffer - startOfValueInBuffer;
        return fallBackCopyBufferLength <= 0 && len > 0 && len <= 18;
    }

    @Override
    public BigDecimal getBigDecimal() {
        if (previousEvent != VALUE_NUMBER) {
            throw new IllegalStateException(EVT_MAP[previousEvent] + " doesn't support getBigDecimal()");
            //        } else if (currentBigDecimalNumber != null) {
            //            return currentBigDecimalNumber;
        } else if (isCurrentNumberIntegral && currentIntegralNumber != Integer.MIN_VALUE) {
            return new BigDecimal(currentIntegralNumber);
        }
        //if there a content in the value buffer read from them, if not use main buffer
        return (/*currentBigDecimalNumber = */fallBackCopyBufferLength > 0 ? new BigDecimal(fallBackCopyBuffer, 0,
                fallBackCopyBufferLength) : new BigDecimal(buffer, startOfValueInBuffer, (endOfValueInBuffer - startOfValueInBuffer)));
    }

    @Override
    public JsonLocation getLocation() {
        return createLocation();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        bufferProvider.release(buffer);
        if (releaseFallBackCopyBufferLength) {
            valueProvider.release(fallBackCopyBuffer);
        }

        try {
            in.close();
        } catch (final IOException e) {
            throw new JsonException("Unexpected IO exception " + e.getMessage(), e);
        } finally {
            closed = true;
        }
    }

    //parse a char[] to long while checking overflow
    //if overflowed return null
    //no additional checks since we are sure here that there are no non digits in the array
    private static Long parseLongFromChars(final char[] chars, final int start, final int end) {

        long retVal = 0;
        final boolean negative = chars[start] == MINUS;
        for (int i = negative ? start + 1 : start; i < end; i++) {
            final long tmp = retVal * 10 + (chars[i] - ZERO);
            if (tmp < retVal) { //check overflow
                return null;
            } else {
                retVal = tmp;
            }
        }

        return negative ? -retVal : retVal;
    }

    //parse a char[] to int while checking overflow
    //if overflowed return null
    //no additional checks since we are sure here that there are no non digits in the array
    private static Integer parseIntegerFromChars(final char[] chars, final int start, final int end) {

        int retVal = 0;
        final boolean negative = chars[start] == MINUS;
        for (int i = negative ? start + 1 : start; i < end; i++) {
            final int tmp = retVal * 10 + (chars[i] - ZERO);
            if (tmp < retVal) { //check overflow
                return null;
            } else {
                retVal = tmp;
            }
        }

        return negative ? -retVal : retVal;
    }

    private JsonParsingException uexc(final char c, final String message) {
        final JsonLocation location = createLocation();
        return new JsonParsingException("Unexpected character '" + c + "' (Codepoint: " + String.valueOf(c).codePointAt(0) + ") on "
                + location + ". Reason is [[" + message + "]]", location);
    }

    private JsonParsingException uexc(final String message) {
        final char c = bufferPos < 0 ? 0 : buffer[bufferPos];
        return uexc(c, message);
    }

    private JsonParsingException tmc() {
        final JsonLocation location = createLocation();
        return new JsonParsingException("Too many characters. Maximum string/number length of " + maxValueLength + " exceeded on "
                + location + ". Maybe increase org.apache.johnzon.max-string-length in jsonp factory properties or system properties.", location);
    }

    private JsonParsingException uexio(final IOException e) {
        final JsonLocation location = createLocation();
        return new JsonParsingException("Unexpected IO exception on " + location, e, location);
    }

    private JsonParsingException cust(final String message) {
        final JsonLocation location = createLocation();
        return new JsonParsingException("General exception on " + location + ". Reason is [[" + message + "]]", location);
    }

}