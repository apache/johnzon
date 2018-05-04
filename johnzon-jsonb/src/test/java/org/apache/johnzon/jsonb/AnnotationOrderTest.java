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

import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.config.PropertyNamingStrategy;

import static org.junit.Assert.assertEquals;

public class AnnotationOrderTest {
    /**
     * Test that @JsonbPropertyOrder takes java names and pushes at the end not mentionned properties.
     */
    @Test
    public void run() {
        final Person p = new Person();
        p.setPersonAge(12);
        p.setPersonName("David");
        p.setPersonGender("Male");
        assertEquals(
                "{\"person_gender\":\"Male\",\"person_name\":\"David\",\"person_age\":12}",
                JsonbBuilder.create(new JsonbConfig().withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES))
                        .toJson(p));
    }

    @JsonbPropertyOrder({"personGender", "personName"})
    public class Person {
        private String personName;
        private int personAge;
        private String personGender;

        public String getPersonName() {
            return personName;
        }

        public void setPersonName(String name) {
            this.personName = name;
        }

        public int getPersonAge() {
            return personAge;
        }

        public void setPersonAge(int age) {
            this.personAge = age;
        }

        public String getPersonGender() {
            return personGender;
        }

        public void setPersonGender(String personGender) {
            this.personGender = personGender;
        }
    }

}
