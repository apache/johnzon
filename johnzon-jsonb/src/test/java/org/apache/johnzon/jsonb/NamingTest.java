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
import org.junit.Rule;
import org.junit.Test;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.config.PropertyNamingStrategy;
import java.util.StringJoiner;

import static org.junit.Assert.assertEquals;

public class NamingTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule()
            .withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES);

    @Test
    public void lower() {
        assertEquals("{\"first_name\":\"test\"}", jsonb.toJson(new Model("test")));
        assertEquals("Model[firstName='test']", jsonb.fromJson("{\"first_name\":\"test\"}", Model.class).toString());
    }

    public static class Model {
        public final String firstName;

        private Model(final String firstName) {
            this.firstName = firstName;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Model.class.getSimpleName() + "[", "]")
                    .add("firstName='" + firstName + "'")
                    .toString();
        }

        @JsonbCreator
        public static Model create(final String firstName) {
            return new Model(firstName);
        }
    }
}
