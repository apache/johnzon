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

import org.junit.Assert;
import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonPatch;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonPatchBuilderTest {

    private static final JsonProvider PROVIDER = JsonProvider.provider();

    @Test
    public void testPatchBuilderAddString() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.ADD,
                "/foo",
                null,
                new JsonStringImpl("bar")));

        JsonPatch patch = Json.createPatchBuilder().add("/foo", "bar")
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddStringNull() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.ADD,
                "/foo",
                null,
                JsonValue.NULL));

        String nullString = null;
        JsonPatch patch = Json.createPatchBuilder().add("/foo", nullString)
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddJsonObject() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.ADD,
                "/foo",
                null,
                Json.createObjectBuilder()
                        .add("bar", "qux")
                        .build()));

        JsonPatch patch = Json.createPatchBuilder().add("/foo", Json.createObjectBuilder()
                .add("bar", "qux")
                .build())
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddJsonArray() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.ADD,
                "/path",
                null,
                Json.createArrayBuilder()
                        .add("test")
                        .build()));

        JsonPatch patch = Json.createPatchBuilder().add("/path", Json.createArrayBuilder()
                .add("test")
                .build())
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddJsonValueNull() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.ADD,
                "/path",
                null,
                JsonValue.NULL));

        JsonPatch patch = Json.createPatchBuilder().add("/path", JsonValue.NULL)
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddInt() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.ADD,
                "/foo",
                null,
                new JsonStringImpl("bar")));

        JsonPatch patch = Json.createPatchBuilder().add("/foo", "bar")
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddBoolean() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.ADD,
                "/path/true",
                null,
                JsonValue.TRUE),
                new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                        "/path/false",
                        null,
                        JsonValue.FALSE));

        JsonPatch patch = Json.createPatchBuilder().add("/path/true", true)
                .add("/path/false", false)
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderAddMissingPath() {
        Json.createPatchBuilder().add(null, 0);
    }


    @Test
    public void testPatchBuilderRemove() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.REMOVE,
                "/path/to/remove",
                null,
                null));

        JsonPatch patch = Json.createPatchBuilder().remove("/path/to/remove")
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderRemoveMissingPath() {
        Json.createPatchBuilder().remove(null);
    }


    @Test
    public void testPatchBuilderReplaceString() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.REPLACE,
                "/path/to/replace",
                null,
                new JsonStringImpl("new value")));

        JsonPatch patch = Json.createPatchBuilder().replace("/path/to/replace", "new value")
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderReplaceInt() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.REPLACE,
                "/replace/me",
                null,
                new JsonLongImpl(42)));

        JsonPatch patch = Json.createPatchBuilder().replace("/replace/me", 42)
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderReplaceBoolean() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.REPLACE,
                "/true/to/replace",
                null,
                JsonValue.FALSE),
                new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REPLACE,
                        "/false/to/replace",
                        null,
                        JsonValue.TRUE));

        JsonPatch patch = Json.createPatchBuilder().replace("/true/to/replace", false)
                .replace("/false/to/replace", true)
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderReplaceJsonObject() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.REPLACE,
                "/replace/the/object",
                null,
                Json.createObjectBuilder()
                        .add("foo", "bar")
                        .build()));

        JsonPatch patch = Json.createPatchBuilder().replace("/replace/the/object", Json.createObjectBuilder()
                .add("foo", "bar")
                .build())
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderReplaceJsonArray() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.REPLACE,
                "/replace/my/array",
                null,
                Json.createArrayBuilder()
                        .add("test")
                        .build()));

        JsonPatch patch = Json.createPatchBuilder().replace("/replace/my/array", Json.createArrayBuilder()
                .add("test")
                .build())
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderReplaceMissingPath() {
        Json.createPatchBuilder().replace(null, "ignored");
    }


    @Test
    public void testPatchBuilderMove() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.MOVE,
                "/move/to",
                "/move/from",
                null));

        JsonPatch patch = Json.createPatchBuilder().move("/move/to", "/move/from")
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderMoveMissingPath() {
        Json.createPatchBuilder().move(null, "/ignored");
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderMoveMissingFrom() {
        Json.createPatchBuilder().move("/the/path", null);
    }


    @Test
    public void testPatchBuilderCopy() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.COPY,
                "/to",
                "/from",
                null));

        JsonPatch patch = Json.createPatchBuilder().copy("/to", "/from")
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderCopyMissingPath() {
        Json.createPatchBuilder().copy(null, "/ignored");
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderCopyMissingFrom() {
        Json.createPatchBuilder().copy("/the/path", null);
    }


    @Test
    public void testPatchBuilderTestString() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.TEST,
                "/to/test",
                null,
                new JsonStringImpl("value")));

        JsonPatch patch = Json.createPatchBuilder().test("/to/test", "value")
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderTestBoolean() {

        JsonPatchImpl exptected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.TEST,
                "/true/to/test",
                null,
                JsonValue.TRUE),
                new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.TEST,
                        "/false/to/test",
                        null,
                        JsonValue.FALSE));

        JsonPatch patch = Json.createPatchBuilder().test("/true/to/test", true)
                .test("/false/to/test", false)
                .build();
        assertNotNull(patch);
        assertEquals(exptected, patch);
    }

    @Test
    public void testPatchBuilderTestInt() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.TEST,
                "/test/int",
                null,
                new JsonLongImpl(16)));

        JsonPatch patch = Json.createPatchBuilder().test("/test/int", 16)
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderTestJsonValue() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.TEST,
                "/test/value",
                null,
                JsonValue.NULL));

        JsonPatch patch = Json.createPatchBuilder().test("/test/value", JsonValue.NULL)
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderTestJsonObject() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.TEST,
                "/test/the/object",
                null,
                Json.createObjectBuilder()
                        .add("foo", "bar")
                        .build()));

        JsonPatch patch = Json.createPatchBuilder().test("/test/the/object", Json.createObjectBuilder()
                .add("foo", "bar")
                .build())
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderTestJsonArray() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.TEST,
                "/test/my/array",
                null,
                Json.createArrayBuilder()
                        .add("element")
                        .build()));

        JsonPatch patch = Json.createPatchBuilder().test("/test/my/array", Json.createArrayBuilder()
                .add("element")
                .build())
                .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderTestMissingPath() {
        Json.createPatchBuilder().test(null, "ignored");
    }


    @Test
    public void testPatchBuilderWithinitialData() {

        JsonPatchImpl expected = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(
                PROVIDER, JsonPatch.Operation.ADD,
                "/add/an/object",
                null,
                Json.createObjectBuilder()
                        .add("name", "Cassius")
                        .build()),
                new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REPLACE,
                        "/replace/me",
                        null,
                        Json.createArrayBuilder()
                                .add(16)
                                .add(27)
                                .add("test")
                                .build()),
                new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REMOVE,
                        "/remove/it",
                        null,
                        null));

        JsonPatch patch = new JsonPatchBuilderImpl(PROVIDER, expected.toJsonArray()).build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderWithJsonArrayInitialData() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        arrayBuilder.add(Json.createObjectBuilder()
                             .add("op", JsonPatch.Operation.ADD.operationName())
                             .add("path", "/add/an/object")
                             .add("value", "someValue").build());
        arrayBuilder.add(Json.createObjectBuilder()
                             .add("op", JsonPatch.Operation.TEST.operationName())
                             .add("path", "/test/someObject")
                             .add("value", "someValue").build());

        JsonArray initialPatchData = arrayBuilder.build();

        JsonPatchBuilder jsonPatchBuilder = Json.createPatchBuilder(initialPatchData);
        jsonPatchBuilder.move("/move/me/to", "/move/me/from");
        JsonPatch jsonPatch = jsonPatchBuilder.build();
        Assert.assertNotNull(jsonPatch);

        //X TODO apply the patch to some data
    }
}