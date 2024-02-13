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

import org.junit.Ignore;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonStreamParserImplTest {
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
        StringBuilder jsonBuilder = new StringBuilder("{\"data\":\"");
        for (int i = 0; i < 50 * 1024 * 1024 + 1; i++) {
            jsonBuilder.append("a");
        }
        jsonBuilder.append("\"}");
        String json = jsonBuilder.toString();

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
        StringBuilder jsonBuilder = new StringBuilder("{\"data\":\"");
        for (int i = 0; i < JsonParserFactoryImpl.DEFAULT_MAX_STRING_LENGTH * 2; i++) {
            jsonBuilder.append("a");
        }
        jsonBuilder.append("\"}");
        String json = jsonBuilder.toString();

        JsonReaderFactory readerFactory = JsonProvider.provider().createReaderFactory(Collections.singletonMap(
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
