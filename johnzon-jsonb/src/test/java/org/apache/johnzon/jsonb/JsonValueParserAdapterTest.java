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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

import org.junit.Test;

public class JsonValueParserAdapterTest {
    @Test
    public void currentEventForString() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(Json.createValue("test"), null)) {
            assertEquals(Event.VALUE_STRING, parser.currentEvent());
        }
    }

    @Test
    public void currentEventForNumber() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(Json.createValue(42), null)) {
            assertEquals(Event.VALUE_NUMBER, parser.currentEvent());
        }
    }

    @Test
    public void currentEventForTrue() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(JsonValue.TRUE, null)) {
            assertEquals(Event.VALUE_TRUE, parser.currentEvent());
        }
    }

    @Test
    public void currentEventForFalse() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(JsonValue.FALSE, null)) {
            assertEquals(Event.VALUE_FALSE, parser.currentEvent());
        }
    }

    @Test
    public void currentEventForNull() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(JsonValue.NULL, null)) {
            assertEquals(Event.VALUE_NULL, parser.currentEvent());
        }
    }

    @Test
    public void currentEventForObject() {
        try (final JsonParser parser = new JsonValueParserAdapter<>(JsonValue.EMPTY_JSON_OBJECT)) {
            assertEquals(Event.START_OBJECT, parser.currentEvent());
        }
    }

    @Test
    public void currentEventForArray() {
        try (final JsonParser parser = new JsonValueParserAdapter<>(JsonValue.EMPTY_JSON_ARRAY)) {
            assertEquals(Event.START_ARRAY, parser.currentEvent());
        }
    }

    @Test
    public void createForObjectDelegatesToParserFactory() {
        final JsonObject object = Json.createObjectBuilder().add("value", "simple").build();
        try (final JsonParser parser = JsonValueParserAdapter.createFor(
                object, () -> Json.createParserFactory(Collections.emptyMap()))) {
            assertTrue(parser.hasNext());
            assertEquals(Event.START_OBJECT, parser.next());
            assertEquals(Event.KEY_NAME, parser.next());
            assertEquals("value", parser.getString());
        }
    }

    @Test
    public void createForArrayDelegatesToParserFactory() {
        final JsonArray array = Json.createArrayBuilder().add(1).build();
        try (final JsonParser parser = JsonValueParserAdapter.createFor(
                array, () -> Json.createParserFactory(Collections.emptyMap()))) {
            assertTrue(parser.hasNext());
            assertEquals(Event.START_ARRAY, parser.next());
            assertEquals(Event.VALUE_NUMBER, parser.next());
            assertEquals(1, parser.getInt());
        }
    }

    @Test
    public void hasNextIsFalse() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(Json.createValue("test"), null)) {
            assertFalse(parser.hasNext());
        }
    }

    @Test
    public void nextIsUnsupported() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(Json.createValue("test"), null)) {
            assertThrows(UnsupportedOperationException.class, parser::next);
        }
    }

    @Test
    public void getValueReturnsWrappedValue() {
        final JsonValue value = Json.createValue("test");
        try (final JsonParser parser = JsonValueParserAdapter.createFor(value, null)) {
            assertSame(value, parser.getValue());
        }
    }

    @Test
    public void getStringForString() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(Json.createValue("test"), null)) {
            assertEquals("test", parser.getString());
        }
    }

    @Test
    public void numberAccessorsForIntegralNumber() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(Json.createValue(42), null)) {
            assertTrue(parser.isIntegralNumber());
            assertEquals(42, parser.getInt());
            assertEquals(42L, parser.getLong());
            assertEquals(new BigDecimal(42), parser.getBigDecimal());
        }
    }

    @Test
    public void numberAccessorsForDecimalNumber() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(Json.createValue(new BigDecimal("1.5")), null)) {
            assertFalse(parser.isIntegralNumber());
            assertEquals(new BigDecimal("1.5"), parser.getBigDecimal());
        }
    }

    @Test
    public void unsupportedOperationsForNonNumberValue() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(JsonValue.TRUE, null)) {
            assertThrows(UnsupportedOperationException.class, parser::getString);
            assertThrows(UnsupportedOperationException.class, parser::isIntegralNumber);
            assertThrows(UnsupportedOperationException.class, parser::getInt);
            assertThrows(UnsupportedOperationException.class, parser::getLong);
            assertThrows(UnsupportedOperationException.class, parser::getBigDecimal);
            assertThrows(UnsupportedOperationException.class, parser::getLocation);
        }
    }

    @Test
    public void getStringForNumberIsUnsupported() {
        try (final JsonParser parser = JsonValueParserAdapter.createFor(Json.createValue(42), null)) {
            assertThrows(UnsupportedOperationException.class, parser::getString);
        }
    }
}
