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

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.config.PropertyOrderStrategy;
import jakarta.json.bind.spi.JsonbProvider;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class JsonbWriteTest {
    @Test
    public void rawAdapter() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withAdapters(new JsonbAdapter<SimpleProperty, String>() {
                    @Override
                    public String adaptToJson(final SimpleProperty obj) {
                        return obj.getValue();
                    }

                    @Override
                    public SimpleProperty adaptFromJson(final String obj) {
                        throw new UnsupportedOperationException();
                    }
                }))) {
            final SimpleProperty property = new SimpleProperty();
            property.setValue("ok");
            final String json = jsonb.toJson(property, Throwable.class);
            assertEquals("\"ok\"", json);
        }
    }

    @Test
    public void throwable() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))){
            final IllegalStateException exception = new IllegalStateException("oops");
            exception.setStackTrace(new StackTraceElement[]{
                    new StackTraceElement("foo", "bar", "dummy", 1),
                    new StackTraceElement("foo2", "bar2", "dummy2", 2)
            });
            final String json = jsonb.toJson(exception, Throwable.class);
            final Throwable throwable = jsonb.fromJson(json, Throwable.class);
            assertEquals(exception.getMessage(), throwable.getMessage());
            assertArrayEquals(exception.getStackTrace(), throwable.getStackTrace());
            assertEquals("{\"message\":\"oops\"," +
                    "\"stackTrace\":[{\"className\":\"foo\",\"fileName\":\"dummy\",\"lineNumber\":1,\"methodName\":\"bar\"}," +
                    "{\"className\":\"foo2\",\"fileName\":\"dummy2\",\"lineNumber\":2,\"methodName\":\"bar2\"}]}", json);
        }
    }

    @Test
    public void mapOfSimple() throws Exception {
        final Map<String, Simple> list = new TreeMap<>();
        list.put("1", new Simple());
        list.put("2", new Simple());
        try (final Jsonb jsonb = JsonbBuilder.create()){
            assertEquals("{\"1\":{},\"2\":{}}", jsonb.toJson(list, Map.class));
        }
    }

    @Test
    public void listOfSimple() throws Exception {
        final List<Simple> list = new ArrayList<>();
        list.add(new Simple());
        list.add(new Simple());
        try (final Jsonb jsonb = JsonbBuilder.create()){
            assertEquals("[{},{}]", jsonb.toJson(list, List.class));
        }
    }

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

    public static class CustomException extends RuntimeException {
        private final int code;

        @JsonbCreator
        public CustomException(final String message, final int code) {
            super(message);
            this.code = code;
        }
    }
}
