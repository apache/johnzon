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
import jakarta.json.JsonException;
import jakarta.json.JsonPointer;
import jakarta.json.JsonStructure;
import jakarta.json.spi.JsonProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This test class was originally located in the johnzon-jsonp-strict module to bring full JSON Pointer
 * compatibility. Since in version 2.0 it's the default in Johnzon to be compliant, this test migrated to core
 * as it does provide some interesting code coverage
 */
public class StrictJsonPointerFactoryTest {
    @Test
    public void validMinusUsage() {
        final JsonPointerImpl jsonPointer = new JsonPointerImpl(JsonProvider.provider(), "/-");
        final JsonStructure target = Json.createArrayBuilder()
                .add(Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // [["bar","qux","baz"]]

        final JsonStructure result = jsonPointer.add(target, Json.createValue("xyz"));
        assertEquals("[[\"bar\",\"qux\",\"baz\"],\"xyz\"]", result.toString());
    }

    @Test(expected = JsonException.class)
    public void testReplaceLastArrayElement2() {
        final JsonPointer jsonPointer = Json.createPointer("/0/-");
        final JsonStructure target = Json.createArrayBuilder()
                .add(Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // [["bar","qux","baz"]]
        jsonPointer.replace(target, Json.createValue("won't work"));
    }

    @Test(expected = JsonException.class)
    public void testRemoveLastArrayElement() {
        JsonPointer jsonPointer = Json.createPointer("/0/-");
        JsonStructure target = Json.createArrayBuilder()
                .add(Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // [["bar","qux","baz"]]

        JsonStructure result = jsonPointer.remove(target);
        assertEquals("[[\"bar\",\"qux\"]]", result.toString()); // [["bar","qux"]]
    }

    @Test(expected = JsonException.class)
    public void testReplaceLastArrayElement() {
        final JsonPointer jsonPointer = Json.createPointer("/-");
        final JsonStructure target = Json.createArrayBuilder()
                .add(Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // [["bar","qux","baz"]]

        jsonPointer.replace(target, Json.createValue("won't work"));
    }

    @Test(expected = JsonException.class)
    public void testGetLastArrayElementSimple() {
        final JsonPointer jsonPointer = Json.createPointer("/-");
        final JsonStructure target = Json.createArrayBuilder()
                .add("bar")
                .add("qux")
                .add("baz")
                .build();

        jsonPointer.getValue(target);
    }

    @Test(expected = JsonException.class)
    public void testGetLastArrayElement2() {
        final JsonPointer jsonPointer = Json.createPointer("/0/-");
        final JsonStructure target = Json.createArrayBuilder()
                .add(Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // [["bar","qux","baz"]]

        jsonPointer.getValue(target);
    }

    @Test(expected = JsonException.class)
    public void testRemoveLastArrayElementFromEmpty() {
        final JsonPointer jsonPointer = Json.createPointer("/0/-");
        final JsonStructure target = Json.createArrayBuilder()
                .add(Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // [["bar","qux","baz"]]
        jsonPointer.remove(target);
    }

    @Test(expected = JsonException.class)
    public void testRemoveLastArrayElementSimple() {
        JsonPointer jsonPointer = Json.createPointer("/-");
        JsonStructure target = Json.createArrayBuilder()
                .add("bar")
                .add("qux")
                .add("baz")
                .build();

        JsonStructure result = jsonPointer.remove(target);
        assertEquals("[\"bar\",\"qux\"]", result.toString());
    }

    @Test(expected = JsonException.class)
    public void testGetLastArrayElement() {
        final JsonPointer jsonPointer = Json.createPointer("/-");
        final JsonStructure target = Json.createArrayBuilder()
                .add(Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // [["bar","qux","baz"]]

        jsonPointer.getValue(target);
    }

    @Test(expected = JsonException.class)
    public void testReplaceLastArrayElementSimple() {
        final JsonPointer jsonPointer = Json.createPointer("/-");
        final JsonStructure target = Json.createArrayBuilder()
                .add("bar")
                .add("qux")
                .add("baz")
                .build();

        jsonPointer.replace(target, Json.createValue("won't work"));
    }
}
