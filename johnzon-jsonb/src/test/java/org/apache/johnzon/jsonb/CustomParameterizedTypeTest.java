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

import jakarta.json.bind.spi.JsonbProvider;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CustomParameterizedTypeTest {
    @Test
    public void run() {
        final String value = "{\"val1\":{\"name\":\"the name\",\"age\":1234}}";
        final Object map = JsonbProvider.provider().create().build().fromJson(value, new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{String.class, Value.class};
            }

            @Override
            public Type getRawType() {
                return Map.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        });
        assertThat(map, instanceOf(Map.class));
        final Map<String, Value> asMap = Map.class.cast(map);
        assertTrue(asMap.containsKey("val1"));
        assertEquals(1, asMap.size());
        assertEquals(1234, asMap.get("val1").age);
        assertEquals("the name", asMap.get("val1").name);
    }

    public static class Value {
        private String name;
        private int age;

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return this.age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }
            Value other = (Value) obj;
            return Objects.equals(this.name, other.name)
                    && Objects.equals(this.age, other.age);
        }

        @Override
        public String toString() {
            return this.name + " " + this.age;
        }

    }

}
