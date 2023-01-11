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

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.config.PropertyOrderStrategy;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

import org.apache.johnzon.jsonb.model.Holder;
import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

public class SerializerTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule()
            .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL);

    @Test // https://issues.apache.org/jira/browse/JOHNZON-335
    public void testNestedSerializer() {
        final String s = jsonb.toJson(new OuterTestModel());
        assertEquals("{\"foo\":\"generated in outer serializer\",\"inner\":{\"bar\":\"generated in inner serializer\"}}", s);
    }

    @Test
    public void passthroughSerializer() {
        final NameHolder nameHolder = new NameHolder();
        nameHolder.name = new Named();
        nameHolder.name.name = "Test String";
        nameHolder.name.detailName = new DetailName();
        nameHolder.name.detailName.name = "Another Test String";
        assertEquals(
                "{\"detailName\":{\"name\":\"Another Test String\",\"detail\":true},\"name\":{\"name\":\"Test String\"}}",
                jsonb.toJson(nameHolder));

    }

    @Test
    public void typeSerializer() {
        final HolderHolder container = new HolderHolder();
        final StringHolder instance = new StringHolder();
        instance.setInstance("Test String");
        container.setInstance(instance);

        final String json = jsonb.toJson(container);
        assertTrue(json, json.matches(
                "\\{\\s*\"instance\"\\s*:\\s*\\{\\s*\"instance\"\\s*:\\s*\"Test String Serialized\"\\s*}\\s*}"));

        final HolderHolder unmarshalledObject = jsonb.fromJson("{ \"instance\" : { \"instance\" : \"Test String\" } }", HolderHolder.class);
        assertEquals("Test String Deserialized", unmarshalledObject.getInstance().getInstance());
    }

    @Test
    public void arrayTypes() {
        final ArrayHolder container = new ArrayHolder();
        final StringHolder instance1 = new StringHolder();
        instance1.setInstance("Test String 1");
        final StringHolder instance2 = new StringHolder();
        instance2.setInstance("Test String 2");
        container.setInstance(new StringHolder[] { instance1, instance2 });

        final String json = jsonb.toJson(container);
        assertEquals("{\"instance\":[{\"instance\":\"Test String 1\"},{\"instance\":\"Test String 2\"}]}", json);

        final ArrayHolder unmarshalledObject = jsonb.fromJson(
                "{ \"instance\" : [ { \"instance\" : \"Test String 1\" }, { \"instance\" : \"Test String 2\" } ] }",
                ArrayHolder.class);
        assertEquals("Test String 1", unmarshalledObject.getInstance()[0].getInstance());
    }


    @Test
    public void roundTrip() {
        final Jsonb jsonb = JsonbBuilder.create();

        final String expectedJson = "{\"foo\":{\"full\":true,\"name\":\"SerializerTest\"},\"moreFoos\":[{\"full\":true,\"name\":\"foo2\"},{\"full\":true,\"name\":\"foo3\"}]}";

        final Foo foo = new Foo();
        foo.name = "SerializerTest";
        final Wrapper wrapper = new Wrapper();
        wrapper.foo = foo;

        Foo foo2 = new Foo();
        foo2.name = "foo2";
        Foo foo3 = new Foo();
        foo3.name = "foo3";
        wrapper.moreFoos.add(foo2);
        wrapper.moreFoos.add(foo3);

        assertEquals(expectedJson, jsonb.toJson(wrapper));

        final Wrapper deser = jsonb.fromJson(expectedJson, Wrapper.class);
        assertEquals(foo.name, deser.foo.name);
        assertEquals(foo.name.length(), deser.foo.value);
        assertTrue(deser.foo.flag);

        assertEquals(2, deser.moreFoos.size());
        assertEquals("foo2", deser.moreFoos.get(0).name);
        assertEquals(4, deser.moreFoos.get(0).value);
        assertEquals(4, deser.moreFoos.get(1).value);
    }

    @Test
    public void nullValuesInList() {
        Jsonb jsonb = JsonbBuilder.create();

        IntList wrapper = new IntList();
        wrapper.elems.add(null);
        assertEquals("{\"elems\":[null]}", jsonb.toJson(wrapper));

        wrapper.elems.add(1);
        assertEquals("{\"elems\":[null,1]}", jsonb.toJson(wrapper));

        wrapper.elems.add(0, 2);
        assertEquals("{\"elems\":[2,null,1]}", jsonb.toJson(wrapper));

        wrapper.elems.remove(2);
        assertEquals("{\"elems\":[2,null]}", jsonb.toJson(wrapper));
    }

    @Test
    public void nullValuesInEnumList() {
        Jsonb jsonb = JsonbBuilder.create();

        EnumList wrapper = new EnumList();
        wrapper.elems.add(null);
        assertEquals("{\"elems\":[null]}", jsonb.toJson(wrapper));

        wrapper.elems.add(MyStatus.OK);
        assertEquals("{\"elems\":[null,\"OK\"]}", jsonb.toJson(wrapper));

        wrapper.elems.add(0, MyStatus.WRONG);
        assertEquals("{\"elems\":[\"WRONG\",null,\"OK\"]}", jsonb.toJson(wrapper));

        wrapper.elems.remove(2);
        assertEquals("{\"elems\":[\"WRONG\",null]}", jsonb.toJson(wrapper));
    }

    @Test
    public void uuid() throws Exception {
        final Jsonb jsonb = JsonbBuilder.create();
        final UUIDWrapper wrapper = new UUIDWrapper();
        wrapper.uuid = UUID.randomUUID();
        assertEquals("{\"uuid\":\"" + wrapper.uuid + "\"}", jsonb.toJson(wrapper));
        jsonb.close();
    }

    @Test
    public void serializeWithKey() throws Exception {
        final Jsonb jsonb = JsonbBuilder.create();
        final MyWrapper wrapper = new MyWrapper();
        wrapper.myWrapper = new MyWrapper();
        wrapper.myWrapper.map = singletonMap("a", "b");
        assertEquals("{\"myWrapper\":{\"a\":\"b\"}}", jsonb.toJson(wrapper));
        jsonb.close();
    }

    @Test
    public void fromConfig() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new AnimalSerializer()).withDeserializers(new AnimalDeserializer()))) {
            final Animals animals = new Animals();
            animals.animals.add(new Cat(5, "Garfield", 10.5f, true, true));
            animals.animals.add(new Dog(3, "Milo", 5.5f, false, true));
            animals.animals.add(new Animal(6, "Tweety", 0.5f, false));

            final String jsonString = jsonb.toJson(animals);
            assertEquals("{\"animals\":[" +
                    "{\"type\":\"cat\",\"cuddly\":true,\"age\":5,\"furry\":true,\"name\":\"Garfield\",\"weight\":10.5}," +
                    "{\"type\":\"dog\",\"barking\":true,\"age\":3,\"furry\":false,\"name\":\"Milo\",\"weight\":5.5}," +
                    "{\"type\":\"animal\",\"age\":6,\"furry\":false,\"name\":\"Tweety\",\"weight\":0.5}]}",
                    jsonString);

            final Animals deserialized = jsonb.fromJson("{ \"animals\" : [ "
                    + "{ \"type\" : \"cat\", \"cuddly\" : true, \"age\" : 5, \"furry\" : true, \"name\" : \"Garfield\" , \"weight\" : 10.5}, "
                    + "{ \"type\" : \"dog\", \"barking\" : true, \"age\" : 3, \"furry\" : false, \"name\" : \"Milo\", \"weight\" : 5.5}, "
                    + "{ \"type\" : \"animal\", \"age\" : 6, \"furry\" : false, \"name\" : \"Tweety\", \"weight\" : 0.5}"
                    + " ] }", Animals.class);
            assertEquals(animals.animals, deserialized.animals);
        }
    }

    @Test
    public void fromAnnotation() {
        final AnimalsAnnotation animals = new AnimalsAnnotation();
        animals.animals.add(new Cat(5, "Garfield", 10.5f, true, true));
        animals.animals.add(new Dog(3, "Milo", 5.5f, false, true));
        animals.animals.add(new Animal(6, "Tweety", 0.5f, false));

        final String jsonString = jsonb.toJson(animals);
        assertEquals("{\"animals\":[" +
                "{\"type\":\"cat\",\"cuddly\":true,\"age\":5,\"furry\":true,\"name\":\"Garfield\",\"weight\":10.5}," +
                "{\"type\":\"dog\",\"barking\":true,\"age\":3,\"furry\":false,\"name\":\"Milo\",\"weight\":5.5}," +
                "{\"type\":\"animal\",\"age\":6,\"furry\":false,\"name\":\"Tweety\",\"weight\":0.5}]}", jsonString);

        final AnimalsAnnotation deserialized = jsonb.fromJson("{ \"animals\" : [ "
                + "{ \"type\" : \"cat\", \"cuddly\" : true, \"age\" : 5, \"furry\" : true, \"name\" : \"Garfield\" , \"weight\" : 10.5}, "
                + "{ \"type\" : \"dog\", \"barking\" : true, \"age\" : 3, \"furry\" : false, \"name\" : \"Milo\", \"weight\" : 5.5}, "
                + "{ \"type\" : \"animal\", \"age\" : 6, \"furry\" : false, \"name\" : \"Tweety\", \"weight\" : 0.5}"
                + " ] }", AnimalsAnnotation.class);
        assertEquals(animals.animals, deserialized.animals);
    }

    public static class Animal {
        public int age;
        public String name;
        public float weight;
        public boolean furry;

        public Animal() {
            // no-op
        }

        public Animal(int age, String name, float weight, boolean furry) {
            this.age = age;
            this.name = name;
            this.weight = weight;
            this.furry = furry;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Animal animal = (Animal) o;
            return age == animal.age &&
                    Float.compare(animal.weight, weight) == 0 &&
                    furry == animal.furry &&
                    Objects.equals(name, animal.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(age, name, weight, furry);
        }
    }

    public static class Dog extends Animal {
        public boolean barking;

        public Dog() {
            // no-op
        }

        public Dog(int age, String name, float weight, boolean furry,
                   boolean barking) {
            super(age, name, weight, furry);
            this.barking = barking;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            final Dog dog = (Dog) o;
            return barking == dog.barking;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), barking);
        }
    }

    public static class Cat extends Animal {
        public boolean cuddly;

        public Cat() {
            // no-op
        }

        public Cat(int age, String name, float weight, boolean furry,
                   boolean cuddly) {
            super(age, name, weight, furry);
            this.cuddly = cuddly;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            final Cat cat = (Cat) o;
            return cuddly == cat.cuddly;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), cuddly);
        }
    }

    public static class AnimalListDeserializer implements JsonbDeserializer<List<Animal>> {
        private final AnimalDeserializer animalDeserializer = new AnimalDeserializer();

        @Override
        public List<Animal> deserialize(final JsonParser parser,
                                        final DeserializationContext ctx,
                                        final Type type) {
            final List<Animal> animals = new ArrayList<>();
            while (parser.hasNext()) {
                JsonParser.Event event = parser.next();
                while (event == JsonParser.Event.START_OBJECT) {
                    animals.add(animalDeserializer.deserialize(parser, ctx, type));
                    event = parser.next();
                }
            }
            return animals;
        }
    }

    public static class AnimalListSerializer implements JsonbSerializer<List<Animal>> {
        private final AnimalSerializer animalSerializer = new AnimalSerializer();

        @Override
        public void serialize(final List<Animal> animals, final JsonGenerator gen,
                              final SerializationContext ctx) {
            gen.writeStartArray();
            for (final Animal animal : animals) {
                animalSerializer.serialize(animal, gen, ctx);
            }
            gen.writeEnd();
        }
    }

    public static class AnimalsAnnotation {
        @JsonbTypeSerializer(AnimalListSerializer.class)
        @JsonbTypeDeserializer(AnimalListDeserializer.class)
        public List<Animal> animals = new ArrayList<>();
    }

    public static class Animals {
        public List<Animal> animals = new ArrayList<>();
    }

    public static class AnimalDeserializer implements JsonbDeserializer<Animal> {
        @Override
        public Animal deserialize(final JsonParser jsonParser,
                                  final DeserializationContext deserializationContext,
                                  final Type type) {
            Animal animal = null;
            while (jsonParser.hasNext()) {
                JsonParser.Event event = jsonParser.next();
                if (event == JsonParser.Event.START_OBJECT) {
                    continue;
                }
                if (event == JsonParser.Event.END_OBJECT) {
                    break;
                }
                if (event == JsonParser.Event.KEY_NAME) {
                    switch (jsonParser.getString()) {
                        case "type":
                            jsonParser.next();
                            switch (jsonParser.getString()) {
                                case "cat":
                                    animal = new Cat();
                                    break;
                                case "dog":
                                    animal = new Dog();
                                    break;
                                default:
                                    animal = new Animal();
                            }
                            break;
                        case "name":
                            jsonParser.next();
                            animal.name = jsonParser.getString();
                            break;
                        case "age":
                            jsonParser.next();
                            animal.age = jsonParser.getInt();
                            break;
                        case "furry":
                            event = jsonParser.next();
                            animal.furry = event == JsonParser.Event.VALUE_TRUE;
                            break;
                        case "weight":
                            jsonParser.next();
                            animal.weight = jsonParser.getBigDecimal().floatValue();
                            break;
                        case "cuddly":
                            event = jsonParser.next();
                            ((Cat) animal).cuddly = event == JsonParser.Event.VALUE_TRUE;
                            break;
                        case "barking":
                            event = jsonParser.next();
                            ((Dog) animal).barking = event == JsonParser.Event.VALUE_TRUE;
                            break;
                        default:
                    }
                }
            }
            return animal;
        }
    }

    public static class AnimalSerializer implements JsonbSerializer<Animal> {
        @Override
        public void serialize(final Animal animal, final JsonGenerator jsonGenerator,
                              final SerializationContext serializationContext) {
            if (animal != null) {
                jsonGenerator.writeStartObject();
                if (Cat.class.isAssignableFrom(animal.getClass())) {
                    jsonGenerator.write("type", "cat");
                    jsonGenerator.write("cuddly", ((Cat) animal).cuddly);
                } else if (Dog.class.isAssignableFrom(animal.getClass())) {
                    jsonGenerator.write("type", "dog");
                    jsonGenerator.write("barking", ((Dog) animal).barking);
                } else {
                    jsonGenerator.write("type", "animal");
                }
                jsonGenerator.write("age", animal.age);
                jsonGenerator.write("furry", animal.furry);
                jsonGenerator.write("name", animal.name);
                jsonGenerator.write("weight", animal.weight);
                jsonGenerator.writeEnd();
            } else {
                serializationContext.serialize(null, jsonGenerator);
            }
        }
    }

    public static class NameHolder {
        @JsonbTypeSerializer(FooPassthroughSerializer.class)
        public Named name;
    }

    public static class Named {
        public String name;

        @JsonbTypeSerializer(DetailNameSerializer.class)
        public DetailName detailName;
    }

    public static class DetailName {
        public String name;
    }

    public static class DetailNameSerializer implements JsonbSerializer<DetailName> {
        @Override
        public void serialize(final DetailName foo, final JsonGenerator jsonGenerator,
                              final SerializationContext serializationContext) {
            serializationContext.serialize(foo, jsonGenerator);
            jsonGenerator.write("detail", true);
        }
    }

    public static class FooPassthroughSerializer implements JsonbSerializer<Named> {
        @Override
        public void serialize(final Named foo, final JsonGenerator jsonGenerator,
                              final SerializationContext serializationContext) {
            serializationContext.serialize(foo, jsonGenerator);
        }
    }

    public static class StringHolder implements Holder<String> {
        private String instance = "Test";

        public String getInstance() {
            return instance;
        }

        public void setInstance(final String instance) {
            this.instance = instance;
        }
    }

    public static class SimpleContainerDeserializer implements JsonbDeserializer<StringHolder> {
        @Override
        public StringHolder deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
            final StringHolder container = new StringHolder();

            while (parser.hasNext()) {
                final JsonParser.Event event = parser.next();
                if (event == JsonParser.Event.START_OBJECT) {
                    continue;
                }
                if (event == JsonParser.Event.END_OBJECT) {
                    break;
                }
                if (event == JsonParser.Event.KEY_NAME && "instance".equals(parser.getString())) {
                    container.setInstance(ctx.deserialize(String.class, parser) + " Deserialized");
                }
            }

            return container;
        }
    }

    public static class SimpleContainerSerializer implements JsonbSerializer<StringHolder> {
        @Override
        public void serialize(final StringHolder container, final JsonGenerator generator,
                              final SerializationContext ctx) {
            generator.writeStartObject();
            ctx.serialize("instance", container.getInstance() + " Serialized", generator);
            generator.writeEnd();
        }
    }

    public static class HolderHolder implements Holder<StringHolder> {
        @JsonbTypeSerializer(SimpleContainerSerializer.class)
        @JsonbTypeDeserializer(SimpleContainerDeserializer.class)
        private StringHolder instance;

        @Override
        public StringHolder getInstance() {
            return instance;
        }

        @Override
        public void setInstance(StringHolder instance) {
            this.instance = instance;
        }
    }

    public static class ArrayHolder implements Holder<StringHolder[]> {
        @JsonbTypeSerializer(StringArraySerializer.class)
        @JsonbTypeDeserializer(StringArrayDeserializer.class)
        private StringHolder[] instance;

        @Override
        public StringHolder[] getInstance() {
            return instance;
        }

        @Override
        public void setInstance(final StringHolder[] instance) {
            this.instance = instance;
        }
    }

    public static class StringArraySerializer implements JsonbSerializer<StringHolder[]> {
        @Override
        public void serialize(final StringHolder[] containers,
                              final JsonGenerator jsonGenerator,
                              final SerializationContext serializationContext) {
            jsonGenerator.writeStartArray();
            for (final StringHolder container : containers) {
                serializationContext.serialize(container, jsonGenerator);
            }
            jsonGenerator.writeEnd();
        }
    }

    public static class StringArrayDeserializer implements JsonbDeserializer<StringHolder[]> {
        @Override
        public StringHolder[] deserialize(final JsonParser jsonParser,
                                          final DeserializationContext deserializationContext,
                                          final Type type) {
            final List<StringHolder> containers = new LinkedList<>();

            while (jsonParser.hasNext()) {
                JsonParser.Event event = jsonParser.next();
                if (event == JsonParser.Event.START_OBJECT) {
                    containers.add(deserializationContext.deserialize(
                            new StringHolder() {}.getClass().getGenericSuperclass(), jsonParser));
                }
                if (event == JsonParser.Event.END_OBJECT) {
                    break;
                }
            }

            return containers.toArray(new StringHolder[0]);
        }
    }

    public static class Foo {
        public String name;
        public int value;
        public boolean flag;
    }

    public static class MyWrapper {
        @JsonbTransient
        public Map<String, String> map;

        @JsonbTypeSerializer(MyMapSerializer.class)
        public MyWrapper myWrapper;
    }

    public static class MyMapSerializer implements JsonbSerializer<MyWrapper> {
        @Override
        public void serialize(final MyWrapper obj, final JsonGenerator generator, final SerializationContext ctx) {
            ctx.serialize(obj, generator);
            if (obj.map != null) {
                obj.map.forEach((k, v) -> ctx.serialize(k, v, generator));
            }
        }
    }

    public static class UUIDSerializer implements JsonbSerializer<UUID> {
        @Override
        public void serialize(final UUID obj, final JsonGenerator generator, final SerializationContext ctx) {
            generator.write(obj.toString());
        }
    }

    public static class UUIDWrapper {
        @JsonbTypeSerializer(UUIDSerializer.class)
        public UUID uuid;
    }

    public static class Wrapper {

        @JsonbTypeSerializer(FooSer.class)
        @JsonbTypeDeserializer(FooDeser.class)
        public Foo foo;

        @JsonbTypeSerializer(FooSer.class)
        @JsonbTypeDeserializer(FooDeser.class)
        public List<Foo> moreFoos = new ArrayList<>();
    }

    public static class IntList {
        public List<Integer> elems = new ArrayList<>();
    }

    public enum MyStatus {
        OK, WRONG;
    }

    public static class EnumList {
        public List<MyStatus> elems = new ArrayList<>();
    }

    public static class FooDeser implements JsonbDeserializer<Foo> {
        @Override
        public Foo deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
            final Foo f = new Foo();
            assertTrue(parser.hasNext());
            assertEquals(JsonParser.Event.START_OBJECT, parser.next());
            assertTrue(parser.hasNext());
            assertEquals(JsonParser.Event.KEY_NAME, parser.next());
            assertEquals("full", parser.getString());
            assertTrue(parser.hasNext());
            assertEquals(JsonParser.Event.VALUE_TRUE, parser.next());
            assertTrue(parser.hasNext());
            assertEquals(JsonParser.Event.KEY_NAME, parser.next());
            assertEquals("name", parser.getString());
            assertTrue(parser.hasNext());
            assertEquals(JsonParser.Event.VALUE_STRING, parser.next());
            f.name = parser.getString();
            assertTrue(parser.hasNext());
            assertEquals(JsonParser.Event.END_OBJECT, parser.next());

            // to be sure we passed there
            f.flag = true;
            f.value = f.name.length();
            return f;
        }
    }

    public static class FooSer implements JsonbSerializer<Foo> {
        @Override
        public void serialize(final Foo obj, final JsonGenerator generator, final SerializationContext ctx) {
            generator.write("full", true);
            generator.write("name", obj.name);
        }
    }

    /**
     * see JOHNZON-169
     */
    @Test
    public void testArrayParseWithDeserializer() {
        String json = "{\"student\":[{\"val\":\"max,24\"}]}";
        Jsonb jsonb = JsonbBuilder.create();

        StudentHolder studentHolder = jsonb.fromJson(json, StudentHolder.class);
        assertNotNull(studentHolder);
        assertNotNull(studentHolder.getStudent());
        assertEquals(1, studentHolder.getStudent().size());
        assertEquals("max", studentHolder.getStudent().get(0).getName());
        assertEquals(24, studentHolder.getStudent().get(0).getAge());
    }

    public static class Student {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class StudentDeserializer implements JsonbDeserializer<Student> {
        @Override
        public Student deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            String val = parser.getObject().getString("val");
            String[] parts = val.split(",");
            Student s = new Student();
            s.setName(parts[0]);
            s.setAge(Integer.parseInt(parts[1]));

            return s;
        }
    }

    public static class StudentHolder {
        @JsonbTypeDeserializer(StudentDeserializer.class)
        private List<Student> student;

        public List<Student> getStudent() {
            return student;
        }

        public void setStudent(List<Student> student) {
            this.student = student;
        }
    }

    @JsonbTypeSerializer(OuterTestSerializer.class)
    public static class OuterTestModel {
    }

    @JsonbTypeSerializer(InnerTestSerializer.class)
    public static class InnerTestModel {
    }

    public static class OuterTestSerializer implements JsonbSerializer<OuterTestModel> {
        @Override
        public void serialize(final OuterTestModel obj, final JsonGenerator generator, final SerializationContext ctx) {
            generator.writeStartObject();
            generator.write("foo", "generated in outer serializer");
            ctx.serialize("inner", new InnerTestModel(), generator);
            generator.writeEnd();
        }
    }

    public static class InnerTestSerializer implements JsonbSerializer<InnerTestModel> {
        @Override
        public void serialize(final InnerTestModel obj, final JsonGenerator generator, final SerializationContext ctx) {
            generator.writeStartObject();
            generator.write("bar", "generated in inner serializer");
            generator.writeEnd();
        }
    }
}
