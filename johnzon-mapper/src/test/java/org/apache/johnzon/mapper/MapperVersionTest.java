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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MapperVersionTest {

    private final int mapperVersion;
    private final String name;
    private final String expectedJson;

    public MapperVersionTest(int mapperVersion, String name, String expectedJson) {
        this.mapperVersion = mapperVersion;
        this.name = name;
        this.expectedJson = expectedJson;
    }

    @Parameterized.Parameters(name = "Run {index}: mapperVersion={0}, name={1}, expectedJson={2}")
    public static Object[][] data() {
        return new Object[][] {
                { -1, "foo", "{\"name\":\"foo\"}"}, // no version eg version of -1
                { 0, "foo", "{}"},                  // version 0 < minVersion 2 -- dont serialize it
                { 1, "foo", "{}"},                  // version 1 < minVersion 2 -- dont serialize it
                { 2, "foo", "{\"name\":\"foo\"}"},  // version 2 >= minVersion 2 -- serialize it
                { 3, "foo", "{\"name\":\"foo\"}"}   // version 2 >= minVersion 2 -- serialize it
        };
    }

    @Test
    public void test() {
        final Mapper mapper = new MapperBuilder().setVersion(mapperVersion).build();
        final Versioned versioned = new Versioned();
        versioned.name = name;
        final StringWriter writer = new StringWriter();
        mapper.writeObject(versioned, writer);
        assertEquals(expectedJson, writer.toString());
    }

    public static class Versioned {
        private String name;

        @JohnzonIgnore(minVersion =  2)
        public String getName() {
            return name;
        }
    }
}
