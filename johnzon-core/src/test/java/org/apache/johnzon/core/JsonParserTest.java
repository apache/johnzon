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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.NoSuchElementException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParsingException;

import org.junit.Test;

public class JsonParserTest {
    
    
    static final Charset UTF_8 = Charset.forName("UTF-8");
    static final Charset UTF_16BE = Charset.forName("UTF-16BE");
    static final Charset UTF_16LE = Charset.forName("UTF-16LE");
    static final Charset UTF_16 = Charset.forName("UTF-16");
    static final Charset UTF_32LE = Charset.forName("UTF-32LE");
    static final Charset UTF_32BE = Charset.forName("UTF-32BE");
    
    public JsonParserTest() {
        if (!Charset.defaultCharset().equals(Charset.forName("UTF-8"))) {
            System.err.println("Default charset is " + Charset.defaultCharset() + ", must must be UTF-8");
        }
    }

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
        final JsonObjectBuilder ob = Json.createObjectBuilder();
        ob.add("a", new JsonStringImpl("b"));
        ob.add("c", new JsonNumberImpl(new BigDecimal(4)));
        JsonArrayBuilder ab = Json.createArrayBuilder();
        ab.add(new JsonNumberImpl(new BigDecimal(1)));
        ab.add(new JsonNumberImpl(new BigDecimal(-2)));
        
        ob.add("d", ab);

        final JsonParser parser = Json.createParserFactory(Collections.EMPTY_MAP).createParser(ob.build());
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
    public void simpleAttempting() {
        final JsonParser parser = Json.createParser(new AttemptingInputStream("{\"a\":      \"b\",\"c\": 4,\"d\": [1,-2]}".getBytes(UTF_8)));
        assertNotNull(parser);
        assertSimple(parser);
    }

    @Test
    public void simpleUTF16LE() {
        final JsonParser parser = Json.createParserFactory(null).createParser(Thread.currentThread()
                .getContextClassLoader().getResourceAsStream("json/simple_utf16le.json"), UTF_16LE);
        assertNotNull(parser);
        assertSimple(parser);
    }
    
