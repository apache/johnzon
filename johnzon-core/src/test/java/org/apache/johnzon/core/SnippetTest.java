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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

public class SnippetTest {

    @Test
    public void simple() {
        final String jsonText = "{\"name\":\"string\",\"value\":\"string\",\"type\":\"string\"}";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonObject object = jsonParser.getObject();

        // This snippet is smaller than the allowed size.  It should show in entirety.
        assertEquals("{\"name\":\"string\",\"value\":\"string\",\"type\":\"string\"}", Snippet.of(object, 100));

        // This snippet is exactly 50 characters when formatted.  We should see no "..." at the end.
        assertEquals("{\"name\":\"string\",\"value\":\"string\",\"type\":\"string\"}", Snippet.of(object, 50));

        // This snippet is too large.  We should see the "..." at the end.
        assertEquals("{\"name\":\"string\",\"value\":\"stri...", Snippet.of(object, 30));
    }

    @Test
    public void mapOfArray() {
        final String jsonText = "{\"name\": [\"red\", \"green\", \"blue\"], \"value\": [\"orange\", \"yellow\", \"purple\"]}";

        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonObject object = jsonParser.getObject();

        assertEquals("{\"name\":[\"red\",\"green\",\"blue\"],\"value\":[\"orange\",\"yellow\",\"purple\"]}", Snippet.of(object, 200));
        assertEquals("{\"name\":[\"red\",\"green\",\"blue\"],\"value\":[\"orange\",\"...", Snippet.of(object, 50));
    }

    @Test
    public void mapOfObject() {
        final String jsonText = "{\"name\": {\"name\": \"red\", \"value\": \"green\", \"type\": \"blue\"}," +
                " \"value\": {\"name\": \"orange\", \"value\": \"purple\", \"type\": \"yellow\"}}";

        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonObject object = jsonParser.getObject();

        assertEquals("{\"name\":{\"name\":\"red\",\"value\":\"green\",\"type\":\"blue\"}," +
                "\"value\":{\"name\":\"orange\",\"value\":\"purple\",\"type\":\"yellow\"}}", Snippet.of(object, 200));

        assertEquals("{\"name\":{\"name\":\"red\",\"value\":\"green\",\"type\":\"blue\"}," +
                "\"value\":{\"name\":\"orange\",\"value\":\"purple\",\"type...", Snippet.of(object, 100));
    }

    @Test
    public void mapOfNestedMaps() {
        final String jsonText = "{\"name\": {\"name\": {\"name\": {\"name\": \"red\", \"value\": \"green\", \"type\": \"blue\"}}}}";

        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonObject object = jsonParser.getObject();

        assertEquals("{\"name\":{\"name\":{\"name\":{\"name\":\"red\"," +
                "\"value\":\"green\",\"type\":\"blue\"}}}}", Snippet.of(object, 100));

        assertEquals("{\"name\":{\"name\":{\"name\":{\"name\":\"red\",\"value\":\"gre...", Snippet.of(object, 50));
    }

    @Test
    public void mapOfString() {
        final String jsonText = "{\"name\":\"string\",\"value\":\"string\",\"type\":\"string\"}";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonObject object = jsonParser.getObject();
        assertEquals("{\"name\":\"string\",\"value\":\"string\",\"type\":\"string\"}", Snippet.of(object, 50));
        assertEquals("{\"name\":\"string\",\"value\":\"stri...", Snippet.of(object, 30));
    }

    @Test
    public void mapOfNumber() {
        final String jsonText = "{\"name\":1234,\"value\":5,\"type\":67890}";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonObject object = jsonParser.getObject();

        assertEquals("{\"name\":1234,\"value\":5,\"type\":67890}", Snippet.of(object, 40));
        assertEquals("{\"name\":1234,\"value\":5,\"type\":...", Snippet.of(object, 30));
    }

    @Test
    public void mapOfTrue() {
        final String jsonText = "{\"name\":true,\"value\":true,\"type\":true}";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonObject object = jsonParser.getObject();

        assertEquals("{\"name\":true,\"value\":true,\"type\":true}", Snippet.of(object, 40));
        assertEquals("{\"name\":true,\"value\":true,\"typ...", Snippet.of(object, 30));
    }

    @Test
    public void mapOfFalse() {
        final String jsonText = "{\"name\":false,\"value\":false,\"type\":false}";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonObject object = jsonParser.getObject();

        assertEquals("{\"name\":false,\"value\":false,\"type\":false}", Snippet.of(object, 50));
        assertEquals("{\"name\":false,\"value\":false,\"t...", Snippet.of(object, 30));
    }

