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
package org.apache.fleece.core;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class JsonReaderImplTest {
    @Test
    public void simple() {
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/simple.json"));
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(3, object.size());
        assertEquals("b", object.getString("a"));
        assertEquals(4, object.getInt("c"));
        assertThat(object.get("d"), instanceOf(JsonArray.class));
        final JsonArray array = object.getJsonArray("d");
        assertNotNull(array);
        assertEquals(2, array.size());
        assertEquals(1, array.getInt(0));
        assertEquals(2, array.getInt(1));
        reader.close();
    }
}
