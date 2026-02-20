/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.jsonb.serializer;

import java.lang.reflect.Type;

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.annotation.JsonbTypeSerializer;
import javax.json.bind.config.PropertyOrderStrategy;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import static org.junit.Assert.assertTrue;

/**
 * This test checks a JsonbSerialize/JsonbDeserialize roundtrip when using a primitive as placeholder
 */
public class SerialiseAsPrimitiveTest {

    @Rule
    public final JsonbRule jsonb = new JsonbRule()
            .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL);


    public static class TestConstant {
        public final static TestConstant VAL_1 = new TestConstant("A");
        public final static TestConstant VAL_2 = new TestConstant("B");
        public final static TestConstant VAL_3 = new TestConstant("C");

        private final String code;

        public TestConstant(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static TestConstant getByCode(String code) {
            switch (code) {
                case "A": return VAL_1;
                case "B": return VAL_2;
                case "C": return VAL_3;
                default: return null;
            }
        }
    }
    
    public static class ConstantUsage {
        private int i;

        @JsonbTypeSerializer(ConstantJsonbSerialiser.class)
        @JsonbTypeDeserializer(ConstantJsonbDeserializer.class)
        private TestConstant testConstant;

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public TestConstant getTestConstant() {
            return testConstant;
        }

        public void setTestConstant(TestConstant testEnum) {
            this.testConstant = testEnum;
        }
    }

    public static class ConstantJsonbSerialiser implements JsonbSerializer<TestConstant> {
        @Override
        public void serialize(TestConstant val, JsonGenerator generator, SerializationContext ctx) {
            if (val == null) {
                ctx.serialize(null, generator);
            } else {
                ctx.serialize(val.getCode(), generator);
            }
        }
    }

    public static class ConstantJsonbDeserializer implements JsonbDeserializer<TestConstant> {

        @Override
        public TestConstant deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            if (rtType instanceof TestConstant) {
                final String key = parser.getString();
                if (key != null) {
                    return TestConstant.getByCode(key);
                }
            }

            return null;
        }
    }



    @Test
    public void testEnumJsonb() throws Exception {
        ConstantUsage enumVerwender = new ConstantUsage();
        enumVerwender.setI(1);
        enumVerwender.setTestConstant(TestConstant.VAL_2);
        
        final String json = jsonb.toJson(enumVerwender);
        assertTrue(json.contains("\"testConstant\":\"B\""));
    }

}
