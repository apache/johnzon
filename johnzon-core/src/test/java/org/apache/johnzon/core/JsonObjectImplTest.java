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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.Test;

public class JsonObjectImplTest {
    @Test
    public void boolErrors() {
        {
            final JsonObject val = Json.createObjectBuilder().add("a", true).build();
            assertTrue(val.getBoolean("a"));
        }
        {
            final JsonObject val = Json.createObjectBuilder().add("a", "wrong").build();
            try {
                val.getBoolean("a");
                fail();
            } catch (final ClassCastException cce) {
                // ok
            }
        }
    }
    @Test
    public void objectToString() {
        final JsonObjectBuilder ob = Json.createObjectBuilder();
        ob.add("a", new JsonStringImpl("b"));
        assertEquals("{\"a\":\"b\"}", ob.build().toString());
    }


    @Test(expected = NullPointerException.class)
    public void testGetBooleanMissingKeyShouldThrowNullPointerException() {
        getObject().getBoolean("missing");
    }

    @Test
    public void testGetBooleanWithDefaultMissingKeyShouldReturnDefault() {
        assertTrue(getObject().getBoolean("missing", true));
    }


    @Test(expected = NullPointerException.class)
    public void testGetIntMissingKeyShouldThrowNullPointerException() {
        getObject().getInt("missing");
    }

    @Test
    public void testGetIntWithDefaultShouldReturnDefault() {
        assertEquals(42, getObject().getInt("missing", 42));
    }


    @Test
    public void testGetJsonArrayMissingKeyShouldReturnNull() {
        assertNull(getObject().getJsonArray("missing"));
    }


    @Test
    public void testGetJsonNumberMissingKeyShouldReturnNull() {
        assertNull(getObject().getJsonNumber("missing"));
    }


    @Test
    public void testGetJsonObjectMissingKeyShouldReturnNull() {
        assertNull(getObject().getJsonObject("missing"));
    }


    @Test
    public void testGetJsonStringMissingKeyShouldReturnNull() {
        assertNull(getObject().getJsonString("missing"));
    }


    @Test(expected = NullPointerException.class)
    public void testGetStringMissingKeyShouldThrowNullPointerException() {
        getObject().getString("missing");
    }

    @Test
    public void testGetStringWithDefaultShouldReturnDefault() {
        String expected = "default";
        assertEquals(expected, getObject().getString("missing", expected));
    }


    @Test(expected = NullPointerException.class)
    public void testIsNullMissingKeyShouldThrowNullPointerException() {
        getObject().isNull("missing");
    }

    @Test
    public void testIsNullShouldReturnTrue() {
        assertTrue(Json.createObjectBuilder()
                       .add("key", JsonValue.NULL)
                       .build()
                       .isNull("key"));
    }

    @Test
    public void testIsNullShouldReturnFalse() {
        assertFalse(Json.createObjectBuilder()
                        .add("key", "value")
                        .build()
                        .isNull("key"));
    }


    private JsonObject getObject() {
        return Json.createObjectBuilder()
                   .build();
    }


}
