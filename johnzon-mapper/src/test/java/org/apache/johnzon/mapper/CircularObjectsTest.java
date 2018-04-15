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
package org.apache.johnzon.mapper;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test serialising objects which contain the same Object multiple times,
 * sometimes even with cycles.
 */
public class CircularObjectsTest {

    @Test
    public void testSimpleCyclicPerson() {
        Person john = new Person("John");
        Person marry = new Person("Marry");

        john.setMarriedTo(marry);
        marry.setMarriedTo(john);

        Mapper mapper = new MapperBuilder().setAccessModeName("field").setDeduplicateObjects(true).build();
        String ser = mapper.writeObjectAsString(john);

        assertNotNull(ser);
        assertTrue(ser.contains("\"name\":\"John\""));
        assertTrue(ser.contains("\"marriedTo\":\"/\""));
        assertTrue(ser.contains("\"name\":\"Marry\""));

        // and now de-serialise it back
        Person john2 = mapper.readObject(ser, Person.class);
        assertNotNull(john2);
        assertEquals("John", john2.getName());

        Person marry2 = john2.getMarriedTo();
        assertNotNull(marry2);
        assertEquals("Marry", marry2.getName());

        assertEquals(john2, marry2.getMarriedTo());
    }

    @Test
    public void testSimpleCyclicPersonAnnotatedDedup() {
        DeduplicatedPerson john = new DeduplicatedPerson("John");
        DeduplicatedPerson marry = new DeduplicatedPerson("Marry");

        john.setMarriedTo(marry);
        marry.setMarriedTo(john);

        Mapper mapper = new MapperBuilder().setAccessModeName("field").build();
        String ser = mapper.writeObjectAsString(john);

        assertNotNull(ser);
        assertTrue(ser.contains("\"name\":\"John\""));
        assertTrue(ser.contains("\"marriedTo\":\"/\""));
        assertTrue(ser.contains("\"name\":\"Marry\""));

        // and now de-serialise it back
        DeduplicatedPerson john2 = mapper.readObject(ser, DeduplicatedPerson.class);
        assertNotNull(john2);
        assertEquals("John", john2.getName());

        DeduplicatedPerson marry2 = john2.getMarriedTo();
        assertNotNull(marry2);
        assertEquals("Marry", marry2.getName());

        assertEquals(john2, marry2.getMarriedTo());
    }

    @Test
    public void testComplexCyclicPerson() {
        Person karl = new Person("Karl");
        Person andrea = new Person("Andrea");
        Person lu = new Person("Lu");
        Person sue = new Person("Sue");

        karl.setMarriedTo(andrea);
        karl.getKids().add(lu);
        karl.getKids().add(sue);

        andrea.setMarriedTo(karl);
        andrea.getKids().add(lu);
        andrea.getKids().add(sue);

        lu.setFather(karl);
        lu.setMother(andrea);

        sue.setFather(karl);
        sue.setMother(andrea);

        Mapper mapper = new MapperBuilder().setAccessModeName("field").setDeduplicateObjects(true).build();

        {
            // test karl
            String karlJson = mapper.writeObjectAsString(karl);
            Person karl2 = mapper.readObject(karlJson, Person.class);
            assertEquals("Karl", karl2.getName());
            assertEquals("Andrea", karl2.getMarriedTo().getName());
            assertEquals(karl2, karl2.getMarriedTo().getMarriedTo());
            assertEquals(2, karl2.getKids().size());
            assertEquals("Lu", karl2.getKids().get(0).getName());
            assertEquals("Sue", karl2.getKids().get(1).getName());
            assertEquals(2, karl2.getMarriedTo().getKids().size());
            assertEquals("Lu", karl2.getMarriedTo().getKids().get(0).getName());
            assertEquals("Sue", karl2.getMarriedTo().getKids().get(1).getName());
        }

        {
            // test Sue
            String sueJson = mapper.writeObjectAsString(sue);
            Person sue2 = mapper.readObject(sueJson, Person.class);

            assertEquals("Sue", sue2.getName());
            assertNull(sue2.getMarriedTo());
            assertEquals("Andrea", sue2.getMother().getName());
            assertEquals("Karl", sue2.getFather().getName());

            assertEquals(sue2.getMother().getKids().get(0), sue2.getFather().getKids().get(0));
            assertEquals(sue2.getMother().getKids().get(1), sue2.getFather().getKids().get(1));
        }
    }

    @Test
    public void testCyclesInArrays() {
        Person karl = new Person("Karl");
        Person andrea = new Person("Andrea");
        Person lu = new Person("Lu");
        Person sue = new Person("Sue");

        karl.setMarriedTo(andrea);
        karl.getKids().add(lu);
        karl.getKids().add(sue);

        andrea.setMarriedTo(karl);
        andrea.getKids().add(lu);
        andrea.getKids().add(sue);

        lu.setFather(karl);
        lu.setMother(andrea);

        sue.setFather(karl);
        sue.setMother(andrea);

        Mapper mapper = new MapperBuilder().setAccessModeName("field").setDeduplicateObjects(true).build();

        // test deep array
        Person[] people = new Person[4];
        people[0] = karl;
        people[1] = andrea;
        people[2] = lu;
        people[3] = sue;

        String peopleJson = mapper.writeArrayAsString(people);
        Person[] people2 = mapper.readArray(new StringReader(peopleJson), Person.class);
        assertNotNull(people2);
        assertEquals(4, people2.length);
        assertEquals("Karl",   people2[0].getName());
        assertEquals("Andrea", people2[1].getName());
        assertEquals("Lu",     people2[2].getName());
        assertEquals("Sue",    people2[3].getName());

    }

    public static class Person {
        private String name;
        private Person marriedTo;
        private Person mother;
        private Person father;
        private List<Person> kids = new ArrayList<Person>();

        public Person() {
        }

        public Person(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Person getMarriedTo() {
            return marriedTo;
        }

        public void setMarriedTo(Person marriedTo) {
            this.marriedTo = marriedTo;
        }

        public Person getMother() {
            return mother;
        }

        public void setMother(Person mother) {
            this.mother = mother;
        }

        public Person getFather() {
            return father;
        }

        public void setFather(Person father) {
            this.father = father;
        }

        public List<Person> getKids() {
            return kids;
        }

        public void setKids(List<Person> kids) {
            this.kids = kids;
        }
    }

    @JohnzonDeduplicateObjects
    public static class DeduplicatedPerson {
        private String name;
        private DeduplicatedPerson marriedTo;
        private DeduplicatedPerson mother;
        private DeduplicatedPerson father;
        private List<DeduplicatedPerson> kids = new ArrayList<DeduplicatedPerson>();

        public DeduplicatedPerson() {
        }

        public DeduplicatedPerson(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public DeduplicatedPerson getMarriedTo() {
            return marriedTo;
        }

        public void setMarriedTo(DeduplicatedPerson marriedTo) {
            this.marriedTo = marriedTo;
        }

        public DeduplicatedPerson getMother() {
            return mother;
        }

        public void setMother(DeduplicatedPerson mother) {
            this.mother = mother;
        }

        public DeduplicatedPerson getFather() {
            return father;
        }

        public void setFather(DeduplicatedPerson father) {
            this.father = father;
        }

        public List<DeduplicatedPerson> getKids() {
            return kids;
        }

        public void setKids(List<DeduplicatedPerson> kids) {
            this.kids = kids;
        }
    }

}