    @Test
    public void simpleUTF16LEAutoDetect() {
        final JsonParser parser = Json.createParserFactory(null).createParser(Thread.currentThread().
                getContextClassLoader().getResourceAsStream("json/simple_utf16le.json"));
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
    public void escapedStringAwareParser() {
        final JsonParser parser = Json.createParser(Thread.currentThread()
                .getContextClassLoader().getResourceAsStream("json/stringescape.json"));
        parser.next();
        parser.next();
        parser.next();
        assertEquals("s\"mit\"", parser.getString());
        assertEquals("\"s\\\"mit\\\"\"", new JsonStringImpl(parser.getString()).toString());
        parser.close();
    }
    
    @Test
    public void bufferOverFlow() {
        JsonParser parser1 = Json.createParser(new StringReader("{}{}"));
        
        try {
            while(parser1.hasNext()) {
                parser1.next();
            }
            fail();
        } catch (Exception e1) {
            //expected
        } finally {
            parser1.close();
        }
        
        
        
        JsonParser parser2 = Json.createParser(new StringReader("{"));
        try {
            while(parser2.hasNext()) {
                    parser2.next();                                
            }
            fail();
        } catch (JsonParsingException e) {
           //expected
        } finally {
            parser2.close();
        }
        
        
    }
    
    @Test
    public void bufferOverFlow2() {
        JsonParser parser1 = Json.createParser(new StringReader("{  }{}"));
        
        try {
            while(parser1.hasNext()) {
                parser1.next();
            }
            fail();
        } catch (Exception e1) {
            //expected
        } finally {
            parser1.close();
        }
        
        
        
        JsonParser parser2 = Json.createParser(new StringReader("{"));
        try {
            while(parser2.hasNext()) {
                    parser2.next();                                
            }
            fail();
        } catch (JsonParsingException e) {
           //expected
        } finally {
            parser2.close();
        }
        
        
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
    
    @Test(expected = JsonException.class)
    public void zeroByteInput() {
        // using a reader as wrapper of parser
  
        Json.createReader(new ByteArrayInputStream(new byte[]{})).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void zeroCharInput() {
        // using a reader as wrapper of parser
  
        Json.createReader(new CharArrayReader(new char[]{})).read();
    }

    @Test
    public void testIOException() {
        final InputStream bin = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Expected");
            }
        };
        JsonParser parser = null;
        try {
            parser = Json.createParser(bin);
        } catch (JsonException e) {
            assertEquals("We were expecting another cause", "Expected", e.getCause().getMessage());
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    @Test
    public void testUTF32LEStream() {
        ByteArrayInputStream bin = new ByteArrayInputStream("[\"UTF32LE\"]".getBytes(UTF_32LE));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF32LE", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
    }
    
    @Test
    public void testUTF32BEStream() {
        ByteArrayInputStream bin = new ByteArrayInputStream("[\"UTF32BE\"]".getBytes(UTF_32BE));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF32BE", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
    }
    
    @Test
    public void testUTF16BEStream() {
        ByteArrayInputStream bin = new ByteArrayInputStream("[\"UTF16BE\"]".getBytes(UTF_16BE));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF16BE", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
    }
    
    @Test
    public void testUTF16LEStream() {
        ByteArrayInputStream bin = new ByteArrayInputStream("[\"UTF16LE\"]".getBytes(UTF_16LE));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF16LE", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
    }
    
    @Test
    public void testUTF8Stream() {
        ByteArrayInputStream bin = new ByteArrayInputStream("[\"UTF8\"]".getBytes(UTF_8));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF8", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
    }
    
    @Test
    public void testUTF16Stream() {
        //this writes UTF 16 with Byte Order Mark (BOM)
        ByteArrayInputStream bin = new ByteArrayInputStream("[\"UTF16\"]".getBytes(UTF_16));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF16", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
    }
    
    @Test
    public void testUTF16LEBOMStream() {
        ByteArrayInputStream bin = new ByteArrayInputStream("\ufeff[\"UTF16LEBOM\"]".getBytes(UTF_16LE));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF16LEBOM", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
    }
    
    @Test
    public void testUTF16BEBOMStream() {
        ByteArrayInputStream bin = new ByteArrayInputStream("\ufeff[\"UTF16BEBOM\"]".getBytes(UTF_16BE));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF16BEBOM", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
    }
    
    @Test
    public void testUTF32LEBOMStream() {
        ByteArrayInputStream bin = new ByteArrayInputStream("\ufeff[\"UTF32LEBOM\"]".getBytes(UTF_32LE));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF32LEBOM", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
    }
    
    @Test
    public void testBinaryNullStreamBOM() {
        ByteArrayInputStream bin = new ByteArrayInputStream("\ufeff\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0".getBytes(UTF_32LE));
        JsonParser parser = Json.createParser(bin);
        
        try {
            parser.next();
            fail();
        } catch (JsonParsingException e) {
            //expected
        }
       
    }
    
    @Test(expected=JsonParsingException.class)
    public void testBinaryNullStream() {
        ByteArrayInputStream bin = new ByteArrayInputStream("\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0".getBytes(UTF_8));
        JsonParser parser = Json.createParser(bin);
        parser.next();
    }
    
    @Test
    public void testUTF32BEBOMStream() {
        
        ByteArrayInputStream bin = new ByteArrayInputStream("\ufeff[\"UTF32BEBOM\"]".getBytes(UTF_32BE));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF32BEBOM", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
    }
    
    @Test
    public void testUTF8BEBOMStream() {
        ByteArrayInputStream bin = new ByteArrayInputStream("\ufeff[\"UTF8BOM\"]".getBytes(UTF_8));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF8BOM", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
    }
    
    @Test
    public void testStreamReadNotAllBytes() {       
        AttemptingInputStream bin = new AttemptingInputStream("\ufeff[\"UTF8BOM\"]".getBytes(UTF_8));
        JsonParser parser = Json.createParser(bin);
        parser.next();
        parser.next();
        assertEquals("UTF8BOM", parser.getString());
        parser.next();
        assertTrue(!parser.hasNext());
        parser.close();
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
    
    @Test
    public void escapeStart() {
        // using a reader as wrapper of parser
  
        assertEquals("\\abcdef", Json.createReader(new ByteArrayInputStream("[\"\\\\abcdef\"]".getBytes())).readArray().getString(0));
    }
    
    @Test
    public void escapeStart2() {
        // using a reader as wrapper of parser
  
        assertEquals("\"abcdef", Json.createReader(new ByteArrayInputStream("[\"\\\"abcdef\"]".getBytes())).readArray().getString(0));
    }

    @Test
    public void testSlowIs() {
        // using a reader as wrapper of parser
        class SlowIs extends ByteArrayInputStream {
            private boolean slowDown = true;

            @Override
            public synchronized int read(byte[] b, int off, int len) {
                if(slowDown) {
                    this.count = 5;
                    slowDown = false;
                } else {
                    this.count = this.buf.length;
                }
                return super.read(b, off, len);
            }

            protected SlowIs() {
                super("{\"message\":\"Hi REST!\"}".getBytes());
            }
        }

        assertEquals("Hi REST!", Json.createReaderFactory(null).createReader(new SlowIs(), UTF_8).readObject().getString("message"));
    }

    @Test
    public void threeLiterals() {
        final JsonParser parser = Json.createParserFactory(new HashMap<String, Object>() {{
            put(JsonParserFactoryImpl.MAX_STRING_LENGTH, 10);
        }}).createParser(new ByteArrayInputStream("{\"a\":true,\"b\":null,\"c\":false,\"arr\":[false, true, null]}".getBytes()));
        parser.next();
        parser.next();
        assertEquals(JsonParser.Event.VALUE_TRUE, parser.next());
        parser.next();
        assertEquals(JsonParser.Event.VALUE_NULL, parser.next());
        parser.next();
        assertEquals(JsonParser.Event.VALUE_FALSE, parser.next());
        parser.next();
        parser.next();
        assertEquals(JsonParser.Event.VALUE_FALSE, parser.next());
        assertEquals(JsonParser.Event.VALUE_TRUE, parser.next());
        assertEquals(JsonParser.Event.VALUE_NULL, parser.next());
        parser.close();
    }
    
    @Test
    public void maxStringStringOK() {
        // using a reader as wrapper of parser
        Json.createReaderFactory(new HashMap<String, Object>() {
            {
                put("org.apache.johnzon.max-string-length", "5");
            }
        }).createReader(new ByteArrayInputStream("[\"abcde\"]".getBytes())).read();
       
    }
    
    @Test(expected = JsonParsingException.class)
    public void maxStringStringFail() {
        // using a reader as wrapper of parser
        Json.createReaderFactory(new HashMap<String, Object>() {
            {
                put("org.apache.johnzon.max-string-length", "5");
            }
        }).createReader(new ByteArrayInputStream("[\"abcdef\"]".getBytes())).read();
       
    }
    
    @Test
    public void maxStringNumberOK() {
        // using a reader as wrapper of parser
        Json.createReaderFactory(new HashMap<String, Object>() {
            {
                put("org.apache.johnzon.max-string-length", "5");
            }
        }).createReader(new ByteArrayInputStream("[12.3]".getBytes())).read();
       
    }
    
    @Test(expected = JsonParsingException.class)
    public void maxStringNumberFail() {
        // using a reader as wrapper of parser
        Json.createReaderFactory(new HashMap<String, Object>() {
            {
                put("org.apache.johnzon.max-string-length", "5");
            }
        }).createReader(new ByteArrayInputStream("[12.333]".getBytes())).read();
       
    }
    
    @Test(expected = JsonParsingException.class)
    public void maxStringWhitespace() {
        // using a reader as wrapper of parser
        Json.createReaderFactory(new HashMap<String, Object>() {
            {
                put("org.apache.johnzon.max-string-length", "5");
            }
        }).createReader(new ByteArrayInputStream("[\"12\"           ]".getBytes())).read();
       
    }
    
    
    @Test
    public void testEmptyArray() {
       JsonParser parser = Json.createParser(new ByteArrayInputStream("[]".getBytes()));
        assertEquals(Event.START_ARRAY, parser.next());
        assertEquals(Event.END_ARRAY, parser.next());
        assertEquals(false, parser.hasNext());
        try {
            parser.next();
            fail("Should have thrown a NoSuchElementException");
        } catch (NoSuchElementException ne) {
            //expected
        }
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
    
    @Test(expected = JsonParsingException.class)
    public void fail26() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail26.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail27() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail27.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail28() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail28.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail29() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail29.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail30() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail30.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail31() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail31.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail32() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail32.json")).read();
    }
    
    
    @Test(expected = JsonParsingException.class)
    public void fail33() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail33.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail34() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail34.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail35() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail35.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail36() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail36.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail37() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail37.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail38() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail38.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail39() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail39.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail40() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail40.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail41() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail41.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail42() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail42.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail43() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail43.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail44() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail44.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail45() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail45.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail46() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail46.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail47() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail47.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail48() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail48.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail49() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail49.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail50() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail50.json")).read();
    }
    
    //@Test(expected = JsonParsingException.class)
    public void fail51() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail51.json")).read();
    }
    
    //@Test(expected = JsonParsingException.class)
    public void fail52() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail52.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail53() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail53.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail54() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail54.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail55() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail55.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail56() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail56.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail57() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail57.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail58() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail58.json")).read();
    }
    
    @Test(expected = JsonException.class)
    public void fail59() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail59.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail60() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail60.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail61() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail61.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail62() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail62.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail63() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail63.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail64() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail64.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail65() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail65.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail66() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail66.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail67() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail67.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail68() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail68.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail69() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail69.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail70() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail70.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail71() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail71.json")).read();
    }
    
