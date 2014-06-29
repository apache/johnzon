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

import org.apache.fleece.core.JsonArrayImpl;
import org.apache.fleece.core.JsonNumberImpl;
import org.apache.fleece.core.JsonObjectImpl;
import org.apache.fleece.core.JsonParserFactoryImpl;
import org.apache.fleece.core.JsonReaderImpl;
import org.apache.fleece.core.JsonStreamParser;
import org.apache.fleece.core.JsonStringImpl;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JsonParserTest {
    private void assertSimple(final JsonParser parser) {
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.START_OBJECT, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.KEY_NAME, event);
            assertEquals("a", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_STRING, event);
            assertEquals("b", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.KEY_NAME, event);
            assertEquals("c", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(4, parser.getInt());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.KEY_NAME, event);
            assertEquals("d", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.START_ARRAY, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertEquals(1, parser.getInt());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertEquals(2, parser.getInt());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.END_ARRAY, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.END_OBJECT, event);
        }
        {
            assertFalse(parser.hasNext());
        }
        parser.close();
    }

    @Test
    public void array() {
        final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/array.json"));
        assertNotNull(loadInMemReader);
        final JsonArray array = loadInMemReader.readArray();
        assertNotNull(array);

        final JsonParser parser = Json.createParserFactory(Collections.<String, Object>emptyMap()).createParser(array);
        assertNotNull(parser);

        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.START_ARRAY, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_STRING, event);
            assertEquals("a", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertEquals(1, parser.getInt());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.START_OBJECT, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.KEY_NAME, event);
            assertEquals("b", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_STRING, event);
            assertEquals("c", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.END_OBJECT, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertEquals(5, parser.getInt());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.END_ARRAY, event);
        }
        {
            assertFalse(parser.hasNext());
        }
    }

    @Test
    public void simpleInMemory() {
        final JsonObjectImpl simple = new JsonObjectImpl();
        simple.putInternal("a", new JsonStringImpl("b"));
        simple.putInternal("c", new JsonNumberImpl(new BigDecimal(4)));
        final JsonArrayImpl array = new JsonArrayImpl();
        array.addInternal(new JsonNumberImpl(new BigDecimal(1)));
        array.addInternal(new JsonNumberImpl(new BigDecimal(2)));
        simple.putInternal("d", array);

        final JsonParser parser = Json.createParserFactory(Collections.<String, Object>emptyMap()).createParser(simple);
        assertNotNull(parser);
        assertSimple(parser);
    }

    @Test
    public void simple() {
        final JsonParser parser = Json.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/simple.json"));
        assertNotNull(parser);
        assertSimple(parser);
    }

    @Test
    public void nested() {
        final JsonParser parser = Json.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/nested.json"));
        assertNotNull(parser);
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.START_OBJECT, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.KEY_NAME, event);
            assertEquals("a", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_STRING, event);
            assertEquals("b", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.KEY_NAME, event);
            assertEquals("c", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.START_OBJECT, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.KEY_NAME, event);
            assertEquals("d", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.START_ARRAY, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertEquals(1, parser.getInt());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertEquals(2, parser.getInt());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.END_ARRAY, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.END_OBJECT, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.END_OBJECT, event);
        }
        {
            assertFalse(parser.hasNext());
        }
        parser.close();
    }

    @Test
    public void bigdecimal() {
        final JsonParser parser = Json.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/bigdecimal.json"));
        assertNotNull(parser);
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.START_OBJECT, event);
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.KEY_NAME, event);
            assertEquals("a", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertFalse(parser.isIntegralNumber());
            assertEquals(new BigDecimal("1.23E3"), parser.getBigDecimal());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.END_OBJECT, event);
        }
        {
            assertFalse(parser.hasNext());
        }
        parser.close();
    }
    
    @Test(expected=IllegalStateException.class)
    public void isIntegralThrowsISE() {
        final JsonParser parser = Json.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/bigdecimal.json"));
        assertNotNull(parser);
        assertTrue(parser.hasNext());
        final JsonParser.Event event = parser.next();
        assertNotNull(event);
        assertEquals(JsonParser.Event.START_OBJECT, event);
        assertFalse(parser.isIntegralNumber());
            
    }

    @Test
    public void escaping() {
        final JsonParser parser = Json.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/escaping.json"));
        parser.next();
        parser.next();
        assertEquals("\"", parser.getString());
        parser.next();
        assertEquals("\t", parser.getString());
        parser.next();
        assertFalse(parser.hasNext());
        parser.close();
    }

    @Test
    public void dosProtected() {
        // strings
        {
            final JsonParser parser = Json.createParserFactory(new HashMap<String, Object>() {{
                        put(JsonParserFactoryImpl.MAX_STRING_LENGTH, 10);
                    }}).createParser(new InputStream() {
                private int index = 0;

                @Override
                public int read() throws IOException {
                    switch (index) {
                        case 0:
                            index++;
                            return '{';
                        case 1:
                            index++;
                            return '"';
                    }
                    return 'a'; // infinite key
                }
            });
            assertEquals(JsonParser.Event.START_OBJECT, parser.next());
            try {
                parser.next(); // should fail cause we try to make a OOME
                fail();
            } catch (final JsonParsingException expected) {
                // no-op
            }
            parser.close();
        }

        // spaces
        {
            final JsonParser parser = Json.createParserFactory(new HashMap<String, Object>() {{
                        put(JsonParserFactoryImpl.MAX_STRING_LENGTH, 10);
                    }}).createParser(new InputStream() {
                private int index = 0;

                @Override
                public int read() throws IOException {
                    switch (index) {
                        case 0:
                            index++;
                            return '{';
                    }
                    return ' '; // infinite spaces
                }
            });
            assertEquals(JsonParser.Event.START_OBJECT, parser.next());
            try { // should fail cause we try to make a OOME
                while (parser.hasNext()) {
                    parser.next();
                }
                fail();
            } catch (final JsonParsingException expected) {
                // no-op
            }
            parser.close();
        }
    }

    @Test
    public void hasNext() {
        final JsonParser parser = new JsonStreamParser(new ByteArrayInputStream("{}".getBytes()), 1000);
        assertTrue(parser.hasNext());
        assertTrue(parser.hasNext());
        assertTrue(parser.hasNext());
        assertTrue(parser.hasNext());
        assertEquals(JsonParser.Event.START_OBJECT, parser.next());
        parser.close();
    }

    @Test(expected = JsonParsingException.class)
    public void commaChecks() {
        // using a reader as wrapper of parser
        new JsonReaderImpl(new ByteArrayInputStream("{\"z\":\"b\"\"j\":\"d\"}".getBytes())).read();
    }
}
