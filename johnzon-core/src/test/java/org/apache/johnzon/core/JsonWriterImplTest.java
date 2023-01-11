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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;

import org.junit.Test;

public class JsonWriterImplTest {
    @Test
    public void objectWriter() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final JsonWriter writer = Json.createWriter(out);
        final JsonObjectBuilder ob = Json.createObjectBuilder();
        ob.add("a", 123);
        ob.add("b", new BigDecimal("234.567"));
        ob.add("c", "string");
        ob.add("d", JsonValue.TRUE);
        ob.add("e", JsonValue.FALSE);
        ob.add("f", JsonValue.NULL);
        writer.write(ob.build());
        writer.close();
        assertEquals("{\"a\":123,\"b\":234.567,\"c\":\"string\",\"d\":true,\"e\":false,\"f\":null}",
                     new String(out.toByteArray()));
    }

    @Test
    public void arrayValueWriter() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final JsonWriter writer = Json.createWriter(out);
        final JsonArrayBuilder ab = Json.createArrayBuilder();
        ab.add(123);
        ab.add(new BigDecimal("234.567"));
        ab.add("string");
        ab.add(JsonValue.TRUE);
        ab.add(JsonValue.FALSE);
        ab.add(JsonValue.NULL);
        writer.write(ab.build());
        writer.close();
        assertEquals("[123,234.567,\"string\",true,false,null]",
                     new String(out.toByteArray()));
    }

    @Test
    public void integralNumberValueWriter() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final JsonWriter writer = Json.createWriter(out);
        final JsonValue value = Json.createValue(123);
        writer.write(value);
        writer.close();
        assertEquals("123", new String(out.toByteArray()));
    }

    @Test
    public void nonIntegralNumberValueWriter() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final JsonWriter writer = Json.createWriter(out);
        final JsonValue value = Json.createValue(new BigDecimal("123.456"));
        writer.write(value);
        writer.close();
        assertEquals("123.456", new String(out.toByteArray()));
    }

    @Test
    public void stringValueWriter() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final JsonWriter writer = Json.createWriter(out);
        final JsonValue value = Json.createValue("test-value");
        writer.write(value);
        writer.close();
        assertEquals("\"test-value\"", new String(out.toByteArray()));
    }

    @Test
    public void nullValueWriter() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final JsonWriter writer = Json.createWriter(out);
        writer.write(JsonValue.NULL);
        writer.close();
        assertEquals("null", new String(out.toByteArray()));
    }

    @Test
    public void trueValueWriter() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final JsonWriter writer = Json.createWriter(out);
        writer.write(JsonValue.TRUE);
        writer.close();
        assertEquals("true", new String(out.toByteArray()));
    }

    @Test
    public void falseValueWriter() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final JsonWriter writer = Json.createWriter(out);
        writer.write(JsonValue.FALSE);
        writer.close();
        assertEquals("false", new String(out.toByteArray()));
    }
}
