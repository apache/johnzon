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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;

import org.junit.Test;

public class JsonReaderImplTest {
    
    
    
    public JsonReaderImplTest() {
        super();
        if (!Charset.defaultCharset().equals(Charset.forName("UTF-8"))) {
            throw new RuntimeException("Default charset is " + Charset.defaultCharset() + ", must must be UTF-8");
        }
    }

    protected static Charset utf8Charset = Charset.forName("UTF8");
    protected static Charset asciiCharset = Charset.forName("ASCII");

    @SuppressWarnings("unchecked")
    protected Map<String, ?> getFactoryConfig() {
        return Collections.EMPTY_MAP;
    }

    @Test
    public void simple() {
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("json/simple.json"), utf8Charset);
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
        assertEquals(-2, array.getInt(1));
        reader.close();
    }

    @Test
    public void unicode() {
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("json/unicode.json"), utf8Charset);
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(String.valueOf('\u6565'), object.getString("a"));
        assertEquals("", object.getString("z"));
        assertEquals(String.valueOf('\u0000'), object.getString("c"));
        assertThat(object.get("d"), instanceOf(JsonArray.class));
        final JsonArray array = object.getJsonArray("d");
        assertNotNull(array);
        assertEquals(3, array.size());
        assertEquals(-2, array.getInt(0));
        assertEquals(" ", array.getString(1));
        assertEquals("", array.getString(2));
        assertEquals(5, object.size());
        reader.close();
    }

    @Test
    public void unicodeWithIoReader() {
        final Reader ioReader = new InputStreamReader(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("json/unicode.json"), utf8Charset);
        final JsonReader reader = Json.createReader(ioReader);
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(String.valueOf('\u6565'), object.getString("a"));
        assertEquals("", object.getString("z"));
        assertEquals(String.valueOf('\u0000'), object.getString("c"));
        assertThat(object.get("d"), instanceOf(JsonArray.class));
        final JsonArray array = object.getJsonArray("d");
        assertNotNull(array);
        assertEquals(3, array.size());
        assertEquals(-2, array.getInt(0));
        assertEquals(" ", array.getString(1));
        assertEquals("", array.getString(2));
        assertEquals(5, object.size());
        reader.close();
    }

    @Test
    public void special() {
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("json/special.json"), utf8Charset);
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(9, object.size());
        assertEquals("b,,", object.getString("a{"));
        assertEquals(":4::,[{", object.getString("c::::"));
        assertTrue(object.getJsonNumber("w").doubleValue() > 4 && object.getJsonNumber("w").doubleValue() < 5);
        assertEquals(110, object.getInt("1.4312"));
        assertEquals("\"", object.getString("\""));
        assertTrue(object.isNull("\u0044"));
        assertEquals("ন:4::,[{", object.getString("থii:üäöÖ.,;.-<>!§$%&()=?ß´'`*+#"));
        reader.close();
    }

    @Test
    public void specialWithIoReader() {
        final Reader ioReader = new InputStreamReader(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("json/special.json"), utf8Charset);
        final JsonReader reader = Json.createReader(ioReader);
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(9, object.size());
        assertEquals("b,,", object.getString("a{"));
        assertEquals(":4::,[{", object.getString("c::::"));
        assertTrue(object.getJsonNumber("w").doubleValue() > 4 && object.getJsonNumber("w").doubleValue() < 5);
        assertEquals(110, object.getInt("1.4312"));
        assertEquals("\"", object.getString("\""));
        assertEquals("ন:4::,[{", object.getString("থii:üäöÖ.,;.-<>!§$%&()=?ß´'`*+#"));
        reader.close();
    }

    @Test
    public void specialWithStringAsByteArrayInputStream() {
        final String s = "{\"নa\":\"hallo\u20acö\uffff \u08a5 থ?ß§$%&´'`*+#\udbff\udfff\"}";
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                new ByteArrayInputStream(s.getBytes(utf8Charset)), utf8Charset);
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(1, object.size());
        assertEquals("hallo\u20acö\uffff \u08a5 থ?ß§$%&´'`*+#\udbff\udfff", object.getString("নa"));
        reader.close();
    }
    
    @Test
    public void specialKeysWithStringAsByteArrayInputStream() {
        final String s = "{\"\\\"a\":\"\u0055\",\"\u0055\":\"test2\"}";
        System.out.println(s);
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                new ByteArrayInputStream(s.getBytes(utf8Charset)), utf8Charset);
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(2, object.size());
        assertEquals("U", object.getString("\"a"));
        assertEquals("test2", object.getString("U"));
        reader.close();
    }

    @Test
    public void specialWithStringReader() {
        final String s = "{\"ন:4::,[{\u08a5\":\"থii:üäöÖ.,;.-<>!§$%&()=?ß´'`*+#\ua5a5\"}";
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                new InputStreamReader(new ByteArrayInputStream(s.getBytes(utf8Charset)), utf8Charset));
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(1, object.size());
        assertEquals("থii:üäöÖ.,;.-<>!§$%&()=?ß´'`*+#\ua5a5", object.getString("ন:4::,[{\u08a5"));
        reader.close();
    }

    @Test
    public void unicode4Bytes() {
        final int codepoint = 128149;
        final char[] charPair = Character.toChars(codepoint);
        assertNotNull(charPair);
        assertEquals(2, charPair.length);
        assertTrue(Character.isHighSurrogate(charPair[0]));
        assertTrue(Character.isLowSurrogate(charPair[1]));
        assertTrue(Character.isSurrogatePair(charPair[0], charPair[1]));
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                (new ByteArrayInputStream(("{\"\":\"Ö" + charPair[0] + charPair[1] + "\"}").getBytes(utf8Charset))),
                utf8Charset);
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);

        assertEquals(codepoint, object.getString("").codePointAt(1));
        assertEquals("Ö" + new String(charPair), object.getString(""));
        assertEquals(1, object.size());
        reader.close();
    }

    @Test
    public void unicode3Bytes() {
        final char[] charPair = Character.toChars("\uffff".codePointAt(0));
        assertNotNull(charPair);
        assertEquals(1, charPair.length);
        assertTrue(!Character.isLowSurrogate(charPair[0]));
        assertTrue(!Character.isHighSurrogate(charPair[0]));
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                new ByteArrayInputStream(("{\"\":\"\uffff\"}").getBytes(utf8Charset)), utf8Charset);
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(String.valueOf('\uffff'), object.getString(""));
        assertEquals(1, object.size());
        reader.close();
    }

    @Test
    public void unicode2Bytes() {
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                new ByteArrayInputStream(("{\"\":\"Ö\u00d6\"}").getBytes(utf8Charset)), utf8Charset);
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals("Ö\u00d6", object.getString(""));
        assertEquals(1, object.size());
        reader.close();
    }

    @Test(expected = NullPointerException.class)
    public void unicodeFailAscii() {
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                new ByteArrayInputStream(
                        "{\"ন:4::,[{\udbff\udfff\":\"থii:üäöÖ.,;.-<>!§$%&()=?ß´'`*+#\udbff\udfff\"}".getBytes(asciiCharset)),
                utf8Charset);
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(1, object.size());
        assertEquals("থii:üäöÖ.,;.-<>!§$%&()=?ß´'`*+#\udbff\udfff", object.getString("ন:4::,[{\udbff\udfff"));
        reader.close();
    }

    @Test
    public void parseHuge1MbJsonFile() {
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/huge_1mb.json"), utf8Charset);
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }

    @Test
    public void parseBig600KbJsonFile() {
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/big_600kb.json"), utf8Charset);
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }

    @Test
    public void parseLarge130KbJsonFile() {
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/large_130kb.json"), utf8Charset);
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }

    @Test
    public void parseMedium11KbJsonFile() {
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/medium_11kb.json"), utf8Charset);
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }

    @Test
    public void parseSmall3KbJsonFile() {
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/small_3kb.json"), utf8Charset);
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }

    @Test
    public void parseTiny50BJsonFile() {
        final JsonReader reader = Json.createReaderFactory(getFactoryConfig()).createReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/tiny_50b.json"), utf8Charset);
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }

    @Test
    public void simpleBadBufferSize8() {
        final JsonReader reader = Json.createReaderFactory(new HashMap<String, Object>() {
            {
                put("org.apache.fleece.default-char-buffer", "8");
            }
        }).createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/simple.json"), utf8Charset);
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
        assertEquals(-2, array.getInt(1));
        reader.close();
    }
    
    @Test
    public void simple2BadBufferSize8() {
        final JsonReader reader = Json.createReaderFactory(new HashMap<String, Object>() {
            {
                put("org.apache.fleece.default-char-buffer", "8");
            }
        }).createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/simple2.json"), utf8Charset);
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
        assertEquals(-2, array.getInt(1));
        reader.close();
    }

    @Test
    public void simpleBadBufferSize9() {
        final JsonReader reader = Json.createReaderFactory(new HashMap<String, Object>() {
            {
                put("org.apache.fleece.default-char-buffer", "9");
            }
        }).createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/simple.json"), utf8Charset);
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
        assertEquals(-2, array.getInt(1));
        reader.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyZeroCharBuffersize() {
        final JsonReader reader = Json.createReaderFactory(new HashMap<String, Object>() {
            {
                put("org.apache.fleece.default-char-buffer", "0");
            }
        }).createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/empty.json"), utf8Charset);
        assertNotNull(reader);
        reader.readObject();
        reader.close();
    }

    @Test
    public void emptyOneCharBufferSize() {
        final JsonReader reader = Json.createReaderFactory(new HashMap<String, Object>() {
            {
                put("org.apache.fleece.default-char-buffer", "1");
            }
        }).createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/empty.json"), utf8Charset);
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(0, object.size());
        reader.close();
    }

    @Test
    public void emptyArrayOneCharBufferSize() {
        final JsonReader reader = Json.createReaderFactory(new HashMap<String, Object>() {
            {
                put("org.apache.fleece.default-char-buffer", "1");
            }
        }).createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/emptyarray.json"), utf8Charset);
        assertNotNull(reader);
        final JsonArray array = reader.readArray();
        assertNotNull(array);
        assertEquals(0, array.size());
        reader.close();
    }

    @Test
    public void stringescapeVariousBufferSizes() {
        final int[] buffersizes = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
                26, 27, 28, 32, 64, 128, 1024, 8192 };

        for (int i = 0; i < buffersizes.length; i++) {
            final String value = String.valueOf(buffersizes[i]);
            final JsonReader reader = Json.createReaderFactory(new HashMap<String, Object>() {
                {
                    put("org.apache.fleece.default-char-buffer", value);
                }
            }).createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/stringescape.json"),
                    utf8Charset);
            assertNotNull(reader);
            final JsonObject object = reader.readObject();
            assertNotNull(object);
            assertEquals(1, object.size());
            assertEquals("s\"mit\"", object.getString("name"));
            reader.close();
        }
    }
}