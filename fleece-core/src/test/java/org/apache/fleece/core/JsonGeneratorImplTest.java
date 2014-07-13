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
package org.apache.fleece.core;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class JsonGeneratorImplTest {
    @Test
    public void notFluentGeneratorUsage() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final JsonGenerator generator = Json.createGenerator(baos);
        generator.writeStartArray();
        generator.writeStartObject();
        generator.writeEnd();
        generator.writeEnd();
        generator.close();
        assertEquals("[{}]", new String(baos.toByteArray()));
    }

    @Test
    public void emptyArray() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final JsonGenerator generator = Json.createGenerator(baos);
        generator.writeStartArray().writeEnd().close();
        assertEquals("[]", new String(baos.toByteArray()));
    }

    @Test
    public void simpleArray() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos).writeStartArray().write(1).write(2).writeEnd().close();
        assertEquals("[1,2]", new String(baos.toByteArray()));
    }
    
    @Test
    public void stringArray() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos).writeStartArray().write("val1").write("val2").writeEnd().close();
        assertEquals("[\"val1\",\"val2\"]", new String(baos.toByteArray()));
    }
    
    @Test
    public void stringArrayEscapes() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos).writeStartArray().write("\"val1\t\u0080").write("val2\\").writeEnd().close();
        assertEquals("[\"\\\"val1\\t\\u0080\",\"val2\\\\\"]", new String(baos.toByteArray()));
    }
    
    @Test
    public void emptyStringArray() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos).writeStartArray().writeNull().write("").writeEnd().close();
        assertEquals("[null,\"\"]", new String(baos.toByteArray()));
    }
    
    @Test
    public void nullLiteralArray() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos).writeStartArray().writeNull().write(JsonValue.NULL).writeEnd().close();
        assertEquals("[null,null]", new String(baos.toByteArray()));
    }
    
    @Test
    public void boolLiteralArray() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos).writeStartArray().write(JsonValue.FALSE).write(JsonValue.TRUE).writeEnd().close();
        assertEquals("[false,true]", new String(baos.toByteArray()));
    }

    @Test
    public void generate() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final JsonGenerator generator = Json.createGenerator(baos);

        generator.writeStartObject().write("firstName", "John").write("lastName", "Smith").write("age", 25)
        .writeStartObject("address").write("streetAddress", "21 2nd Street").write("city", "New York")
        .write("state", "NY").write("postalCode", "10021").writeEnd().writeStartArray("phoneNumber")
        .writeStartObject().write("type", "home").write("number", "212 555-1234").writeEnd().writeStartObject()
        .write("type", "fax").write("number", "646 555-4567").writeEnd().writeEnd().writeEnd().close();

        assertEquals("{\"firstName\":\"John\",\"lastName\":\"Smith\",\"age\":25,\"address\":"
                + "{\"streetAddress\":\"21 2nd Street\",\"city\":\"New York\",\"state\":\"NY\",\"postalCode\":\"10021\"},"
                + "\"phoneNumber\":[{\"type\":\"home\",\"number\":\"212 555-1234\"},{\"type\":\"fax\",\"number\":\"646 555-4567\"}]}", 
                new String(baos.toByteArray()));
    }

    @Test
    public void pretty() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final JsonGenerator generator = Json.createGeneratorFactory(new HashMap<String, Object>() {{
            put(JsonGenerator.PRETTY_PRINTING, true);
        }}).createGenerator(baos);

        generator.writeStartObject().write("firstName", "John").write("lastName", "Smith")
        .write("age", 25).writeStartObject("address").write("streetAddress", "21 2nd Street")
        .write("city", "New York").write("state", "NY").write("postalCode", "10021").writeEnd()
        .writeStartArray("phoneNumber").writeStartObject().write("type", "home").write("number", "212 555-1234")
        .writeEnd().writeStartObject().write("type", "fax").write("number", "646 555-4567").writeEnd().writeEnd()
        .writeEnd().close();

        assertEquals("{\n" +
                        "  \"firstName\":\"John\",\n" +
                        "  \"lastName\":\"Smith\",\n" +
                        "  \"age\":25,\n" +
                        "  \"address\":{\n" +
                        "    \"streetAddress\":\"21 2nd Street\",\n" +
                        "    \"city\":\"New York\",\n" +
                        "    \"state\":\"NY\",\n" +
                        "    \"postalCode\":\"10021\"\n" +
                        "  },\n" +
                        "  \"phoneNumber\":[\n" +
                        "    {\n" +
                        "      \"type\":\"home\",\n" +
                        "      \"number\":\"212 555-1234\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"type\":\"fax\",\n" +
                        "      \"number\":\"646 555-4567\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}", new String(baos.toByteArray()));
    }
}
