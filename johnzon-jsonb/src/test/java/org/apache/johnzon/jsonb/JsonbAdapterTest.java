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

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

import jakarta.json.bind.adapter.JsonbAdapter;

import static org.junit.Assert.assertEquals;

public class JsonbAdapterTest {

    @Rule
    public JsonbRule jsonbRule = new JsonbRule().withTypeAdapter(new ColorIdAdapter());

    @Test
    public void testInheritedAdapterRecognized() {
        // given
        ColorId colorId = new ColorId("#336699");

        // when
        String json = jsonbRule.toJson(colorId);

        // then
        assertEquals("\"#336699\"", json);
    }

    interface ValueType<T> {
        T value();
    }

    static class ColorId implements ValueType<String> {

        private final String value;

        public ColorId(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return this.value;
        }
    }

    static abstract class AbstractValueTypeAdapter<T extends ValueType<String>> implements JsonbAdapter<T, String> {

        @Override
        public String adaptToJson(T t) throws Exception {
            return t.value();
        }
    }

    static class ColorIdAdapter extends AbstractValueTypeAdapter<ColorId> /*IMPLICIT: implements JsonbAdapter<ValueType<String>, JsonString>*/ {

        @Override
        public ColorId adaptFromJson(String s) throws Exception {
            return new ColorId(s);
        }
    }
}