    @Test(expected = JsonParsingException.class)
    public void fail72() {
        
        Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/fails/fail72.json")).read();
    }
    
    
    @Test
    public void stringescapeVariousBufferSizesBogus() {
        

        StringBuilder sb = new StringBuilder();
        sb.append("\t\"special-\":" + "\"" + "\\\\f\\n\\r\\t\\u6565\uDC00\uD800" + "\",\n");
        sb.append("\t\"unicode-\\u0000- \":\"\\u5656\uDC00\uD800\"\n");
        String s = "{"+sb.toString()+"}";
       
        for (int i = 1; i < s.length()+100; i++) {
            final String value = String.valueOf(i);

            final JsonParser parser = Json.createParserFactory(new HashMap<String, Object>() {
                {
                    put("org.apache.johnzon.default-char-buffer", value);
                }
            }).createParser(new ByteArrayInputStream(s.getBytes()));
            assertNotNull(parser);
            
            while(parser.hasNext()) {
                Event e = parser.next();
                if(e==null) {
                    fail();
                }
            }
            
            assertTrue(!parser.hasNext());
            parser.close();
            
        }
    }

    /*
    @Test(expected=JsonParsingException.class)
    public void rfc7159MustFailForLiteral() {
        Json.createReader(new ByteArrayInputStream("null ".getBytes())).read();
    }
    */

