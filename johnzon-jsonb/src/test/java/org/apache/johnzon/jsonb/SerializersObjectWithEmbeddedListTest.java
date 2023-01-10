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

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SerializersObjectWithEmbeddedListTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule().withFormatting(true);

    @Test
    public void serializeTest() throws Exception {
        ObjectModel objectModel = new ObjectModel();
        objectModel.embeddedList.add("Text1");
        objectModel.embeddedList.add("Text2");
        objectModel.otherField = "Other Text";

        WrapperModel wrapper = new WrapperModel();
        wrapper.object = objectModel;

        assertEquals("" +
                "{\n" +
                "  \"object\":{\n" +
                "    \"embeddedList\":[\n" +
                "      \"Text1\",\n" +
                "      \"Text2\"\n" +
                "    ],\n" +
                "    \"otherField\":\"Other Text\",\n" +
                "    \"otherField2\":\"Other Text\",\n" +
                "    \"embeddedList2\":[\n" +
                "      \"Text1\",\n" +
                "      \"Text2\"\n" +
                "    ],\n" +
                "    \"otherField3\":\"Other Text\"\n" +
                "  }\n" +
                "}" +
                "", jsonb.toJson(wrapper));
    }

    public static class WrapperModel {
        public ObjectModel object;
    }

    @JsonbTypeSerializer(ObjectDeSer.class)
    public static class ObjectModel {
        public List<String> embeddedList = new ArrayList<>();
        public String otherField;
    }

    public static class ObjectDeSer implements JsonbSerializer<ObjectModel> {
        @Override
        public void serialize(final ObjectModel obj, final JsonGenerator generator, final SerializationContext ctx) {
            ctx.serialize("embeddedList", obj.embeddedList, generator);
            ctx.serialize("otherField", obj.otherField, generator);
            ctx.serialize("otherField2", obj.otherField, generator);
            ctx.serialize("embeddedList2", obj.embeddedList, generator);
            ctx.serialize("otherField3", obj.otherField, generator);
        }
    }
}
