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
package org.apache.johnzon.jsonb.api.experimental;

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;
import org.junit.Rule;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

public class JsonbExtensionTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    private final JsonObject defaultValue = Json.createObjectBuilder()
            .add("foo", "ok")
            .add("bar", 1)
            .build();

    @Test
    public void fromJsonValue() {
        final Value value = jsonb.fromJsonValue(defaultValue, Value.class);
        assertEquals("ok", value.foo);
        assertEquals(1, value.bar);
    }

    @Test
    public void fromJsonValue2() {
        final JsonValue json = Json.createArrayBuilder()
                .add(defaultValue)
                .add(Json.createObjectBuilder()
                        .add("foo", "still ok")
                        .add("bar", 2)
                        .build())
                .build();
        final List<Value> values = jsonb.fromJsonValue(json, new JohnzonParameterizedType(Collection.class, Value.class));
        assertEquals(2, values.size());
        {
            final Value value = values.get(0);
            assertEquals("ok", value.foo);
            assertEquals(1, value.bar);
        }
        {
            final Value value = values.get(1);
            assertEquals("still ok", value.foo);
            assertEquals(2, value.bar);
        }
    }

    @Test
    public void toJsonValue() {
        assertEquals(defaultValue, jsonb.toJsonValue(new Value("ok", 1)));
    }

    @Test
    public void toJsonValue2() {
        assertEquals(defaultValue, jsonb.toJsonValue(new Value("ok", 1), Value.class));
    }

    @Test
    public void localTime() {
        final LocalDate date = LocalDate.of(2021, Month.valueOf("MAY"), 12);
        final LocalTime time = LocalTime.of(1, 2, 0, 0);
        final OffsetDateTime offsetDateTime = OffsetDateTime.of(date, time, ZoneOffset.UTC);
        final LocalDateTime localDateTime = LocalDateTime.of(date, time);

        final Sink attribs = new Sink();
        attribs.add("ldateTime", singletonList(offsetDateTime));
        attribs.add("llocalDate", singletonList(date));
        attribs.add("llocalTime", singletonList(time));
        attribs.add("llocalDateTime", singletonList(localDateTime));

        final String json = jsonb.toJson(attribs);
        assertJsonEquals("" +
                        "{\"attributes\":{" +
                        "\"ldateTime\":{\"value\":[\"2021-05-12T01:02Z\"]}" +
                        ",\"llocalDate\":{\"value\":[\"2021-05-12\"]}" +
                        ",\"llocalDateTime\":{\"value\":[\"2021-05-12T01:02\"]}" +
                        ",\"llocalTime\":{\"value\":[\"01:02\"]}" +
                        "}}",
                json);

        final Sink deserialized = jsonb.fromJson(json, Sink.class);
        assertEquals(attribs, deserialized);
    }

    // assumes json are valid but enables nicer diff
    private void assertJsonEquals(final String expected, final String actual) {
        final JsonWriterFactory writerFactory = Json.createWriterFactory(singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        final StringWriter s1 = new StringWriter();
        final StringWriter s2 = new StringWriter();
        try (final JsonReader r1 = Json.createReader(new StringReader(expected));
             final JsonReader r2 = Json.createReader(new StringReader(expected));
             final JsonWriter w1 = writerFactory.createWriter(s1);
             final JsonWriter w2 = writerFactory.createWriter(s2)) {
            w1.write(r1.readValue());
            w2.write(r2.readValue());
        }
        assertEquals(s1.toString(), s2.toString());
    }

    public static class Wrapper {
        public Object value;

        public Wrapper() {
            // no-op
        }

        public Wrapper(final Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public boolean equals(final Object obj) { // for test
            return Wrapper.class.isInstance(obj) &&
                    // Object so deserialization of times will be a string
                    String.valueOf(Wrapper.class.cast(obj).value).equals(String.valueOf(value));
        }

        @Override
        public int hashCode() { // for test
            return super.hashCode();
        }
    }

    public static class Sink implements Serializable {
        private Map<String, Wrapper> attributes = new TreeMap<>();

        public Sink add(final String name, final Object value) {
            attributes.put(name, new Wrapper(value));
            return this;
        }

        public Object get(final String name) {
            final Wrapper att = attributes.get(name);
            return att != null ? att.getValue() : null;
        }

        public void setAttributes(final Map<String, Wrapper> attributes) {
            this.attributes = attributes;
        }

        public Map<String, Wrapper> getAttributes() {
            return attributes;
        }

        @Override
        public boolean equals(final Object obj) { // for test
            return Sink.class.isInstance(obj) && Objects.equals(Sink.class.cast(obj).attributes, attributes);
        }

        @Override
        public int hashCode() { // for test
            return super.hashCode();
        }
    }

    public static class Value {
        public String foo;
        public int bar;

        public Value() {
            // no-op
        }

        public Value(final String foo, final int bar) {
            this.foo = foo;
            this.bar = bar;
        }
    }
}
