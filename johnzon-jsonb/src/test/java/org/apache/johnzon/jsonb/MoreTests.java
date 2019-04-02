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

import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.UUID;

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

public class MoreTests {
    
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
    }
    
    public static class VATNumber {
        
        private final long value;

        public VATNumber(long value) {
            this.value = value;
        }
        
        public long getValue() {
            return value;
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

    public static class Wrapper {
        
        @JsonbTypeSerializer(UUIDComposite.class)
        @JsonbTypeDeserializer(UUIDComposite.class)
        public UUID uuid = UUID.randomUUID();

        @JsonbTypeAdapter(UUIDComposite.class)
        public UUID uuid2 = UUID.randomUUID();
        
        @JsonbTypeSerializer(OptionDeSer.class)
        @JsonbTypeDeserializer(OptionDeSer.class)
        public Option option = Option.YES;
        
        @JsonbTypeSerializer(VATDeSer.class)
        @JsonbTypeDeserializer(VATDeSer.class)
        public VATNumber vatNumber = new VATNumber(42);
        
    }

    @Test
    public void testIt() {
        Jsonb jsonb = JsonbBuilder.create();
        StringWriter w = new StringWriter();
        jsonb.toJson(new Wrapper(), w);
        jsonb.fromJson(w.toString(), Wrapper.class);
    }
}
