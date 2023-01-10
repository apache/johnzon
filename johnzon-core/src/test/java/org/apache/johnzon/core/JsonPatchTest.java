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
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonPatch;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class JsonPatchTest {
    private static final JsonProvider PROVIDER = JsonProvider.provider();

    @Test
    public void testAddObjectMember() {

        JsonObject object = Json.createReader(new StringReader("{ \"foo\": \"bar\" }"))
                                .readObject();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/baz",
                                                                             null, // no from
                                                                             new JsonStringImpl("qux")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertEquals("bar", patched.getString("foo"));
        assertEquals("qux", patched.getString("baz"));

        assertEquals("{\"foo\":\"bar\",\"baz\":\"qux\"}", toJsonString(patched));
    }

    /**
     * {@linkplain} https://issues.apache.org/jira/browse/JOHNZON-172
     */
    @Test
    public void testAddToRootContainingEmptyJsonObject() {
        JsonObject object = Json.createObjectBuilder()
                               .add("request", Json.createObjectBuilder()
                                                   .add("test", JsonValue.EMPTY_JSON_OBJECT))
                               .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/name",
                                                                             null,
                                                                             new JsonStringImpl("aName")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);

        JsonObject requestJson = patched.getJsonObject("request");
        assertNotNull(requestJson);
        assertEquals(JsonValue.EMPTY_JSON_OBJECT, requestJson.getJsonObject("test"));

        assertEquals("aName", patched.getString("name"));
    }

    @Test
    public void testAddArrayElementWithIndex() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("baz"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/foo/1",
                                                                             null, // no from
                                                                             new JsonStringImpl("qux")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);

        JsonArray array = patched.getJsonArray("foo");
        assertNotNull(array);
        assertEquals("bar", array.getString(0));
        assertEquals("qux", array.getString(1));
        assertEquals("baz", array.getString(2));

        assertEquals("{\"foo\":[\"bar\",\"qux\",\"baz\"]}", toJsonString(patched));
    }

    @Test
    public void testAddArrayElementAppend() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("baz"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/foo/-",
                                                                             null, // no from
                                                                             new JsonStringImpl("qux")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);

        JsonArray array = patched.getJsonArray("foo");
        assertNotNull(array);
        assertEquals("bar", array.getString(0));
        assertEquals("baz", array.getString(1));
        assertEquals("qux", array.getString(2));

        assertEquals("{\"foo\":[\"bar\",\"baz\",\"qux\"]}", toJsonString(patched));
    }

    @Test
    public void testAddArrayElementPlainArray() {
        JsonArray array = Json.createArrayBuilder()
                              .add("bar")
                              .add("baz")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/-",
                                                                             null, // no from
                                                                             new JsonStringImpl("qux")));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertNotSame(array, patched);
        assertEquals("bar", patched.getString(0));
        assertEquals("baz", patched.getString(1));
        assertEquals("qux", patched.getString(2));

        assertEquals("[\"bar\",\"baz\",\"qux\"]", toJsonString(patched));
    }

    @Test(expected = JsonException.class)
    public void testAddNonexistentTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/baz/bat",
                                                                             null, // no from
                                                                             new JsonStringImpl("qux")));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testAddArrayIndexOutOfBounds() {

        JsonArray array = Json.createArrayBuilder()
                              .add("bar")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/5",
                                                                             null,
                                                                             new JsonStringImpl("baz")));

        patch.apply(array);
    }


    @Test
    public void testRemoveObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("baz", "qux")
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REMOVE,
                                                                             "/baz",
                                                                             null,
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertEquals("bar", patched.getString("foo"));
        assertFalse("patched JsonObject must no contain \"baz\"", patched.containsKey("baz"));

        assertEquals("{\"foo\":\"bar\"}", toJsonString(patched));
    }

    @Test
    public void testRemoveArrayElement() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("qux")
                                                .add("baz"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REMOVE,
                                                                             "/foo/1",
                                                                             null,
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);

        JsonArray array = patched.getJsonArray("foo");
        assertNotNull(array);
        assertEquals(2, array.size());
        assertEquals("bar", array.getString(0));
        assertEquals("baz", array.getString(1));

        assertEquals("{\"foo\":[\"bar\",\"baz\"]}", toJsonString(patched));
    }

    @Test
    public void testRemoveArrayElementPlainArray() {

        JsonArray array = Json.createArrayBuilder()
                              .add("bar")
                              .add("qux")
                              .add("baz")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REMOVE,
                                                                             "/1",
                                                                             null,
                                                                             null));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertEquals(2, patched.size());
        assertEquals("bar", patched.getString(0));
        assertEquals("baz", patched.getString(1));

        assertEquals("[\"bar\",\"baz\"]", toJsonString(patched));
    }

    @Test(expected = JsonException.class)
    public void testRemoveObjectElementNonexistentTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .add("baz", "qux")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REMOVE,
                                                                             "/nomatch",
                                                                             null,
                                                                             null));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testRemoveArrayElementIndexOutOfBounds() {

        JsonArray array = Json.createArrayBuilder()
                              .add("bar")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REMOVE,
                                                                             "/5",
                                                                             null,
                                                                             null));

        patch.apply(array);
    }


    @Test
    public void testReplacingObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("baz", "qux")
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REPLACE,
                                                                             "/baz",
                                                                             null,
                                                                             new JsonStringImpl("boo")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);
        assertEquals("boo", patched.getString("baz"));
        assertEquals("bar", patched.getString("foo"));

        assertEquals("{\"foo\":\"bar\",\"baz\":\"boo\"}", toJsonString(patched));
    }

    @Test
    public void testReplacingArrayElement() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("qux"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REPLACE,
                                                                             "/foo/1",
                                                                             null,
                                                                             new JsonStringImpl("boo")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);

        JsonArray array = patched.getJsonArray("foo");
        assertNotNull(array);
        assertNotSame(object.getJsonArray("foo"), array);
        assertEquals(2, array.size());
        assertEquals("bar", array.getString(0));
        assertEquals("boo", array.getString(1));

        assertEquals("{\"foo\":[\"bar\",\"boo\"]}", toJsonString(patched));
    }

    @Test
    public void testReplacingArrayElementPlainArray() {

        JsonArray array = Json.createArrayBuilder()
                              .add("bar")
                              .add("qux")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REPLACE,
                                                                             "/0",
                                                                             null,
                                                                             new JsonStringImpl("boo")));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertNotSame(array, patched);
        assertEquals(2, patched.size());
        assertEquals("boo", patched.getString(0));
        assertEquals("qux", patched.getString(1));

        assertEquals("[\"boo\",\"qux\"]", toJsonString(patched));
    }

    @Test(expected = JsonException.class)
    public void testReplacingObjectMemberNonexistingTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REPLACE,
                                                                             "/nomatch",
                                                                             null,
                                                                             new JsonStringImpl("notneeded")));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testReplacingArrayElementIndexOutOfBounds() {

        JsonArray array = Json.createArrayBuilder()
                              .add("foo")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REPLACE,
                                                                             "/1",
                                                                             null,
                                                                             new JsonStringImpl("notneeded")));

        patch.apply(array);
    }


    @Test
    public void testMovingObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createObjectBuilder()
                                                .add("bar", "baz")
                                                .add("waldo", "fred"))
                                .add("qux", Json.createObjectBuilder()
                                                .add("corge", "grault"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.MOVE,
                                                                             "/qux/thud",
                                                                             "/foo/waldo",
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);

        JsonObject foo = patched.getJsonObject("foo");
        assertNotNull(foo);
        assertEquals("baz", foo.getString("bar"));
        assertFalse("JsonObject with key 'foo' must not contain 'waldo'", foo.containsKey("waldo"));

        JsonObject qux = patched.getJsonObject("qux");
        assertNotNull(qux);
        assertEquals("grault", qux.getString("corge"));
        assertEquals("fred", qux.getString("thud"));

        assertEquals("{\"foo\":{\"bar\":\"baz\"},\"qux\":{\"corge\":\"grault\",\"thud\":\"fred\"}}", toJsonString(patched));
    }

    @Test
    public void testMovingArrayElement() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("all")
                                                .add("grass")
                                                .add("cows")
                                                .add("eat"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.MOVE,
                                                                             "/foo/3",
                                                                             "/foo/1",
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);

        JsonArray array = patched.getJsonArray("foo");
        assertNotNull(array);
        assertEquals("all", array.getString(0));
        assertEquals("cows", array.getString(1));
        assertEquals("eat", array.getString(2));
        assertEquals("grass", array.getString(3));

        assertEquals("{\"foo\":[\"all\",\"cows\",\"eat\",\"grass\"]}", toJsonString(patched));
    }

    @Test
    public void testMovingArrayElementPlainArray() {

        JsonArray array = Json.createArrayBuilder()
                              .add("two")
                              .add("three")
                              .add("four")
                              .add("one")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.MOVE,
                                                                             "/0",
                                                                             "/3",
                                                                             null));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertNotSame(array, patched);
        assertEquals("one", patched.getString(0));
        assertEquals("two", patched.getString(1));
        assertEquals("three", patched.getString(2));
        assertEquals("four", patched.getString(3));
    }

    @Test
    public void testMovingArrayElementToObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("one")
                                                .add("two")
                                                .add("dog"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.MOVE,
                                                                             "/bar",
                                                                             "/foo/2",
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertEquals(2, patched.size());

        JsonArray array = patched.getJsonArray("foo");
        assertEquals(2, array.size());
        assertEquals("one", array.getString(0));
        assertEquals("two", array.getString(1));

        assertEquals("dog", patched.getString("bar"));

        assertEquals("{\"foo\":[\"one\",\"two\"],\"bar\":\"dog\"}", toJsonString(patched));
    }

    @Test(expected = JsonException.class)
    public void testMovingObjectMemberNonexistingFrom() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.MOVE,
                                                                             "/baz",
                                                                             "/nomatch",
                                                                             null));

        patch.apply(object);

    }

    @Test(expected = JsonException.class)
    public void testMovingObjectMemberNonexistingTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.MOVE,
                                                                             "/nomatch/child",
                                                                             "/foo",
                                                                             null));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testMovingObjectMemberMoveToSubFrom() {

        JsonObject object = Json.createObjectBuilder()
                                .add("object", Json.createObjectBuilder()
                                                   .add("key", "value"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.MOVE,
                                                                             "/object/key",
                                                                             "/object",
                                                                             null));

        patch.apply(object);
    }


    @Test
    public void testCopyObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                               .add("foo", "bar")
                               .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.COPY,
                                                                             "/baz",
                                                                             "/foo",
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertEquals(2, patched.size());
        assertEquals("bar", patched.getString("foo"));
        assertEquals("bar", patched.getString("baz"));

        assertEquals("{\"foo\":\"bar\",\"baz\":\"bar\"}", toJsonString(patched));
    }

    @Test
    public void testCopyArrayMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("baz"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.COPY,
                                                                                 "/foo/-",
                                                                                 "/foo/0",
                                                                                 null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);

        JsonArray array = patched.getJsonArray("foo");
        assertEquals(3, array.size());
        assertEquals("bar", array.getString(0));
        assertEquals("baz", array.getString(1));
        assertEquals("bar", array.getString(2));

        assertEquals("{\"foo\":[\"bar\",\"baz\",\"bar\"]}", toJsonString(patched));
    }

    @Test
    public void testCopyArrayMemberPlainArray() {

        JsonArray array = Json.createArrayBuilder()
                              .add("foo")
                              .add("bar")
                              .build();


        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.COPY,
                                                                             "/0",
                                                                             "/1",
                                                                             null));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertNotSame(array, patched);
        assertEquals(3, patched.size());
        assertEquals("bar", patched.getString(0));
        assertEquals("foo", patched.getString(1));
        assertEquals("bar", patched.getString(2));

        assertEquals("[\"bar\",\"foo\",\"bar\"]", toJsonString(patched));
    }

    @Test
    public void testCopyObjectMemberToObjectMember() {

        JsonObject object = Json.createObjectBuilder()
                                .add("name", "Hugo")
                                .add("partner", Json.createObjectBuilder()
                                                    .add("name", "Leia")
                                                    .add("partner", JsonValue.EMPTY_JSON_OBJECT))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.COPY,
                                                                             "/partner/partner/name",
                                                                             "/name",
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);
        assertEquals("Hugo", patched.getString("name"));

        JsonObject partner = patched.getJsonObject("partner");
        assertEquals("Leia", partner.getString("name"));

        JsonObject parent = partner.getJsonObject("partner");
        assertEquals(patched.getString("name"), parent.getString("name"));

        assertEquals("{\"name\":\"Hugo\",\"partner\":{\"name\":\"Leia\",\"partner\":{\"name\":\"Hugo\"}}}", toJsonString(patched));
    }

    @Test(expected = JsonException.class)
    public void testCopyObjectMemberFromNonexistentTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.COPY,
                                                                             "/notneeded",
                                                                             "/nomatch",
                                                                             null));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testCopyObjectMemberToNonexistingTarget() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.COPY,
                                                                             "/path/nomatch",
                                                                             "/foo",
                                                                             null));

        patch.apply(object);
    }

    @Test(expected = JsonException.class)
    public void testCopyArrayMemberFromIndexOutOfBounds() {

        JsonArray array = Json.createArrayBuilder()
                              .add("foo")
                              .add("bar")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.COPY,
                                                                             "/-",
                                                                             "/2",
                                                                             null));

        patch.apply(array);
    }

    @Test(expected = JsonException.class)
    public void testCopyArrayMemberToIndexOutOfBounds() {

        JsonArray array = Json.createArrayBuilder()
                              .add("foo")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.COPY,
                                                                             "/2",
                                                                             "/-",
                                                                             null));

        patch.apply(array);
    }


    @Test
    public void testTestingObjectMemberValueSuccess() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "qux")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.TEST,
                                                                             "/foo",
                                                                             null,
                                                                             new JsonStringImpl("qux")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertSame(object, patched);
    }

    @Test(expected = JsonException.class)
    public void testTestingObjectMemberValueFailed() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "qux")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.TEST,
                                                                             "/foo",
                                                                             null,
                                                                             Json.createArrayBuilder().build()));

        patch.apply(object);
    }

    @Test
    public void testTestingArrayAsObjectMemberSuccess() {

        JsonObject object = Json.createObjectBuilder()
                                .add("name", "Thor")
                                .add("parents", Json.createArrayBuilder()
                                                    .add("Odin")
                                                    .add("Forjgyn"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.TEST,
                                                                             "/parents",
                                                                             null,
                                                                             Json.createArrayBuilder() // yessss, we really want to create a new JsonArray ;)
                                                                                 .add("Odin")
                                                                                 .add("Forjgyn")
                                                                                 .build()));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertSame(object, patched);
    }

    @Test(expected = JsonException.class)
    public void testTestingArrayAsObjectMemberFailed() {

        JsonObject object = Json.createObjectBuilder()
                                .add("magic", "array")
                                .add("numbers", Json.createArrayBuilder()
                                                    .add(1)
                                                    .add(2))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.TEST,
                                                                             "/numbers",
                                                                             null,
                                                                             Json.createArrayBuilder() // different ordering
                                                                                 .add(2)
                                                                                 .add(1)
                                                                                 .build()));

        patch.apply(object);
    }

    @Test
    public void testTestingArrayElementSuccess() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", Json.createArrayBuilder()
                                                .add("bar")
                                                .add("baz"))
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.TEST,
                                                                             "/foo/1",
                                                                             null,
                                                                             new JsonStringImpl("baz")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertSame(object, patched);
    }

    @Test
    public void testTestingArrayElementPlainArraySuccess() {

        JsonArray array = Json.createArrayBuilder()
                              .add("foo")
                              .add("bar")
                              .add("qux")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.TEST,
                                                                             "/2",
                                                                             null,
                                                                             new JsonStringImpl("qux")));

        JsonArray patched = patch.apply(array);
        assertNotNull(patched);
        assertSame(array, patched);
    }

    @Test(expected = JsonException.class)
    public void testTestingArrayElementPlainArrayFailed() {

        JsonArray array = Json.createArrayBuilder()
                              .add(1)
                              .add("2")
                              .add("qux")
                              .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.TEST,
                                                                             "/0",
                                                                             null,
                                                                             new JsonStringImpl("bar")));

        patch.apply(array);
    }

    @Test(expected = JsonException.class)
    public void testTestingObjectMemeberNonexistentTarget() {

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.TEST,
                                                                             "/nomatch",
                                                                             null,
                                                                             JsonValue.EMPTY_JSON_OBJECT));

        patch.apply(JsonValue.EMPTY_JSON_OBJECT);
    }

    @Test(expected = JsonException.class)
    public void testTestingArrayElementIndexOutOfBounds() {

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.TEST,
                                                                             "/3",
                                                                             null,
                                                                             JsonValue.EMPTY_JSON_OBJECT));

        patch.apply(JsonValue.EMPTY_JSON_ARRAY);
    }


    @Test
    public void testAddObjectMemberAlreadyExists() {

        JsonObject object = Json.createObjectBuilder()
                                .add("foo", "bar")
                                .add("baz", "qux")
                                .build();

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/foo",
                                                                             null,
                                                                             new JsonStringImpl("abcd")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);
        assertEquals("abcd", patched.getString("foo"));
        assertEquals("qux", patched.getString("baz"));

        assertEquals("{\"foo\":\"abcd\",\"baz\":\"qux\"}", toJsonString(patched));
    }

    @Test
    public void testAddArrayElementToEmptyArray() {

        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/-",
                                                                             null,
                                                                             new JsonStringImpl("foo")));

        JsonArray patched = patch.apply(JsonValue.EMPTY_JSON_ARRAY);
        assertNotNull(patched);
        assertEquals(1, patched.size());
        assertEquals("foo", patched.getString(0));
    }

    @Test
    public void testPatchWithMoreOperations() {

        JsonObject object = Json.createObjectBuilder()
                                .add("family", Json.createObjectBuilder()
                                                   .add("children", JsonValue.EMPTY_JSON_ARRAY))
                                .build();

        // i know this can be done with PatchBuilder but
        // currently it's not implemented and its fun ;)
        JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/family/father",
                                                                             null,
                                                                             Json.createObjectBuilder()
                                                                                 .add("name", "Gaio Modry Effect")
                                                                                 .build()),
                                                new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/family/mother",
                                                                             null,
                                                                             Json.createObjectBuilder()
                                                                                 .add("name", "Cassius vom Hause Clarabella")
                                                                                 .build()),
                                                new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.MOVE,
                                                                             "/family/children/0",
                                                                             "/family/mother",
                                                                             null),
                                                new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.ADD,
                                                                             "/family/mother",
                                                                             null,
                                                                             Json.createObjectBuilder()
                                                                                 .add("name", "Aimee vom Hause Clarabella")
                                                                                 .build()),
                                                new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.COPY,
                                                                             "/pedigree",
                                                                             "/family",
                                                                             null),
                                                new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REMOVE,
                                                                             "/family",
                                                                             null,
                                                                             null));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);

        JsonObject pedigree = patched.getJsonObject("pedigree");
        assertEquals("Gaio Modry Effect", pedigree.getJsonObject("father").getString("name"));
        assertEquals("Aimee vom Hause Clarabella", pedigree.getJsonObject("mother").getString("name"));
        assertEquals("Cassius vom Hause Clarabella", pedigree.getJsonArray("children").getJsonObject(0).getString("name"));

        assertEquals("{\"pedigree\":{" +
                     "\"children\":[" +
                     "{\"name\":\"Cassius vom Hause Clarabella\"}]," +
                     "\"mother\":{\"name\":\"Aimee vom Hause Clarabella\"}," +
                     "\"father\":{\"name\":\"Gaio Modry Effect\"}}}", toJsonString(patched));
    }

    @Test
    public void testCreatePatch() {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        arrayBuilder.add(Json.createObjectBuilder()
                .add("op", JsonPatch.Operation.ADD.operationName())
                .add("path", "/add")
                .add("value", "someValue").build());
        arrayBuilder.add(Json.createObjectBuilder()
                .add("op", JsonPatch.Operation.TEST.operationName())
                .add("path", "/test/someObject")
                .add("value", "someValue").build());

        JsonArray initialPatchData = arrayBuilder.build();

        JsonPatch patch = Json.createPatch(initialPatchData);

        String jsonToPatch = "{\"add\":{\"a\":\"b\"},\"test\":{\"someObject\":\"someValue\"}}";
        JsonObject jsonObjectToPatch = Json.createReader(new StringReader(jsonToPatch)).readObject();

        JsonObject patchedJsonObject = patch.apply(jsonObjectToPatch);
        Assert.assertNotNull(patchedJsonObject);
    }

    @Test
    public void testReplacingObjectAttribute() {
        final JsonObject object = Json.createObjectBuilder()
            .add("foo", Json.createObjectBuilder()
                    .add("baz", Json.createValue("1")))
            .add("bar", Json.createObjectBuilder()
                    .add("baz", Json.createValue("2")))
            .build();
        final JsonPatchImpl patch = new JsonPatchImpl(
            PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REPLACE,
                "/bar/baz",
                null,
                Json.createValue("3")));
        final JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);
        {
            final JsonObject o = patched.getJsonObject("foo");
            assertNotNull(o);
            assertEquals(Json.createValue("1"), o.getJsonString("baz"));
        }
        {
            final JsonObject o = patched.getJsonObject("bar");
            assertNotNull(o);
            assertEquals(Json.createValue("3"), o.getJsonString("baz"));
            assertEquals("{\"foo\":{\"baz\":\"1\"},\"bar\":{\"baz\":\"3\"}}", toJsonString(patched));
        }
    }

    @Test
    public void testReplacingArrayElementAttribute() {
        final JsonObject object = Json.createObjectBuilder()
            .add("foo", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder().add("bar", Json.createValue("1")))
                    .add(Json.createObjectBuilder().add("bar", Json.createValue("2"))))
            .build();
        final JsonPatchImpl patch = new JsonPatchImpl(PROVIDER, new JsonPatchImpl.PatchValue(PROVIDER, JsonPatch.Operation.REPLACE,
            "/foo/1/bar",
            null,
            Json.createValue("3")));
        final JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertNotSame(object, patched);
        final JsonArray array = patched.getJsonArray("foo");
        assertNotNull(array);
        assertNotSame(object.getJsonArray("foo"), array);
        assertEquals(2, array.size());
        assertEquals(Json.createValue("3"), array.getJsonObject(1).getJsonString("bar"));
        assertEquals(Json.createValue("1"), array.getJsonObject(0).getJsonString("bar"));
        assertEquals("{\"foo\":[{\"bar\":\"1\"},{\"bar\":\"3\"}]}", toJsonString(patched));
    }


    private static String toJsonString(final JsonStructure value) {
        return value.toString();
    }
}
