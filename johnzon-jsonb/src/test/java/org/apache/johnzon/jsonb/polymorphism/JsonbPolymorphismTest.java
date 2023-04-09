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
package org.apache.johnzon.jsonb.polymorphism;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.annotation.JsonbSubtype;
import jakarta.json.bind.annotation.JsonbTypeInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonbPolymorphismTest {

    private Jsonb jsonb = JsonbBuilder.create();

    @Test
    public void testSerialization() {
        Labrador labrador = new Labrador();
        labrador.dogAge = 3;
        labrador.labradorName = "john";

        assertEquals("{\"@animal\":\"dog\",\"@dog\":\"labrador\",\"dogAge\":3,\"labradorName\":\"john\"}",
                jsonb.toJson(labrador));
    }

    @Test
    public void testDeserialization() {
        Animal deserialized = jsonb.fromJson("{\"@animal\":\"dog\",\"@dog\":\"labrador\",\"dogAge\":3,\"labradorName\":\"john\"}", Animal.class);
        assertTrue(deserialized instanceof Labrador);
        assertEquals(3, ((Labrador) deserialized).dogAge);
        assertEquals("john", ((Labrador) deserialized).labradorName);
    }

    @Test
    public void testNoTypeInformationInJson() {
        Dog dog = jsonb.fromJson("{\"dogAge\":3}", Dog.class);

        assertEquals(3, dog.dogAge);
    }

    @JsonbTypeInfo(key = "@animal", value = @JsonbSubtype(alias = "dog", type = Dog.class))
    public interface Animal {
    }

    @JsonbTypeInfo(key = "@dog", value = @JsonbSubtype(alias = "labrador", type = Labrador.class))
    public static class Dog implements Animal {
        public int dogAge;
    }

    public static class Labrador extends Dog {
        public String labradorName;
    }
}
