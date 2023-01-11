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

import java.io.StringReader;
import java.io.StringWriter;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

public class ParserGeneratorMappingTest {
    @Rule
    public final JsonbRule rule = new JsonbRule();

    @Test
    public void parser() {
        try (final JsonParser parser = Json.createParser(new StringReader("{\"name\":\"bar\"}"))) {
            final Foo foo = rule.fromJson(parser, Foo.class);
            assertEquals("bar", foo.name);
        }
    }

    @Test
    public void generator() {
        final StringWriter writer = new StringWriter();
        try (final JsonGenerator generator = Json.createGenerator(writer)) {
            final Foo foo = new Foo();
            foo.name = "bar";
            rule.toJson(foo, generator);
        }
        assertEquals("{\"name\":\"bar\"}", writer.toString());
    }

    public static class Foo {
        public String name;
    }
}
