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

import org.junit.Test;

import java.io.StringReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class JohnzonAnyMappingTest {
    @Test
    public void roundTrip() {
        final Mapper mapper = new MapperBuilder().setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        }).build();

        final AnyMe instance = new AnyMe();
        instance.name = "test";
        instance.any.putAll(new HashMap<String, Object>() {{
            put("a", "n");
            put("y", ".");
        }});
        // sorting is expected to be fields then any with the Map ordering
        assertEquals("{\"name\":\"test\",\"a\":\"n\",\"y\":\".\"}", mapper.writeObjectAsString(instance));

        final AnyMe loaded = mapper.readObject(new StringReader("{\"name\":\"test\",\"z\":2016, \"a\":\"n\",\"y\":\".\"}"), AnyMe.class);
        assertEquals("test", loaded.name);
        assertEquals(new HashMap<String, Object>() {{
            put("a", "n");
            put("y", ".");
            put("z", 2016);
        }}, loaded.any);
    }

    public static class AnyMe {
        private String name;

        @JohnzonIgnore
        private Map<String, Object> any = new TreeMap<String, Object>();

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        @JohnzonAny
        public Map<String, Object> getAny() {
            return any;
        }

        @JohnzonAny
        public void handle(final String key, final Object val) {
            any.put(key, val);
        }
    }
}
