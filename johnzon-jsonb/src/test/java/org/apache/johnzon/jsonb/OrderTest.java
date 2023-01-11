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
import static org.junit.Assert.assertTrue;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

public class OrderTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    @Test
    public void partial() {
        final String jsonb = this.jsonb.toJson(new PartialModel());
        assertTrue(jsonb, jsonb.matches(
                "\\{\\s*\"third\"\\s*:\\s*\"Third\"\\s*,\\s*\"fourth\"\\s*:\\s*\"Fourth\".*}"));
    }

    @Test // TODO: not sure it is good to respect json and not mapping, we can challenge the spec on it
    public void deserializationRespectsOrderToo() {
        final String json = this.jsonb.toJson(new PartialOrder() {{ setStringInstance("Test String"); }});
        assertEquals("{\"longInstance\":0,\"intInstance\":0,\"stringInstance\":\"Test String\",\"anIntInstance\":0,\"anotherIntInstance\":0,\"yetAnotherIntInstance\":0}", json);
        assertTrue(json, json.contains("anotherIntInstance"));
        assertTrue(json, json.contains("anIntInstance"));
        assertTrue(json, json.contains("yetAnotherIntInstance"));

        final PartialOrder unmarshalledObject = jsonb.fromJson(
                "{ \"anIntInstance\" : 100, \"yetAnotherIntInstance\":100, \"anotherIntInstance\": 100, " +
                        "\"intInstance\" : 1, \"stringInstance\" : \"Test String\", \"longInstance\" : 0 }",
                PartialOrder.class);
        assertEquals(3, unmarshalledObject.getIntInstance());
        assertEquals(100, unmarshalledObject.getAnotherIntInstance());
        assertEquals(100, unmarshalledObject.getYetAnotherIntInstance());
        assertEquals(100, unmarshalledObject.getAnIntInstance());
    }

    @JsonbPropertyOrder({ "longInstance", "intInstance", "stringInstance" })
    public static class PartialOrder {
        private int intInstance;

        private String stringInstance;

        private long longInstance;

        private int anIntInstance;

        private int anotherIntInstance;

        private int yetAnotherIntInstance;

        public int getAnIntInstance() {
            intInstance -= 10;
            return anIntInstance;
        }

        public void setAnIntInstance(int anIntInstance) {
            intInstance -= 30;
            this.anIntInstance = anIntInstance;
        }

        public int getAnotherIntInstance() {
            intInstance -= 100;
            return anotherIntInstance;
        }

        public void setAnotherIntInstance(int anotherIntInstance) {
            intInstance -= 300;
            this.anotherIntInstance = anotherIntInstance;
        }

        public int getYetAnotherIntInstance() {
            intInstance -= 1000;
            return yetAnotherIntInstance;
        }

        public void setYetAnotherIntInstance(int yetAnotherIntInstance) {
            intInstance -= 3000;
            this.yetAnotherIntInstance = yetAnotherIntInstance;
        }

        public String getStringInstance() {
            return stringInstance;
        }

        public void setStringInstance(String stringInstance) {
            this.stringInstance = stringInstance;
            if (intInstance == 1) {
                intInstance = 2;
            }
        }

        public int getIntInstance() {
            return intInstance;
        }

        public void setIntInstance(int intInstance) {
            this.intInstance = intInstance;
        }

        public long getLongInstance() {
            return longInstance;
        }

        public void setLongInstance(long longInstance) {
            this.longInstance = longInstance;
            if (intInstance == 2) {
                intInstance = 3;
            }
        }
    }

    @JsonbPropertyOrder({ "third", "fourth" })
    public class PartialModel {
        private String first = "First";

        private String second = "Second";

        private String third = "Third";

        private String fourth = "Fourth";

        private String anyOther = "Fifth String property starting with A";

        public String getThird() {
            return third;
        }

        public void setThird(final String third) {
            this.third = third;
        }

        public String getFourth() {
            return fourth;
        }

        public void setFourth(final String fourth) {
            this.fourth = fourth;
        }

        public String getAnyOther() {
            return anyOther;
        }

        public void setAnyOther(final String anyOther) {
            this.anyOther = anyOther;
        }

        public String getFirst() {
            return first;
        }

        public void setFirst(final String first) {
            this.first = first;
        }

        public String getSecond() {
            return second;
        }

        public void setSecond(final String second) {
            this.second = second;
        }
    }
}
