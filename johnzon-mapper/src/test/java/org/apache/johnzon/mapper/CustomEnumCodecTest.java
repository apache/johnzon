/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.mapper;

import org.junit.Test;

import java.io.StringReader;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class CustomEnumCodecTest {
    @Test
    public void roundTrip() {
        final Mapper mapper = new MapperBuilder().addAdapter(E.class, String.class, new EConverter()).build();
        final String json = "{\"e\":\"a\"}";
        final EHolder holder = mapper.readObject(json, EHolder.class);
        assertEquals(E.A, holder.e);
        assertEquals(json, mapper.writeObjectAsString(holder));
    }

    @Test
    public void roundTripArray() {
        final Mapper mapper = new MapperBuilder().addAdapter(E.class, String.class, new EConverter()).build();
        final String json = "[\"b\"]";
        final E[] es = mapper.readArray(new StringReader(json), E.class);
        assertEquals(1, es.length);
        assertEquals(E.B, es[0]);
        assertEquals(json, mapper.writeArrayAsString(es));
    }

    public static class EHolder {
        public E e;
    }

    public enum E {
        A, B
    }

    public static class EConverter implements Adapter<E, String> {
        @Override
        public String from(final E instance) {
            return instance.name().toLowerCase(Locale.ENGLISH);
        }

        @Override
        public E to(final String text) {
            return E.valueOf(text.toUpperCase(Locale.ENGLISH));
        }
    }
}