    /*
    @Test(expected=JsonParsingException.class)
    public void rfc7159MustFailForString() {
        Json.createReader(new ByteArrayInputStream("\"hello\"".getBytes())).read();
    }
    */

    /*
    @Test(expected=JsonParsingException.class)
    public void rfc7159MustFailForNumber() {
        Json.createReader(new ByteArrayInputStream("  12  ".getBytes())).read();
    }
    */

    @Test(expected=JsonParsingException.class)
    public void arrayFollowedByGarbage() {
        Json.createReader(new ByteArrayInputStream("[12],".getBytes())).read();
    }

    @Test(expected=JsonParsingException.class)
    public void arrayFollowedByGarbage1() {
        Json.createReader(new ByteArrayInputStream("[12]:".getBytes())).read();
    }

    @Test(expected=JsonParsingException.class)
    public void arrayFollowedByGarbage2() {
        Json.createReader(new ByteArrayInputStream("[12]:,".getBytes())).read();
    }

    @Test(expected=JsonParsingException.class)
    public void objectFollowedByGarbage() {
        Json.createReader(new ByteArrayInputStream("{\"a\":2},".getBytes())).read();
    }

    @Test(expected=JsonParsingException.class)
    public void objectFollowedByGarbage1() {
        Json.createReader(new ByteArrayInputStream("{\"a\":2}:".getBytes())).read();
    }

