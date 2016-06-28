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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Comparator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MetaMapperTest {
    @Test
    public void customMappingAPI() {
        final Mapper mapper = new MapperBuilder().setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        }).setAccessModeName("field").build();

        final String expectedJson = "{\"id\":123456,\"name\":\"Johnzon Mapper\",\"overriden\":\"API Rocks\",\"set\":\"yes\"}";
        final User expectedUser = new User();
        expectedUser.setId(123456);
        expectedUser.setName("Johnzon Mapper");
        expectedUser.setName2("You will not see me");
        expectedUser.setCustom("API Rocks");
        expectedUser.setCustom2("yes");

        assertEquals(expectedJson, mapper.writeObjectAsString(expectedUser));

        final User u = mapper.readObject(expectedJson.substring(0, expectedJson.length() - 1) + ",\"name2\":\"should be null\"}", User.class);
        assertEquals(expectedUser.getId(), u.getId());
        assertEquals(expectedUser.getName(), u.getName());
        assertEquals(expectedUser.getCustom(), u.getCustom());
        assertNull(u.getName2());
    }

    public static class User {
        @Id
        private long id;

        @Name
        private String name;

        @Custom
        private String custom;

        @Custom("set")
        private String custom2;

        @Ignored
        private String name2;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getCustom() {
            return custom;
        }

        public void setCustom(final String custom) {
            this.custom = custom;
        }

        public String getName2() {
            return name2;
        }

        public void setName2(final String name2) {
            this.name2 = name2;
        }

        public String getCustom2() {
            return custom2;
        }

        public void setCustom2(final String custom2) {
            this.custom2 = custom2;
        }
    }

    @Target(FIELD)
    @Retention(RUNTIME)
    @JohnzonProperty("id")
    public @interface Id {
    }

    @Target(FIELD)
    @Retention(RUNTIME)
    @JohnzonIgnore
    public @interface Ignored {
    }

    @Target(FIELD)
    @Retention(RUNTIME)
    @JohnzonProperty("name")
    public @interface Name {
    }

    @Target(FIELD)
    @Retention(RUNTIME)
    @JohnzonProperty("ignored")
    public @interface Custom {
        String value() default "overriden";
    }
}
