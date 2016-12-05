/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.jsonb;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.json.bind.Jsonb;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbVisibility;
import javax.json.bind.config.PropertyVisibilityStrategy;
import javax.json.bind.spi.JsonbProvider;

import org.junit.Assert;
import org.junit.Test;

public class JsonbVisitilityTest {

    @Test
    public void testJsonVisibilityAllFields() {
        MyDataVisibility data = new MyDataVisibility();
        data.put("x", "a");
        data.put("y", "b");

        Jsonb jsonb = JsonbProvider.provider().create().build();
        String json = jsonb.toJson(data);
        Assert.assertEquals("{\"attribs\":{\"x\":\"a\",\"y\":\"b\"}}", json);

        MyDataVisibility dataBack = jsonb.fromJson(json, MyDataVisibility.class);
        Assert.assertEquals("a", dataBack.get("x"));
        Assert.assertEquals("b", dataBack.get("y"));
    }

    @Test
    public void testJsonPropertyInternalField() {
        MyDataJsonField data = new MyDataJsonField();
        data.put("x", "a");
        data.put("y", "b");

        Jsonb jsonb = JsonbProvider.provider().create().build();
        String json = jsonb.toJson(data);
        Assert.assertEquals("{\"attribs\":{\"x\":\"a\",\"y\":\"b\"}}", json);

        MyDataJsonField dataBack = jsonb.fromJson(json, MyDataJsonField.class);
        Assert.assertEquals("a", dataBack.get("x"));
        Assert.assertEquals("b", dataBack.get("y"));
    }


    @JsonbVisibility(VisibleAllFields.class)
    public static class MyDataVisibility {
        private Map<String, String> attribs = new HashMap<>();

        public void put(String key, String value) {
            attribs.put(key, value);
        }

        public String get(String key) {
            return attribs.get(key);
        }
    }

    public static class MyDataJsonField {
        @JsonbProperty
        private Map<String, String> attribs = new HashMap<>();

        public void put(String key, String value) {
            attribs.put(key, value);
        }

        public String get(String key) {
            return attribs.get(key);
        }
    }

    /**
     * All fields are visible. Even private, which by default won't get jsonified.
     */
    public static class VisibleAllFields implements PropertyVisibilityStrategy {
        @Override
        public boolean isVisible(Field field) {
            return true;
        }

        @Override
        public boolean isVisible(Method method) {
            return false;
        }
    }
}
