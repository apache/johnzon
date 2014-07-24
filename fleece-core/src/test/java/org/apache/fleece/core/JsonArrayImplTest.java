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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.json.JsonArray;

import org.junit.Test;

public class JsonArrayImplTest {
    @Test
    public void arrayToString() {
        final JsonArrayImpl object = new JsonArrayImpl();
        object.addInternal(new JsonStringImpl("a"));
        object.addInternal(new JsonStringImpl("b"));
        assertEquals("[\"a\",\"b\"]", object.toString());
    }
    
    @Test
    public void arrayIndex() {
        final JsonArrayImpl object = new JsonArrayImpl();
        object.addInternal(new JsonStringImpl("a"));
        object.addInternal(new JsonStringImpl("b"));
        object.addInternal(new JsonLongImpl(5));
        final JsonArray array = (JsonArray) object;
        assertFalse(array.isEmpty());
        assertEquals("a", object.getJsonString(0).getString());
        assertEquals("b", object.getJsonString(1).getString());
        assertEquals(5, object.getJsonNumber(2).longValue());
        assertEquals("[\"a\",\"b\",5]", object.toString());
    }
    
    @Test
    public void emptyArray() {
        final JsonArray array = new JsonArrayImpl();
        assertTrue(array.isEmpty());
        assertEquals("[]", array.toString());
    }
}
