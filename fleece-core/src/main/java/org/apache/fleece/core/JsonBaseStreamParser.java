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
import javax.json.stream.JsonParsingException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.fleece.core.Strings.asEscapedChar;

public abstract class JsonBaseStreamParser implements JsonChars,
        EscapedStringAwareJsonParser {
    private static final Logger LOGGER = Logger.getLogger(JsonBaseStreamParser.class.getName());
    private static final boolean LOG = LOGGER.isLoggable(Level.FINE);

    private final int maxStringSize;

    // current state
    private Event event = null;
    private Event lastEvent = null;
    private int lastSignificantChar = -1;

    protected final char[] currentValue;
    private int valueLength = 0;

    // location
    private int line = 1;
    private int column = 1;
    private int offset = 0;

    private boolean constructingStringValue = false;
    private boolean withinArray = false;
    private boolean stringValueIsKey = false;
    
    private boolean isCurrentNumberIntegral = false;
    private Integer currentIntegralNumber = null; //for number from 0 - 9
    private BigDecimal currentBigDecimalNumber = null;

    private int openObjects = 0;
    private int openArrays = 0;
    private boolean escaped = false;

    protected JsonBaseStreamParser(final int maxStringLength, final char[] valueBuffer) {
        this.maxStringSize = maxStringLength <= 0 ? 8192 : maxStringLength;
        this.currentValue = valueBuffer;
    }

    private void appendValue(final char c) {
        if (valueLength >= maxStringSize) {
            throw new JsonParsingException("to many chars", createLocation());
        }

        currentValue[valueLength] = c;
        valueLength++;
    }

    private void resetValue() {
        valueLength = 0;

    }

    private String getValue() {
        return new String(currentValue, 0, valueLength);
    }

    @Override
    public final boolean hasNext() {
        return event == null || !(openArrays == 0 && openObjects == 0);
    }

    private static boolean isAsciiDigit(final char value) {
        return value >= ZERO && value <= NINE;
    }

    private static boolean isHexDigit(final char value) {
        return isAsciiDigit(value) || (value >= 'a' && value <= 'f')
                || (value >= 'A' && value <= 'F');
    }

    private JsonLocationImpl createLocation() {
        return new JsonLocationImpl(line, column, offset);
    }

    private boolean ifConstructingStringValueAdd(char c) throws IOException {
        if (escaped) {

            if (c == 'u') {
                final char[] tmp = read(4);

                for (final char aTmp : tmp) {
                    if (!isHexDigit(aTmp)) {
                        throw new JsonParsingException("unexpected character " + aTmp, createLocation());
                    }
                }

                if (LOG) {
                    LOGGER.fine((int) tmp[3] + "/" + (int) tmp[2] + "/"
                            + (int) tmp[1] + "/" + (int) tmp[0]);
                }

                final int decimal = ((tmp[3]) - 48) + ((tmp[2]) - 48) * 16
                        + ((tmp[1]) - 48) * 256 + ((tmp[0]) - 48) * 4096;
                c = (char) decimal;

            } else {
                c = asEscapedChar(c);
            }

            escaped = false;
        }

        return ifConstructingStringValueAdd(c, false);
    }

    private boolean ifConstructingStringValueAdd(final char c,
            final boolean escape) {
        if (constructingStringValue) {

            appendValue(escape ? Strings.asEscapedChar(c) : c);
        }
        return constructingStringValue;
    }

    protected abstract char readNextChar() throws IOException;

    protected abstract void mark();

    private void resetToMark() {

        reset();
        offset--;
        column--;
    }

    protected abstract void reset();

    private char read() throws IOException {
        final char c = readNextChar();

        if (LOG) {
            LOGGER.fine("reading: " + c + " -> " + ((int) c));
        }

        if (c == -1) {
            // hasNext = false;
            throw new NoSuchElementException();
        }

        offset++;
        column++;

        return c;
    }

    private char[] read(final int count) throws IOException {
        final char[] tmp = new char[count];

        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = read();

        }

        return tmp;
    }

    // Event.START_ARRAY
    // Event.START_OBJECT

    // Event.END_ARRAY
    // Event.END_OBJECT

    // Event.KEY_NAME

    // ** 5 Value Event
    // Event.VALUE_FALSE
    // Event.VALUE_NULL
    // Event.VALUE_NUMBER
    // Event.VALUE_STRING
    // Event.VALUE_TRUE

    // ***********************
    // ***********************
    // Significant chars (8)

    // 0 - start doc
    // " - quote
    // , - comma

    // : - separator
    // { - start obj
    // } - end obj
    // [ - start arr
    // ] - end arr

    @Override
    public final Event next() {
        
        //fast fail
        if(!hasNext()) {
            throw new NoSuchElementException();
        }

        int dosCount = 0;
        lastEvent = event;
        event = null;
        isCurrentNumberIntegral = false;
        currentBigDecimalNumber = null;
        currentIntegralNumber = null;

        resetValue();

        try {
            while (true) {
                final char c = read();

                switch (c) {

                case START_OBJECT_CHAR:

                    if (ifConstructingStringValueAdd(c)) {
                        continue;
                    }

                    handleStartObject(c);

                    break;

                case END_OBJECT_CHAR:

                    if (ifConstructingStringValueAdd(c)) {
                        continue;
                    }

                    handleEndObject(c);

                    break;
                case START_ARRAY_CHAR:

                    if (ifConstructingStringValueAdd(c)) {
                        continue;
                    }

                    handleStartArray(c);

                    break;
                case END_ARRAY_CHAR:

                    if (ifConstructingStringValueAdd(c)) {
                        continue;
                    }

                    handleEndArray(c);
                    break;
                case EOL:
                    if (ifConstructingStringValueAdd(c)) {
                        throw new JsonParsingException("Unexpected character "
                                + c + " (" + (int) c + ")", createLocation());
                    }
                    line++;
                    continue; // eol no allowed within a value

                case TAB:
                case CR:
                case SPACE:
                    if (ifConstructingStringValueAdd(c)) { // escaping

                        continue;

                    } else {
                        // dos check
                        if (dosCount >= maxStringSize) {
                            throw new JsonParsingException(
                                    "max string size reached", createLocation());
                        }
                        dosCount++;
                    }

                    break;
                case COMMA:
                    if (ifConstructingStringValueAdd(c)) {
                        continue;
                    }

                    if (lastSignificantChar >= 0
                            && (char) lastSignificantChar != QUOTE
                            && (char) lastSignificantChar != END_ARRAY_CHAR
                            && (char) lastSignificantChar != END_OBJECT_CHAR) {
                        throw new JsonParsingException("Unexpected character "
                                + c + " (last significant was "
                                + lastSignificantChar + ")", createLocation());
                    }

                    lastSignificantChar = c;

                    stringValueIsKey = true;
                    if (LOG) {
                        LOGGER.fine(" VAL_IS_KEY");
                    }

                    break;
                case KEY_SEPARATOR:
                    if (ifConstructingStringValueAdd(c)) {
                        continue;
                    }

                    if (lastSignificantChar >= 0
                            && (char) lastSignificantChar != QUOTE) {
                        throw new JsonParsingException("Unexpected character "
                                + c, createLocation());
                    }

                    lastSignificantChar = c;

                    stringValueIsKey = false;
                    if (LOG) {
                        LOGGER.fine(" VAL_IS_VALUE");
                    }

                    break;

                case QUOTE: // must be escaped within a value

                    if (handleQuote(c)) {
                        continue;
                    } else {
                        break;
                    }

                    // non string values (literals)
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

                    if (ifConstructingStringValueAdd(c)) {
                        continue;
                    }

                    handleLiteral(c);

                    break;

                // escape char
                case ESCAPE_CHAR:// must be escaped within a value
                    if (!constructingStringValue) {
                        throw new JsonParsingException("Unexpected character "
                                + c, createLocation());
                    }

                    if (escaped) {
                        if (LOG) {
                            LOGGER.fine(" ESCAPEDESCAPED");
                        }

                        appendValue(ESCAPE_CHAR);
                        escaped = false;
                    } else {
                        if (LOG) {
                            LOGGER.fine(" ESCAPECHAR");
                        }
                        escaped = true;
                    }

                    break;

                // eof
                case EOF:

                    throw new NoSuchElementException();

                default:
                    if (ifConstructingStringValueAdd(c)) {
                        continue;
                    }
                    lastSignificantChar = -2;
                    throw new JsonParsingException("Unexpected character " + c,
                            createLocation());

                }

                if (event != null) {

                    if (LOG) {
                        LOGGER.fine(" +++ +++ +++ +++ +++ +++" + event
                                + "::" + getValue());
                    }

                    return event;

                }

            }
        } catch (final IOException e) {
            throw new JsonParsingException("Unexpected IO Exception", e, createLocation());
        }
    }

    private void handleStartObject(final char c) {

        if (LOG) {
            LOGGER.fine(" LASIC " + lastSignificantChar);
        }

        if (lastSignificantChar == -2
                || (lastSignificantChar != -1
                        && (char) lastSignificantChar != KEY_SEPARATOR
                        && (char) lastSignificantChar != COMMA && (char) lastSignificantChar != START_ARRAY_CHAR)) {
            throw new JsonParsingException("Unexpected character " + c
                    + " (last significant was " + lastSignificantChar + ")",
                    createLocation());
        }

        stringValueIsKey = true;
        withinArray = false;
        if (LOG) {
            LOGGER.fine(" VAL_IS_KEY");
        }

        lastSignificantChar = c;
        openObjects++;
        event = Event.START_OBJECT;

    }

    private void handleEndObject(final char c) {
        if (lastSignificantChar >= 0
                && (char) lastSignificantChar != START_OBJECT_CHAR
                && (char) lastSignificantChar != END_ARRAY_CHAR
                && (char) lastSignificantChar != QUOTE
                && (char) lastSignificantChar != END_OBJECT_CHAR) {
            throw new JsonParsingException("Unexpected character " + c
                    + " (last significant was " + lastSignificantChar + ")",
                    createLocation());
        }

        if (openObjects == 0) {
            throw new JsonParsingException("Unexpected character " + c,
                    createLocation());
        }

        lastSignificantChar = c;
        openObjects--;
        event = Event.END_OBJECT;
    }

    private void handleStartArray(final char c) {
        withinArray = true;

        if (lastSignificantChar == -2
                || (lastSignificantChar != -1
                        && (char) lastSignificantChar != KEY_SEPARATOR
                        && (char) lastSignificantChar != COMMA && (char) lastSignificantChar != START_ARRAY_CHAR)) {
            throw new JsonParsingException("Unexpected character " + c
                    + " (last significant was " + lastSignificantChar + ")",
                    createLocation());
        }

        lastSignificantChar = c;
        openArrays++;
        event = Event.START_ARRAY;
    }

    private void handleEndArray(final char c) {
        withinArray = false;

        if (lastSignificantChar >= 0
                && (char) lastSignificantChar != START_ARRAY_CHAR
                && (char) lastSignificantChar != END_ARRAY_CHAR
                && (char) lastSignificantChar != END_OBJECT_CHAR
                && (char) lastSignificantChar != QUOTE) {
            throw new JsonParsingException("Unexpected character " + c
                    + " (last significant was " + lastSignificantChar + ")",
                    createLocation());
        }

        if (openArrays == 0) {
            throw new JsonParsingException("Unexpected character " + c,
                    createLocation());
        }

        lastSignificantChar = c;
        openArrays--;

        event = Event.END_ARRAY;
    }

    private boolean handleQuote(final char c) {

        if (lastSignificantChar >= 0 && (char) lastSignificantChar != QUOTE
                && (char) lastSignificantChar != KEY_SEPARATOR
                && (char) lastSignificantChar != START_OBJECT_CHAR
                && (char) lastSignificantChar != START_ARRAY_CHAR
                && (char) lastSignificantChar != COMMA) {
            throw new JsonParsingException("Unexpected character " + c
                    + " (last significant was " + lastSignificantChar + ")",
                    createLocation());
        }

        lastSignificantChar = c;

        if (constructingStringValue) {

            if (escaped) {
                appendValue(QUOTE);
                escaped = false;
                return true;
            } else {

                if (!withinArray && stringValueIsKey) {
                    event = Event.KEY_NAME;
                    stringValueIsKey = false;
                    if (LOG) {
                        LOGGER.fine(" VAL_IS_VALUE");
                    }
                } else {

                    if (lastEvent != Event.KEY_NAME && !withinArray) {
                        throw new JsonParsingException("Unexpected character "
                                + c + " (lastevent " + lastEvent
                                + ", comma missing)", createLocation());
                    }

                    // string value end
                    event = Event.VALUE_STRING;
                }

                constructingStringValue = false;

                return false;
            }
        } else {

            if (escaped) {
                throw new JsonParsingException("Unexpected character " + c,
                        createLocation());
            }

            // string value start
            resetValue();
            constructingStringValue = true;
            return false;
        }

    }

    private void handleLiteral(final char c) throws IOException {
        if (lastSignificantChar >= 0 && lastSignificantChar != KEY_SEPARATOR
                && lastSignificantChar != COMMA
                && lastSignificantChar != START_ARRAY_CHAR) {
            throw new JsonParsingException("unexpected character " + c,
                    createLocation());
        }

        lastSignificantChar = -2;

        resetValue();

        if (lastSignificantChar != QUOTE) {
            // probe literals
            switch (c) {
            case TRUE_T:
                final char[] tmpt = read(3);
                if (tmpt[0] != TRUE_R || tmpt[1] != TRUE_U || tmpt[2] != TRUE_E) {
                    throw new JsonParsingException("Unexpected literal " + c
                            + new String(tmpt), createLocation());
                }
                event = Event.VALUE_TRUE;
                break;
            case FALSE_F:
                final char[] tmpf = read(4);
                if (tmpf[0] != FALSE_A || tmpf[1] != FALSE_L
                        || tmpf[2] != FALSE_S || tmpf[3] != FALSE_E) {
                    throw new JsonParsingException("Unexpected literal " + c
                            + new String(tmpf), createLocation());
                }

                event = Event.VALUE_FALSE;
                break;
            case NULL_N:
                final char[] tmpn = read(3);
                if (tmpn[0] != NULL_U || tmpn[1] != NULL_L || tmpn[2] != NULL_L) {
                    throw new JsonParsingException("Unexpected literal " + c
                            + new String(tmpn), createLocation());
                }
                event = Event.VALUE_NULL;
                break;

            default: // number
                appendValue(c);

                boolean endExpected = false;
                final boolean zeropassed = c == '0';
                final boolean beginningMinusPassed = c == '-';
                boolean dotpassed = false;
                boolean epassed = false;
                char last = c;
                int i = -1;

                while (true) {
                    i++;

                    if (LOG) {
                        LOGGER.fine("while i:" + i);
                    }

                    final char n = read();
                    mark();

                    if (n == COMMA || n == END_ARRAY_CHAR
                            || n == END_OBJECT_CHAR) {
                        resetToMark();
                        
                        isCurrentNumberIntegral=(!dotpassed && !epassed);
                        
                        if(isCurrentNumberIntegral && beginningMinusPassed && i==1 && last >= '0' && last <= '9')
                        {
                            currentIntegralNumber=-((int)last - 48); //optimize -0 till -99
                        }
                        
                        if(isCurrentNumberIntegral && !beginningMinusPassed && i==0 && last >= '0' && last <= '9')
                        {
                            currentIntegralNumber=((int)last - 48); //optimize 0 till 9
                        }
                        
                        
                        event = Event.VALUE_NUMBER;
                        break;
                    }

                    if (n == EOL) {
                        last = n;
                        continue;
                    }

                    if (endExpected && n != SPACE && n != TAB && n != CR) {
                        throw new JsonParsingException("unexpected character "
                                + n + " (" + (int) n + ")", createLocation());
                    }

                    if (n == SPACE || n == TAB || n == CR) {
                        endExpected = true;
                        last = n;
                        continue;
                    }

                    if (!isNumber(n)) {
                        throw new JsonParsingException("unexpected character "
                                + n, createLocation());
                    }

                    // minus only allowed as first char or after e/E
                    if (n == MINUS && i != 0 && last != EXP_LOWERCASE
                            && last != EXP_UPPERCASE) {
                        throw new JsonParsingException("unexpected character "
                                + n, createLocation());
                    }

                    // plus only allowed after e/E
                    if (n == PLUS && last != EXP_LOWERCASE
                            && last != EXP_UPPERCASE) {
                        throw new JsonParsingException("unexpected character "
                                + n, createLocation());
                    }

                    if (!dotpassed && zeropassed && i == 0 && n != DOT) {
                        throw new JsonParsingException("unexpected character "
                                + n + " (no leading zeros allowed)",
                                createLocation());
                    }

                    if (LOG) {
                        LOGGER.fine("dotpassed:" + dotpassed
                                + "/zeropassed:" + zeropassed + "/i:" + i
                                + "/n:" + n);
                    }

                    if (n == DOT) {

                        if (dotpassed) {
                            throw new JsonParsingException("more than one dot",
                                    createLocation());
                        }

                        dotpassed = true;

                    }

                    if (n == EXP_LOWERCASE || n == EXP_UPPERCASE) {

                        if (epassed) {
                            throw new JsonParsingException("more than one e/E",
                                    createLocation());
                        }

                        epassed = true;
                    }

                    appendValue(n);
                    last = n;

                }

                break;

            }

        } else {
            throw new JsonParsingException("Unexpected character " + c,
                    createLocation());
        }

    }

    private boolean isNumber(final char c) {
        return isAsciiDigit(c) || c == DOT || c == MINUS || c == PLUS
                || c == EXP_LOWERCASE || c == EXP_UPPERCASE;
    }

    @Override
    public String getString() {
        if (event == Event.KEY_NAME || event == Event.VALUE_STRING
                || event == Event.VALUE_NUMBER) {
            return getValue();
        }
        throw new IllegalStateException(event + " doesn't support getString()");
    }

    @Override
    public boolean isIntegralNumber() {

        if (event != Event.VALUE_NUMBER) {
            throw new IllegalStateException(event
                    + " doesn't support isIntegralNumber()");
        }

        return isCurrentNumberIntegral;
    }

    @Override
    public int getInt() {
        if (event != Event.VALUE_NUMBER) {
            throw new IllegalStateException(event + " doesn't support getInt()");
        }

        if (isCurrentNumberIntegral && currentIntegralNumber != null) {
            return currentIntegralNumber.intValue();
        }

        return getBigDecimal().intValue();
    }

    @Override
    public long getLong() {
        if (event != Event.VALUE_NUMBER) {
            throw new IllegalStateException(event + " doesn't support getLong()");
        }

        if (isCurrentNumberIntegral && currentIntegralNumber != null) {
            return currentIntegralNumber.intValue();
        } // int is ok, its only from 0-9

        return getBigDecimal().longValue();
    }

    @Override
    public BigDecimal getBigDecimal() {
        if (event != Event.VALUE_NUMBER) {
            throw new IllegalStateException(event + " doesn't support getBigDecimal()");
        }

        if (currentBigDecimalNumber != null) {
            return currentBigDecimalNumber;
        }

        return (currentBigDecimalNumber = new BigDecimal(getString()));
    }

    @Override
    public JsonLocation getLocation() {
        return createLocation();
    }

    protected abstract void closeUnderlyingSource() throws IOException;

    @Override
    public void close() {
        try {
            closeUnderlyingSource();
        } catch (final IOException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }
     
    @Override
    public String getEscapedString() {
        return Strings.escape(getValue());
    }

}