    @Test(expected=JsonParsingException.class)
    public void objectFollowedByGarbage2() {
        Json.createReader(new ByteArrayInputStream("{\"a\":2},:".getBytes())).read();
    }

    @Test(expected=JsonParsingException.class)
    public void objectFollowedByGarbage3() {
        Json.createReader(new ByteArrayInputStream("{\"a\":2}-".getBytes())).read();
    }

    @Test(expected=JsonParsingException.class)
    public void objectFollowedByGarbage4() {
        Json.createReader(new ByteArrayInputStream("{\"a\":2}------------".getBytes())).read();
    }

    @Test(expected=JsonParsingException.class)
    public void objectFollowedByGarbage5() {
        Json.createReader(new ByteArrayInputStream("{\"a\":2}{\"a\":2}".getBytes())).read();
    }

    @Test(expected=JsonParsingException.class)
    public void objectFollowedByGarbage6() {
        Json.createReader(new ByteArrayInputStream("{\"a\":2}\"".getBytes())).read();
    }

    @Test(expected=JsonParsingException.class)
    public void objectFollowedByGarbage7() {
        Json.createReader(new ByteArrayInputStream("{\"a\":2} \"".getBytes())).read();
    }

    @Test(expected=JsonParsingException.class)
    public void objectPrependedByGarbage7() {
        Json.createReader(new ByteArrayInputStream("-{\"a\":2}".getBytes())).read();
    }

    @Test(expected = JsonParsingException.class)
    // TODO read key and : in once
    public void invalidArrayMissingSeparator() {
        final JsonParser parser = Json.createParser(new StringReader("{\"a\":5, \"a\"[1,2,3,4,5,[2,2,3,4,5]], \"z\":8}"));
        assertNotNull(parser);
        assertTrue(parser.next() == Event.START_OBJECT);
        assertTrue(parser.next() == Event.KEY_NAME);
        assertTrue(parser.next() == Event.VALUE_NUMBER);
        assertTrue(parser.next() == Event.KEY_NAME);
        parser.next();
    }

    @Test(expected = JsonParsingException.class)
    public void invalidArray() {
        final JsonParser parser = Json.createParser(new StringReader("{\"a\":5, [1,2,3,4,5,[2,2,3,4,5]], \"z\":8}"));
        assertNotNull(parser);
        assertTrue(parser.next() == Event.START_OBJECT);
        assertTrue(parser.next() == Event.KEY_NAME);
        assertTrue(parser.next() == Event.VALUE_NUMBER);
        parser.next();
    }

    @Test(expected = JsonParsingException.class)
    public void invalidEmptyObject() {
        final JsonParser parser = Json.createParser(new StringReader("{\"a\":5, {}, \"z\":8}"));
        assertNotNull(parser);
        assertTrue(parser.next() == Event.START_OBJECT);
        assertTrue(parser.next() == Event.KEY_NAME);
        assertTrue(parser.next() == Event.VALUE_NUMBER);
        parser.next();
    }

    @Test(expected = JsonParsingException.class)
    public void invalidObject() {
        final JsonParser parser = Json.createParser(new StringReader("{\"a\":5, {\"w\":1}, \"z\":8}"));
        assertNotNull(parser);
        assertTrue(parser.next() == Event.START_OBJECT);
        assertTrue(parser.next() == Event.KEY_NAME);
        assertTrue(parser.next() == Event.VALUE_NUMBER);
        parser.next();
    }

