/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.core;

import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SnippetTest {

    @Test
    public void simple() {
        final JsonValue value = parse("{\"name\":\"string\",\"value\":\"string\",\"type\":\"string\"}");

        // This snippet is smaller than the allowed size.  It should show in entirety.
        assertSnippet(value, "{\"name\":\"string\",\"value\":\"string\",\"type\":\"string\"}", 100);

        // This snippet is exactly 50 characters when formatted.  We should see no "..." at the end.
        assertSnippet(value, "{\"name\":\"string\",\"value\":\"string\",\"type\":\"string\"}", 50);

        // This snippet is too large.  We should see the "..." at the end.
        assertSnippet(value, "{\"name\":\"string\",\"value\":\"stri...", 30);
    }

    @Test
    public void mapOfArray() {
        final JsonValue value = parse("{\"name\": [\"red\", \"green\", \"blue\"], \"value\": [\"orange\", \"yellow\", \"purple\"]}");

        assertSnippet(value, "{\"name\":[\"red\",\"green\",\"blue\"],\"value\":[\"orange\",\"yellow\",\"purple\"]}", 200);
        assertSnippet(value, "{\"name\":[\"red\",\"green\",\"blue\"],\"value\":[\"orange\",\"yellow\",\"purple\"]}", 68);
        assertSnippet(value, "{\"name\":[\"red\",\"green\",\"blue\"],\"value\":[\"orange\",\"...", 50);
    }

    @Test
    public void mapOfObject() {
        final JsonValue value = parse("{\"name\": {\"name\": \"red\", \"value\": \"green\", \"type\": \"blue\"}," +
                " \"value\": {\"name\": \"orange\", \"value\": \"purple\", \"type\": \"yellow\", \"scope\": \"brown\"}}");

        assertSnippet(value, "{\"name\":{\"name\":\"red\",\"value\":\"green\",\"type\":\"blue\"}," +
                "\"value\":{\"name\":\"orange\",\"value\":\"purple\",\"type\":\"yellow\",\"scope\":\"brown\"}}", 200);

        assertSnippet(value, "{\"name\":{\"name\":\"red\",\"value\":\"green\",\"type\":\"blue\"}," +
                "\"value\":{\"name\":\"orange\",\"value\":\"purple\",\"type\":\"yellow\",\"scope\":\"brown\"}}", 128);

        assertSnippet(value, "{\"name\":{\"name\":\"red\",\"value\":\"green\",\"type\":\"blue\"}," +
                "\"value\":{\"name\":\"orange\",\"value\":\"purple\",\"type...", 100);
    }

    @Test
    public void mapOfNestedMaps() {
        final JsonValue value = parse("{\"name\": {\"name\": {\"name\": {\"name\": \"red\", \"value\": \"green\", \"type\": \"blue\"}}}}");

        assertSnippet(value, "{\"name\":{\"name\":{\"name\":{\"name\":\"red\",\"value\":\"green\",\"type\":\"blue\"}}}}", 100);
        assertSnippet(value, "{\"name\":{\"name\":{\"name\":{\"name\":\"red\",\"value\":\"green\",\"type\":\"blue\"}}}}", 71);
        assertSnippet(value, "{\"name\":{\"name\":{\"name\":{\"name\":\"red\",\"value\":\"gre...", 50);
    }

    @Test
    public void mapOfString() {
        final JsonValue value = parse("{\"name\":\"string\",\"value\":\"string\",\"type\":\"string\"}");
        assertSnippet(value, "{\"name\":\"string\",\"value\":\"string\",\"type\":\"string\"}", 100);
        assertSnippet(value, "{\"name\":\"string\",\"value\":\"string\",\"type\":\"string\"}", 50);
        assertSnippet(value, "{\"name\":\"string\",\"value\":\"stri...", 30);
    }

    @Test
    public void mapOfNumber() {
        final JsonValue value = parse("{\"name\":1234,\"value\":5,\"type\":67890,\"scope\":null}");

        assertSnippet(value, "{\"name\":1234,\"value\":5,\"type\":67890,\"scope\":null}", 100);
        assertSnippet(value, "{\"name\":1234,\"value\":5,\"type\":67890,\"scope\":null}", 49);
        assertSnippet(value, "{\"name\":1234,\"value\":5,\"type\":...", 30);
    }

    @Test
    public void mapOfTrue() {
        final JsonValue value = parse("{\"name\":true,\"value\":true,\"type\":true,\"scope\":true}");

        assertSnippet(value, "{\"name\":true,\"value\":true,\"type\":true,\"scope\":true}", 60);
        assertSnippet(value, "{\"name\":true,\"value\":true,\"type\":true,\"scope\":true}", 51);
        assertSnippet(value, "{\"name\":true,\"value\":true,\"typ...", 30);
    }

    @Test
    public void mapOfFalse() {
        final JsonValue value = parse("{\"name\":false,\"value\":false,\"type\":false}");

        assertSnippet(value, "{\"name\":false,\"value\":false,\"type\":false}", 50);
        assertSnippet(value, "{\"name\":false,\"value\":false,\"type\":false}", 41);
        assertSnippet(value, "{\"name\":false,\"value...", 20);
    }

    @Test
    public void mapOfNull() {
        final JsonValue value = parse("{\"name\":null,\"value\":null,\"type\":null,\"scope\":null}");

        assertSnippet(value, "{\"name\":null,\"value\":null,\"type\":null,\"scope\":null}", 60);
        assertSnippet(value, "{\"name\":null,\"value\":null,\"type\":null,\"scope\":null}", 51);
        assertSnippet(value, "{\"name\":null,\"value\":null,\"typ...", 30);
    }

    @Test
    public void arrayOfArray() {
        final JsonValue value = parse("[[\"red\",\"green\"], [1,22,333], [{\"r\":  255,\"g\": 165}], [true, false]]");

        assertSnippet(value, "[[\"red\",\"green\"],[1,22,333],[{\"r\":255,\"g\":165}],[true,false]]", 100);
        assertSnippet(value, "[[\"red\",\"green\"],[1,22,333],[{\"r\":255,\"g\":165}],[true,false]]", 61);
        assertSnippet(value, "[[\"red\",\"green\"],[1,22,333],[{\"r\":255,\"g...", 40);
    }

    @Test
    public void arrayOfObject() {
        final JsonValue value = parse("[{\"r\":  255,\"g\": \"165\"},{\"g\":  0,\"a\": \"0\"},{\"transparent\": false}]");

        assertSnippet(value, "[{\"r\":255,\"g\":\"165\"},{\"g\":0,\"a\":\"0\"},{\"transparent\":false}]", 100);
        assertSnippet(value, "[{\"r\":255,\"g\":\"165\"},{\"g\":0,\"a\":\"0\"},{\"transparent\":false}]", 59);
        assertSnippet(value, "[{\"r\":255,\"g\":\"165\"},{\"g\":0,\"a...", 30);
    }

    @Test
    public void arrayOfString() {
        final JsonValue value = parse("[\"red\", \"green\", \"blue\", \"orange\", \"yellow\", \"purple\"]");

        assertSnippet(value, "[\"red\",\"green\",\"blue\",\"orange\",\"yellow\",\"purple\"]", 100);
        assertSnippet(value, "[\"red\",\"green\",\"blue\",\"orange\",\"yellow\",\"purple\"]", 49);
        assertSnippet(value, "[\"red\",\"green\",\"blue\",\"orange\"...", 30);
    }

    @Test
    public void arrayOfNumber() {
        final JsonValue value = parse("[1,22,333,4444,55555,666666,7777777,88888888,999999999]");

        assertSnippet(value, "[1,22,333,4444,55555,666666,7777777,88888888,999999999]", 100);
        assertSnippet(value, "[1,22,333,4444,55555,666666,7777777,88888888,999999999]", 55);
        assertSnippet(value, "[1,22,333,4444,55555,666666,77...", 30);
    }

    @Test
    public void arrayOfTrue() {
        final JsonValue value = parse("[true,true,true,true,true,true,true,true]");

        assertSnippet(value, "[true,true,true,true,true,true,true,true]", 100);
        assertSnippet(value, "[true,true,true,true,true,true,true,true]", 41);
        assertSnippet(value, "[true,true,true,true,true,true...", 30);
    }

    @Test
    public void arrayOfFalse() {
        final JsonValue value = parse("[false,false,false,false,false,false,false]");

        assertSnippet(value, "[false,false,false,false,false,false,false]", 100);
        assertSnippet(value, "[false,false,false,false,false,false,false]", 43);
        assertSnippet(value, "[false,false,false,false,false...", 30);
    }

    @Test
    public void arrayOfNull() {
        final JsonValue value = parse("[null,null,null,null,null,null]");

        assertSnippet(value, "[null,null,null,null,null,null]", 50);
        assertSnippet(value, "[null,null,null,null,null,null]", 31);
        assertSnippet(value, "[null,null,null...", 15);
    }

    @Test
    public void string() {
        final JsonValue value = parse("\"This is a \\\"string\\\" with quotes in it.  It should be properly escaped.\"");

        assertSnippet(value, "\"This is a \\\"string\\\" with quotes in it.  It should be properly escaped.\"", 100);
        assertSnippet(value, "\"This is a \\\"string\\\" with quotes in it.  It should be properly escaped.\"", 73);
        assertSnippet(value, "\"This is a \\\"string\\\" with quotes in it.  It shoul...", 50);
    }

    @Test
    public void number() {
        final JsonValue value = parse("1223334444555556666667777777.88888888999999999");

        assertSnippet(value, "1223334444555556666667777777.88888888999999999", 50);
        assertSnippet(value, "1223334444555556666667777777.88888888999999999", 46);
        assertSnippet(value, "1223334444555556666667777777.8...", 30);
    }

    @Test
    public void trueValue() {
        final JsonValue value = parse("true");

        assertSnippet(value, "true", 50);
        // we don't trim 'true' -- showing users something like 't...' doesn't make much sense
        assertSnippet(value, "t...", 1);
    }

    @Test
    public void falseValue() {
        final JsonValue value = parse("false");

        assertSnippet(value, "false", 50);
        // we don't trim 'false' -- showing users something like 'f...' doesn't make much sense
        assertSnippet(value, "f...", 1);
    }

    @Test
    public void nullValue() {
        final JsonValue value = parse("null");

        assertSnippet(value, "null", 50);
        // we don't trim 'null' -- showing users something like 'n...' doesn't make much sense
        assertSnippet(value, "n...", 1);
    }

    private JsonValue parse(final String json) {
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(json.getBytes()));
        return jsonParser.getValue();
    }

    private void assertSnippet(final JsonValue object, final String expected, final int i) {
        final TrackingJsonGeneratorFactory factory = new TrackingJsonGeneratorFactory();
        final String actual = new Snippet(i, factory).of(object);

        // Assert the resulting string contents
        assertEquals(expected, actual);

        // Assert the resulting string length
        if (expected.endsWith("...")) {
            assertEquals(i + 3, actual.length());
        } else {
            assertTrue(actual.length() <= i);
        }

        /*
         * Close methods are supposed to idempotent and
         * safe to call many times, but let's be nice and
         * ensure it is only called once.
         */
        assertEquals(1, factory.calls.stream()
                .filter("close()"::equals)
                .count());

        /*
         * When writing arrays or objects, assert we stopped
         * calling write methods on the JsonGenerator once the
         * end of the snippet is reached.
         */
        if (expected.endsWith("...") && isType(object, ARRAY, OBJECT)) {
            /*
             * Serialize the json value, truncating nothing
             */
            final TrackingJsonGeneratorFactory full = new TrackingJsonGeneratorFactory();
            new Snippet(8192, full).of(object);

            /*
             * Assert that the calls made in truncated version are less
             * than when we serialized the entire json value.
             */
            assertTrue(factory.calls.size() < full.calls.size());
        }
    }

    private static boolean isType(final JsonValue value, final JsonValue.ValueType... types) {
        for (final JsonValue.ValueType type : types) {
            if (value.getValueType() == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Track all calls made to JsonGenerator so we can ensure Snippet is
     * not trying to serialize entire Json documents.  Despite the output
     * of Snippet appearing properly truncated, buffering can still cause
     * the entire json value to be serialized.
     *
     * The only way to test this is not happening is to track all calls
     * made to the JsonGenerator and assert they do in fact stop.
     */
    public static class TrackingJsonGeneratorFactory extends JsonGeneratorFactoryImpl {
        private final List<String> calls = new ArrayList<>();

        public TrackingJsonGeneratorFactory() {
            super(Collections.EMPTY_MAP);
        }

        @Override
        public JsonGenerator createGenerator(final OutputStream out) {
            return new TrackingGenerator(super.createGenerator(out));
        }

        @Override
        public JsonGenerator createGenerator(final Writer writer) {
            return new TrackingGenerator(super.createGenerator(writer));
        }

        class TrackingGenerator implements JsonGenerator {

            private final JsonGenerator delegate;

            public TrackingGenerator(final JsonGenerator delegate) {
                record("<init>");
                this.delegate = delegate;
            }

            private void record(final String method, final Object... args) {
                final String argString = Stream.of(args)
                        .map(Object::toString)
                        .reduce((s, s2) -> s + "," + s2)
                        .orElse("");
                calls.add(String.format("%s(%s)", method, argString));
            }

            public List<String> getCalls() {
                return calls;
            }

            @Override
            public JsonGenerator writeStartObject() {
                record("writeStartObject");
                return delegate.writeStartObject();
            }

            @Override
            public JsonGenerator writeStartObject(final String name) {
                record("writeStartObject", name);
                return delegate.writeStartObject(name);
            }

            @Override
            public JsonGenerator writeStartArray() {
                record("writeStartArray");
                return delegate.writeStartArray();
            }

            @Override
            public JsonGenerator writeStartArray(final String name) {
                record("writeStartArray", name);
                return delegate.writeStartArray(name);
            }

            @Override
            public JsonGenerator writeKey(final String name) {
                record("writeKey", name);
                return delegate.writeKey(name);
            }

            @Override
            public JsonGenerator write(final String name, final JsonValue value) {
                record("write", name, value.getValueType());
                assertJsonType(value);
                return delegate.write(name, value);
            }

            @Override
            public JsonGenerator write(final String name, final String value) {
                record("write", name, value);
                return delegate.write(name, value);
            }

            @Override
            public JsonGenerator write(final String name, final BigInteger value) {
                record("write", name, value);
                return delegate.write(name, value);
            }

            @Override
            public JsonGenerator write(final String name, final BigDecimal value) {
                record("write", name, value);
                return delegate.write(name, value);
            }

            @Override
            public JsonGenerator write(final String name, final int value) {
                record("write", name, value);
                return delegate.write(name, value);
            }

            @Override
            public JsonGenerator write(final String name, final long value) {
                record("write", name, value);
                return delegate.write(name, value);
            }

            @Override
            public JsonGenerator write(final String name, final double value) {
                record("write", name, value);
                return delegate.write(name, value);
            }

            @Override
            public JsonGenerator write(final String name, final boolean value) {
                record("write", name, value);
                return delegate.write(name, value);
            }

            @Override
            public JsonGenerator writeNull(final String name) {
                record("writeNull");
                return delegate.writeNull(name);
            }

            @Override
            public JsonGenerator writeEnd() {
                record("writeEnd");
                return delegate.writeEnd();
            }

            @Override
            public JsonGenerator write(final JsonValue value) {
                record("write", value.getValueType());
                assertJsonType(value);
                return delegate.write(value);
            }

            @Override
            public JsonGenerator write(final String value) {
                record("write", value);
                return delegate.write(value);
            }

            @Override
            public JsonGenerator write(final BigDecimal value) {
                record("write", value);
                return delegate.write(value);
            }

            @Override
            public JsonGenerator write(final BigInteger value) {
                record("write", value);
                return delegate.write(value);
            }

            @Override
            public JsonGenerator write(final int value) {
                record("write", value);
                return delegate.write(value);
            }

            @Override
            public JsonGenerator write(final long value) {
                record("write", value);
                return delegate.write(value);
            }

            @Override
            public JsonGenerator write(final double value) {
                record("write", value);
                return delegate.write(value);
            }

            @Override
            public JsonGenerator write(final boolean value) {
                record("write", value);
                return delegate.write(value);
            }

            @Override
            public JsonGenerator writeNull() {
                record("writeNull");
                return delegate.writeNull();
            }

            @Override
            public void close() {
                record("close");
                delegate.close();
            }

            @Override
            public void flush() {
                record("flush");
                delegate.flush();
            }

            /**
             * Snippet should not be asking the JsonGenerator to be serializing
             * an objet or an array as this would cause the entire portion of
             * json to be written even if we need a small chunk.
             */
            private void assertJsonType(final JsonValue value) {
                if (isType(value, ARRAY, OBJECT)) {
                    fail("should never be called");
                }
            }
        }
    }

}
