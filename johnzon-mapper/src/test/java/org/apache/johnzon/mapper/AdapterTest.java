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

import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AdapterTest {
    @Test
    public void run() {
        final Mapper mapper = new MapperBuilder()
            .setAccessModeName("field")
            .addAdapter(new SimpleAdapter())
            .setAttributeOrder(new Comparator<String>() {
                @Override
                public int compare(final String o1, final String o2) {
                    return o1.compareTo(o2);
                }
            }).build();

        final String asString = "{\"foo\":{\"count\":2,\"simple\":\"yeah\"}}";
        assertEquals(asString, mapper.writeObjectAsString(new Root()));

        final Root root = mapper.readObject(asString, Root.class);
        assertNotNull(root.foo);
        assertEquals(2, root.foo.bar.count);
        assertEquals("yeah", root.foo.bar.simple);
    }

    public static class SimpleAdapter implements Adapter<Foo, Bar> {
        @Override
        public Foo to(final Bar bar) {
            final Foo foo = new Foo();
            foo.bar = bar;
            return foo;
        }

        @Override
        public Bar from(final Foo foo) {
            return new Bar("yeah", 2);
        }
    }

    public static class Root {
        Foo foo = new Foo();
    }

    public static class Foo {
        @JohnzonIgnore
        private Bar bar;
    }

    public static class Bar {
        String simple;
        int count;

        public Bar() {
            // no-op
        }

        public Bar(final String simple, final int count) {
            this.simple = simple;
            this.count = count;
        }
    }
}
