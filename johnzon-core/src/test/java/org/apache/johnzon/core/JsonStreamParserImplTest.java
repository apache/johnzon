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

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.spi.JsonProvider;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.json.stream.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonStreamParserImplTest {
    @Test
    public void getBigDecimalChecksDefaultScale() {
        final JsonProviderImpl provider = new JsonProviderImpl();
        assertEquals(new BigDecimal("1e1000"), parseBigDecimal(provider, "1e1000"));
        assertThrows(ArithmeticException.class, () -> parseBigDecimal(provider, "1e1001"));
        assertThrows(ArithmeticException.class, () -> parseBigDecimal(provider, "1e-1001"));
    }

    @Test
    public void getBigDecimalChecksConfiguredScale() {
        final JsonProviderImpl provider = new JsonProviderImpl();
        provider.setMaxBigDecimalScale(2);
        assertEquals(new BigDecimal("1e2"), parseBigDecimal(provider, "1e2"));
        assertEquals(new BigDecimal("1e-2"), parseBigDecimal(provider, "1e-2"));
        assertThrows(ArithmeticException.class, () -> parseBigDecimal(provider, "1e3"));
        assertThrows(ArithmeticException.class, () -> parseBigDecimal(provider, "1e-3"));
    }

    @Test
    public void getValueKeepsCompactNumberGuarded() {
        final JsonProviderImpl provider = new JsonProviderImpl();
        provider.setMaxBigDecimalScale(2);
        try (JohnzonJsonParser parser = (JohnzonJsonParser) provider.createParser(new StringReader("[1e3]"))) {
            assertEquals(JsonParser.Event.START_ARRAY, parser.next());
            assertEquals(JsonParser.Event.VALUE_NUMBER, parser.next());
            assertEquals(1000, parser.getInt());
            assertEquals(1000L, parser.getLong());
            assertThrows(ArithmeticException.class, parser::getBigDecimal);
            final JsonNumber number = (JsonNumber) parser.getValue();
            assertEquals("1E+3", number.toString());
            assertThrows(ArithmeticException.class, number::bigDecimalValue);
            assertThrows(ArithmeticException.class, number::numberValue);
        }
    }

    private BigDecimal parseBigDecimal(final JsonProviderImpl provider, final String value) {
        try (JsonParser parser = provider.createParser(new StringReader("[" + value + "]"))) {
            assertEquals(JsonParser.Event.START_ARRAY, parser.next());
            assertEquals(JsonParser.Event.VALUE_NUMBER, parser.next());
            return parser.getBigDecimal();
        }
    }

    @Test
    public void testSpecCurrentEvent() {
        String json = "{}";

        final JsonParser parser = new JsonStreamParserImpl(new ByteArrayInputStream(json
                .getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8,
                10,
                BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10),
                BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10),
                true, (JsonProviderImpl) JsonProviderImpl.provider());

        assertEquals(null, parser.currentEvent());

        parser.next();
        assertEquals(JsonParser.Event.START_OBJECT, parser.currentEvent());
    }

    @Test
    public void testJohnzonParserCurrent() {
        String json = "{}";

        final JohnzonJsonParser parser = new JsonStreamParserImpl(new ByteArrayInputStream(json
                .getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8,
                10,
                BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10),
                BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10),
                true, (JsonProviderImpl) JsonProviderImpl.provider());

        assertEquals(JsonParser.Event.START_OBJECT, parser.current());
    }

    @Test
    public void ensureNoArrayBoundErrorWhenOverflow() throws IOException {
        final String json = new JsonObjectBuilderImpl(
            emptyMap(),
            BufferStrategyFactory.valueOf("QUEUE").newCharProvider(100),
            RejectDuplicateKeysMode.TRUE, (JsonProviderImpl) JsonProviderImpl.provider())
                .add("content", "{\"foo\":\"barbar\\barbarbar\"}")
                .build()
                .toString();
        final JsonParser parser = new JsonStreamParserImpl(new ByteArrayInputStream(json
                .getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8,
                10,
                BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10),
                BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10),
                true, (JsonProviderImpl) JsonProviderImpl.provider());
        final List<String> events = new ArrayList<>();
        while (parser.hasNext()) {
            final JsonParser.Event event = parser.next();
            events.add(event.name());
            switch (event) {
                case VALUE_STRING:
                    events.add(parser.getString());
                    break;
                default:
            }
        }
        parser.close();
        assertEquals(
                asList("START_OBJECT", "KEY_NAME", "VALUE_STRING", "{\"foo\":\"barbar\\barbarbar\"}", "END_OBJECT"),
                events);
    }

    @Test
    @Ignore("No real test, just run directly from your IDE")
    public void largeStringPerformance() {
        String json = "{\"data\":\"" + "a".repeat(50 * 1024 * 1024 + 1) + "\"}";

        // Warmup
        for (int i = 0; i < 10; i++) {
            try (JsonReader reader = Json.createReader(new StringReader(json))) {
                reader.readObject();
            }
        }

        long start = System.currentTimeMillis();
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            reader.readObject();
        }
        System.err.println("Took " + (System.currentTimeMillis() - start) + "ms");
    }

    @Test
    public void allBuffersReleased() {
        String json = "{\"data\":\"" + "a".repeat(JsonParserFactoryImpl.DEFAULT_MAX_STRING_LENGTH * 2) + "\"}";

        JsonReaderFactory readerFactory = JsonProvider.provider().createReaderFactory(Map.of(
                JsonParserFactoryImpl.BUFFER_STRATEGY, TrackingBufferStrategy.class.getName()));

        try (JsonReader reader = readerFactory.createReader(new StringReader(json))) {
            reader.readObject();
        }

        assertTrue(TrackingBufferStrategy.TrackingBufferProvider.borrowed.isEmpty());
    }

    public static class TrackingBufferStrategy implements BufferStrategy {
        private final BufferStrategy delegate = BufferStrategyFactory.valueOf("BY_INSTANCE");

        @Override
        public BufferProvider<char[]> newCharProvider(int size) {
            return new TrackingBufferProvider(delegate.newCharProvider(size));
        }

        public static class TrackingBufferProvider implements BufferStrategy.BufferProvider<char[]> {
            protected static List<char[]> borrowed = new ArrayList<>();

            private final BufferStrategy.BufferProvider<char[]> delegate;

            public TrackingBufferProvider(BufferStrategy.BufferProvider<char[]> delegate) {
                this.delegate = delegate;
            }

            @Override
            public char[] newBuffer() {
                char[] result = delegate.newBuffer();
                borrowed.add(result);

                return result;
            }

            @Override
            public void release(char[] value) {
                borrowed.remove(value);
                delegate.release(value);
            }
        }
    }
}
