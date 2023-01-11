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
package org.apache.johnzon.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;

import org.junit.Test;

public class JsonArrayImplTest {
    @Test
    public void arrayToString() {       
        JsonArrayBuilder ab = Json.createArrayBuilder();
        
        ab.add(new JsonStringImpl("a"));
        ab.add(new JsonStringImpl("b"));
        assertEquals("[\"a\",\"b\"]", ab.build().toString());
    }
    
    @Test
    public void arrayIndex() {
        JsonArrayBuilder ab = Json.createArrayBuilder();
        ab.add(new JsonStringImpl("a"));
        ab.add(new JsonStringImpl("b"));
        ab.add(new JsonLongImpl(5));
        final JsonArray array = (JsonArray) ab.build();
        assertFalse(array.isEmpty());
        assertEquals("a", array.getJsonString(0).getString());
        assertEquals("b", array.getJsonString(1).getString());
        assertEquals(5, array.getJsonNumber(2).longValue());
        assertEquals("[\"a\",\"b\",5]", array.toString());
    }
    
    @Test
    public void emptyArray() {
        final JsonArray array = Json.createArrayBuilder().build();
        assertTrue(array.isEmpty());
        assertEquals("[]", array.toString());
    }

    @Test
    public void equals() {
        assertTrue(Json.createArrayBuilder().build().equals(Json.createArrayBuilder().build()));
        assertTrue(Json.createArrayBuilder().add(1).build().equals(Json.createArrayBuilder().add(1).build()));
        assertFalse(Json.createArrayBuilder().add(1).build().equals(Json.createArrayBuilder().add(2).build()));
        assertFalse(Json.createArrayBuilder().add(1).build().equals(Json.createArrayBuilder().build()));
    }
}
