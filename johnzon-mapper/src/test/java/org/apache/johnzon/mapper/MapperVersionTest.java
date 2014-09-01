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

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class MapperVersionTest {
    @Test
    public void version() {
        { // no version
            final Mapper mapper = new MapperBuilder().build();
            final Versioned versioned = new Versioned();
            versioned.name = "foo";
            final StringWriter writer = new StringWriter();
            mapper.writeObject(versioned, writer);
            assertEquals("{\"name\":\"foo\"}", writer.toString());
        }
        { // version ko
            for (int i = 0; i < 2; i++) {
                final Mapper mapper = new MapperBuilder().setVersion(i).build();
                final Versioned versioned = new Versioned();
                versioned.name = "foo";
                final StringWriter writer = new StringWriter();
                mapper.writeObject(versioned, writer);
                assertEquals("{\"name\":\"foo\"}", writer.toString());
            }
        }
        { // version ok
            for (int i = 2; i < 4; i++) {
                final Mapper mapper = new MapperBuilder().setVersion(i).build();
                final Versioned versioned = new Versioned();
                versioned.name = "foo";
                final StringWriter writer = new StringWriter();
                mapper.writeObject(versioned, writer);
                assertEquals("{}", writer.toString());
            }
        }
    }

    public static class Versioned {
        private String name;

        @JohnzonIgnore(minVersion =  2)
        public String getName() {
            return name;
        }
    }
}
