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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.junit.Test;

public class ObjectSerializationTest {
    private final Jsonb mapper = JsonbBuilder.create();

    @Test
    public void primitiveBoolean() {
        assertEquals("{\"data\":true}", toJson(new Wrapper(true)));
    }

    @Test
    public void numberBoolean() {
        assertEquals("{\"data\":1}", toJson(new Wrapper(1)));
    }

    @Test
    public void stringBoolean() {
        assertEquals("{\"data\":\"ok\"}", toJson(new Wrapper("ok")));
    }

    @Test
    public void objectBoolean() {
        assertEquals("{\"data\":{\"data\":\"ok\"}}", toJson(new Wrapper(new Wrapper("ok"))));
    }

    @Test
    public void arrayString() {
        assertEquals("{\"data\":[\"10\",\"2\"]}", toJson(new Wrapper(asList("10", "2"))));
    }

    @Test
    public void nestedArrayString() {
        assertEquals("{\"data\":{\"data\":[\"10\",\"2\"]}}", toJson(new Wrapper(new Wrapper(asList("10", "2")))));
    }

    private String toJson(final Wrapper wrapper) {
        return mapper.toJson(wrapper);
    }

    public static class Wrapper {
        private final Object data;

        private Wrapper(final Object data) {
            this.data = data;
        }

        public Object getData() {
            return data;
        }
    }
}
