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

import static org.junit.Assert.assertEquals;

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

public class HidingPublicFieldTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    @Test
    public void hidden() {
        final Model model = new Model();
        model.value = "something";
        assertEquals("{}", jsonb.toJson(model));
    }

    public static class Model {
        public String value;

        /**
         * As of section 3.7.1 of the JSON-B spec a private method 'hides' fields
         * The rules are as following:
         * <ul>
         *     <li>if public setter/getter exists -> take that</li>
         *     <li>if non public setter/getter exists -> ignore</li>
         *     <li>OTHERWISE (no setter/getter at all) -> use fields</li>
         * </ul>
         */
        private String getValue() {
            return value;
        }

        private void setValue(final String value) {
            this.value = value;
        }
    }
}
