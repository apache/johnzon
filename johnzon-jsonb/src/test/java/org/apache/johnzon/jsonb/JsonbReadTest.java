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

import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.spi.JsonbProvider;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertEquals;

public class JsonbReadTest {
    @Test
    public void simple() {
        assertEquals("test", JsonbProvider.provider().create().build().fromJson(new StringReader("{\"value\":\"test\"}"), Simple.class).value);
    }

    @Test
    public void propertyMapping() {
        assertEquals("test", JsonbProvider.provider().create().build().fromJson(new StringReader("{\"simple\":\"test\"}"), SimpleProperty.class).value);
    }

    @Test
    public void date() { // ok, can fail at midnight, acceptable risk
        final String date = DateTimeFormatter.ofPattern("d. LLLL yyyy").format(LocalDate.now());
        assertEquals(
            LocalDate.now().getYear(),
            JsonbProvider.provider().create().build().fromJson(new StringReader("{\"date\":\"" + date + "\"}"), DateFormatting.class).date.getYear());
    }

    public static class Simple {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public static class SimpleProperty {
        @JsonbProperty("simple")
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public static class SimplePropertyNillable {
        @JsonbProperty(nillable = true)
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public static class DateFormatting {
        @JsonbDateFormat(value = "d. LLLL yyyy")
        @JsonbProperty(nillable = true)
        private LocalDate date;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(final LocalDate value) {
            this.date = value;
        }
    }
}
