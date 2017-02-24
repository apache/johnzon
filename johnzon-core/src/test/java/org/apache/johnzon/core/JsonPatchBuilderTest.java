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

import javax.json.Json;
import javax.json.JsonPatch;
import javax.json.JsonValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonPatchBuilderTest {

    @Test
    public void testPatchBuilderAddString() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                                "/foo",
                                                                                null,
                                                                                new JsonStringImpl("bar")));

        JsonPatch patch = new JsonPatchBuilderImpl().add("/foo", "bar")
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddStringNull() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                                "/foo",
                                                                                null,
                                                                                JsonValue.NULL));

        String nullString = null;
        JsonPatch patch = new JsonPatchBuilderImpl().add("/foo", nullString)
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddJsonObject() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                                "/foo",
                                                                                null,
                                                                                Json.createObjectBuilder()
                                                                                    .add("bar", "qux")
                                                                                    .build()));

        JsonPatch patch = new JsonPatchBuilderImpl().add("/foo", Json.createObjectBuilder()
                                                                     .add("bar", "qux")
                                                                     .build())
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddJsonArray() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                                "/path",
                                                                                null,
                                                                                Json.createArrayBuilder()
                                                                                    .add("test")
                                                                                    .build()));

        JsonPatch patch = new JsonPatchBuilderImpl().add("/path", Json.createArrayBuilder()
                                                                      .add("test")
                                                                      .build())
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddJsonValueNull() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                                "/path",
                                                                                null,
                                                                                JsonValue.NULL));

        JsonPatch patch = new JsonPatchBuilderImpl().add("/path", JsonValue.NULL)
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddInt() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                                "/foo",
                                                                                null,
                                                                                new JsonStringImpl("bar")));

        JsonPatch patch = new JsonPatchBuilderImpl().add("/foo", "bar")
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderAddBoolean() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                                "/path/true",
                                                                                null,
                                                                                JsonValue.TRUE),
                                                   new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                                "/path/false",
                                                                                null,
                                                                                JsonValue.FALSE));

        JsonPatch patch = new JsonPatchBuilderImpl().add("/path/true", true)
                                                    .add("/path/false", false)
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderAddMissingPath() {
        new JsonPatchBuilderImpl().add(null, 0);
    }


    @Test
    public void testPatchBuilderRemove() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REMOVE,
                                                                                "/path/to/remove",
                                                                                null,
                                                                                null));

        JsonPatch patch = new JsonPatchBuilderImpl().remove("/path/to/remove")
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderRemoveMissingPath() {
        new JsonPatchBuilderImpl().remove(null);
    }


    @Test
    public void testPatchBuilderReplaceString() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                                 "/path/to/replace",
                                                                                 null,
                                                                                 new JsonStringImpl("new value")));

        JsonPatch patch = new JsonPatchBuilderImpl().replace("/path/to/replace", "new value")
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderReplaceInt() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                                "/replace/me",
                                                                                null,
                                                                                new JsonLongImpl(42)));

        JsonPatch patch = new JsonPatchBuilderImpl().replace("/replace/me", 42)
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderReplaceBoolean() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                                "/true/to/replace",
                                                                                null,
                                                                                JsonValue.FALSE),
                                                   new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                                "/false/to/replace",
                                                                                null,
                                                                                JsonValue.TRUE));

        JsonPatch patch = new JsonPatchBuilderImpl().replace("/true/to/replace", false)
                                                    .replace("/false/to/replace", true)
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderReplaceJsonObject() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                                "/replace/the/object",
                                                                                null,
                                                                                Json.createObjectBuilder()
                                                                                    .add("foo", "bar")
                                                                                    .build()));

        JsonPatch patch = new JsonPatchBuilderImpl().replace("/replace/the/object", Json.createObjectBuilder()
                                                                                        .add("foo", "bar")
                                                                                        .build())
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderReplaceJsonArray() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                                "/replace/my/array",
                                                                                null,
                                                                                Json.createArrayBuilder()
                                                                                    .add("test")
                                                                                    .build()));

        JsonPatch patch = new JsonPatchBuilderImpl().replace("/replace/my/array", Json.createArrayBuilder()
                                                                                      .add("test")
                                                                                      .build())
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderReplaceMissingPath() {
        new JsonPatchBuilderImpl().replace(null, "ignored");
    }


    @Test
    public void testPatchBuilderMove() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.MOVE,
                                                                                "/move/to",
                                                                                "/move/from",
                                                                                null));

        JsonPatch patch = new JsonPatchBuilderImpl().move("/move/to", "/move/from")
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderMoveMissingPath() {
        new JsonPatchBuilderImpl().move(null, "/ignored");
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderMoveMissingFrom() {
        new JsonPatchBuilderImpl().move("/the/path", null);
    }


    @Test
    public void testPatchBuilderCopy() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.COPY,
                                                                                "/to",
                                                                                "/from",
                                                                                null));

        JsonPatch patch = new JsonPatchBuilderImpl().copy("/to", "/from")
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderCopyMissingPath() {
        new JsonPatchBuilderImpl().copy(null, "/ignored");
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderCopyMissingFrom() {
        new JsonPatchBuilderImpl().copy("/the/path", null);
    }


    @Test
    public void testPatchBuilderTestString() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                                "/to/test",
                                                                                null,
                                                                                new JsonStringImpl("value")));

        JsonPatch patch = new JsonPatchBuilderImpl().test("/to/test", "value")
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderTestBoolean() {

        JsonPatchImpl exptected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                                 "/true/to/test",
                                                                                 null,
                                                                                 JsonValue.TRUE),
                                                    new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                                 "/false/to/test",
                                                                                 null,
                                                                                 JsonValue.FALSE));

        JsonPatch patch = new JsonPatchBuilderImpl().test("/true/to/test", true)
                                                    .test("/false/to/test", false)
                                                    .build();
        assertNotNull(patch);
        assertEquals(exptected, patch);
    }

    @Test
    public void testPatchBuilderTestInt() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                                "/test/int",
                                                                                null,
                                                                                new JsonLongImpl(16)));

        JsonPatch patch = new JsonPatchBuilderImpl().test("/test/int", 16)
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderTestJsonValue() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                                "/test/value",
                                                                                null,
                                                                                JsonValue.NULL));

        JsonPatch patch = new JsonPatchBuilderImpl().test("/test/value", JsonValue.NULL)
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderTestJsonObject() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                                "/test/the/object",
                                                                                null,
                                                                                Json.createObjectBuilder()
                                                                                    .add("foo", "bar")
                                                                                    .build()));

        JsonPatch patch = new JsonPatchBuilderImpl().test("/test/the/object", Json.createObjectBuilder()
                                                                                  .add("foo", "bar")
                                                                                  .build())
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test
    public void testPatchBuilderTestJsonArray() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.TEST,
                                                                                "/test/my/array",
                                                                                null,
                                                                                Json.createArrayBuilder()
                                                                                    .add("element")
                                                                                    .build()));

        JsonPatch patch = new JsonPatchBuilderImpl().test("/test/my/array", Json.createArrayBuilder()
                                                                                .add("element")
                                                                                .build())
                                                    .build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }

    @Test(expected = NullPointerException.class)
    public void testPatchBuilderTestMissingPath() {
        new JsonPatchBuilderImpl().test(null, "ignored");
    }


    @Test
    public void testPatchBuilderWithinitialData() {

        JsonPatchImpl expected = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                                "/add/an/object",
                                                                                null,
                                                                                Json.createObjectBuilder()
                                                                                    .add("name", "Cassius")
                                                                                    .build()),
                                                   new JsonPatchImpl.PatchValue(JsonPatchOperation.REPLACE,
                                                                                "/replace/me",
                                                                                null,
                                                                                Json.createArrayBuilder()
                                                                                    .add(16)
                                                                                    .add(27)
                                                                                    .add("test")
                                                                                    .build()),
                                                   new JsonPatchImpl.PatchValue(JsonPatchOperation.REMOVE,
                                                                                "/remove/it",
                                                                                null,
                                                                                null));

        JsonPatch patch = new JsonPatchBuilderImpl(expected.toJsonArray()).build();
        assertNotNull(patch);
        assertEquals(expected, patch);
    }


}
