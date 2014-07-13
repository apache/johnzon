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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;

import org.junit.Test;

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
            assertEquals(-2, parser.getInt());
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
        array.addInternal(new JsonNumberImpl(new BigDecimal(-2)));
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
    public void numbers() {
        final JsonParser parser = Json.createParser(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/numbers.json"));
        assertNotNull(parser);
        parser.next();
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(0, parser.getInt());
            assertEquals(0, parser.getLong());
            assertEquals(new BigDecimal(0), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(0, parser.getInt());
            assertEquals(0, parser.getLong());
            assertEquals(new BigDecimal(0), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(1, parser.getInt());
            assertEquals(1, parser.getLong());
            assertEquals(new BigDecimal(1), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(-1, parser.getInt());
            assertEquals(-1L, parser.getLong());
            assertEquals(new BigDecimal(-1), parser.getBigDecimal());
        }

        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(9, parser.getInt());
            assertEquals(9L, parser.getLong());
            assertEquals(new BigDecimal(9), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(-9, parser.getInt());
            assertEquals(-9, parser.getLong());
            assertEquals(new BigDecimal(-9), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(10, parser.getInt());
            assertEquals(10, parser.getLong());
            assertEquals(new BigDecimal(10), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(-10, parser.getInt());
            assertEquals(-10, parser.getLong());
            assertEquals(new BigDecimal(-10), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(100, parser.getInt());
            assertEquals(100, parser.getLong());
            assertEquals(new BigDecimal(100), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(-100, parser.getInt());
            assertEquals(-100, parser.getLong());
            assertEquals(new BigDecimal(-100), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(456, parser.getInt());
            assertEquals(456, parser.getLong());
            assertEquals(new BigDecimal(456), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            assertEquals(-456, parser.getInt());
            assertEquals(-456, parser.getLong());
            assertEquals(new BigDecimal(-456), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(!parser.isIntegralNumber());
            assertEquals(123, parser.getInt());
            assertEquals(123, parser.getLong());
            assertEquals(new BigDecimal("123.12345"), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(!parser.isIntegralNumber());
            assertEquals(-123, parser.getInt());
            assertEquals(-123, parser.getLong());
            assertEquals(new BigDecimal("-123.12345"), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            //assertEquals(Integer.MAX_VALUE, parser.getInt());
            //assertEquals(Long.MAX_VALUE, parser.getLong());
            assertEquals(new BigDecimal("999999999999999999999999999999"), parser.getBigDecimal());
        }
        parser.next();
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertTrue(parser.isIntegralNumber());
            //assertEquals(Integer.MIN_VALUE, parser.getInt());
            //assertEquals(Long.MIN_VALUE, parser.getLong());
            assertEquals(new BigDecimal("-999999999999999999999999999999"), parser.getBigDecimal());
        }
        parser.next();
        
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
            assertEquals(JsonParser.Event.KEY_NAME, event);
            assertEquals("b", parser.getString());
        }
        {
            assertTrue(parser.hasNext());
            final JsonParser.Event event = parser.next();
            assertNotNull(event);
            assertEquals(JsonParser.Event.VALUE_NUMBER, event);
            assertFalse(parser.isIntegralNumber());
            assertEquals(new BigDecimal("1.23E-3"), parser.getBigDecimal());
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
            assertFalse(parser.isIntegralNumber());
            assertEquals(new BigDecimal("1.23E+3"), parser.getBigDecimal());
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
        assertEquals("\\", parser.getString());
        parser.next();
        assertEquals("/", parser.getString());
        parser.next();
        assertEquals("\b", parser.getString());
        parser.next();
        assertEquals("\f", parser.getString());
        parser.next();
        assertEquals("\n", parser.getString());
        parser.next();
        assertEquals("\r", parser.getString());
        parser.next();
        assertEquals("\t", parser.getString());
        parser.next();
        assertEquals("D", parser.getString());
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
                        default: break;
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
                        default: break;
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
        final JsonParser parser = Json.createParserFactory(new HashMap<String, Object>() {{
            put(JsonParserFactoryImpl.MAX_STRING_LENGTH, 10);
        }}).createParser(new ByteArrayInputStream("{}".getBytes()));
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
  
        Json.createReader(new ByteArrayInputStream("{\"z\":\"b\"\"j\":\"d\"}".getBytes())).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void literalFailChecksTrue() {
        // using a reader as wrapper of parser
  
        Json.createReader(new ByteArrayInputStream("{\"z\":truet}".getBytes())).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void literalFailChecksNull() {
        // using a reader as wrapper of parser
  
        Json.createReader(new ByteArrayInputStream("{\"z\":nulll}".getBytes())).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void zeroByteInput() {
        // using a reader as wrapper of parser
  
        Json.createReader(new ByteArrayInputStream(new byte[]{})).read();
    }
    
    @Test
    public void shortestNonEmtyJsonFile() {
        // using a reader as wrapper of parser
  
        assertEquals(0L, Json.createReader(new ByteArrayInputStream("[0]".getBytes())).readArray().getJsonNumber(0).longValue());
    }
    
    @Test
    public void shortestNonEmtyJsonFileWithWhiteSpaceChars() {
        // using a reader as wrapper of parser
  
        assertEquals(0L, Json.createReader(new ByteArrayInputStream("  \n\n   [   0  ]  \n\n".getBytes())).readArray().getJsonNumber(0).longValue());
    }
    
    
    @Test(expected = JsonParsingException.class)
    public void fail1() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail1.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail2() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail2.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail3() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail3.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail4() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail4.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail5() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail5.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail6() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail6.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail7() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail7.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail8() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail8.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail9() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail9.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail10() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail10.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail11() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail11.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail12() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail12.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail13() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail13.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail14() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail14.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail15() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail15.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail16() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail16.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail17() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail17.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail18() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail18.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail19() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail19.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail20() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail20.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail21() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail21.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail22() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail22.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail23() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail23.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail24() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail24.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail25() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail25.json")).read();
    }
    
}
