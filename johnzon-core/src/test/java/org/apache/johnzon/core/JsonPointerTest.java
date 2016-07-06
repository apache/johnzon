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
import javax.json.JsonException;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class JsonPointerTest {

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullShouldThrowNullPointerException() {
        new JsonPointer(null);
    }

    @Test(expected = JsonException.class)
    public void testConstructorWithInvalidJsonPointerShouldThrowJsonException() {
        new JsonPointer("a");
    }

    @Test(expected = NullPointerException.class)
    public void testGetValueWithNullShouldThrowNullPointerException() {
        JsonPointer jsonPointer = new JsonPointer("");
        jsonPointer.getValue(null);
    }

    @Test
    public void testGetValueWholeDocument() {
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
    public void testGetValueElementNotExistentShouldThrowJsonException() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/fool");
        jsonPointer.getValue(jsonDocument);
    }

    @Test
    public void testGetValueWholeJsonArray() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/foo");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("[\"bar\",\"baz\"]", result.toString());
    }

    @Test
    public void testGetValueJsonArrayElement() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/foo/0");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("\"bar\"", result.toString());
    }

    @Test(expected = JsonException.class)
    public void testGetValueJsonArrayElementNotExistentShouldThrowJsonException() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/foo/2");
        jsonPointer.getValue(jsonDocument);
    }

    @Test(expected = JsonException.class)
    public void testGetValueJsonArrayElementNoNumberShouldThrowJsonException() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/foo/a");
        jsonPointer.getValue(jsonDocument);
    }

    @Test(expected = JsonException.class)
    public void testGetValueJsonArrayElementLeadingZeroShouldThrowJsonException() {
        JsonStructure jsonDocument = getJsonDocument();

        JsonPointer jsonPointer = new JsonPointer("/foo/001");
        JsonValue result = jsonPointer.getValue(jsonDocument);
        assertEquals("\"bar\"", result.toString());
    }

    private JsonStructure getJsonDocument() {
        JsonReader reader = Json.createReaderFactory(Collections.<String, Object>emptyMap()).createReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("json/jsonPointerTest.json"));
        return reader.read();
    }

}
