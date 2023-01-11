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
import org.apache.johnzon.mapper.JohnzonRecord;
import org.junit.Rule;
import org.junit.Test;

import jakarta.json.bind.annotation.JsonbProperty;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class JsonbRecordTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    @Test
    public void roundTrip() {
        final Record ref = new Record(119, "Santa");
        final String expectedJson = "{\"_name\":\"Santa\",\"age\":119}";
        assertEquals(expectedJson, jsonb.toJson(ref));
        assertEquals(ref, jsonb.fromJson(expectedJson, Record.class));
    }

    @JohnzonRecord
    public static class Record {
        private final int age;
        private final String name;

        public Record(@JohnzonRecord.Name("age") final int age,
                           @JohnzonRecord.Name("name") @JsonbProperty("_name") final String name) { // simulate custom constructor
            this.age = age;
            this.name = name;
        }

        public int age() {
            return age;
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return "Record{" +
                    "age=" + age +
                    ", name='" + name + '\'' +
                    '}';
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Record record = Record.class.cast(o);
            return age == record.age && Objects.equals(name, record.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(age, name);
        }
    }
}
