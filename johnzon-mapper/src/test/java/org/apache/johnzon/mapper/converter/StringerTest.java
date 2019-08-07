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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.JohnzonConverter;
import org.apache.johnzon.mapper.MapperBuilder;
import org.junit.Test;

public class StringerTest {
    private static final String STRING = "hello johnzon";
    private static final byte[] BYTES = STRING.getBytes(StandardCharsets.UTF_8);

    @Test
    public void testSerialize() {
        final Holder datatype = new Holder(BYTES);
        final String json = new MapperBuilder().build().writeObjectAsString(datatype);
        assertEquals("{\"data\":\"" + STRING + "\"}", json);
    }

    @Test
    public void testDeserialize() {
        final String json = "{\"data\":\"" + STRING + "\"}";
        final Holder datatype = new MapperBuilder().build().readObject(json, Holder.class);
        assertArrayEquals(BYTES, datatype.data);
    }

    public static class Stringer implements Adapter<byte[], String> {
        @Override
        public byte[] to(final String b) {
            return b.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String from(final byte[] a) {
            return new String(a, StandardCharsets.UTF_8);
        }
    }


    public static class Holder {
        @JohnzonConverter(Stringer.class)
        public byte[] data;

        public Holder() {
            // no-op
        }

        public Holder(byte[] data) {
            this.data = data;
        }
    }

}
