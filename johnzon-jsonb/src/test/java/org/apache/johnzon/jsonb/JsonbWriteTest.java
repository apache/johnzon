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

import javax.json.bind.Jsonb;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.spi.JsonbProvider;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class JsonbWriteTest {
    @Test
    public void boolAsString() {
        assertEquals("true", JsonbProvider.provider().create().build().toJson(Boolean.TRUE));
    }

    @Test
    public void boolAsStringWriter() {
        StringWriter sw = new StringWriter();
        JsonbProvider.provider().create().build().toJson(Boolean.TRUE,sw);
        assertEquals("true", sw.toString());
    }

    @Test
    public void boolAsStream() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonbProvider.provider().create().build().toJson(Boolean.TRUE, baos);
        assertEquals("true", baos.toString());
    }

    @Test
    public void boolAsStreamInObject() {
        SimpleBool simple = new SimpleBool();
        simple.setBool(Boolean.TRUE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonbProvider.provider().create().build().toJson(simple, baos);
        assertEquals("{\"bool\":true}", baos.toString());
    }

    @Test
    public void simple() {
        final Simple simple = new Simple();
        simple.setValue("test");
        assertEquals("{\"value\":\"test\"}", JsonbProvider.provider().create().build().toJson(simple));
    }

    @Test
    public void propertyMapping() {
        final SimpleProperty simple = new SimpleProperty();
        simple.setValue("test");
        assertEquals("{\"simple\":\"test\"}", JsonbProvider.provider().create().build().toJson(simple));
    }

    @Test
    public void map() {
        final Map<String, String> map = new HashMap<>();
        map.put("a", "b");
        assertEquals("{\"a\":\"b\"}", JsonbProvider.provider().create().build().toJson(map));
    }

    @Test
    public void list() {
        final Collection<String> map = asList("a", "b");
        assertEquals("[\"a\",\"b\"]", JsonbProvider.provider().create().build().toJson(map));
    }

    @Test
    public void propertyMappingNotNillable() {
        final SimpleProperty simple = new SimpleProperty();
        assertEquals("{}", JsonbProvider.provider().create().build().toJson(simple));
    }

    @Test
    public void propertyNillable() {
        final SimplePropertyNillable simple = new SimplePropertyNillable();
        assertEquals("{\"value\":null}", JsonbProvider.provider().create().build().toJson(simple));
    }

    @Test
    public void date() {
        final DateFormatting simple = new DateFormatting();
        simple.setDate(LocalDateTime.now());
        final Jsonb build = JsonbProvider.provider().create().build();
        final int currentYear = LocalDateTime.now().getYear();
        assertEquals("{\"date\":\"" + currentYear + "\"}", build.toJson(simple));
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

    public static class SimpleBool {
        private Boolean bool;

        public Boolean getBool() {
            return bool;
        }

        public SimpleBool setBool(Boolean bool) {
            this.bool = bool;
            return this;
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
        @JsonbDateFormat(value = "yyyy")
        @JsonbProperty(nillable = true)
        private LocalDateTime date;

        public LocalDateTime getDate() {
            return date;
        }

        public void setDate(final LocalDateTime value) {
            this.date = value;
        }
    }


}
