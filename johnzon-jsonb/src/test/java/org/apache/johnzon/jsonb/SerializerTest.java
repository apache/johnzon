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

import org.junit.Test;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.annotation.JsonbTypeSerializer;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SerializerTest {

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


    public static class Foo {
        public String name;
        public int value;
        public boolean flag;
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
}
