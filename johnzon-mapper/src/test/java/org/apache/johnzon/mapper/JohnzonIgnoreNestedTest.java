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

import java.util.Collection;
import java.util.Comparator;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class JohnzonIgnoreNestedTest {
    @Test
    public void ignoreNested() {
        final To to = new To();
        to.name = "to";

        final From from = new From();
        from.name = "from";

        to.from = from;
        to.froms = singletonList(from);
        from.to = to;
        from.tos = singletonList(to);

        final Mapper mapper = new MapperBuilder().setAttributeOrder(Comparator.naturalOrder()).build();
        assertEquals("{\"from\":{\"name\":\"from\"},\"froms\":[{\"name\":\"from\"}],\"name\":\"to\"}", mapper.writeObjectAsString(to));
        assertEquals("{\"name\":\"from\",\"to\":{\"name\":\"to\"},\"tos\":[{\"name\":\"to\"}]}", mapper.writeObjectAsString(from));
    }

    public static class To {
        public String name;

        @JohnzonIgnoreNested(properties = {"to", "tos"})
        public From from;

        @JohnzonIgnoreNested(properties = {"to", "tos"})
        public Collection<From> froms;
    }

    public static class From {
        public String name;

        @JohnzonIgnoreNested(properties = {"from", "froms"})
        public To to;

        @JohnzonIgnoreNested(properties = {"from", "froms"})
        public Collection<To> tos;
    }
}
