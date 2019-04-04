/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.jsonb;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.adapter.JsonbAdapter;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.annotation.JsonbTypeSerializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.junit.Test;

public class SeriaizersRoundTripTest {
    
    public enum Color {
        
        RED, GREEN, BLUE
    }
       
    // Does not seem to work with enums
    public static class Option {
        
        public static final Option YES = new Option(true);
        public static final Option NO = new Option(false);
        
        private final boolean value;
        
        private Option(boolean value) {
            this.value = value;
        }

        public boolean asBoolean() {
            return value;
        }
        
        public static Option of(boolean value) {
            return value ? YES : NO;
        }

        @Override
        public String toString() {
            return "Option{value=" + value + '}';
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Option option = (Option) o;
            return value == option.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
    
    public static class VATNumber {
        
        private final long value;

        public VATNumber(long value) {
            this.value = value;
        }
        
        public long getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "VATNumber{value=" + value + '}';
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            VATNumber vatNumber = (VATNumber) o;
            return value == vatNumber.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
    
    public interface Composite<T, X> extends JsonbSerializer<T>, JsonbDeserializer<T>, JsonbAdapter<T, X> {}
    
    public static abstract class AbstractComposite<T, X> implements Composite<T, X> {}
    
    public static abstract class StringValueComposite<T> extends AbstractComposite<T, String> {}
    
    public static class UUIDComposite extends StringValueComposite<UUID> {
        
        public void serialize(UUID obj, JsonGenerator generator, SerializationContext ctx) {
            generator.write(obj.toString());
        }
        
        public UUID deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            return UUID.fromString(parser.getString());
        }

        @Override
        public UUID adaptFromJson(String obj) throws Exception {
            return UUID.fromString(obj);
        }
        
        @Override
        public String adaptToJson(UUID obj) throws Exception {
            return obj.toString();
        }
    }
    
    public static class OptionDeSer implements JsonbSerializer<Option>, JsonbDeserializer<Option> {

        @Override
        public Option deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            return Option.of(parser.getValue().equals(JsonValue.TRUE));
        }

        @Override
        public void serialize(Option obj, JsonGenerator generator, SerializationContext ctx) {
            generator.write(obj.asBoolean());
        }
    }
    
    public static class VATDeSer implements JsonbSerializer<VATNumber>, JsonbDeserializer<VATNumber> {

        @Override
        public VATNumber deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            return new VATNumber(parser.getLong());
        }

        @Override
        public void serialize(VATNumber obj, JsonGenerator generator, SerializationContext ctx) {
            generator.write(obj.getValue());
        }
    }
    
    public static class CharsDeSer implements JsonbSerializer<String>, JsonbDeserializer<String> {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            return parser.getArrayStream().map(JsonString.class::cast).map(JsonString::getString).collect(Collectors.joining());
        }

        @Override
        public void serialize(String obj, JsonGenerator generator, SerializationContext ctx) {
            generator.writeStartArray();
            obj.chars().forEach(c -> generator.write(Character.toString((char) c)));
            generator.writeEnd();
        }
    }
    
    public static class ColorDeSer implements JsonbSerializer<Color>, JsonbDeserializer<Color> {

        @Override
        public Color deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            switch (parser.getString()) {
                case "R" : return Color.RED;
                case "G" : return Color.GREEN;
                case "B" : return Color.BLUE;
                default : throw new IllegalArgumentException();
            }
        }

        @Override
        public void serialize(Color obj, JsonGenerator generator, SerializationContext ctx) {
            generator.write(obj.name().substring(0,  1));
        }
    }

    public static class Wrapper {
        
        @JsonbTypeSerializer(UUIDComposite.class)
        @JsonbTypeDeserializer(UUIDComposite.class)
        public UUID uuid;

        @JsonbTypeAdapter(UUIDComposite.class)
        public UUID uuid2;
        
        @JsonbTypeSerializer(OptionDeSer.class)
        @JsonbTypeDeserializer(OptionDeSer.class)
        public Option option;
        
        @JsonbTypeSerializer(VATDeSer.class)
        @JsonbTypeDeserializer(VATDeSer.class)
        public VATNumber vatNumber;

        @JsonbTypeSerializer(CharsDeSer.class)
        @JsonbTypeDeserializer(CharsDeSer.class)
        public String hello;

        @JsonbTypeSerializer(ColorDeSer.class)
        @JsonbTypeDeserializer(ColorDeSer.class)
        public Color color;

        @Override
        public String toString() {
            return "Wrapper{uuid=" + uuid + ", uuid2=" + uuid2 + ", option=" + option +
                    ", vatNumber=" + vatNumber + ", hello='" + hello + '\'' + ", color=" + color + '}';
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Wrapper wrapper = (Wrapper) o;
            return Objects.equals(uuid, wrapper.uuid) && Objects.equals(uuid2, wrapper.uuid2) && Objects.equals(option,
                    wrapper.option) && Objects.equals(vatNumber, wrapper.vatNumber) && Objects.equals(hello,
                    wrapper.hello) && color == wrapper.color;
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, uuid2, option, vatNumber, hello, color);
        }
    }

    @Test
    public void roundTrip() throws Exception {
        final Wrapper original = new Wrapper();
        original.uuid = UUID.randomUUID();
        original.uuid2 = UUID.randomUUID();
        original.option = Option.YES;
        original.vatNumber  = new VATNumber(42);
        original.hello = "hello world";
        original.color = Color.GREEN;

        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Wrapper deserialized = jsonb.fromJson(jsonb.toJson(original), Wrapper.class);
            assertEquals(original, deserialized);
        }
    }
}
