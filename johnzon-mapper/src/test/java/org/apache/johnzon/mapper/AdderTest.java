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
package org.apache.johnzon.mapper;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;

import org.junit.Test;

public class AdderTest {
    @Test
    public void adderString() {
        assertEquals(singletonMap("foo", "bar"), new MapperBuilder().build()
                .readObject("{\"foo\":\"bar\"}", MyMap.class));
    }
    @Test
    public void adderObject() {
        final MyMapOfObjects map = new MapperBuilder().build()
                                                 .readObject("{\"foo\":{\"value\":\"bar\"}}", MyMapOfObjects.class);
        assertEquals("bar", map.get("foo").value);
    }

    public static class MyMap extends LinkedHashMap<String, String>  {
        public void addString(final String name, final String value) {
            put(name, value);
        }
    }

    public static class MyObject {
        public String value;
    }

    public static class MyMapOfObjects extends LinkedHashMap<String, MyObject>  {
        public void addMyObject(final String name, final MyObject value) {
            put(name, value);
        }
    }
}
