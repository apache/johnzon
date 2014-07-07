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

import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JsonReaderImplTest {
    
    @Before
    public void setup(){
        System.setProperty("org.apache.fleece.default-char-buffer", "8192");
    }
    
    @Test
    public void simple() {
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/simple.json"));
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
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/unicode.json"));
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
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/special.json"));
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
    public void parseHuge1MbJsonFile() {
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/huge_1mb.json"));
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }
    
    @Test
    public void parseBig600KbJsonFile() {
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/big_600kb.json"));
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }
    
    @Test
    public void parseLarge130KbJsonFile() {
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/large_130kb.json"));
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }
    
    @Test
    public void parseMedium11KbJsonFile() {
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/medium_11kb.json"));
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }
    
    @Test
    public void parseSmall3KbJsonFile() {
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/small_3kb.json"));
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }
    
    @Test
    public void parseTiny50BJsonFile() {
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("bench/tiny_50b.json"));
        assertNotNull(reader);
        final JsonStructure object = reader.read();
        assertNotNull(object);
        reader.close();
    }
    
    
    @Test
    public void simpleBadBufferSize8() {
        System.setProperty("org.apache.fleece.default-char-buffer", "8");
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/simple.json"));
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
        System.setProperty("org.apache.fleece.default-char-buffer", "9");
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/simple.json"));
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
    
    @Test(expected=IllegalArgumentException.class)
    public void emptyZeroCharBuffersize() {
        System.setProperty("org.apache.fleece.default-char-buffer", "0");
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/empty.json"));
        assertNotNull(reader);
        reader.readObject();
        reader.close();
    }
    
    @Test
    public void emptyOneCharBufferSize() {
        System.setProperty("org.apache.fleece.default-char-buffer", "1");
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/empty.json"));
        assertNotNull(reader);
        final JsonObject object = reader.readObject();
        assertNotNull(object);
        assertEquals(0, object.size());
        reader.close();
    }
    
    @Test
    public void emptyArrayOneCharBufferSize() {
        System.setProperty("org.apache.fleece.default-char-buffer", "1");
        final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/emptyarray.json"));
        assertNotNull(reader);
        final JsonArray array = reader.readArray();
        assertNotNull(array);
        assertEquals(0, array.size());
        reader.close();
    }
    
    
    @Test
    public void stringescapeVariousBufferSizes() {

        int[] buffersizes = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
                28, 32, 64, 128, 1024, 8192 };

        for (int i = 0; i < buffersizes.length; i++) {
            System.setProperty("org.apache.fleece.default-char-buffer", String.valueOf(buffersizes[i]));
            final JsonReader reader = Json.createReader(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("json/stringescape.json"));
            assertNotNull(reader);
            final JsonObject object = reader.readObject();
            assertNotNull(object);
            assertEquals(1, object.size());
            assertEquals("s\"mit\"", object.getString("name"));
            reader.close();
        }
    }
}
