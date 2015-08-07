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

import java.io.StringReader;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;

import org.junit.Assert;
import org.junit.Test;

public class LocationTest {
    @Test
    public void failBytesInput() {
  
        try {
            JsonReader reader = Json.createReader(new StringReader("{\"z\":nulll}"));
            reader.read();
            Assert.fail("Exception expected");
        } catch (JsonParsingException e) {
            JsonLocation location = e.getLocation();
            Assert.assertNotNull(location);
            Assert.assertEquals(new JsonLocationImpl(1, 11, 10), location);
            
        }
        
        
        try {
            JsonReader reader = Json.createReader(new StringReader("{\"z\":\nnulll}"));
            reader.read();
            Assert.fail("Exception expected");
        } catch (JsonParsingException e) {
            JsonLocation location = e.getLocation();
            Assert.assertNotNull(location);
            Assert.assertEquals(new JsonLocationImpl(2, 6, 11), location);
            
        }
        
        try {
            JsonReader reader = Json.createReader(new StringReader("aaa"));
            reader.read();
            Assert.fail("Exception expected");
        } catch (JsonParsingException e) {
            JsonLocation location = e.getLocation();
            Assert.assertNotNull(location);
            Assert.assertEquals(new JsonLocationImpl(1, 2, 1), location);
            
        }
    }
    
    
    @Test
    public void simpleLocation() {
  
  
        JsonParser parser = Json.createParser(new StringReader("{\n    \"ö \uffff c\": null ,\n    \"test\"  :\"testval\",\n    \"num\": 45.1e-12  \n}"));
        
        /*
         
         
{
    "z a c": null ,
    "test"  :"testval",
    "num": 45.1e-12  //2 ws
}
         
         
         */
        
        Assert.assertEquals(new JsonLocationImpl(1, 1, 0), parser.getLocation());
        parser.next(); //after first {
        Assert.assertEquals(new JsonLocationImpl(1, 2, 1), parser.getLocation());
        parser.next(); //after "ö \uffff c"
        Assert.assertEquals(new JsonLocationImpl(2, 12, 13), parser.getLocation());
        parser.next(); //after null   
        Assert.assertEquals(new JsonLocationImpl(2, 18, 19), parser.getLocation());
        parser.next(); //after test
        Assert.assertEquals(new JsonLocationImpl(3, 11, 32), parser.getLocation());    
        parser.next(); //after testval
        Assert.assertEquals(new JsonLocationImpl(3, 23, 44), parser.getLocation());    
        parser.next(); //after num
        Assert.assertEquals(new JsonLocationImpl(4, 10, 55), parser.getLocation());    
        parser.next(); //after 45.1e-12
        Assert.assertEquals(new JsonLocationImpl(4, 20, 65), parser.getLocation());    
        parser.next(); //after }
        Assert.assertEquals(new JsonLocationImpl(5, 2, 69), parser.getLocation());    
       
    }
    
    /*@Test
    public void simpleLocationBytes() {
  
        JsonParser parser = Json.createParser(new ByteArrayInputStream("{\n    \"ö \uffff c\": null ,\n    \"test\"  :\"testval\",\n    \"num\": 45.1e-12  \n}".getBytes()));
        
        Assert.assertEquals(new JsonLocationImpl(1, 1, 0), parser.getLocation());
        parser.next(); //after first {
        Assert.assertEquals(new JsonLocationImpl(1, 2, 2), parser.getLocation());
        parser.next(); //after "ö \uffff c"
        Assert.assertEquals(new JsonLocationImpl(2, 12, 26), parser.getLocation());
        parser.next(); //after null   
        Assert.assertEquals(new JsonLocationImpl(2, 18, 38), parser.getLocation());
        parser.next(); //after test
        Assert.assertEquals(new JsonLocationImpl(3, 11, 64), parser.getLocation());    
        parser.next(); //after testval
        Assert.assertEquals(new JsonLocationImpl(3, 23, 88), parser.getLocation());    
        parser.next(); //after num
        Assert.assertEquals(new JsonLocationImpl(4, 10, 110), parser.getLocation());    
        parser.next(); //after 45.1e-12
        Assert.assertEquals(new JsonLocationImpl(4, 20, 130), parser.getLocation());    
        parser.next(); //after }
        Assert.assertEquals(new JsonLocationImpl(5, 2, 138), parser.getLocation());    
       
    }*/
    
