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

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbSubtype;
import jakarta.json.bind.annotation.JsonbTypeInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class JsonbPolymorphismValidationTest {

    @Rule public JsonbRule jsonb = new JsonbRule();

    @Test
    public void testMultipleParentsSerialization() {
        Dog dog = new Dog();

        JsonbException exception = assertThrows(JsonbException.class, () -> jsonb.toJson(dog));
        assertEquals("More than one interface/superclass of " +
                "org.apache.johnzon.jsonb.polymorphism.JsonbPolymorphismValidationTest$Dog" +
                " has JsonbTypeInfo Annotation", exception.getMessage());
    }

    @Test
    public void testMultipleParentsDeserialization() {
        String json = "{\"@animal\": \"dog\", \"@pet\": \"dog\"}";

        JsonbException exception = assertThrows(JsonbException.class, () -> jsonb.fromJson(json, Dog.class));
        assertEquals("More than one interface/superclass of " +
                "org.apache.johnzon.jsonb.polymorphism.JsonbPolymorphismValidationTest$Dog" +
                " has JsonbTypeInfo Annotation", exception.getMessage());
    }


    @Test
    public void testIncompatibleSubtypeSerialization() {
        InvalidSubTypeOther invalidSubTypeOther = new InvalidSubTypeOther();

        JsonbException exception = assertThrows(JsonbException.class, () -> jsonb.toJson(invalidSubTypeOther));
        assertEquals("JsonbSubtype 'invalid' (java.lang.String)" + " is not a subclass of class" +
                        " org.apache.johnzon.jsonb.polymorphism.JsonbPolymorphismValidationTest$InvalidSubTypeOther",
                exception.getMessage());
    }

    @Test
    public void testIncompatibleSubtypeDeserialization() {
        String json = "{\"@type\": \"invalid\"}";
        JsonbException exception = assertThrows(JsonbException.class, () -> jsonb.fromJson(json, InvalidSubTypeOther.class));

        assertEquals("JsonbSubtype 'invalid' (java.lang.String)" + " is not a subclass of class" +
                        " org.apache.johnzon.jsonb.polymorphism.JsonbPolymorphismValidationTest$InvalidSubTypeOther",
                exception.getMessage());
    }

    @Test
    public void testPropertyNameCollision() {
        Excavator excavator = new Excavator();
        excavator.type = "other";

        JsonbException exception = assertThrows(JsonbException.class, () -> jsonb.toJson(excavator));
        assertEquals("JsonbTypeInfo key 'type' collides with other properties in json", exception.getMessage());
    }

    @Test
    public void testTypeInfoKeyCollision() {
        JsonbException exception = assertThrows(JsonbException.class, () -> jsonb.toJson(new MyCar()));

        assertEquals("JsonbTypeInfo key '@type' found more than once in type hierarchy of class " +
                "org.apache.johnzon.jsonb.polymorphism.JsonbPolymorphismValidationTest$MyCar" +
                " (first defined in org.apache.johnzon.jsonb.polymorphism.JsonbPolymorphismValidationTest$Car," +
                " then defined again in org.apache.johnzon.jsonb.polymorphism.JsonbPolymorphismValidationTest$Vehicle)", exception.getMessage());
    }

    @Test
    public void testTypePropertyNotString() {
        JsonbException exception = assertThrows(JsonbException.class, () ->jsonb.fromJson("{\"@animal\": 42}", Animal.class));
        assertEquals("Property '@animal' isn't a String, resolving JsonbSubtype is impossible", exception.getMessage());
    }

    @Test
    public void testUnknownAlias() {
        JsonbException exception = assertThrows(JsonbException.class, () ->jsonb.fromJson("{\"@animal\": \"cat\"}", Animal.class));
        assertEquals("No JsonbSubtype found for alias 'cat' on" +
                " org.apache.johnzon.jsonb.polymorphism.JsonbPolymorphismValidationTest$Animal", exception.getMessage());
    }

    @JsonbTypeInfo(key = "@animal", value = @JsonbSubtype(alias = "dog", type = Dog.class))
    public interface Animal {
    }

    @JsonbTypeInfo(key = "pet", value = @JsonbSubtype(alias = "dog", type = Dog.class))
    public interface Pet {
    }

    public static final class Dog implements Animal, Pet {
    }

    @JsonbTypeInfo(@JsonbSubtype(alias = "invalid", type = String.class))
    public static final class InvalidSubTypeOther {
    }


    @JsonbTypeInfo(key = "type", value = @JsonbSubtype(alias = "excavator", type = Excavator.class))
    public static class Machine {
        public String type;
    }

    public static class Excavator extends Machine {
    }

    @JsonbTypeInfo(@JsonbSubtype(alias = "car", type = Car.class))
    public static class Vehicle {
    }

    @JsonbTypeInfo(@JsonbSubtype(alias = "myCar", type = MyCar.class))
    public static class Car extends Vehicle {
    }

    public static class MyCar extends Car {
    }
}