    @Test(expected = JsonParsingException.class)
    public void invalidLiteral() {
        final JsonParser parser = Json.createParser(new StringReader("{\"a\":5, true, \"z\":8}"));
        assertNotNull(parser);
        assertTrue(parser.next() == Event.START_OBJECT);
        assertTrue(parser.next() == Event.KEY_NAME);
        assertTrue(parser.next() == Event.VALUE_NUMBER);
        parser.next();
    }

    @Test(expected = JsonParsingException.class)
    public void invalidString() {
        final JsonParser parser = Json.createParser(new StringReader("{\"a\":5, \"a\", \"z\":8}"));
        assertNotNull(parser);
        assertTrue(parser.next() == Event.START_OBJECT);
        assertTrue(parser.next() == Event.KEY_NAME);
        assertTrue(parser.next() == Event.VALUE_NUMBER);
        assertTrue(parser.next() == Event.KEY_NAME);
        parser.next();
    }

    @Test(expected = JsonParsingException.class)
    public void invalidKeyWithoutValue() {
        final JsonParser parser = Json.createParser(new StringReader("{\"a\":5, \"a\":, \"z\":8}"));
        assertNotNull(parser);
        assertTrue(parser.next() == Event.START_OBJECT);
        assertTrue(parser.next() == Event.KEY_NAME);
        assertTrue(parser.next() == Event.VALUE_NUMBER);
        assertTrue(parser.next() == Event.KEY_NAME);
        parser.next();

    }

    @Test(expected = JsonParsingException.class)
    public void invalidArrayMissingKeyname() {
        final JsonParser parser = Json.createParser(new StringReader("{\"a\":5, :[1,2,3,4,5,[2,2,3,4,5]], \"z\":8}"));
        assertNotNull(parser);
        assertTrue(parser.next() == Event.START_OBJECT);
        assertTrue(parser.next() == Event.KEY_NAME);
        assertTrue(parser.next() == Event.VALUE_NUMBER);
        parser.next();
    }

    @Test(expected = JsonParsingException.class)
    public void missingClosingObject() {
        final JsonParser parser = Json.createParser(new StringReader("{\"a\":5, \"d\": {\"m\":6}, \"z\":true"));
        assertNotNull(parser);
        while (parser.hasNext()) {
            parser.next();
        }
    }

    @Test
    public void plainValues() {
        { // string
            final JsonParser string = Json.createParser(new StringReader("\"a\""));
            assertTrue(string.hasNext());
            assertEquals(Event.VALUE_STRING, string.next());
            assertEquals("a", string.getString());
            assertFalse(string.hasNext());
        }
        { // true
            final JsonParser parser = Json.createParser(new StringReader("true"));
            assertTrue(parser.hasNext());
            assertEquals(Event.VALUE_TRUE, parser.next());
            assertFalse(parser.hasNext());
        }
        { // false
            final JsonParser parser = Json.createParser(new StringReader("false"));
            assertTrue(parser.hasNext());
            assertEquals(Event.VALUE_FALSE, parser.next());
            assertFalse(parser.hasNext());
        }
        { // null
            final JsonParser parser = Json.createParser(new StringReader("null"));
            assertTrue(parser.hasNext());
            assertEquals(Event.VALUE_NULL, parser.next());
            assertFalse(parser.hasNext());
        }
        { // number
            final JsonParser parser = Json.createParser(new StringReader("1234"));
            assertTrue(parser.hasNext());
            assertEquals(Event.VALUE_NUMBER, parser.next());
            assertEquals(1234, parser.getInt());
            assertFalse(parser.hasNext());
        }
    }

    class AttemptingInputStream extends ByteArrayInputStream {

        public AttemptingInputStream(byte[] buf) {
            super(buf);
            
        }

        @Override
        public synchronized int read(byte b[], int off, int len) {
            
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || 1 > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
         
            if (pos >= count) {
                return -1;
            }

            int avail = count - pos;
            
            if (avail == 0) {
                return 0;
            }
            System.arraycopy(buf, pos, b, off, 1);
            pos++;
            return 1;
        }
    }
}