    @Test
    public void simpleLocationCrossingBufferBoundaries() {
 

        for (int i = 1; i <= 100; i++) {
            final String value = String.valueOf(i);
            final JsonParser parser = Json.createParserFactory(new HashMap<String, Object>() {
                {
                    put("org.apache.johnzon.default-char-buffer", value);
                }
            }).createParser(new StringReader("{\n    \"z a c\": null ,\n    \"test\"  :\"testval\",\n    \"num\": 45.1e-12  \n}"));

             
            /*
             
             
    {
        "z a c": null ,
        "test"  :"testval",
        "num": 45.1e-12  //2 ws
    }
             
             
             */
            
           
            
            Assert.assertEquals(new JsonLocationImpl(1, 1, 0), parser.getLocation());
            parser.next(); //after first {
            Assert.assertEquals(new JsonLocationImpl(1, 2, 1), parser.getLocation());
            parser.next(); //after "z a c"
            Assert.assertEquals(new JsonLocationImpl(2, 12, 13), parser.getLocation());
            parser.next(); //after null   
            Assert.assertEquals(new JsonLocationImpl(2, 18, 19), parser.getLocation());
            parser.next(); //after test
            Assert.assertEquals(new JsonLocationImpl(3, 11, 32), parser.getLocation());    
            parser.next(); //after testval
            Assert.assertEquals(new JsonLocationImpl(3, 23, 44), parser.getLocation());    
            parser.next(); //after num
            Assert.assertEquals(new JsonLocationImpl(4, 10, 55), parser.getLocation());    
            parser.next(); //after 45.1e-12
            Assert.assertEquals(new JsonLocationImpl(4, 20, 65), parser.getLocation());    
            parser.next(); //after }
            Assert.assertEquals(new JsonLocationImpl(5, 2, 69), parser.getLocation());   
            
            Assert.assertFalse(parser.hasNext());
            Assert.assertFalse(parser.hasNext());
        }
        
        
         
       
    }
    
    
    
    @Test
    public void testLocationOnParsingException() {
        //line number, column, offset (measured in chars)
        //line number and column start at 1
        //offset start at 0
        assertJsonLocation("a", new JsonLocationImpl(1, 2, 1));
        assertJsonLocation("aa", new JsonLocationImpl(1, 2, 1));
        assertJsonLocation("asa", new JsonLocationImpl(1, 2, 1));
        assertJsonLocation("{]", new JsonLocationImpl(1, 3, 2));
        assertJsonLocation("[}", new JsonLocationImpl(1, 3, 2));
        assertJsonLocation("[a", new JsonLocationImpl(1, 3, 2));
        assertJsonLocation("[nuLl]", new JsonLocationImpl(1, 5, 4));
        assertJsonLocation("[falsE]", new JsonLocationImpl(1, 7, 6));
        assertJsonLocation("[][]", new JsonLocationImpl(1, 4, 3));
        assertJsonLocation("[1234L]", new JsonLocationImpl(1, 7, 6));
        assertJsonLocation("[null\n}", new JsonLocationImpl(2, 2, 7));
        assertJsonLocation("[null\r\n}", new JsonLocationImpl(2, 2, 8));
        assertJsonLocation("[null\n, null\n}", new JsonLocationImpl(3, 2, 14));
        assertJsonLocation("[null\r\n, null\r\n}", new JsonLocationImpl(3, 2, 16));
    }


    private void assertJsonLocation(String jsonString, JsonLocation expectedLocation) {
        JsonParser parser = Json.createParser(new StringReader(jsonString));
        try {
            while(parser.hasNext()) {
                parser.next();
            }
            Assert.fail("Expected to throw JsonParsingException for "+jsonString);
        } catch(JsonParsingException je) {
            // Expected
            if (expectedLocation != null) {
                JsonLocation loc = je.getLocation();
                assertEquals("for line number", expectedLocation.getLineNumber(), loc.getLineNumber());
                assertEquals("for column number", expectedLocation.getColumnNumber(), loc.getColumnNumber());
                assertEquals("for stream offset", expectedLocation.getStreamOffset(), loc.getStreamOffset());
            }
        } finally {
            parser.close();
        }
    }
}
