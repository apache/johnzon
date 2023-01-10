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

import java.util.stream.IntStream;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import org.junit.Test;

public class JsonObjectImplTest {
    @Test
    public void reuseObjectBuilder() {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        IntStream.range(1, 10).forEach(numer -> {
            jsonObjectBuilder.add("key", String.format("Key %d", numer));
            jsonObjectBuilder.add("value", String.format("Value %d", numer));
            jsonArrayBuilder.add(jsonObjectBuilder);
        });
        final String message = jsonArrayBuilder.build().toString();
        assertEquals("[{\"key\":\"Key 1\",\"value\":\"Value 1\"},{\"key\":\"Key 2\",\"value\":\"Value 2\"}," +
                "{\"key\":\"Key 3\",\"value\":\"Value 3\"},{\"key\":\"Key 4\",\"value\":\"Value 4\"},{\"key\":\"Key " +
                "5\",\"value\":\"Value 5\"},{\"key\":\"Key 6\",\"value\":\"Value 6\"},{\"key\":\"Key 7\"," +
                "\"value\":\"Value 7\"},{\"key\":\"Key 8\",\"value\":\"Value 8\"},{\"key\":\"Key 9\"," +
                "\"value\":\"Value 9\"}]", message);
    }

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

    @Test
    public void testToStringShouldReturnEscapedKey() {
        final JsonObjectBuilder ob = Json.createObjectBuilder();
        ob.add("foo\"bar", new JsonLongImpl(42));
        assertEquals("{\"foo\\\"bar\":42}", ob.build().toString());
    }

    @Test
    public void testToStringShouldReturnEscapedValue() {
        final JsonObjectBuilder ob = Json.createObjectBuilder();
        ob.add("a", new JsonStringImpl("foo\"bar"));
        assertEquals("{\"a\":\"foo\\\"bar\"}", ob.build().toString());
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
