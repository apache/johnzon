/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.jsonb;

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SerializersMapTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule().withFormatting(true);

    @Before
    public void init() {
        MapDeSer.serializerCalled = false;
        MapDeSer.deserializerCalled = false;
    }

    @Test
    public void serializeMapTest() {
        MapModel mapModel = new MapModel();
        mapModel.map.put("key1", "value1");
        mapModel.map.put("key2", "value2");

        assertEquals("" +
                "{\n" +
                "  \"map\":{\n" +
                "    \"key1\":\"value1\",\n" +
                "    \"key2\":\"value2\"\n" +
                "  }\n" +
                "}" +
                "", jsonb.toJson(mapModel));

        assertTrue(MapDeSer.serializerCalled);
        assertFalse(MapDeSer.deserializerCalled);
    }

    @Test
    public void deserializeMapTest() {
        final Map<String, String> expected = new HashMap<>();
        expected.put("key1", "value1");
        expected.put("key2", "value2");

        assertEquals(expected, jsonb.fromJson("{\"map\":{\"key1\":\"value1\",\"key2\":\"value2\"}}", MapModel.class).map);

        assertFalse(MapDeSer.serializerCalled);
        assertTrue(MapDeSer.deserializerCalled);
    }

    public static class MapModel implements Serializable {
        @JsonbTypeSerializer(MapDeSer.class)
        @JsonbTypeDeserializer(MapDeSer.class)
        public Map<String, String> map = new HashMap<>();
    }

    public static class MapDeSer<T> implements JsonbSerializer<T>, JsonbDeserializer<T> {
        private static boolean serializerCalled;
        private static boolean deserializerCalled;

        @Override
        public T deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
            deserializerCalled = true;
            return ctx.deserialize(rtType, parser);
        }

        @Override
        public void serialize(final T obj, final JsonGenerator generator, final SerializationContext ctx) {
            serializerCalled = true;
            ctx.serialize(obj, generator);
        }
    }
}
