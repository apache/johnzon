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

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.annotation.JsonbTypeSerializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.apache.johnzon.jsonb.model.Holder;
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

    @Test
    public void typeSerializer() {
        final HolderHolder container = new HolderHolder();
        final StringHolder instance = new StringHolder();
        instance.setInstance("Test String");
        container.setInstance(instance);

        final String json = jsonb.toJson(container);
        assertTrue(json, json.matches(
                "\\{\\s*\"instance\"\\s*:\\s*\\{\\s*\"instance\"\\s*:\\s*\"Test String Serialized\"\\s*}\\s*}"));

        final HolderHolder unmarshalledObject = jsonb.fromJson("{ \"instance\" : { \"instance\" : \"Test String\" } }", HolderHolder.class);
        assertEquals("Test String Deserialized", unmarshalledObject.getInstance().getInstance());
    }

    @Test
    public void arrayTypes() {
        final ArrayHolder container = new ArrayHolder();
        final StringHolder instance1 = new StringHolder();
        instance1.setInstance("Test String 1");
        final StringHolder instance2 = new StringHolder();
        instance2.setInstance("Test String 2");
        container.setInstance(new StringHolder[] { instance1, instance2 });

        final String json = jsonb.toJson(container);
        assertEquals("{\"instance\":[{\"instance\":\"Test String 1\"},{\"instance\":\"Test String 2\"}]}", json);

        final ArrayHolder unmarshalledObject = jsonb.fromJson(
                "{ \"instance\" : [ { \"instance\" : \"Test String 1\" }, { \"instance\" : \"Test String 2\" } ] }",
                ArrayHolder.class);
        assertEquals("Test String 1", unmarshalledObject.getInstance()[0].getInstance());
    }

    public static class StringHolder implements Holder<String> {
        private String instance = "Test";

        public String getInstance() {
            return instance;
        }

        public void setInstance(final String instance) {
            this.instance = instance;
        }
    }

    public static class SimpleContainerDeserializer implements JsonbDeserializer<StringHolder> {
        @Override
        public StringHolder deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
            final StringHolder container = new StringHolder();

            while (parser.hasNext()) {
                final JsonParser.Event event = parser.next();
                if (event == JsonParser.Event.START_OBJECT) {
                    continue;
                }
                if (event == JsonParser.Event.END_OBJECT) {
                    break;
                }
                if (event == JsonParser.Event.KEY_NAME && "instance".equals(parser.getString())) {
                    container.setInstance(ctx.deserialize(String.class, parser) + " Deserialized");
                }
            }

            return container;
        }
    }

    public static class SimpleContainerSerializer implements JsonbSerializer<StringHolder> {
        @Override
        public void serialize(final StringHolder container, final JsonGenerator generator,
                              final SerializationContext ctx) {
            generator.writeStartObject();
            ctx.serialize("instance", container.getInstance() + " Serialized", generator);
            generator.writeEnd();
        }
    }

    public static class HolderHolder implements Holder<StringHolder> {
        @JsonbTypeSerializer(SimpleContainerSerializer.class)
        @JsonbTypeDeserializer(SimpleContainerDeserializer.class)
        private StringHolder instance;

        @Override
        public StringHolder getInstance() {
            return instance;
        }

        @Override
        public void setInstance(StringHolder instance) {
            this.instance = instance;
        }
    }

    public static class ArrayHolder implements Holder<StringHolder[]> {
        @JsonbTypeSerializer(StringArraySerializer.class)
        @JsonbTypeDeserializer(StringArrayDeserializer.class)
        private StringHolder[] instance;

        @Override
        public StringHolder[] getInstance() {
            return instance;
        }

        @Override
        public void setInstance(final StringHolder[] instance) {
            this.instance = instance;
        }
    }

    public static class StringArraySerializer implements JsonbSerializer<StringHolder[]> {
        @Override
        public void serialize(final StringHolder[] containers,
                              final JsonGenerator jsonGenerator,
                              final SerializationContext serializationContext) {
            jsonGenerator.writeStartArray();
            for (final StringHolder container : containers) {
                serializationContext.serialize(container, jsonGenerator);
            }
            jsonGenerator.writeEnd();
        }
    }

    public static class StringArrayDeserializer implements JsonbDeserializer<StringHolder[]> {
        @Override
        public StringHolder[] deserialize(final JsonParser jsonParser,
                                          final DeserializationContext deserializationContext,
                                          final Type type) {
            final List<StringHolder> containers = new LinkedList<>();

            while (jsonParser.hasNext()) {
                JsonParser.Event event = jsonParser.next();
                if (event == JsonParser.Event.START_OBJECT) {
                    containers.add(deserializationContext.deserialize(
                            new StringHolder() {}.getClass().getGenericSuperclass(), jsonParser));
                }
                if (event == JsonParser.Event.END_OBJECT) {
                    break;
                }
            }

            return containers.toArray(new StringHolder[0]);
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
