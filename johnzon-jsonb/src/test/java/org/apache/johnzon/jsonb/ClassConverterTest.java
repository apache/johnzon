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

import org.junit.Test;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.bind.spi.JsonbProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.Type;

import static org.junit.Assert.assertEquals;

public class ClassConverterTest {
    @Test
    public void roundTripSerDeser() {
        final Jsonb jsonb = JsonbProvider.provider().create().build();
        final Whole2 whole = new Whole2();
        whole.name = "test";
        assertEquals("{\"text\":\"test\"}", jsonb.toJson(whole));
        assertEquals("test", jsonb.fromJson("{\"text\":\"test\"}", Whole2.class).name);
    }

    @Test
    public void writeAdapters() {
        final Jsonb jsonb = JsonbProvider.provider().create().build();
        final Whole whole = new Whole();
        whole.name = "test";
        assertEquals("{\"name2\":\">test<\"}", jsonb.toJson(whole));

        // not really doable properly
        // assertEquals("test", jsonb.fromJson("{\"name2\":\">test<\"}", Whole.class).name);
    }

    @JsonbTypeAdapter(MyAdapter.class)
    public static class Whole {
        String name;
    }

    @JsonbTypeSerializer(MySerializer.class)
    @JsonbTypeDeserializer(MyDeserializer.class)
    public static class Whole2 {
        String name;
    }

    public static class Switch {
        public String name2;
    }

    public static class MyDeserializer implements JsonbDeserializer<Whole2> {
        @Override
        public Whole2 deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
            parser.next(); // start
            parser.next();
            assertEquals("text", parser.getString());
            parser.next();
            final Whole2 whole2 = new Whole2();
            whole2.name = parser.getString();
            parser.next(); // end
            return whole2;
        }
    }

    public static class MySerializer implements JsonbSerializer<Whole2> {
        @Override
        public void serialize(final Whole2 obj, final JsonGenerator generator, final SerializationContext ctx) {
            generator.write("text", obj.name);
        }
    }

    public static class MyAdapter implements JsonbAdapter<Whole, Switch> {
        @Override
        public Whole adaptFromJson(final Switch obj) throws Exception {
            final Whole whole = new Whole();
            whole.name = obj.name2.replace("<", "").replace(">", "");
            return whole;
        }

        @Override
        public Switch adaptToJson(final Whole obj) throws Exception {
            final Switch aSwitch = new Switch();
            aSwitch.name2 = '>' + obj.name + '<';
            return aSwitch;
        }
    }
}
