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
package org.apache.johnzon.mapper.access;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class FieldAndMethodAccessModeTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return asList(
                new Object[]{
                        new FieldAndMethodAccessMode(true, true, false, true, false),
                        POJO.class,
                        singletonList("foo"),
                        singletonList("foo")
                },
                new Object[]{
                        new FieldAndMethodAccessMode(true, true, false, true, false),
                        POJOProtectedSetter.class,
                        singletonList("foo"),
                        emptyList()
                },
                new Object[]{
                        new FieldAndMethodAccessMode(true, true, false, true, false),
                        POJOProtectedGetter.class,
                        emptyList(),
                        singletonList("foo")
                },
                new Object[]{
                        new FieldAndMethodAccessMode(true, true, false, true, true),
                        POJO.class,
                        singletonList("foo"),
                        singletonList("foo")
                },
                new Object[]{
                        new FieldAndMethodAccessMode(true, true, false, true, true),
                        POJOProtectedSetter.class,
                        singletonList("foo"),
                        singletonList("foo")
                },
                new Object[]{
                        new FieldAndMethodAccessMode(true, true, false, true, true),
                        POJOProtectedGetter.class,
                        singletonList("foo"),
                        singletonList("foo")
                }
        );
    }

    private final FieldAndMethodAccessMode accessMode;
    private final Class<?> model;
    private final Collection<String> resultGetters;
    private final Collection<String> resultSetters;

    public FieldAndMethodAccessModeTest(final FieldAndMethodAccessMode accessMode,
                                        final Class<?> model,
                                        final List<String> resultGetters,
                                        final List<String> resultSetters) {
        this.accessMode = accessMode;
        this.model = model;
        this.resultGetters = new HashSet<>(resultGetters);
        this.resultSetters = new HashSet<>(resultSetters);
    }

    @Test
    public void getters() {
        assertEquals(resultGetters, accessMode.findReaders(model).keySet());
    }

    @Test
    public void setters() {
        assertEquals(resultSetters, accessMode.findWriters(model).keySet());
    }

    public static class POJO {
        private int foo;

        public int getFoo() {
            return foo;
        }

        public void setFoo(final int foo) {
            this.foo = foo;
        }
    }

    public static class POJOProtectedSetter {
        private int foo;

        public int getFoo() {
            return foo;
        }

        protected void setFoo(final int foo) {
            this.foo = foo;
        }
    }

    public static class POJOProtectedGetter {
        private int foo;

        protected int getFoo() {
            return foo;
        }

        public void setFoo(final int foo) {
            this.foo = foo;
        }
    }
}
