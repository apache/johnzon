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
package org.apache.johnzon.mapper.jsonp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

import org.junit.Test;

public class RewindableJsonParserTest {
    private RewindableJsonParser parser(final String json) {
        return new RewindableJsonParser(Json.createParser(new StringReader(json)));
    }

    @Test
    public void currentEventBeforeNextIsReplayedByNext() {
        final RewindableJsonParser parser = parser("{\"a\":1}");
        assertNull(parser.getLast());
        assertEquals(Event.START_OBJECT, parser.currentEvent());
        // JSON-P 2.1 JsonParser#currentEvent: "current parsing state".
        assertEquals(Event.START_OBJECT, parser.currentEvent());
        assertTrue(parser.hasNext());
        assertEquals(Event.START_OBJECT, parser.next());
        assertEquals(Event.START_OBJECT, parser.currentEvent());
        assertEquals(Event.KEY_NAME, parser.next());
        assertEquals("a", parser.getString());
        assertEquals(Event.KEY_NAME, parser.currentEvent());
        assertEquals(Event.VALUE_NUMBER, parser.next());
        assertEquals(1, parser.getInt());
        assertEquals(Event.END_OBJECT, parser.next());
        assertFalse(parser.hasNext());
    }

    @Test
    public void nextFirstKeepsWorking() {
        final RewindableJsonParser parser = parser("[true]");
        assertEquals(Event.START_ARRAY, parser.next());
        assertEquals(Event.START_ARRAY, parser.currentEvent());
        assertEquals(Event.VALUE_TRUE, parser.next());
        assertEquals(Event.END_ARRAY, parser.next());
    }

    @Test
    public void getObjectAfterPeek() {
        final RewindableJsonParser parser = parser("{\"a\":\"b\"}");
        assertEquals(Event.START_OBJECT, parser.currentEvent());
        assertEquals("b", parser.getObject().getString("a"));
        // JSON-P 2.1 JsonParser#getObject: advance to the "corresponding END_OBJECT".
        assertEquals(Event.END_OBJECT, parser.currentEvent());
        assertFalse(parser.hasNext());
    }

    @Test
    public void getValueAfterPeek() {
        final RewindableJsonParser parser = parser("[1,2]");
        assertEquals(Event.START_ARRAY, parser.currentEvent());
        assertEquals(2, parser.getValue().asJsonArray().size());
        // JSON-P 2.1 JsonParser#getValue: "behavior is the same as getArray()".
        assertEquals(Event.END_ARRAY, parser.currentEvent());
        assertFalse(parser.hasNext());
    }

    @Test
    public void getArrayAfterPeek() {
        final RewindableJsonParser parser = parser("[1,2]");
        assertEquals(Event.START_ARRAY, parser.currentEvent());
        assertEquals(2, parser.getArray().size());
        // JSON-P 2.1 JsonParser#getArray: advance to the "corresponding END_ARRAY".
        assertEquals(Event.END_ARRAY, parser.currentEvent());
        assertFalse(parser.hasNext());
    }

    @Test
    public void getArrayStreamAfterPeek() {
        final RewindableJsonParser parser = parser("[1,2]");
        assertEquals(Event.START_ARRAY, parser.currentEvent());
        assertEquals(2, parser.getArrayStream().count());
        // JSON-P 2.1 JsonParser#getArrayStream: elements are "read lazily"; full consumption reaches END_ARRAY.
        assertEquals(Event.END_ARRAY, parser.currentEvent());
        assertFalse(parser.hasNext());
    }

    @Test
    public void getObjectStreamAfterPeek() {
        final RewindableJsonParser parser = parser("{\"a\":1,\"b\":2}");
        assertEquals(Event.START_OBJECT, parser.currentEvent());
        assertEquals(2, parser.getObjectStream().count());
        // JSON-P 2.1 JsonParser#getObjectStream: pairs are "read lazily"; full consumption reaches END_OBJECT.
        assertEquals(Event.END_OBJECT, parser.currentEvent());
        assertFalse(parser.hasNext());
    }

    @Test
    public void getValueStreamTracksCurrentEvent() {
        final RewindableJsonParser parser = parser("[1,2]");
        assertNull(parser.getLast());
        assertEquals(1, parser.getValueStream().count());
        // JSON-P 2.1 JsonParser#getValueStream: values are "read lazily"; track the final state.
        assertEquals(Event.END_ARRAY, parser.getLast());
        assertEquals(Event.END_ARRAY, parser.currentEvent());
        assertFalse(parser.hasNext());
    }

    @Test
    public void skipAfterPeek() {
        final JsonParser parser = parser("{\"a\":{\"b\":1},\"c\":2}");
        assertEquals(Event.START_OBJECT, parser.currentEvent());
        assertEquals(Event.START_OBJECT, parser.next());
        assertEquals(Event.KEY_NAME, parser.next());
        assertEquals(Event.START_OBJECT, parser.next());
        parser.skipObject();
        // JSON-P 2.1 JsonParser#skipObject: advance the parser to "END_OBJECT".
        assertEquals(Event.END_OBJECT, parser.currentEvent());
        assertEquals(Event.KEY_NAME, parser.next());
        assertEquals("c", parser.getString());
    }

    @Test
    public void skipArrayAfterPeek() {
        final RewindableJsonParser parser = parser("[1,2]");
        assertEquals(Event.START_ARRAY, parser.currentEvent());
        parser.skipArray();
        // JSON-P 2.1 JsonParser#skipArray: advance the parser to "END_ARRAY".
        assertEquals(Event.END_ARRAY, parser.currentEvent());
        assertFalse(parser.hasNext());
    }
}
