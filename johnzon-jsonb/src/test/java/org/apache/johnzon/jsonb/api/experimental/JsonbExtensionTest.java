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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
    public void listInAdapter() {
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

    @Test
    public void complexDeserializer() {
        final LocalDate date = LocalDate.of(2021, Month.valueOf("MAY"), 12);
        final LocalTime time = LocalTime.of(1, 2, 0, 0);
        final OffsetDateTime offsetDateTime = OffsetDateTime.of(date, time, ZoneOffset.UTC);
        final LocalDateTime localDateTime = LocalDateTime.of(date, time);

        final Sink2 attribs = new Sink2();
        attribs.attributes.put("ldateTime", new Wrapper2(singletonList(offsetDateTime)));
        attribs.attributes.put("llocalDate", new Wrapper2(singletonList(date)));
        attribs.attributes.put("llocalTime", new Wrapper2(singletonList(time)));
        attribs.attributes.put("llocalDateTime", new Wrapper2(singletonList(localDateTime)));

        final String json = jsonb.toJson(attribs);
        assertJsonEquals("" +
                        "{\"attributes\":{" +
                        "\"ldateTime\":{\"value\":[\"2021-05-12T01:02Z\"]}" +
                        ",\"llocalDate\":{\"value\":[\"2021-05-12\"]}" +
                        ",\"llocalDateTime\":{\"value\":[\"2021-05-12T01:02\"]}" +
                        ",\"llocalTime\":{\"value\":[\"01:02\"]}" +
                        "}}",
                json);

        final Sink2 deserialized = jsonb.fromJson(json, Sink2.class);
        assertEquals(attribs, deserialized);
    }

    @Test
    public void complexNoOpenCloseDeserializer() {
        final LocalDate date = LocalDate.of(2021, Month.valueOf("MAY"), 12);
        final LocalTime time = LocalTime.of(1, 2, 0, 0);
        final OffsetDateTime offsetDateTime = OffsetDateTime.of(date, time, ZoneOffset.UTC);
        final LocalDateTime localDateTime = LocalDateTime.of(date, time);

        final Sink3 attribs = new Sink3();
        attribs.attributes.put("ldateTime", new Wrapper3(singletonList(offsetDateTime)));
        attribs.attributes.put("llocalDate", new Wrapper3(singletonList(date)));
        attribs.attributes.put("llocalTime", new Wrapper3(singletonList(time)));
        attribs.attributes.put("llocalDateTime", new Wrapper3(singletonList(localDateTime)));

        final String json = jsonb.toJson(attribs);
        assertJsonEquals("" +
                        "{\"attributes\":{" +
                        "\"ldateTime\":{\"value\":[\"2021-05-12T01:02Z\"]}" +
                        ",\"llocalDate\":{\"value\":[\"2021-05-12\"]}" +
                        ",\"llocalDateTime\":{\"value\":[\"2021-05-12T01:02\"]}" +
                        ",\"llocalTime\":{\"value\":[\"01:02\"]}" +
                        "}}",
                json);

        final Sink3 deserialized = jsonb.fromJson(json, Sink3.class);
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

    @JsonbTypeSerializer(WrapperCodec.class)
    @JsonbTypeDeserializer(WrapperCodec.class)
    public static class Wrapper2 {
        public Object value;

        public Wrapper2() {
            // no-op
        }

        public Wrapper2(final Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public boolean equals(final Object obj) { // for test
            return Wrapper2.class.isInstance(obj) &&
                    // Object so deserialization of times will be a string
                    String.valueOf(Wrapper2.class.cast(obj).value).equals(String.valueOf(value));
        }

        @Override
        public int hashCode() { // for test
            return super.hashCode();
        }
    }

    public static class Sink2 implements Serializable {
        private Map<String, Wrapper2> attributes = new TreeMap<>();

        public Object get(final String name) {
            final Wrapper2 att = attributes.get(name);
            return att != null ? att.getValue() : null;
        }

        public void setAttributes(final Map<String, Wrapper2> attributes) {
            this.attributes = attributes;
        }

        public Map<String, Wrapper2> getAttributes() {
            return attributes;
        }

        @Override
        public boolean equals(final Object obj) { // for test
            return Sink2.class.isInstance(obj) && Objects.equals(Sink2.class.cast(obj).attributes, attributes);
        }

        @Override
        public int hashCode() { // for test
            return super.hashCode();
        }
    }

    @JsonbTypeSerializer(OpenWrapperCodec.class)
    @JsonbTypeDeserializer(OpenWrapperCodec.class)
    public static class Wrapper3 extends Wrapper2 {
        public Wrapper3() {
            super();
        }

        public Wrapper3(final Object value) {
            super(value);
        }
    }

    public static class Sink3 implements Serializable {
        private Map<String, Wrapper3> attributes = new TreeMap<>();

        public Object get(final String name) {
            final Wrapper3 att = attributes.get(name);
            return att != null ? att.getValue() : null;
        }

        public void setAttributes(final Map<String, Wrapper3> attributes) {
            this.attributes = attributes;
        }

        public Map<String, Wrapper3> getAttributes() {
            return attributes;
        }

        @Override
        public boolean equals(final Object obj) { // for test
            return Sink3.class.isInstance(obj) && Objects.equals(Sink3.class.cast(obj).attributes, attributes);
        }

        @Override
        public int hashCode() { // for test
            return super.hashCode();
        }
    }

    public static class OpenWrapperCodec extends WrapperCodec {
        @Override
        protected void afterList(final JsonGenerator generator) {
            // no-op
        }

        @Override
        protected void beforeList(final JsonGenerator generator) {
            // no-op
        }
    }

    public static class WrapperCodec implements JsonbDeserializer<Wrapper2>, JsonbSerializer<Wrapper2> {
        @Override
        public void serialize(final Wrapper2 wrapper, final JsonGenerator generator, final SerializationContext ctx) {
            final Object value = wrapper.getValue();
            if (value == null) {
                return;
            }
            if (value instanceof List) {
                final List<Object> list = (List<Object>) value;
                if (!list.isEmpty()) {
                    beforeList(generator);
                    writeArray(generator, list);
                    afterList(generator);
                }
            } else if (value instanceof String) {
                generator.write(markerFor(value), (String) value);
            } else {
                throw new IllegalArgumentException(value.toString());
            }
        }

        protected void afterList(final JsonGenerator generator) {
            generator.writeEnd();
        }

        protected void beforeList(final JsonGenerator generator) {
            generator.writeStartObject();
        }

        private void writeArray(final JsonGenerator generator, final List<Object> list) {
            generator.writeStartArray("a" + markerFor(list.get(0)));
            for (final Object o : list) {
                generator.write(o.toString());
            }
            generator.writeEnd();
        }

        private String markerFor(final Object value) {
            if (value instanceof String) {
                return "s";
            }
            if (value instanceof OffsetDateTime) {
                return "dt";
            }
            if (value instanceof LocalDate) {
                return "ld";
            }
            if (value instanceof LocalTime) {
                return "lt";
            }
            if (value instanceof LocalDateTime) {
                return "ldt";
            }
            throw new IllegalArgumentException(value.toString());
        }

        @Override
        public Wrapper2 deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
            if (parser.next() == JsonParser.Event.START_OBJECT &&
                    parser.next() == JsonParser.Event.KEY_NAME) {
                final String marker = parser.getString();
                final Wrapper2 wrapper = new Wrapper2(getValue(parser, marker));
                // close the object
                parser.next();
                if (wrapper != null) {
                    return wrapper;
                }
            }
            throw new IllegalStateException("Not a valid wrapper");
        }

        private Object getValue(final JsonParser parser, final String marker) {
            final JsonParser.Event valueType = parser.next();
            if (valueType == JsonParser.Event.VALUE_NULL) {
                return null;
            }
            if (valueType == JsonParser.Event.VALUE_STRING) {
                final String strVal = parser.getString();
                if ("s".equals(marker)) {
                    return strVal;
                }
                if ("dt".equals(marker)) {
                    return OffsetDateTime.parse(strVal);
                }
                if ("ld".equals(marker)) {
                    return LocalDate.parse(strVal);
                }
                if ("lt".equals(marker)) {
                    return LocalTime.parse(strVal);
                }
                if ("ldt".equals(marker)) {
                    return LocalDateTime.parse(strVal);
                }
                throw new IllegalStateException("Unknown type info: " + marker);
            }
            if (valueType == JsonParser.Event.VALUE_TRUE) {
                return Boolean.TRUE;
            }
            if (valueType == JsonParser.Event.VALUE_FALSE) {
                return Boolean.FALSE;
            }
            if (valueType == JsonParser.Event.START_ARRAY) {
                return parseArrayListJson(marker, parser);
            }
            if (valueType == JsonParser.Event.END_ARRAY) {
                return null;
            }
            throw new IllegalStateException("Unknown JSON valueType " + valueType);
        }

        private List<?> parseArrayListJson(final String arrayTypeInfo, final JsonParser parser) {
            if (!arrayTypeInfo.startsWith("a")) {
                throw new IllegalStateException("Unknown type: " + arrayTypeInfo);
            }
            final String typeInfo = arrayTypeInfo.substring(1);
            final List<Object> content = new ArrayList<>();
            do {
                final Object val = getValue(parser, typeInfo);
                if (val != null) {
                    content.add(val);
                } else {
                    break;
                }
            } while (true);
            return content;
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
