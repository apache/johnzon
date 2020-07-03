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

import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;
import org.junit.Test;

import java.util.Collection;

import static junit.framework.TestCase.assertEquals;

public class MapingsKeyCacheForParameterizedTypesTest {
    // before the fix: java.lang.ClassCastException:
    // MapingsKeyCacheForParameterizedTypesTest$Salmon cannot be cast to MapingsKeyCacheForParameterizedTypesTest$Fish
    @Test
    public void ensureParameterizedTypeDontEndUpOnClass() {
        try (final Mapper mapper = new MapperBuilder().build()) {
            final Foo<Salmon> salmons = mapper.readObject("{\"bars\":[{}]}", new JohnzonParameterizedType(Foo.class, Salmon.class));
            assertEquals(Salmon.class, salmons.bars.iterator().next().getClass());
            final Foo<Fish> fishes = mapper.readObject("{\"bars\":[{}]}", new JohnzonParameterizedType(Foo.class, Fish.class));
            assertEquals(Fish.class, fishes.bars.iterator().next().getClass());
        }
    }

    public static class Foo<Bar> {
        public Collection<Bar> bars;
    }

    public static class Salmon /*NO INHERITANCE*/ {
    }

    public static class Fish /*NO INHERITANCE*/ {
    }
}
