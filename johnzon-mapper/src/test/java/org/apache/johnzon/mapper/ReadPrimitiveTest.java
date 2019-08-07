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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReadPrimitiveTest {
    private final Mapper mapper = new MapperBuilder().build();

    @Test
    public void testBoolean() {
        final Boolean val1 = mapper.readObject("false", Boolean.class);
        final Boolean val2 = mapper.readObject("true", Boolean.TYPE);
        assertFalse(val1);
        assertTrue(val2);
    }

    @Test
    public void testByte() {
        final Byte val1 = mapper.readObject("-50", Byte.class);
        final Byte val2 = mapper.readObject("110", Byte.TYPE);
        assertEquals((byte) -50, val1.byteValue());
        assertEquals((byte) 110, val2.byteValue());
    }

    @Test
    public void testCharacter() {
        final Character val1 = mapper.readObject("\"a\"", Character.class);
        final Character val2 = mapper.readObject("\"-\"", Character.TYPE);
        assertEquals('a', val1.charValue());
        assertEquals('-', val2.charValue());
    }

    @Test
    public void testDouble() {
        final Double val1 = mapper.readObject("5.096684684960", Double.class);
        final Double val2 = mapper.readObject("-886968406846.86468464", Double.TYPE);
        assertEquals(5.096684684960, val1, 0.0000001);
        assertEquals(-886968406846.86468464, val2, 0.0000001);
    }

    @Test
    public void testFloat() {
        final Float val1 = mapper.readObject("5.0964960", Float.class);
        final Float val2 = mapper.readObject("-886946.864", Float.TYPE);
        assertEquals(5.0964960, val1, 0.00001);
        assertEquals(-886946.864, val2, 0.02);
    }

    @Test
    public void testInteger() {
        final Integer val1 = mapper.readObject("450500", Integer.class);
        final Integer val2 = mapper.readObject("8984609", Integer.TYPE);
        assertEquals(450500, val1.intValue());
        assertEquals(8984609, val2.intValue());
    }

    @Test
    public void testLong() {
        final Long val1 = mapper.readObject("45050880", Long.class);
        final Long val2 = mapper.readObject("898464509", Long.TYPE);
        assertEquals(45050880L, val1.longValue());
        assertEquals(898464509L, val2.longValue());
    }

    @Test
    public void testShort() {
        final Short val1 = mapper.readObject("4505", Short.class);
        final Short val2 = mapper.readObject("4509", Short.TYPE);
        assertEquals((short) 4505, val1.shortValue());
        assertEquals((short) 4509, val2.shortValue());
    }
}
