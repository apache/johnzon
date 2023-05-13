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

import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParsingException;

import org.junit.Test;

public class JsonParserStreamingTest {
    @Test
    public void parseEscapeCharacters() throws IOException {
        final int len = 8192;
        final byte[] bytes = ("{\"source\":\"" +
                IntStream.range(0, 16558).mapToObj(it -> "\\").collect(joining("")) +
                "\t\"}").getBytes(StandardCharsets.UTF_8);

        final BufferStrategy.BufferProvider<char[]> bs = BufferStrategyFactory.valueOf("QUEUE").newCharProvider(len);
        try (final InputStream stream = new ByteArrayInputStream(bytes);
                final JsonStreamParserImpl impl = new JsonStreamParserImpl(stream, len, bs, bs, false,
                                                               (JsonProviderImpl) JsonProviderImpl.provider())) {

            while (impl.hasNext()) {
                impl.next();
            }
        } catch (final ArrayIndexOutOfBoundsException aioobe) {
            assertEquals("Buffer too small for such a long string", aioobe.getMessage());
        }
    }

    @Test
    public void testArrayStream() {
        {
            // int
            String json = "[1,2,3,4]";
            String sum = parserAndConcat(json);
            assertEquals(json, sum);
        }
        {
            // STring
            String json = "[\"1\",\"2\",\"3\",\"4\"]";
            String sum = parserAndConcat(json);
            assertEquals(json, sum);
        }
    }

    @Test
    public void testValueStreamForArrays() {
        {
            // int
            String json = "[1,2,3,4]";
            String sum = parserAndConcat(json);
            assertEquals(json, sum);
        }
        {
            // String
            String json = "[\"1\",\"2\",\"3\",\"4\"]";
            String sum = parserAndConcat(json);
            assertEquals(json, sum);
        }

        {
            // single String
            String json = "[\"In a galaxy far far away\"]";
            String sum = parserAndConcat(json);
            assertEquals(json, sum);
        }
    }

    @Test
    public void testValueStream() {
        {
            String json = "\"a string value\"";
            String sum = parserAndConcat(json);
            assertEquals(json, sum);
        }

        {
            String sum = parserAndConcat("234");
            assertEquals("234", sum);
        }
    }

    @Test
    public void testValueStreamForObjects() {
        String json = "{\"address\":\"somewhere else\"}";
        String sum = parserAndConcat(json);
        assertEquals(json, sum);
    }

    private String parserAndConcat(String json) {
        StringReader sr = new StringReader(json);

        try (JsonParser jsonParser = Json.createParser(sr)) {
            String sum = jsonParser.getValueStream()
                    .map(v -> v.toString())
                    .collect(Collectors.joining(","));
            return sum;
        }
    }

    @Test
    public void testGetArrayStream() {
        StringReader sr = new StringReader("[1,2,3,4,5,6]");

        try (JsonParser jsonParser = Json.createParser(sr)) {
            JsonParser.Event firstEvent = jsonParser.next();
            assertEquals(JsonParser.Event.START_ARRAY, firstEvent);

            int sum = jsonParser.getArrayStream()
                    .mapToInt(v -> ((JsonNumber)v).intValue())
                    .sum();
            assertEquals(21, sum);
        }
    }

    @Test(expected = JsonParsingException.class)
    public void testParseErrorInGetArrayStream() {
        StringReader sr = new StringReader("[\"this is\":\"not an object\"]");

        try (JsonParser jsonParser = Json.createParser(sr)) {
            JsonParser.Event firstEvent = jsonParser.next();
            assertEquals(JsonParser.Event.START_ARRAY, firstEvent);

            jsonParser.getArrayStream().forEach(dummy -> {});
        }
    }

    @Test
    public void testGetObjectStream() {
        StringReader sr = new StringReader("{\"foo\":\"bar\",\"baz\":\"quux\",\"something\":\"else\"}");

        try (JsonParser jsonParser = Json.createParser(sr)) {
            JsonParser.Event firstEvent = jsonParser.next();
            assertEquals(JsonParser.Event.START_OBJECT, firstEvent);

            Map<String, String> mappings = jsonParser.getObjectStream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> ((JsonString)e.getValue()).getString()));

            Map<String, String> expectedMappings = new HashMap<>();
            expectedMappings.put("foo", "bar");
            expectedMappings.put("baz", "quux");
            expectedMappings.put("something", "else");
            assertEquals(expectedMappings, mappings);
        }
    }

    @Test(expected = JsonParsingException.class)
    public void testParseErrorInGetObjectStream() {
        StringReader sr = new StringReader("{42}");

        try (JsonParser jsonParser = Json.createParser(sr)) {
            JsonParser.Event firstEvent = jsonParser.next();
            assertEquals(JsonParser.Event.START_OBJECT, firstEvent);

            jsonParser.getObjectStream().forEach(dummy -> {});
        }
    }

}
