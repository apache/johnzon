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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;

import org.junit.Test;

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
        Json.createGenerator(baos).writeStartArray().write("\"val1\t\u0010").write("val2\\").writeEnd().close();
        assertEquals("[\"\\\"val1\\t\\u0010\",\"val2\\\\\"]", new String(baos.toByteArray()));
    }
    
    @Test
    public void stringArrayEscapes2() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos).writeStartArray().write("\"val1\t\u0067").write("val2\\").writeEnd().close();
        assertEquals("[\"\\\"val1\\tg\",\"val2\\\\\"]", new String(baos.toByteArray()));
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
    
    @Test(expected=JsonGenerationException.class)
    public void fail1() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos)
        .writeStartArray("test");      
    }
    
    @Test(expected=JsonGenerationException.class)
    public void fail2() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos)
        .write("test",1);      
    }
    
    @Test(expected=JsonGenerationException.class)
    public void fail3() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos)
        .writeStartObject()
        .writeStartObject();
    }
    
    @Test(expected=JsonGenerationException.class)
    public void fail4() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos)
        .writeEnd();
    }
    
    @Test(expected=JsonGenerationException.class)
    public void fail5() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos)
        .close();
    }
    
    @Test(expected=JsonGenerationException.class)
    public void fail6() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos)
        .writeStartArray()
        .writeStartObject("test");
    }
    
    @Test(expected=JsonGenerationException.class)
    public void fail7() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos)
        .writeStartArray()
        .writeNull()
        .writeStartObject()
        .write("a", new BigDecimal("123.123"))
        .write("b", true)
        .write("c", new BigInteger("3312"))
        .write("d", new JsonStringImpl("mystring"))
        .writeEnd()
        .close();
        
    }
    
    @Test(expected=JsonGenerationException.class)
    public void fail9() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos)
        .writeStartObject()
        .write("a", new BigDecimal("123.123"))
        .write("b", true)
        .write("c", new BigInteger("3312"))
        .write("d", new JsonStringImpl("mystring"))
        .writeEnd()
        .writeStartObject()
        .close();
        
    }
   
    @Test
    public void numbers() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos)
        .writeStartArray()
        .writeNull()
        .writeStartObject()
        .write("a", new BigDecimal("123.123"))
        .write("b", true)
        .write("c", new BigInteger("3312"))
        .write("d", new JsonStringImpl("Mystring"))
        .writeEnd()
        .writeEnd()
        .close();
        assertEquals("[null,{\"a\":123.123,\"b\":true,\"c\":3312,\"d\":\"Mystring\"}]", new String(baos.toByteArray()));
    }
    
    @Test
    public void numbers2() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos)
        .writeStartArray()
        .writeNull()
        .writeStartObject()
        .write("a", 999999999L)
        .write("b", 123)
        .write("c", -444444444L)
        .write("d",-123)
        .writeEnd()
        .writeEnd()
        .close();
        assertEquals("[null,{\"a\":999999999,\"b\":123,\"c\":-444444444,\"d\":-123}]", new String(baos.toByteArray()));
    }
    
    @Test
    public void arrayInArray() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Json.createGenerator(baos)
        .writeStartArray()
        .writeStartArray()
        .writeNull()
        .writeEnd()
        .writeEnd()
        .close();
         assertEquals("[[null]]", new String(baos.toByteArray()));
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
