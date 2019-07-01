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
package org.apache.johnzon.jsonb.api.experimental;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;
import org.junit.Rule;
import org.junit.Test;

public class JsonbExtensionTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    private final JsonObject defaultValue = Json.createObjectBuilder()
            .add("foo", "ok")
            .add("bar", 1)
            .build();

    @Test
    public void fromJsonValue() {
        final Value value = jsonb.fromJsonValue(defaultValue, Value.class);
        assertEquals("ok", value.foo);
        assertEquals(1, value.bar);
    }

    @Test
    public void fromJsonValue2() {
        final JsonValue json = Json.createArrayBuilder()
                .add(defaultValue)
                .add(Json.createObjectBuilder()
                        .add("foo", "still ok")
                        .add("bar", 2)
                        .build())
                .build();
        final List<Value> values = jsonb.fromJsonValue(json, new JohnzonParameterizedType(Collection.class, Value.class));
        assertEquals(2, values.size());
        {
            final Value value = values.get(0);
            assertEquals("ok", value.foo);
            assertEquals(1, value.bar);
        }
        {
            final Value value = values.get(1);
            assertEquals("still ok", value.foo);
            assertEquals(2, value.bar);
        }
    }

    @Test
    public void toJsonValue() {
        assertEquals(defaultValue, jsonb.toJsonValue(new Value("ok", 1)));
    }

    @Test
    public void toJsonValue2() {
        assertEquals(defaultValue, jsonb.toJsonValue(new Value("ok", 1), Value.class));
    }

    public static class Value {
        public String foo;
        public int bar;

        public Value() {
            // no-op
        }

        public Value(final String foo, final int bar) {
            this.foo = foo;
            this.bar = bar;
        }
    }
}
