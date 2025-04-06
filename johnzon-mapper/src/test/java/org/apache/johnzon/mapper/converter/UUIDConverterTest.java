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
package org.apache.johnzon.mapper.converter;

import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperBuilder;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class UUIDConverterTest {
    private static final UUID THE_UUID = UUID.randomUUID();

    @Test
    public void serialize() {
        Mapper mapper = new MapperBuilder().build();
        String json = mapper.writeObjectAsString(THE_UUID);

        assertEquals("\"" + THE_UUID + "\"", json);
    }

    @Test
    public void deserialize() {
        Mapper mapper = new MapperBuilder().build();
        UUID uuid = mapper.readObject("\"" + THE_UUID + "\"", UUID.class);

        assertEquals(THE_UUID, uuid);
    }
}
