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
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

import org.junit.Test;

public class SerializersRoundTripTest {
    
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
    
    public static class ArrayContainer {
        // native types
        private boolean[] bboolean;
        private byte[] bbyte;
        private char[] bchar;
        private short[] bshort;
        private int[] bint;
        private long[] blong;
        private float[] bfloat;
        private double[] bdouble;
        
        // string[]
        private String[] bString;
        
        // wrapper types
        private Boolean[] bWboolean;
        private Byte[] bWbyte;
        private Character[] bWchar;
        private Short[] bWshort;
        private Integer[] bWint;
        private Long[] bWlong;
        private Float[] bWfloat;
        private Double[] bWdouble;

        public boolean[] getBboolean() {
            return bboolean;
        }

        public void setBboolean(boolean[] bboolean) {
            this.bboolean = bboolean;
        }

        public byte[] getBbyte() {
            return bbyte;
        }

        public void setBbyte(byte[] bbyte) {
            this.bbyte = bbyte;
        }

        public char[] getBchar() {
            return bchar;
        }

        public void setBchar(char[] bchar) {
            this.bchar = bchar;
        }

        public short[] getBshort() {
            return bshort;
        }

        public void setBshort(short[] bshort) {
            this.bshort = bshort;
        }

        public int[] getBint() {
            return bint;
        }

        public void setBint(int[] bint) {
            this.bint = bint;
        }

        public long[] getBlong() {
            return blong;
        }

        public void setBlong(long[] blong) {
            this.blong = blong;
        }

        public float[] getBfloat() {
            return bfloat;
        }

        public void setBfloat(float[] bfloat) {
            this.bfloat = bfloat;
        }

        public double[] getBdouble() {
            return bdouble;
        }

        public void setBdouble(double[] bdouble) {
            this.bdouble = bdouble;
        }

        public String[] getbString() {
            return bString;
        }

        public void setbString(String[] bString) {
            this.bString = bString;
        }

        public Boolean[] getbWboolean() {
            return bWboolean;
        }

        public void setbWboolean(Boolean[] bWboolean) {
            this.bWboolean = bWboolean;
        }

        public Byte[] getbWbyte() {
            return bWbyte;
        }

        public void setbWbyte(Byte[] bWbyte) {
            this.bWbyte = bWbyte;
        }

        public Character[] getbWchar() {
            return bWchar;
        }

        public void setbWchar(Character[] bWchar) {
            this.bWchar = bWchar;
        }

        public Short[] getbWshort() {
            return bWshort;
        }

        public void setbWshort(Short[] bWshort) {
            this.bWshort = bWshort;
        }

        public Integer[] getbWint() {
            return bWint;
        }

        public void setbWint(Integer[] bWint) {
            this.bWint = bWint;
        }

        public Long[] getbWlong() {
            return bWlong;
        }

        public void setbWlong(Long[] bWlong) {
            this.bWlong = bWlong;
        }

        public Float[] getbWfloat() {
            return bWfloat;
        }

        public void setbWfloat(Float[] bWfloat) {
            this.bWfloat = bWfloat;
        }

        public Double[] getbWdouble() {
            return bWdouble;
        }

        public void setbWdouble(Double[] bWdouble) {
            this.bWdouble = bWdouble;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ArrayContainer that = (ArrayContainer) o;

            if (!Arrays.equals(bboolean, that.bboolean)) {
                return false;
            }
            if (!Arrays.equals(bbyte, that.bbyte)) {
                return false;
            }
            if (!Arrays.equals(bchar, that.bchar)) {
                return false;
            }
            if (!Arrays.equals(bshort, that.bshort)) {
                return false;
            }
            if (!Arrays.equals(bint, that.bint)) {
                return false;
            }
            if (!Arrays.equals(blong, that.blong)) {
                return false;
            }
            if (!Arrays.equals(bfloat, that.bfloat)) {
                return false;
            }
            if (!Arrays.equals(bdouble, that.bdouble)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(bString, that.bString)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(bWboolean, that.bWboolean)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(bWbyte, that.bWbyte)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(bWchar, that.bWchar)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(bWshort, that.bWshort)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(bWint, that.bWint)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(bWlong, that.bWlong)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(bWfloat, that.bWfloat)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            return Arrays.equals(bWdouble, that.bWdouble);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(bboolean);
            result = 31 * result + Arrays.hashCode(bbyte);
            result = 31 * result + Arrays.hashCode(bchar);
            result = 31 * result + Arrays.hashCode(bshort);
            result = 31 * result + Arrays.hashCode(bint);
            result = 31 * result + Arrays.hashCode(blong);
            result = 31 * result + Arrays.hashCode(bfloat);
            result = 31 * result + Arrays.hashCode(bdouble);
            result = 31 * result + Arrays.hashCode(bString);
            result = 31 * result + Arrays.hashCode(bWboolean);
            result = 31 * result + Arrays.hashCode(bWbyte);
            result = 31 * result + Arrays.hashCode(bWchar);
            result = 31 * result + Arrays.hashCode(bWshort);
            result = 31 * result + Arrays.hashCode(bWint);
            result = 31 * result + Arrays.hashCode(bWlong);
            result = 31 * result + Arrays.hashCode(bWfloat);
            result = 31 * result + Arrays.hashCode(bWdouble);
            return result;
        }
    }

    @Test
    public void testArrayRoundTrip() throws Exception {
        ArrayContainer original = new ArrayContainer();
        original.setBboolean(new boolean[]{true, false, true});
        original.setbWboolean(new Boolean[]{true, false, true});
        
        original.setBbyte(new byte[]{0x00, 0x01, 0x02});
        original.setbWbyte(new Byte[]{0x00, 0x01, 0x02});
        
        original.setBchar(new char[]{'a','b', 'c'});
        original.setbWchar(new Character[]{'a','b', 'c'});

        original.setBshort(new short[]{0, 1, 2});
        original.setbWshort(new Short[]{0, 1, 2});

        original.setBint(new int[]{0, 1, 2});
        original.setbWint(new Integer[]{0, 1, 2});

        original.setBlong(new long[]{0L, 1L, 2L});
        original.setbWlong(new Long[]{0L, 1L, 2L});

        original.setBfloat(new float[]{0f, 1f, 2f});
        original.setbWfloat(new Float[]{0f, 1f, 2f});

        original.setBdouble(new double[]{0d, 1d, 2d});
        original.setbWdouble(new Double[]{0d, 1d, 2d});

        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final ArrayContainer deserialized = jsonb.fromJson(jsonb.toJson(original), ArrayContainer.class);
            assertEquals(original, deserialized);
        }

    }

    @Test
    public void roundTrip() throws Exception {
        final Wrapper original = new Wrapper();
        original.hello = "hello world";
        /*original.uuid = UUID.randomUUID();
        original.uuid2 = UUID.randomUUID();
        original.option = Option.YES;
        original.vatNumber  = new VATNumber(42);
        original.color = Color.GREEN;*/

        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Wrapper deserialized = jsonb.fromJson(jsonb.toJson(original), Wrapper.class);
            assertEquals(original, deserialized);
        }
    }
}
