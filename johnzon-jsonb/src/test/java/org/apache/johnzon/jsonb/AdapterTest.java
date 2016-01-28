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
package org.apache.johnzon.jsonb;

import org.junit.Test;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;
import javax.json.bind.annotation.JsonbTypeAdapter;

import static org.junit.Assert.assertEquals;

public class AdapterTest {
    @Test
    public void adapt() {
        final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withAdapters(new BarAdapter()));
        final Foo foo = new Foo();
        foo.bar = new Bar();
        foo.bar.value = 1;
        foo.dummy = new Dummy();
        foo.dummy.value = 2L;

        final String toString = jsonb.toJson(foo);
        assertEquals("{\"bar\":\"1\",\"dummy\":\"2\"}", toString);

        final Foo read = jsonb.fromJson(toString, Foo.class);
        assertEquals(foo.bar.value, read.bar.value);
        assertEquals(foo.dummy.value, read.dummy.value);
    }

    public static class Foo {
        public Bar bar;

        @JsonbTypeAdapter(DummyAdapter.class)
        public Dummy dummy;
    }

    public static class Bar {
        public int value;
    }

    public static class Dummy {
        public long value;
    }

    public static class DummyAdapter implements JsonbAdapter<Dummy, String> {
        @Override
        public Dummy adaptTo(final String obj) throws Exception {
            final Dummy bar = new Dummy();
            bar.value = Long.parseLong(obj);
            return bar;
        }

        @Override
        public String adaptFrom(final Dummy obj) throws Exception {
            return Long.toString(obj.value);
        }
    }

    public static class BarAdapter implements JsonbAdapter<Bar, String> {
        @Override
        public Bar adaptTo(final String obj) throws Exception {
            final Bar bar = new Bar();
            bar.value = Integer.parseInt(obj);
            return bar;
        }

        @Override
        public String adaptFrom(final Bar obj) throws Exception {
            return Integer.toString(obj.value);
        }
    }
}
