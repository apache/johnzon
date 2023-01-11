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

import static java.util.Arrays.asList;
import static java.util.Locale.ROOT;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.config.PropertyOrderStrategy;

import org.apache.johnzon.jsonb.api.experimental.PolymorphicConfig;
import org.junit.Test;

public class PolymorphicConfigTest {
    @Test
    public void roundTrip() throws Exception {
        final Dog dog = new Dog();
        dog.name = "wof";

        final Cat cat = new Cat();
        cat.otherName = "miaou";

        final Aggregate aggregate = new Aggregate();
        aggregate.animals = asList(dog, cat);

        final String aggJson =
                "{\"animals\":[{\"@type\":\"dog\",\"name\":\"wof\"},{\"@type\":\"cat\",\"otherName\":\"miaou\"}]}";
        final String dogJson = "{\"@type\":\"dog\",\"name\":\"wof\"}";
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
            .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL)
            .setProperty(PolymorphicConfig.class.getName(), new PolymorphicConfig()
                .withDiscriminator("@type")
                .withSerializationPredicate(c -> asList(Dog.class, Cat.class).contains(c)) // inline/bad/slow impl ok test
                .withDeserializationPredicate(c -> Animal.class == c)
                .withDiscriminatorMapper(c -> c.getSimpleName().toLowerCase(ROOT))
                .withTypeLoader(c -> {
                    switch (c) {
                        case "dog":
                            return Dog.class;
                        case "cat":
                            return Cat.class;
                        default:
                            throw new IllegalArgumentException(c);
                    }
                })))) {
            assertEquals(aggJson, jsonb.toJson(aggregate));
            assertEquals(aggregate, jsonb.fromJson(aggJson, Aggregate.class));
            assertEquals(dogJson, jsonb.toJson(dog));
            assertEquals(dog, jsonb.fromJson(dogJson, Dog.class));
        }
    }

    public interface Animal {
    }

    public static class Dog implements Animal {
        public String name;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Dog dog = (Dog) o;
            return Objects.equals(name, dog.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    public static class Cat implements Animal {
        public String otherName;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Cat cat = (Cat) o;
            return Objects.equals(otherName, cat.otherName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(otherName);
        }
    }

    public static class Aggregate {
        public List<Animal> animals;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Aggregate aggregate = (Aggregate) o;
            return animals.equals(aggregate.animals);
        }

        @Override
        public int hashCode() {
            return Objects.hash(animals);
        }
    }
}
