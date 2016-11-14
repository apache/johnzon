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
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonPointerTest {

    @Test(expected = NullPointerException.class)
    public void testConstructorWithJsonPointerNull() {
        new JsonPointer(null);
    }

    @Test(expected = JsonException.class)
    public void testConstructorWithInvalidJsonPointer() {
        new JsonPointer("a");
    }

    @Test(expected = NullPointerException.class)
    public void testGetValueWithTargetNull() {
        JsonPointer jsonPointer = new JsonPointer("");
        jsonPointer.getValue(null);
    }

    @Test
    public void testGetValueWithWholeDocument() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals(jsonDocument.toString(), result.toString());
    }

    @Test
    public void testGetValue0() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("0", result.toString());
    }

    @Test
    public void testGetValue1() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/a~1b");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("1", result.toString());
    }

    @Test
    public void testGetValue2() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/c%d");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("2", result.toString());
    }

    @Test
    public void testGetValue3() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/e^f");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("3", result.toString());
    }

    @Test
    public void testGetValue4() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/g|h");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("4", result.toString());
    }

    @Test
    public void testGetValue5() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/i\\j");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("5", result.toString());
    }

    @Test
    public void testGetValue6() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/k\"l");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("6", result.toString());
    }

    @Test
    public void testGetValue7() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/ ");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("7", result.toString());
    }

    @Test
    public void testGetValue8() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/m~0n");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("8", result.toString());
    }

    @Test(expected = JsonException.class)
    public void testGetValueWithElementNotExistent() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/fool");
        jsonPointer.getValue(jsonDocument);
    }

    @Test
    public void testGetValueWithWholeJsonArray() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/foo");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("[\"bar\",\"baz\"]", result.toString());
    }

    @Test
    public void testGetValueWithJsonArray() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/foo/0");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("\"bar\"", result.toString());
    }

    @Test(expected = JsonException.class)
    public void testGetValueWithJsonArrayIndexOutOfRange() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/foo/2");
        jsonPointer.getValue(jsonDocument);
    }

    @Test(expected = JsonException.class)
    public void testGetValueWithJsonArrayIndexNoNumber() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/foo/a");
        jsonPointer.getValue(jsonDocument);
    }

    @Test(expected = JsonException.class)
    public void testGetValueWithJsonArrayLeadingZeroIndex() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/foo/01");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("\"bar\"", result.toString());
    }

    @Test(expected = JsonException.class)
    public void testGetValueWithJsonArrayInvalidIndex() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/foo/-1");
        jsonPointer.getValue(jsonDocument);
    }

    @Test(expected = NullPointerException.class)
    public void testAddJsonStructureWithTargetNull() {
        JsonPointer jsonPointer = new JsonPointer("");
        jsonPointer.add((JsonStructure) null, new JsonStringImpl("qux"));
    }

    @Test(expected = JsonException.class)
    public void testAddJsonStructureWithTypeValueNotTypeTarget() {
        JsonPointer jsonPointer = new JsonPointer("");
        JsonObject target = Json.createObjectBuilder().build();
        JsonArray value = Json.createArrayBuilder().build();

        jsonPointer.add((JsonStructure) target, value);
    }

    @Test
    public void testAddJsonStructureWithEmptyJsonPointer() {
        JsonPointer jsonPointer = new JsonPointer("");
        JsonStructure target = Json.createObjectBuilder().build();
        JsonObject value = Json.createObjectBuilder()
                .add("foo", "bar").build(); // { "foo": "bar" }

        JsonStructure result = jsonPointer.add(target, value);
        assertEquals("{\"foo\":\"bar\"}", result.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testAddJsonObjectWithTargetNull() {
        JsonPointer jsonPointer = new JsonPointer("");
        jsonPointer.add((JsonObject) null, new JsonStringImpl("qux"));
    }

    @Test(expected = NullPointerException.class)
    public void testAddJsonArrayWithTargetNull() {
        JsonPointer jsonPointer = new JsonPointer("");
        jsonPointer.add((JsonArray) null, new JsonStringImpl("qux"));
    }

    @Test(expected = JsonException.class)
    public void testAddArrayElementWithInvalidIndex() {
        JsonPointer jsonPointer = new JsonPointer("/+");
        JsonStructure target = Json.createArrayBuilder().build();

        jsonPointer.add(target, new JsonStringImpl("qux"));
    }

    @Test(expected = JsonException.class)
    public void testAddArrayElementWithIndexOutOfRange() {
        JsonPointer jsonPointer = new JsonPointer("/1");
        JsonStructure target = Json.createArrayBuilder().build();

        jsonPointer.add(target, new JsonStringImpl("qux"));
    }

    @Test(expected = JsonException.class)
    public void testAddArrayElementWithLeadingZeroIndex() {
        JsonPointer jsonPointer = new JsonPointer("/01");
        JsonStructure target = Json.createArrayBuilder()
                .add("foo").build();

        jsonPointer.add(target, new JsonStringImpl("qux"));
    }

    @Test(expected = JsonException.class)
    public void testAddArrayElementWithIndexNoNumber() {
        JsonPointer jsonPointer = new JsonPointer("/a");
        JsonStructure target = Json.createArrayBuilder()
                .add("foo").build();

        jsonPointer.add(target, new JsonStringImpl("qux"));
    }

    @Test
    public void testAddObject() {
        JsonPointer jsonPointer = new JsonPointer("/child");
        JsonStructure target = Json.createObjectBuilder()
                .add("foo", "bar").build(); // {"foo":"bar"}
        JsonObject value = Json.createObjectBuilder()
                .add("grandchild", Json.createObjectBuilder()).build(); // {"grandchild":{}}

        JsonStructure result = jsonPointer.add(target, value);
        assertEquals("{\"foo\":\"bar\",\"child\":{\"grandchild\":{}}}", result.toString()); // {"foo":"bar","child":{"grandchild":{}}}
    }

    @Test
    public void testAddObjectMember() {
        JsonPointer jsonPointer = new JsonPointer("/baz");
        JsonStructure target = Json.createObjectBuilder()
                .add("foo", "bar").build(); // {"foo":"bar"}

        JsonStructure result = jsonPointer.add(target, new JsonStringImpl("qux"));
        assertEquals("{\"foo\":\"bar\",\"baz\":\"qux\"}", result.toString()); // {"foo":"bar","baz":"qux"}
    }

    @Test
    public void testAddFirstObjectMember() {
        JsonPointer jsonPointer = new JsonPointer("/foo");
        JsonStructure target = Json.createObjectBuilder().build(); // {}

        JsonStructure result = jsonPointer.add(target, new JsonStringImpl("bar"));
        assertEquals("{\"foo\":\"bar\"}", result.toString()); // {"foo":"bar"}
    }

    @Test(expected = JsonException.class)
    public void testAddObjectMemberWithNonexistentTarget() {
        JsonPointer jsonPointer = new JsonPointer("/baz/bat");
        JsonStructure target = Json.createObjectBuilder()
                .add("foo", "bar").build(); // {"foo":"bar"}

        jsonPointer.add(target, new JsonStringImpl("qux"));
    }

    @Test
    public void testAddReplaceObjectMember() {
        JsonPointer jsonPointer = new JsonPointer("/baz");
        JsonStructure target = Json.createObjectBuilder()
                .add("baz", "qux")
                .add("foo", "bar").build(); // {"baz":"qux","foo":"bar"}

        JsonStructure result = jsonPointer.add(target, new JsonStringImpl("boo"));
        assertEquals("{\"baz\":\"boo\",\"foo\":\"bar\"}", result.toString()); // {"baz":"boo","foo":"bar"}
    }

    @Test
    public void testAddArray() {
        JsonPointer jsonPointer = new JsonPointer("/0/-");
        JsonStructure target = Json.createArrayBuilder()
                .add(Json.createArrayBuilder()
                        .add("bar")).build(); // [["bar"]]
        JsonArray value = Json.createArrayBuilder()
                .add("abc")
                .add("def").build();// ["abc","def"]

        JsonStructure result = jsonPointer.add(target, value);
        assertEquals("[[\"bar\",[\"abc\",\"def\"]]]", result.toString()); // [["bar",["abc","def"]]]
    }

    @Test
    public void testAddArrayElement() {
        JsonPointer jsonPointer = new JsonPointer("/foo/1");
        JsonStructure target = Json.createObjectBuilder()
                .add("foo", Json.createArrayBuilder()
                        .add("bar")
                        .add("baz")).build(); // {"foo":["bar","baz"]}

        JsonStructure result = jsonPointer.add(target, new JsonStringImpl("qux"));
        assertEquals("{\"foo\":[\"bar\",\"qux\",\"baz\"]}", result.toString()); // {"foo":["bar","qux","baz"]}
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveJsonObjectWithTargetNull() {
        JsonPointer jsonPointer = new JsonPointer("/");
        jsonPointer.remove((JsonObject) null);
    }

    @Test(expected = JsonException.class)
    public void testRemoveJsonObjectWithEmptyJsonPointer() {
        JsonPointer jsonPointer = new JsonPointer("");
        JsonObject target = Json.createObjectBuilder().build();

        jsonPointer.remove(target);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveJsonArrayWithTargetNull() {
        JsonPointer jsonPointer = new JsonPointer("/");
        jsonPointer.remove((JsonArray) null);
    }

    @Test(expected = JsonException.class)
    public void testRemoveJsonArrayWithEmptyJsonPointer() {
        JsonPointer jsonPointer = new JsonPointer("");
        JsonArray target = Json.createArrayBuilder().build();

        jsonPointer.remove(target);
    }

    @Test(expected = JsonException.class)
    public void testRemoveArrayElementWithIndexNoNumber() {
        JsonPointer jsonPointer = new JsonPointer("/foo/a");
        JsonStructure target = Json.createObjectBuilder()
                .add("foo", Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // {"foo":["bar","qux","baz"]}

        jsonPointer.remove(target);
    }

    @Test(expected = JsonException.class)
    public void testRemoveArrayElementWithIndexOutOfRange() {
        JsonPointer jsonPointer = new JsonPointer("/foo/3");
        JsonStructure target = Json.createObjectBuilder()
                .add("foo", Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // {"foo":["bar","qux","baz"]}

        jsonPointer.remove(target);
    }

    @Test(expected = JsonException.class)
    public void testRemoveArrayElementWithInvalidIndex() {
        JsonPointer jsonPointer = new JsonPointer("/foo/+");
        JsonStructure target = Json.createObjectBuilder()
                .add("foo", Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // {"foo":["bar","qux","baz"]}

        jsonPointer.remove(target);
    }

    @Test(expected = JsonException.class)
    public void testRemoveArrayElementWithLeadingZeroIndex() {
        JsonPointer jsonPointer = new JsonPointer("/foo/01");
        JsonStructure target = Json.createObjectBuilder()
                .add("foo", Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // {"foo":["bar","qux","baz"]}

        jsonPointer.remove(target);
    }

    @Test
    public void testRemoveArrayElement() {
        JsonPointer jsonPointer = new JsonPointer("/0/1");
        JsonStructure target = Json.createArrayBuilder()
                .add(Json.createArrayBuilder()
                        .add("bar")
                        .add("qux")
                        .add("baz")).build(); // [["bar","qux","baz"]]

        JsonStructure result = jsonPointer.remove(target);
        assertEquals("[[\"bar\",\"baz\"]]", result.toString()); // [["bar","baz"]]
    }

    @Test
    public void testRemoveObjectMember() {
        JsonPointer jsonPointer = new JsonPointer("/baz");
        JsonStructure target = Json.createObjectBuilder()
                .add("baz", "qux")
                .add("foo", "bar").build(); // {"baz":"qux","foo":"bar"}

        JsonStructure result = jsonPointer.remove(target);
        assertEquals("{\"foo\":\"bar\"}", result.toString()); // {"foo":"bar"}
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceJsonObjectWithTargetNull() {
        JsonPointer jsonPointer = new JsonPointer("/");
        jsonPointer.replace((JsonObject) null, new JsonStringImpl("qux"));
    }

    @Test(expected = JsonException.class)
    public void testReplaceJsonObjectWithEmptyJsonPointer() {
        JsonPointer jsonPointer = new JsonPointer("");
        JsonObject target = Json.createObjectBuilder().build();

        jsonPointer.replace(target, new JsonStringImpl("qux"));
    }

    @Test(expected = NullPointerException.class)
    public void testReplaceJsonArrayWithTargetNull() {
        JsonPointer jsonPointer = new JsonPointer("/");
        jsonPointer.replace((JsonArray) null, new JsonStringImpl("qux"));
    }

    @Test(expected = JsonException.class)
    public void testReplaceJsonArrayWithEmptyJsonPointer() {
        JsonPointer jsonPointer = new JsonPointer("");
        JsonArray target = Json.createArrayBuilder().build();

        jsonPointer.replace(target, new JsonStringImpl("qux"));
    }

    @Test
    public void testReplaceArrayElement() {
        JsonPointer jsonPointer = new JsonPointer("/1/1");
        JsonStructure target = Json.createArrayBuilder()
                .add("bar")
                .add(Json.createArrayBuilder()
                        .add("abc")
                        .add("def")).build(); // ["bar",["abc","def"]]

        JsonStructure result = jsonPointer.replace(target, new JsonStringImpl("qux"));
        assertEquals("[\"bar\",[\"abc\",\"qux\"]]", result.toString()); // ["bar",["abc","qux"]]
    }

    @Test(expected = JsonException.class)
    public void testReplaceArrayElementWithIndexOutOfRange() {
        JsonPointer jsonPointer = new JsonPointer("/1/2");
        JsonStructure target = Json.createArrayBuilder()
                .add("bar")
                .add(Json.createArrayBuilder()
                        .add("abc")
                        .add("def")).build(); // ["bar",["abc","def"]]

        jsonPointer.replace(target, new JsonStringImpl("qux"));
    }

    @Test(expected = JsonException.class)
    public void testReplaceArrayElementWithIndexNoNumber() {
        JsonPointer jsonPointer = new JsonPointer("/1/a");
        JsonStructure target = Json.createArrayBuilder()
                .add("bar")
                .add(Json.createArrayBuilder()
                        .add("abc")
                        .add("def")).build(); // ["bar",["abc","def"]]

        jsonPointer.replace(target, new JsonStringImpl("qux"));
    }

    @Test(expected = JsonException.class)
    public void testReplaceArrayElementWithLeadingZeroIndex() {
        JsonPointer jsonPointer = new JsonPointer("/1/01");
        JsonStructure target = Json.createArrayBuilder()
                .add("bar")
                .add(Json.createArrayBuilder()
                        .add("abc")
                        .add("def")).build(); // ["bar",["abc","def"]]

        jsonPointer.replace(target, new JsonStringImpl("qux"));
    }

    @Test(expected = JsonException.class)
    public void testReplaceArrayElementWithInvalidIndex() {
        JsonPointer jsonPointer = new JsonPointer("/1/+");
        JsonStructure target = Json.createArrayBuilder()
                .add("bar")
                .add(Json.createArrayBuilder()
                        .add("abc")
                        .add("def")).build(); // ["bar",["abc","def"]]

        jsonPointer.replace(target, new JsonStringImpl("qux"));
    }

    @Test
    public void testReplaceObjectMember() {
        JsonPointer jsonPointer = new JsonPointer("/baz");
        JsonStructure target = Json.createObjectBuilder()
                .add("foo", "bar")
                .add("baz", "qux").build(); // {"foo":"bar","baz":"qux"}

        JsonStructure result = jsonPointer.replace(target, new JsonStringImpl("boo"));
        assertEquals("{\"foo\":\"bar\",\"baz\":\"boo\"}", result.toString()); // {"foo":"bar","baz":"boo"}
    }

    @Test(expected = JsonException.class)
    public void testReplaceObjectMemberWithNonexistentTarget1() {
        JsonPointer jsonPointer = new JsonPointer("/baz/a");
        JsonStructure target = Json.createObjectBuilder()
                .add("foo", "bar")
                .add("baz", "qux").build(); // {"foo":"bar","baz":"qux"}

        JsonStructure result = jsonPointer.replace(target, new JsonStringImpl("boo"));
        assertEquals("{\"foo\":\"bar\",\"baz\":\"boo\"}", result.toString()); // {"foo":"bar","baz":"boo"}
    }

    @Test(expected = JsonException.class)
    public void testReplaceObjectMemberWithNonexistentTarget2() {
        JsonPointer jsonPointer = new JsonPointer("/fo");
        JsonStructure target = Json.createObjectBuilder()
                .add("foo", "bar")
                .add("baz", "qux").build(); // {"foo":"bar","baz":"qux"}

        JsonStructure result = jsonPointer.replace(target, new JsonStringImpl("boo"));
        assertEquals("{\"foo\":\"bar\",\"baz\":\"boo\"}", result.toString()); // {"foo":"bar","baz":"boo"}
    }

    @Test
    public void testEqualsTrue() {
        JsonPointer jsonPointer1 = new JsonPointer("/foo/1");
        JsonPointer jsonPointer2 = new JsonPointer("/foo/1");
        assertTrue(jsonPointer1.equals(jsonPointer2));
    }

    @Test
    public void testEqualsFalse() {
        JsonPointer jsonPointer1 = new JsonPointer("/foo/1");
        JsonPointer jsonPointer2 = new JsonPointer("/foo/2");
        assertFalse(jsonPointer1.equals(jsonPointer2));
    }

    private JsonStructure getJsonDocument() {
        JsonReader reader = Json.createReaderFactory(Collections.<String, Object>emptyMap()).createReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("json/jsonPointerTest.json"));
        return reader.read();
    }

}
