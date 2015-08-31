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

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MapperGenericsTest {
    @Parameterized.Parameter
    public String accessMode;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<String> modes() {
        return asList("field", "method", "both", "strict-method");
    }

    @Test
    public void base() {
        final Mapper mapper = new MapperBuilder().setAccessModeName(accessMode).build();

        final Foo foo = new Foo();
        foo.name = "n";
        final Concrete concrete = new Concrete();
        concrete.value = foo;

        final String asString = mapper.writeObjectAsString(concrete);
        assertEquals("{\"value\":{\"name\":\"n\"}}", asString);
        assertEquals("n", Concrete.class.cast(mapper.readObject(new StringReader(asString), Concrete.class)).getValue().name);
    }

    @Test
    public void list() {
        final Mapper mapper = new MapperBuilder().setAccessModeName(accessMode).build();

        final Foo foo = new Foo();
        foo.name = "n";
        final ConcreteList concrete = new ConcreteList();
        concrete.value = singletonList(foo);

        final String asString = mapper.writeObjectAsString(concrete);
        assertEquals("{\"value\":[{\"name\":\"n\"}]}", asString);
        assertEquals("n", ConcreteList.class.cast(mapper.readObject(new StringReader(asString), ConcreteList.class)).getValue().iterator().next().name);
    }

    @Test
    public void map() {
        final Mapper mapper = new MapperBuilder().setAccessModeName(accessMode).build();

        final Foo foo = new Foo();
        foo.name = "n";

        final ConcreteMap concrete = new ConcreteMap();
        concrete.value = singletonMap("k", foo);

        final String asString = mapper.writeObjectAsString(concrete);
        assertEquals("{\"value\":{\"k\":{\"name\":\"n\"}}}", asString);
        assertEquals(concrete.value, ConcreteMap.class.cast(mapper.readObject(new StringReader(asString), ConcreteMap.class)).getValue());
    }

    public static abstract class Base<T> {
        protected T value;

        public void setValue(final T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }
    }

    public static abstract class BaseList<T> {
        protected List<T> value;

        public void setValue(final List<T> value) {
            this.value = value;
        }

        public List<T> getValue() {
            return value;
        }
    }

    public static abstract class BaseMap<T> {
        protected Map<String, T> value;

        public void setValue(final Map<String, T> value) {
            this.value = value;
        }

        public Map<String, T> getValue() {
            return value;
        }
    }

    public static class Concrete extends Base<Foo> {
    }

    public static class ConcreteList extends BaseList<Foo> {
    }

    public static class ConcreteMap extends BaseMap<Foo> {
    }

    public static class Foo {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Foo foo = (Foo) o;
            return name.equals(foo.name);

        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
