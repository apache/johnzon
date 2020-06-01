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

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.apache.johnzon.mapper.JohnzonAny;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

public class AnySupportTest {
    @Rule
    public final JsonbRule rule = new JsonbRule();

    @Test
    public void roundTrip() {
        final Foo foo = rule.fromJson("{\"a\":\"b\"}", Foo.class);
        assertEquals(singletonMap("a", "b"), foo.values);
        assertEquals("{\"a\":\"b\"}", rule.toJson(foo));
    }

    @Test
    public void subObject() {
        final Bar object = rule.fromJson("{\"a\":{\"b\":\"c\"}}", Bar.class);
        final Foo foo = new Foo();
        foo.values = singletonMap("b", "c");
        assertEquals(singletonMap("a", foo), object.values);
        assertEquals("{\"a\":{\"b\":\"c\"}}", rule.toJson(object));
    }

    @Test
    public void mixed() {
        final Mixed object = rule.fromJson("{\"explicit\":\"a\",\"bar\":\"dummy\"}", Mixed.class);
        assertEquals("a", object.explicit);
        assertEquals(singletonMap("bar", "dummy"), object.values);
        assertEquals("{\"explicit\":\"a\",\"bar\":\"dummy\"}", rule.toJson(object));
    }

    public static class Mixed {
        public String explicit;

        @JohnzonAny
        private Map<String, String> values;
    }

    public static class Foo {
        @JohnzonAny
        private Map<String, String> values;

        public Map<String, String> getValues() {
            return values;
        }

        public void setValues(final Map<String, String> values) {
            this.values = values;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Foo foo = (Foo) o;
            return values.equals(foo.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(values);
        }
    }

    public static class Bar {
        @JohnzonAny
        private Map<String, Foo> values;

        public Map<String, Foo> getValues() {
            return values;
        }

        public void setValues(final Map<String, Foo> values) {
            this.values = values;
        }
    }
}
