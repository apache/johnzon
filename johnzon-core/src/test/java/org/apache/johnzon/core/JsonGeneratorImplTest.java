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

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Queue;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerationException;
import jakarta.json.stream.JsonGenerator;

import org.junit.Assert;
import org.junit.Test;

public class JsonGeneratorImplTest {
    @Test(expected = JsonGenerationException.class)
    public void unexpectedWriteEnd() {
        final JsonGenerator generator = Json.createGenerator(new StringWriter());
        generator.writeStartObject()
                .writeEnd()
                .writeEnd();
    }

    @Test
    public void writeKeyWrite() {
        final StringWriter writer = new StringWriter();
        Json.createGenerator(writer).writeStartObject().writeKey("foo").write("bar").writeEnd().close();
        assertEquals("{\"foo\":\"bar\"}", writer.toString());
    }

    @Test
    public void writeKeyWriteFormatted() {
        final StringWriter writer = new StringWriter();
        Json.createGeneratorFactory(singletonMap(JsonGenerator.PRETTY_PRINTING, "true")).createGenerator(writer)
            .writeStartObject().writeKey("foo").write("bar").writeEnd().close();
        assertEquals("{\n  \"foo\":\"bar\"\n}", writer.toString());
    }

    @Test
    public void writeKeyWriteNull() {
        final StringWriter writer = new StringWriter();
        Json.createGenerator(writer)
            .writeStartObject().writeKey("foo").writeNull().writeEnd().close();
        assertEquals("{\"foo\":null}", writer.toString());
    }

    @Test
    public void closeOnce() throws Throwable {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final JsonGenerator generator = Json.createGenerator(baos).writeStartObject().writeEnd();
        for (int i = 0; i < 10; i++) {
            generator.close();
            assertEquals(1, getBufferSize(generator));
        }
    }

    private int getBufferSize(final JsonGenerator generator) throws Throwable {
        final Field bufferProvider = generator.getClass()
                                              .getDeclaredField("bufferProvider");
        if (!bufferProvider.isAccessible()) {
            bufferProvider.setAccessible(true);
        }
        final Object provider = bufferProvider.get(generator);
        final Field queue = provider.getClass().getSuperclass().getDeclaredField("queue");
        if (!queue.isAccessible()) {
            queue.setAccessible(true);
        }
        return Queue.class.cast(queue.get(provider)).size();
    }

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
    public void testCreateGenerator() {
        StringWriter sw = new StringWriter();
        JsonGenerator jsonGenerator = Json.createGenerator(sw);
        jsonGenerator.writeStartObject()
                .write("a", "b")
                .write("c", "d")
                .writeEnd();

        jsonGenerator.close();

        Assert.assertEquals("{\"a\":\"b\",\"c\":\"d\"}", sw.toString());
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

    @Test
    public void prettyArray() {
        { // root
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final JsonGenerator generator = Json.createGeneratorFactory(new HashMap<String, Object>() {{
                put(JsonGenerator.PRETTY_PRINTING, true);
            }}).createGenerator(baos);
            generator.writeStartArray().write("a").write("b").write(1).writeEnd().close();
            assertEquals("[\n" +
                    "  \"a\",\n" +
                    "  \"b\",\n" +
                    "  1\n" +
                    "]", new String(baos.toByteArray()));
        }
        { // nested
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final JsonGenerator generator = Json.createGeneratorFactory(new HashMap<String, Object>() {{
                put(JsonGenerator.PRETTY_PRINTING, true);
            }}).createGenerator(baos);
            generator.writeStartArray().writeStartArray().write("a").write("b").writeEnd().writeStartArray().writeEnd().writeEnd().close();
            assertEquals("[\n" +
                    "  [\n" +
                    "    \"a\",\n" +
                    "    \"b\"\n" +
                    "  ],\n" +
                    "  [\n" +
                    "  ]\n" +
                    "]", new String(baos.toByteArray()));
        }
        { // empty
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final JsonGenerator generator = Json.createGeneratorFactory(new HashMap<String, Object>() {{
                put(JsonGenerator.PRETTY_PRINTING, true);
            }}).createGenerator(baos);
            generator.writeStartArray().writeEnd().close();
            assertEquals("[\n]", new String(baos.toByteArray()));
        }
        { // empty nested
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final JsonGenerator generator = Json.createGeneratorFactory(new HashMap<String, Object>() {{
                put(JsonGenerator.PRETTY_PRINTING, true);
            }}).createGenerator(baos);
            generator.writeStartArray().writeStartArray().writeEnd().writeStartArray().writeEnd().writeEnd().close();
            assertEquals("[\n" +
                    "  [\n" +
                    "  ],\n" +
                    "  [\n" +
                    "  ]\n" +
                    "]", new String(baos.toByteArray()));
        }
        { // nested in object
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final JsonGenerator generator = Json.createGeneratorFactory(new HashMap<String, Object>() {{
                put(JsonGenerator.PRETTY_PRINTING, true);
            }}).createGenerator(baos);
            generator.writeStartObject().writeStartArray("foo").writeStartArray().writeEnd().writeStartArray().writeEnd().writeEnd().writeEnd().close();
            assertEquals("{\n" +
                    "  \"foo\":[\n" +
                    "    [\n" +
                    "    ],\n" +
                    "    [\n" +
                    "    ]\n" +
                    "  ]\n" +
                    "}", new String(baos.toByteArray()));
        }
    }
    
    @Test
    public void prettySimple() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final JsonGenerator generator = Json.createGeneratorFactory(new HashMap<String, Object>() {{
            put(JsonGenerator.PRETTY_PRINTING, true);
        }}).createGenerator(baos);

        generator.writeStartObject().write("firstName", "John").writeEnd().close();

        assertEquals("{\n" +
                        "  \"firstName\":\"John\"\n" +
                        "}", new String(baos.toByteArray()));
    }
    
    @Test
    public void prettySimpleStructure() {
        final JsonWriterFactory writerFactory = Json.createWriterFactory(new HashMap<String, Object>() {
            {
                put(JsonGenerator.PRETTY_PRINTING, true);
            }
        });

        StringWriter buffer = new StringWriter();

        final JsonWriter writer = writerFactory.createWriter(buffer);
        writer.write(Json.createObjectBuilder().add("firstName", "John").build());
        writer.close();

        assertEquals("{\n" + "  \"firstName\":\"John\"\n" + "}", buffer.toString());
    }

    @Test
    public void prettySimpleWriter() {

        final JsonWriterFactory writerFactory = Json.createWriterFactory(new HashMap<String, Object>() {
            {
                put(JsonGenerator.PRETTY_PRINTING, true);
            }
        });

        StringWriter buffer = new StringWriter();

        final JsonReader reader = Json.createReader(new ByteArrayInputStream("{\"firstName\":\"John\"}".getBytes()));
        final JsonWriter writer = writerFactory.createWriter(buffer);
        writer.write(reader.read());
        writer.close();
        reader.close();

        assertEquals("{\n" + "  \"firstName\":\"John\"\n" + "}", buffer.toString());
    }

    @Test(expected = JsonGenerationException.class)
    public void errorWhenWritingValueInObject() {
        Json.createGenerator(new StringWriter()).writeStartObject().write(1);
    }

    @Test
    public void writeNumber() {
        final StringWriter writer = new StringWriter();
        Json.createGenerator(writer).write(1).close();
        assertEquals("1", writer.toString());
    }

    @Test
    public void writeBoolean() {
        final StringWriter writer = new StringWriter();
        Json.createGenerator(writer).write(true).close();
        assertEquals("true", writer.toString());
    }
}
