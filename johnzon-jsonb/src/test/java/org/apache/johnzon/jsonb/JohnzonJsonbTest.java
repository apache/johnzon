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
package org.apache.johnzon.jsonb;

import static org.junit.Assert.assertEquals;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JohnzonJsonbTest {
    @Rule
    public final JsonbRule rule = new JsonbRule();

    @Test
    public void listJsonValue() {
        assertEquals(Json.createValue(1.1),
                rule.fromJson("{\"value\":[1.1]}", ArrayJsonValueWrapper.class).value.get(0));
    }

    @Test
    public void listObject() {
        assertEquals(1.1, Number.class.cast(
                rule.fromJson("{\"value\":[1.1]}", ArrayObjectWrapper.class).value.get(0)).doubleValue(), 0);
    }

    @Test
    public void jsonArray() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final String json = "[{\"foo\":\"bar\"}]";
            final JsonArray array = jsonb.fromJson(json, JsonArray.class);
            assertEquals(json, array.toString());
        }
    }

    @Test
    public void longBounds() {
        final String max = rule.toJson(new LongWrapper(Long.MAX_VALUE));
        assertEquals("{\"value\":9223372036854775807}", max);
        assertEquals(Long.MAX_VALUE, rule.fromJson(max, LongWrapper.class).value, 0);

        final String min = rule.toJson(new LongWrapper(Long.MIN_VALUE));
        assertEquals("{\"value\":-9223372036854775808}", min);
        assertEquals(Long.MIN_VALUE, rule.fromJson(min, LongWrapper.class).value, 0);
    }

    @Test
    public void intArray() {
        int[] input = new int[] { 0, 1, 2, 3, 4, 5, 6 };
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        rule.toJson(input, output);
        assertEquals("[0,1,2,3,4,5,6]", new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void charArray() {
        char[] input = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g' };
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        rule.toJson(input, output);
        assertEquals("[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\"]", new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void byteArray() {
        byte[] input = new byte[] { 0x00, 0x01 };
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        rule.toJson(input, output);
        assertEquals("[0,1]", new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void longArray() {
        long[] input = new long[] { 0L, 1L, 2L, 3L, 4L, 5L, 6L };
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        rule.toJson(input, output);
        assertEquals("[0,1,2,3,4,5,6]", new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void shortArray() {
        short[] input = new short[] { 0, 1, 2 };
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        rule.toJson(input, output);
        assertEquals("[0,1,2]", new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void booleanArray() {
        boolean[] input = new boolean[] { true, false };
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        rule.toJson(input, output);
        assertEquals("[true,false]", new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void floatArray() {
        float[] input = new float[] { 1.0f, 1.1f };
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        rule.toJson(input, output);
        assertEquals("[1.0,1.1]", new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void doubleArray() {
        double[] input = new double[] { 1.0, 1.1 };
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        rule.toJson(input, output);
        assertEquals("[1.0,1.1]", new String(output.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void intArrayRuntimeType() {
        int[] input = new int[] { 0, 1, 2, 3, 4, 5, 6 };
        final String output = rule.toJson(input, int[].class);
        assertEquals("[0,1,2,3,4,5,6]", output);
    }

    @Test
    public void charArrayRuntimeType() {
        char[] input = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g' };
        final String output = rule.toJson(input, char[].class);
        assertEquals("[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\"]", output);
    }

    @Test
    public void byteArrayRuntimeType() {
        byte[] input = new byte[] { 0x00, 0x01 };
        final String output = rule.toJson(input, byte[].class);
        assertEquals("[0,1]", output);
    }

    @Test
    public void longArrayRuntimeType() {
        long[] input = new long[] { 0L, 1L, 2L, 3L, 4L, 5L, 6L };
        final String output = rule.toJson(input, long[].class);
        assertEquals("[0,1,2,3,4,5,6]", output);
    }

    @Test
    public void shortArrayRuntimeType() {
        short[] input = new short[] { 0, 1, 2 };
        final String output = rule.toJson(input, short[].class);
        assertEquals("[0,1,2]", output);
    }

    @Test
    public void booleanArrayRuntimeType() {
        boolean[] input = new boolean[] { true, false };
        final String output = rule.toJson(input, boolean[].class);
        assertEquals("[true,false]", output);
    }

    @Test
    public void floatArrayRuntimeType() {
        float[] input = new float[] { 1.0f, 1.1f };
        final String output = rule.toJson(input, float[].class);
        assertEquals("[1.0,1.1]", output);
    }

    @Test
    public void doubleArrayRuntimeType() {
        double[] input = new double[] { 1.0, 1.1 };
        final String output = rule.toJson(input, double[].class);
        assertEquals("[1.0,1.1]", output);
    }

    public static class LongWrapper {
        public Long value;

        public LongWrapper() {
            // no-op
        }

        public LongWrapper(final Long value) {
            this.value = value;
        }
    }

    public static class ArrayObjectWrapper {
        public List<Object> value;
    }

    public static class ArrayJsonValueWrapper {
        public List<JsonValue> value;
    }
}