    @Test
    public void mapOfNull() {
        final String jsonText = "{\"name\":null,\"value\":null,\"type\":null}";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonObject object = jsonParser.getObject();

        assertEquals("{\"name\":null,\"value\":null,\"type\":null}", Snippet.of(object, 50));
        assertEquals("{\"name\":null,\"value\":null,\"typ...", Snippet.of(object, 30));
    }

    @Test
    public void arrayOfArray() {
        final String jsonText = "[[\"red\",\"green\"], [1,22,333], [{\"r\":  255,\"g\": 165}], [true, false]]";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("[[\"red\",\"green\"],[1,22,333],[{\"r\":255,\"g\":165}],[true,false]]", Snippet.of(object, 100));
        assertEquals("[[\"red\",\"green\"],[1,22,333],[{\"r\":255,\"g...", Snippet.of(object, 40));
    }

    @Test
    public void arrayOfObject() {
        final String jsonText = "[{\"r\":  255,\"g\": \"165\"},{\"g\":  0,\"a\": \"0\"},{\"transparent\": false}]";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("[{\"r\":255,\"g\":\"165\"},{\"g\":0,\"a\":\"0\"},{\"transparent\":false}]", Snippet.of(object, 100));
        assertEquals("[{\"r\":255,\"g\":\"165\"},{\"g\":0,\"a...", Snippet.of(object, 30));
    }

    @Test
    public void arrayOfString() {
        final String jsonText = "[\"red\", \"green\", \"blue\", \"orange\", \"yellow\", \"purple\"]";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("[\"red\",\"green\",\"blue\",\"orange\",\"yellow\",\"purple\"]", Snippet.of(object, 100));
        assertEquals("[\"red\",\"green\",\"blue\",\"orange\"...", Snippet.of(object, 30));
    }

    @Test
    public void arrayOfNumber() {
        final String jsonText = "[1,22,333,4444,55555,666666,7777777,88888888,999999999]";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("[1,22,333,4444,55555,666666,7777777,88888888,999999999]", Snippet.of(object, 100));
        assertEquals("[1,22,333,4444,55555,666666,77...", Snippet.of(object, 30));
    }

    @Test
    public void arrayOfTrue() {
        final String jsonText = "[true,true,true,true,true,true,true,true]";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("[true,true,true,true,true,true,true,true]", Snippet.of(object, 100));
        assertEquals("[true,true,true,true,true,true...", Snippet.of(object, 30));
    }

    @Test
    public void arrayOfFalse() {
        final String jsonText = "[false,false,false,false,false,false,false]";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("[false,false,false,false,false,false,false]", Snippet.of(object, 100));
        assertEquals("[false,false,false,false,false...", Snippet.of(object, 30));
    }

    @Test
    public void arrayOfNull() {
        final String jsonText = "[null,null,null,null,null,null]";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("[null,null,null,null,null,null]", Snippet.of(object, 50));
        assertEquals("[null,null,null...", Snippet.of(object, 15));
    }

    @Test
    public void string() {
        final String jsonText = "\"This is a \\\"string\\\" with quotes in it.  It should be properly escaped.\"";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("\"This is a \\\"string\\\" with quotes in it.  It should be properly escaped.\"", Snippet.of(object, 100));
        assertEquals("\"This is a \\\"string\\\" with quotes in it.  It shoul...", Snippet.of(object, 50));
    }

    @Test
    public void number() {
        final String jsonText = "1223334444555556666667777777.88888888999999999";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("1223334444555556666667777777.88888888999999999", Snippet.of(object, 50));
        assertEquals("1223334444555556666667777777.8...", Snippet.of(object, 30));
    }

    @Test
    public void trueValue() {
        final String jsonText = "true";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("true", Snippet.of(object, 50));
        // we don't trim 'true' -- showing users something like 't...' doesn't make much sense
        assertEquals("true", Snippet.of(object, 1));
    }

    @Test
    public void falseValue() {
        final String jsonText = "false";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("false", Snippet.of(object, 50));
        // we don't trim 'false' -- showing users something like 'f...' doesn't make much sense
        assertEquals("false", Snippet.of(object, 1));
    }

    @Test
    public void nullValue() {
        final String jsonText = "null";
        final JsonParser jsonParser = Json.createParser(new ByteArrayInputStream(jsonText.getBytes()));
        final JsonValue object = jsonParser.getValue();

        assertEquals("null", Snippet.of(object, 50));
        // we don't trim 'null' -- showing users something like 'n...' doesn't make much sense
        assertEquals("null", Snippet.of(object, 1));
    }

}
