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

import static org.junit.Assert.assertEquals;

public class GetterSetterRespectTest {
    @Test
    public void run() {
        final Mapper mapper = new MapperBuilder().build();
        assertEquals("ok", Mapped.class.cast(mapper.readObject("{\"name_\":\"ok\"}", Mapped.class)).name);

        final Mapped mapped = new Mapped();
        mapped.name = "ok";
        assertEquals("{\"_name\":\"ok\"}", mapper.writeObjectAsString(mapped));
    }

    public static class Mapped {
        private String name;

        @JohnzonProperty("_name")
        public String getName() {
            return name;
        }

        @JohnzonProperty("name_")
        public void setName(final String name) {
            this.name = name;
        }
    }
}
