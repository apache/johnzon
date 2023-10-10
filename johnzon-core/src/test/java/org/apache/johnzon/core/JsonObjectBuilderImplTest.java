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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

import jakarta.json.Json;
import jakarta.json.JsonConfig;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import org.junit.Assert;
import org.junit.Test;

public class JsonObjectBuilderImplTest {
    @Test(expected = JsonException.class)
    public void rejectedKeys() {
        Json.createBuilderFactory(singletonMap("johnzon.rejectDuplicateKeys", true))
                .createObjectBuilder()
                .add("foo", 1)
                .add("foo", 2);
    }

    @Test(expected = JsonException.class)
    public void keyStrategyNone() {
        Json.createBuilderFactory(singletonMap(JsonConfig.KEY_STRATEGY, JsonConfig.KeyStrategy.NONE))
                .createObjectBuilder()
                .add("foo", 1)
                .add("foo", 2);
    }

    @Test
    public void keyStrategyFirst() {
        JsonObject built = Json.createBuilderFactory(singletonMap(JsonConfig.KEY_STRATEGY, JsonConfig.KeyStrategy.FIRST))
                .createObjectBuilder()
                .add("foo", 1)
                .add("foo", 2)
                .build();

        assertEquals("{\"foo\":1}", built.toString());
    }

    @Test
    public void keyStrategyLast() {
        JsonObject built = Json.createBuilderFactory(singletonMap(JsonConfig.KEY_STRATEGY, JsonConfig.KeyStrategy.LAST))
                .createObjectBuilder()
                .add("foo", 1)
                .add("foo", 2)
                .build();

        assertEquals("{\"foo\":2}", built.toString());
    }

    @Test
    public void createObjectBuilderMapSupport() {
        final Map<String, Object> initial = new HashMap<>();
        initial.put("a", "b");
        initial.put("c", singletonMap("d", "e"));
        initial.put("f", singleton("g"));

        final JsonObject build = Json.createObjectBuilder(initial).build();
        assertEquals("{\"a\":\"b\",\"c\":{\"d\":\"e\"},\"f\":[\"g\"]}", build.toString());
    }

    @Test
    public void testBuild() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("a", "b");
        JsonObject jsonObject = builder.build();
        assertEquals("{\"a\":\"b\"}", jsonObject.toString());

        JsonObjectBuilder anotherBuilder = Json.createObjectBuilder(jsonObject);
        anotherBuilder.add("c", "d");
        assertEquals("{\"a\":\"b\",\"c\":\"d\"}", anotherBuilder.build().toString());
    }

    @Test
    public void testCreateObjectBuilderWithMapFlatItems() {
        Map<String, Object> jsonItems = new HashMap<>();

        {
            // build up the items
            jsonItems.put("bigDecimalVal", new BigDecimal(1234567.89));
            jsonItems.put("bigIntegerVal", BigInteger.valueOf(54321L));
            jsonItems.put("booleanVal", true);
            jsonItems.put("doubleVal", 1234567.89d);
            jsonItems.put("intVal", 4711);
            jsonItems.put("jsonValueVal", JsonValue.FALSE);
            jsonItems.put("longVal", 123_456_789L);
            jsonItems.put("stringVal", "b");
            jsonItems.put("nullVal", null);

            // there are 9 addXxxx methods with types in JsonObjectBuilder
            // ensure we have all of them covered
            // we do not have items with JsonObjectBuilder and JsonArrayBuilder itself
            Assert.assertEquals(9, jsonItems.size());
        }

        JsonObjectBuilder builder = Json.createObjectBuilder(jsonItems);
        JsonObject jsonObject = builder.build();

        Assert.assertEquals(new BigDecimal(1234567.89), jsonObject.getJsonNumber("bigDecimalVal").bigDecimalValue());
        Assert.assertEquals(BigInteger.valueOf(54321L), jsonObject.getJsonNumber("bigIntegerVal").bigIntegerValue());
        Assert.assertEquals(true, jsonObject.getBoolean("booleanVal"));
        Assert.assertEquals(1234567.89d, jsonObject.getJsonNumber("doubleVal").doubleValue(), 0.01d);
        Assert.assertEquals(4711, jsonObject.getInt("intVal"));
        Assert.assertEquals(JsonValue.FALSE, jsonObject.get("jsonValueVal"));
        Assert.assertEquals(123_456_789L, jsonObject.getJsonNumber("longVal").longValue());
        Assert.assertEquals("b", jsonObject.getString("stringVal"));
        Assert.assertEquals(true, jsonObject.isNull("nullVal"));
    }

    @Test
    public void testAddAll() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("a", "b");
        builder.add("c", "d");

        final JsonObjectBuilder anotherBuilder = Json.createObjectBuilder();
        anotherBuilder.addAll(builder);

        assertEquals("{\"a\":\"b\",\"c\":\"d\"}", anotherBuilder.build().toString());
    }

    @Test
    public void testRemove() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("a", "b");
        builder.add("c", "d");

        builder.remove("a");

        assertEquals("{\"c\":\"d\"}", builder.build().toString());
    }

    @Test(expected = NullPointerException.class)
    public void testNullCheckValue() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("a", (Integer) null);
    }

    @Test
    public void testNullCheckValueExceptionMessage() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        try {
            builder.add("name", (JsonValue) null);
        } catch (final NullPointerException e) {
            assertEquals("value/builder must not be null for name: name", e.getMessage());
        }
    }
    @Test(expected = NullPointerException.class)
    public void testNullCheckName() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(null, "b");
    }
}
