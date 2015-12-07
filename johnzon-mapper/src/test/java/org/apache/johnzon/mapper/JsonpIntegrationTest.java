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
package org.apache.johnzon.mapper;

import org.junit.Test;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import java.io.StringReader;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JsonpIntegrationTest {
    @Test
    public void readAndWrite() {
        final Mapper mapper = new MapperBuilder().setAccessModeName("field").setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        }).build();

        final String json =
            "{" +
            "\"array\":[\"a\",\"b\"]," +
            "\"number\":2," +
            "\"object\":{\"o\":\"p\"}," +
            "\"string\":\"val\"," +
            "\"structure\":{\"q\":\"r\"}," +
            "\"value\":\"v\"" +
            "}";
        final JsonpIntegModel model = mapper.readObject(new StringReader(json), JsonpIntegModel.class);
        assertNotNull(model.object);
        assertEquals(1, model.object.size());
        assertEquals("p", model.object.getString("o"));
        assertNotNull(model.structure);
        assertTrue(JsonObject.class.isInstance(model.structure));
        assertEquals(1, JsonObject.class.cast(model.structure).size());
        assertEquals("r", JsonObject.class.cast(model.structure).getString("q"));
        assertNotNull(model.array);
        assertEquals(2, model.array.size());
        assertEquals("a", model.array.getString(0));
        assertEquals("b", model.array.getString(1));
        assertNotNull(model.string);
        assertEquals("val", model.string.getString());
        assertNotNull(model.value);
        assertEquals(JsonValue.ValueType.STRING, model.value.getValueType());
        assertEquals("v", JsonString.class.cast(model.value).getString());
        assertNotNull(model.number);
        assertEquals(2, model.number.intValue());
        assertEquals(json, mapper.writeObjectAsString(model));
    }

    public static class JsonpIntegModel {
        private JsonObject object;
        private JsonArray array;
        private JsonStructure structure;
        private JsonValue value;
        private JsonString string;
        private JsonNumber number;
        // TODO: add JsonPointer when we'll support it
    }
}
