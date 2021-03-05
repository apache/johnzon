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
package org.apache.johnzon;

import org.junit.Test;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import java.lang.reflect.Type;

import static javax.json.stream.JsonParser.Event.KEY_NAME;
import static javax.json.stream.JsonParser.Event.START_OBJECT;
import static javax.json.stream.JsonParser.Event.VALUE_NUMBER;
import static org.junit.Assert.assertEquals;

public class RecursivePolymorphismTest {
    @Test
    public void read() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withDeserializers(new PolyDeserializer()))) {
            final Parent parent = jsonb.fromJson("{\"type\":1,\"name\":\"first\",\"uno\":true,\"duo\":true}", Parent.class);
            assertEquals("Child1{name='first', uno=true}", parent.toString());
        }
    }

    @Test
    public void write() throws Exception {
        final Child1 child1 = new Child1();
        child1.name = "first";
        child1.uno = true;
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new PolySerializer()))) {
            final String json = jsonb.toJson(child1);
            assertEquals("{\"type\":1,\"name\":\"first\",\"uno\":true}", json);
        }
    }

    public static class Parent {
        public String name;

        @Override
        public String toString() {
            return "Parent{name='" + name + "'}";
        }
    }

    public static class Child1 extends Parent {
        public boolean uno;

        @Override
        public String toString() {
            return "Child1{name='" + name + "', uno=" + uno + '}';
        }
    }

    public static class Child2 extends Parent {
        public boolean duo;

        @Override
        public String toString() {
            return "Child2{name='" + name + "', duo=" + duo + '}';
        }
    }

    public static class PolySerializer implements JsonbSerializer<Parent> {
        @Override
        public void serialize(final Parent obj, final JsonGenerator generator, final SerializationContext ctx) {
            generator.writeStartObject();
            generator.write("type", Child1.class.isInstance(obj) ? 1 : 2);
            ctx.serialize(obj, generator);
            generator.writeEnd();
        }
    }

    public static class PolyDeserializer implements JsonbDeserializer<Parent> {
        @Override
        public Parent deserialize(final JsonParser parser,
                                  final DeserializationContext ctx,
                                  final Type rtType) {
            moveToType(parser);
            final int type = parser.getInt();
            switch (type) {
                case 1:
                    return ctx.deserialize(Child1.class, parser);
                case 2:
                    return ctx.deserialize(Child2.class, parser);
                default:
                    throw new IllegalArgumentException(String.valueOf(type));
            }
        }

        private void moveToType(final JsonParser parser) {
            ensureNext(parser, START_OBJECT);
            ensureNext(parser, KEY_NAME);
            if (!"type".equals(parser.getString())) {
                throw new IllegalArgumentException("Expected 'type' but got '" + parser.getString() + "'");
            }
            ensureNext(parser, VALUE_NUMBER);
        }

        private void ensureNext(final JsonParser parser, final JsonParser.Event expected) {
            final JsonParser.Event next = parser.next();
            if (expected != next) {
                throw new IllegalArgumentException(next + " != " + expected);
            }
        }
    }
}
