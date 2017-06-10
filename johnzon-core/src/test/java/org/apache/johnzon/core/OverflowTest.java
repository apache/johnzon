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

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class OverflowTest {
    @Test
    public void ok() {
        // normal content size
        Json.createReaderFactory(new HashMap<String, Object>() {{
            put(JsonParserFactoryImpl.MAX_STRING_LENGTH, "10");
            put(JsonParserFactoryImpl.BUFFER_LENGTH, "2");
        }}).createReader(new StringReader("{\"a\":\"b\",\n\"another\":\"value\"}")).readObject();

        // oversized
        final JsonObject object = Json.createReaderFactory(new HashMap<String, Object>() {{
            put(JsonParserFactoryImpl.MAX_STRING_LENGTH, "10");
            put(JsonParserFactoryImpl.BUFFER_LENGTH, "2");
        }}).createReader(new StringReader("{\"a\":\"b\",\n\"another\":\"value very long\"}")).readObject();
        assertEquals("value very long", object.getString("another"));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void ko() {
        Json.createReaderFactory(new HashMap<String, Object>() {{
            put(JsonParserFactoryImpl.MAX_STRING_LENGTH, "10");
            put(JsonParserFactoryImpl.BUFFER_LENGTH, "2");
            put(JsonParserFactoryImpl.AUTO_ADJUST_STRING_BUFFER, "false");
        }}).createReader(new StringReader("{\"another\":\"value too long\"}")).readObject();
    }
}
