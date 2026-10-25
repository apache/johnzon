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

import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.stream.JsonParsingException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NumberLengthTest {
    @Test
    public void numberAtDefaultLimit() {
        final String number = "1".repeat(JsonParserFactoryImpl.DEFAULT_MAX_NUMBER_LENGTH);
        assertEquals(new BigDecimal(number), read(Map.of(), number).getJsonNumber("x").bigDecimalValue());
    }

    @Test(expected = JsonParsingException.class)
    public void numberAboveDefaultLimit() {
        read(Map.of(), "1".repeat(JsonParserFactoryImpl.DEFAULT_MAX_NUMBER_LENGTH + 1));
    }

    @Test(expected = JsonParsingException.class)
    public void decimalAboveDefaultLimit() {
        read(Map.of(), "1." + "1".repeat(JsonParserFactoryImpl.DEFAULT_MAX_NUMBER_LENGTH));
    }

    @Test
    public void numberAcrossBuffersAtLimit() {
        final Map<String, Object> config = new HashMap<>();
        config.put(JsonParserFactoryImpl.MAX_NUMBER_LENGTH, "10");
        config.put(JsonParserFactoryImpl.BUFFER_LENGTH, "2");
        assertEquals(new BigDecimal("1234567890"), read(config, "1234567890").getJsonNumber("x").bigDecimalValue());
    }

    @Test(expected = JsonParsingException.class)
    public void numberAcrossBuffersAboveLimit() {
        final Map<String, Object> config = new HashMap<>();
        config.put(JsonParserFactoryImpl.MAX_NUMBER_LENGTH, "10");
        config.put(JsonParserFactoryImpl.BUFFER_LENGTH, "2");
        read(config, "12345678901");
    }

    @Test
    public void negativeAndExponentAtLimit() {
        final Map<String, Object> config = new HashMap<>();
        config.put(JsonParserFactoryImpl.MAX_NUMBER_LENGTH, 7);
        config.put(JsonParserFactoryImpl.BUFFER_LENGTH, 2);
        assertEquals(new BigDecimal("-1.2e+3"), read(config, "-1.2e+3").getJsonNumber("x").bigDecimalValue());
    }

    @Test(expected = JsonParsingException.class)
    public void negativeNumberAboveLimit() {
        read(Map.of(JsonParserFactoryImpl.MAX_NUMBER_LENGTH, 1), "-1");
    }

    @Test(expected = JsonParsingException.class)
    public void exponentAboveLimit() {
        read(Map.of(JsonParserFactoryImpl.MAX_NUMBER_LENGTH, 6), "-1.2e+3");
    }

    @Test
    public void numberLengthCheckedDuringRead() {
        final CountingReader input = new CountingReader(4_096);
        final Map<String, Object> config = Map.of(
                JsonParserFactoryImpl.MAX_NUMBER_LENGTH, 10,
                JsonParserFactoryImpl.BUFFER_LENGTH, 32);
        try (JsonReader reader = Json.createReaderFactory(config).createReader(input)) {
            try {
                reader.readObject();
                fail("expected a JsonParsingException");
            } catch (final JsonParsingException expected) {
                assertTrue("characters read: " + input.charactersRead,
                        input.charactersRead <= 64);
            }
        }
    }

    @Test
    public void stringLengthAcrossBuffers() {
        final Map<String, Object> config = Map.of(
                JsonParserFactoryImpl.MAX_STRING_LENGTH, 10,
                JsonParserFactoryImpl.BUFFER_LENGTH, 2);
        assertEquals("1".repeat(10), readString(config, "1".repeat(10)));
        try {
            readString(config, "1".repeat(11));
            fail("expected a JsonParsingException");
        } catch (final JsonParsingException expected) {
            assertTrue(expected.getMessage().contains("string/number length"));
        }
    }

    @Test
    public void escapedStringLengthAcrossBuffers() {
        final String atLimit = "aaaaa\\naaaa";
        final String aboveLimit = atLimit + "a";
        for (final boolean comments : new boolean[] {false, true}) {
            final Map<String, Object> config = Map.of(
                    JsonParserFactoryImpl.MAX_STRING_LENGTH, 10,
                    JsonParserFactoryImpl.BUFFER_LENGTH, 2,
                    JsonParserFactoryImpl.SUPPORTS_COMMENTS, comments);
            assertEquals("aaaaa\naaaa", readString(config, atLimit));
            try {
                readString(config, aboveLimit);
                fail("expected a JsonParsingException");
            } catch (final JsonParsingException expected) {
                assertTrue(expected.getMessage().contains("string/number length"));
            }
        }
    }

    @Test
    public void stringAtLimitWithFixedValueBuffer() {
        final Map<String, Object> config = Map.of(
                JsonParserFactoryImpl.MAX_STRING_LENGTH, 10,
                JsonParserFactoryImpl.MAX_NUMBER_LENGTH, 1,
                JsonParserFactoryImpl.BUFFER_LENGTH, 2,
                JsonParserFactoryImpl.AUTO_ADJUST_STRING_BUFFER, false);
        assertEquals("abcdefghij", readString(config, "abcdefghij"));
        assertEquals("\n".repeat(10), readString(config, "\\n".repeat(10)));
    }

    @Test
    public void stringLengthCheckedDuringRead() {
        final CountingReader input = new CountingReader("{\"x\":\"", 4_096, "\"}");
        final Map<String, Object> config = Map.of(
                JsonParserFactoryImpl.MAX_STRING_LENGTH, 10,
                JsonParserFactoryImpl.BUFFER_LENGTH, 32);
        try (JsonReader reader = Json.createReaderFactory(config).createReader(input)) {
            try {
                reader.readObject();
                fail("expected a JsonParsingException");
            } catch (final JsonParsingException expected) {
                assertTrue("characters read: " + input.charactersRead,
                        input.charactersRead <= 64);
            }
        }
    }

    @Test
    public void numberLimitIndependentOfStringLimit() {
        final Map<String, Object> config = Map.of(
                JsonParserFactoryImpl.MAX_STRING_LENGTH, 10,
                JsonParserFactoryImpl.MAX_NUMBER_LENGTH, 20,
                JsonParserFactoryImpl.BUFFER_LENGTH, 2);
        final String number = "1".repeat(20);
        assertEquals(new BigDecimal(number), read(config, number).getJsonNumber("x").bigDecimalValue());
    }

    @Test
    public void numberWithSmallStringLimitAndFixedValueBuffer() {
        final String number = "12345678901234567890";
        for (final String strategy : new String[] {"BY_INSTANCE", "SINGLETON"}) {
            for (final boolean autoAdjust : new boolean[] {false, true}) {
                final Map<String, Object> config = Map.of(
                        JsonParserFactoryImpl.MAX_STRING_LENGTH, 10,
                        JsonParserFactoryImpl.MAX_NUMBER_LENGTH, 20,
                        JsonParserFactoryImpl.BUFFER_LENGTH, 2,
                        JsonParserFactoryImpl.AUTO_ADJUST_STRING_BUFFER, autoAdjust,
                        AbstractJsonFactory.BUFFER_STRATEGY, strategy);
                assertEquals(new BigDecimal(number), read(config, number).getJsonNumber("x").bigDecimalValue());
            }
        }
    }

    @Test(expected = JsonParsingException.class)
    public void topLevelNumberAboveLimit() {
        final Map<String, Object> config = new HashMap<>();
        config.put(JsonParserFactoryImpl.MAX_NUMBER_LENGTH, "10");
        Json.createReaderFactory(config).createReader(new StringReader("12345678901")).readValue();
    }

    @Test
    public void configuredLimit() {
        final String number = "1".repeat(5000);
        final Map<String, Object> config = new HashMap<>();
        config.put(JsonParserFactoryImpl.MAX_NUMBER_LENGTH, "5000");
        assertEquals(new BigDecimal(number), read(config, number).getJsonNumber("x").bigDecimalValue());
    }

    @Test
    public void stringsAreNotAffected() {
        final String value = "1".repeat(JsonParserFactoryImpl.DEFAULT_MAX_NUMBER_LENGTH * 10);
        final JsonObject object = Json.createReader(new StringReader("{\"x\":\"" + value + "\"}")).readObject();
        assertEquals(value, object.getString("x"));
    }

    private static JsonObject read(final Map<String, ?> config, final String number) {
        final JsonReaderFactory factory = Json.createReaderFactory(config);
        return factory.createReader(new StringReader("{\"x\":" + number + "}")).readObject();
    }

    private static String readString(final Map<String, ?> config, final String value) {
        final JsonReaderFactory factory = Json.createReaderFactory(config);
        return factory.createReader(new StringReader("{\"x\":\"" + value + "\"}")).readObject().getString("x");
    }

    private static class CountingReader extends Reader {
        private final String prefix;
        private final String suffix;
        private final int digits;
        private int charactersRead;

        private CountingReader(final int digits) {
            this("{\"x\":", digits, "}");
        }

        private CountingReader(final String prefix, final int digits, final String suffix) {
            this.prefix = prefix;
            this.digits = digits;
            this.suffix = suffix;
        }

        @Override
        public int read(final char[] chars, final int offset, final int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            final int total = prefix.length() + digits + suffix.length();
            if (charactersRead >= total) {
                return -1;
            }
            final int count = Math.min(length, total - charactersRead);
            for (int i = 0; i < count; i++) {
                final int index = charactersRead + i;
                chars[offset + i] = index < prefix.length() ? prefix.charAt(index)
                        : index < prefix.length() + digits ? '1' : suffix.charAt(index - prefix.length() - digits);
            }
            charactersRead += count;
            return count;
        }

        @Override
        public void close() {
        }
    }
}
