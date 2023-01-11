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
package org.apache.johnzon.jsonb;

import static org.junit.Assert.assertEquals;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.apache.johnzon.jsonb.extension.JsonValueReader;
import org.apache.johnzon.jsonb.extension.JsonValueWriter;
import org.junit.Test;

public class JsonbJsonValueTest {
    @Test
    public void from() throws Exception {
        final JsonValueReader<Simple> reader = new JsonValueReader<>(Json.createObjectBuilder().add("value", "simple").build());
        final Jsonb jsonb = JsonbBuilder.create();
        final Simple simple = jsonb.fromJson(reader, Simple.class);
        jsonb.close();
        assertEquals("simple", simple.value);
    }

    @Test
    public void to() throws Exception {
        final Simple object = new Simple();
        object.value = "simple";
        final JsonValueWriter writer = new JsonValueWriter();
        final Jsonb jsonb = JsonbBuilder.create();
        jsonb.toJson(object, writer);
        jsonb.close();
        final JsonObject jsonObject = writer.getObject();
        assertEquals("{\"value\":\"simple\"}", jsonObject.toString());
    }

    public static class Simple {
        public String value;
    }
}
