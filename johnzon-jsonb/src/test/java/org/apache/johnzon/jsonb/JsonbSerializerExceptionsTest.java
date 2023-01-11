/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.johnzon.jsonb;

import org.junit.Test;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JsonbSerializerExceptionsTest {

    private static final RuntimeException SERIALIZE_EXCEPTION = new RuntimeException("I am user, hear me roar");
    private static final RuntimeException DESERIALIZE_EXCEPTION = new RuntimeException("I am user, hear me roar");
    private static final RuntimeException CONSTRUCTOR_EXCEPTION = new RuntimeException("I am user, hear me roar");

    @Test
    public void serializeRuntimeException() {

        final Object object = new WidgetWithFailedSerializer(new Color("red"));

        ExceptionAsserts.toJson(object)
                // TODO Review: shouldn't this be a JsonbException?
                .assertSame(SERIALIZE_EXCEPTION);
    }

    @Test
    public void deserializeRuntimeException() {
        final String json = "{\"color\":{\"red\":2550,\"green\":0,\"blue\":0}}";

        ExceptionAsserts.fromJson(json, WidgetWithFailedSerializer.class)
                .assertInstanceOf(JsonbException.class)
                .assertCauseChain(DESERIALIZE_EXCEPTION)
                .assertMessage("WidgetWithFailedSerializer property 'color' of type Color cannot be" +
                        " mapped to json object value: {\"red\":2550,\"green\":...\n" +
                        "I am user, hear me roar");
    }

    @Test
    public void deserializeConstructorRuntimeException() {
        final String json = "{\"color\":{\"red\":2550,\"green\":0,\"blue\":0}}";

        ExceptionAsserts.fromJson(json, WidgetWithFailedConstructorSerializer.class)
                // TODO Review: shouldn't this be wrapped in a JsonbException?
                .assertSame(CONSTRUCTOR_EXCEPTION);
    }

    @Test
    public void serializeConstructorRuntimeException() {

        final Object object = new WidgetWithFailedConstructorSerializer(new Color("red"));

        ExceptionAsserts.toJson(object)
                // TODO Review: shouldn't this be wrapped in a JsonbException?
                .assertSame(CONSTRUCTOR_EXCEPTION);
    }

    @Test
    public void deserializeReturnNull() throws Exception {
        final String json = "{\"color\":{\"red\":2550,\"green\":0,\"blue\":0}}";

        final JsonbConfig config = new JsonbConfig();
        config.setProperty("johnzon.snippetMaxLength", 20);

        final WidgetWithReturnNullDeserializer widget = ((Callable<WidgetWithReturnNullDeserializer>) () -> {
            try (final Jsonb jsonb = JsonbBuilder.create(config)) {
                return jsonb.fromJson(json, WidgetWithReturnNullDeserializer.class);
            }
        }).call();

        assertNotNull(widget);
        assertNull(widget.getColor());
    }


    public static class FailedSerializer implements JsonbSerializer<Color>, JsonbDeserializer<Color> {
        @Override
        public Color deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext, final Type type) {
            throw DESERIALIZE_EXCEPTION;
        }

        @Override
        public void serialize(final Color color, final JsonGenerator jsonGenerator, final SerializationContext serializationContext) {
            throw SERIALIZE_EXCEPTION;
        }
    }

    public static class FailedConstructorSerializer implements JsonbSerializer<Color>, JsonbDeserializer<Color> {
        public FailedConstructorSerializer() throws Exception {
            throw CONSTRUCTOR_EXCEPTION;
        }

        @Override
        public Color deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext, final Type type) {
            return null;
        }

        @Override
        public void serialize(final Color color, final JsonGenerator jsonGenerator, final SerializationContext serializationContext) {

        }
    }

    public static class ReturnNullDeserializer implements JsonbDeserializer<Color> {
        @Override
        public Color deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext, final Type type) {
            return null;
        }
    }

    public static class WidgetWithReturnNullDeserializer {
        @JsonbTypeDeserializer(ReturnNullDeserializer.class)
        private Color color;

        public WidgetWithReturnNullDeserializer() {
        }

        public WidgetWithReturnNullDeserializer(final Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(final Color color) {
            this.color = color;
        }
    }

    public static class WidgetWithFailedConstructorSerializer {
        @JsonbTypeDeserializer(FailedConstructorSerializer.class)
        @JsonbTypeSerializer(FailedConstructorSerializer.class)
        private Color color;

        public WidgetWithFailedConstructorSerializer() {
        }

        public WidgetWithFailedConstructorSerializer(final Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(final Color color) {
            this.color = color;
        }
    }

    public static class WidgetWithFailedSerializer {
        @JsonbTypeDeserializer(FailedSerializer.class)
        @JsonbTypeSerializer(FailedSerializer.class)
        private Color color;

        public WidgetWithFailedSerializer() {
        }

        public WidgetWithFailedSerializer(final Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(final Color color) {
            this.color = color;
        }
    }

    public static class Color {
        private final String name;

        public Color(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
