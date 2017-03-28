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
package org.apache.johnzon.core;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonCollectors;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static java.util.function.Function.identity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonCollectorTest {


    @Test
    public void testToJsonArray() {

        JsonArray array = IntStream.rangeClosed(1, 5)
                                   .mapToObj(i -> Json.createObjectBuilder()
                                                      .add("key" + i, "value" + i)
                                                      .build())
                                   .collect(JsonCollectors.toJsonArray());

        assertNotNull(array);
        assertEquals(5, array.size());
        IntStream.rangeClosed(1, 5)
                 .forEach(i -> assertEquals("value" + i, array.getJsonObject(i - 1)
                                                              .getString("key" + i)));
    }

    @Test
    public void testToJsonObject() {

        SortedMap<String, JsonValue> source = new TreeMap<>(String::compareTo);
        source.put("a", Json.createValue("string"));
        source.put("b", Json.createObjectBuilder()
                            .add("key", "value")
                            .build());
        source.put("c", Json.createArrayBuilder()
                            .add("c1")
                            .add("c2")
                            .build());

        JsonObject jsonObject = source.entrySet()
                                      .stream()
                                      .collect(JsonCollectors.toJsonObject());

        assertNotNull(jsonObject);
        assertEquals(Json.createObjectBuilder()
                         .add("a", "string")
                         .add("b", Json.createObjectBuilder()
                                       .add("key", "value"))
                         .add("c", Json.createArrayBuilder()
                                       .add("c1")
                                       .add("c2"))
                         .build(),
                     jsonObject);
    }

    @Test
    public void testToJsonObjectCustomKeyAndValueMapper() {

        List<JsonValue> source = new ArrayList<>();
        source.add(Json.createValue("string"));
        source.add(Json.createObjectBuilder()
                       .add("key", "value")
                       .build());
        source.add(Json.createArrayBuilder()
                       .add("c1")
                       .add("c2")
                       .build());

        JsonObject jsonObject = source.stream()
                                      .collect(JsonCollectors.toJsonObject(v -> v.getValueType().toString(),
                                                                           identity()));
        assertNotNull(jsonObject);
        assertEquals(Json.createObjectBuilder()
                         .add(ValueType.STRING.name(), "string")
                         .add(ValueType.OBJECT.name(), Json.createObjectBuilder()
                                                           .add("key", "value"))
                         .add(ValueType.ARRAY.name(), Json.createArrayBuilder()
                                                          .add("c1")
                                                          .add("c2"))
                         .build()
                , jsonObject);
    }

    @Test
    public void testGroupingByJsonObject() {

        JsonArray expectedStrings = Json.createArrayBuilder()
                                        .add("string1")
                                        .add("string2")
                                        .add("string3")
                                        .build();

        JsonArray expectedObjects = Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                 .add("1", "value"))
                                        .add(Json.createObjectBuilder()
                                                 .add("2", "value"))
                                        .add(Json.createObjectBuilder()
                                                 .add("3", "value"))
                                        .build();

        JsonArray expectedArrays = Json.createArrayBuilder()
                                       .add(Json.createArrayBuilder()
                                                .add("1"))
                                       .add(Json.createArrayBuilder()
                                                .add("2"))
                                       .add(Json.createArrayBuilder()
                                                .add("3"))
                                       .build();

        List<JsonValue> source = new ArrayList<>();
        source.add(Json.createValue("string1"));
        source.add(Json.createValue("string2"));
        source.add(Json.createValue("string3"));

        source.add(Json.createObjectBuilder()
                       .add("1", "value")
                       .build());
        source.add(Json.createObjectBuilder()
                       .add("2", "value")
                       .build());
        source.add(Json.createObjectBuilder()
                       .add("3", "value")
                       .build());

        source.add(Json.createArrayBuilder()
                       .add("1")
                       .build());
        source.add(Json.createArrayBuilder()
                       .add("2")
                       .build());
        source.add(Json.createArrayBuilder()
                       .add("3")
                       .build());


        JsonObject jsonObject = source.stream()
                                      .collect(JsonCollectors.groupingBy(v -> v.getValueType().name()));
        assertNotNull(jsonObject);
        assertEquals(jsonObject.getJsonArray(ValueType.ARRAY.name()), expectedArrays);
        assertEquals(jsonObject.getJsonArray(ValueType.OBJECT.name()), expectedObjects);
        assertEquals(jsonObject.getJsonArray(ValueType.STRING.name()), expectedStrings);
    }

}
