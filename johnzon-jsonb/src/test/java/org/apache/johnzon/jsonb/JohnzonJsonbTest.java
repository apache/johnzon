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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

public class JohnzonJsonbTest {
    @Rule
    public final JsonbRule rule = new JsonbRule();

    @Test
    public void listJsonValue() {
        assertEquals(Json.createValue(1.1),
                rule.fromJson("{\"value\":[1.1]}", ArrayJsonValueWrapper.class).value.get(0));
    }

    @Test
    public void listObject() {
        assertEquals(1.1, Number.class.cast(
                rule.fromJson("{\"value\":[1.1]}", ArrayObjectWrapper.class).value.get(0)).doubleValue(), 0);
    }

    @Test
    public void jsonArray() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final String json = "[{\"foo\":\"bar\"}]";
            final JsonArray array = jsonb.fromJson(json, JsonArray.class);
            assertEquals(json, array.toString());
        }
    }

    @Test
    public void longBounds() {
        final String max = rule.toJson(new LongWrapper(Long.MAX_VALUE));
        assertEquals("{\"value\":9223372036854775807}", max);
        assertEquals(Long.MAX_VALUE, rule.fromJson(max, LongWrapper.class).value, 0);

        final String min = rule.toJson(new LongWrapper(Long.MIN_VALUE));
        assertEquals("{\"value\":-9223372036854775808}", min);
        assertEquals(Long.MIN_VALUE, rule.fromJson(min, LongWrapper.class).value, 0);
    }

    public static class LongWrapper {
        public Long value;

        public LongWrapper() {
            // no-op
        }

        public LongWrapper(final Long value) {
            this.value = value;
        }
    }

    public static class ArrayObjectWrapper {
        public List<Object> value;
    }

    public static class ArrayJsonValueWrapper {
        public List<JsonValue> value;
    }
}
