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

        final Person from = new Person();
        from.name = "myname";
        from.street = "blastreet 1";
        from.description = "gets ignored";

        to.person = from;
        to.persons = singletonList(from);

        final Mapper mapper = new MapperBuilder().setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        }).build();
        assertEquals("{\"name\":\"to\",\"person\":{\"name\":\"myname\"},\"persons\":[{\"name\":\"myname\"}]}", mapper.writeObjectAsString(to));
    }

    public static class To {
        public String name;

        @JohnzonIgnoreNested(properties = {"street", "description"})
        public Person person;

        @JohnzonIgnoreNested(properties = {"street", "description"})
        public Collection<Person> persons;
    }

    public static class Person {
        public String name;
        public String street;
        public String description;
    }
}
