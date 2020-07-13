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
package org.apache.johnzon.mapper;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ObjectConverterTest {

    @Test
    public void testObjectConverter() {
        Contact contact = new Contact();
        contact.linkedPersons.addAll(Arrays.asList(new Person("f1", "l1"), new Person("f2", "l2")));
        contact.linkedPersonsArray = new Person[] { new Person("f3", "l3"), new Person("f4", "l4") };
        contact.personMap.put("cinq", new Person("f5", "l5"));
        contact.personMap.put("six", new Person("f6", "l6"));

        MapperBuilder mapperBuilder = new MapperBuilder();
        mapperBuilder.addConverter(Person.class, new PersonConverter());
        Mapper mapper = mapperBuilder.setAccessModeName("both").setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        }).build();

        String s = mapper.writeObjectAsString(contact);
        Contact c = mapper.readObject(s, Contact.class);
        String expected = "{\"linkedPersons\":[\"f1|l1\",\"f2|l2\"],\"linkedPersonsArray\":[\"f3|l3\",\"f4|l4\"],\"personMap\":{\"cinq\":\"f5|l5\",\"six\":\"f6|l6\"}}";
        Assert.assertEquals(expected, s);
        Assert.assertEquals(contact, c);
    }


    public static class PersonConverter implements Converter<Person> {
        @Override
        public String toString(Person instance) {
            if (instance == null) {
                return null;
            }
            return instance.getFirstName() + "|" + instance.getLastName();
        }

        @Override
        public Person fromString(String text) {
            if (text == null) {
                return null;
            }
            String[] split = text.split("\\|");
            if (split.length == 2) {
                return new Person(split[0], split[1]);
            }
            return null;
        }
    }

    public static class Contact {
        @JohnzonConverter(PersonConverter.class)
        private List<Person> linkedPersons = new ArrayList<Person>();

        @JohnzonConverter(PersonConverter.class)
        private Person[] linkedPersonsArray;

        @JohnzonConverter(PersonConverter.class)
        private SortedMap<String, Person> personMap = new TreeMap<String, Person>();

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Contact contact = Contact.class.cast(o);
            return linkedPersons.equals(contact.linkedPersons) && personMap.equals(contact.personMap)
                && Arrays.equals(linkedPersonsArray, contact.linkedPersonsArray);

        }

        @Override
        public int hashCode() {
            int result = linkedPersons.hashCode();
            result = 31 * result + Arrays.hashCode(linkedPersonsArray);
            return result;
        }

        @Override
        public String toString() {
            return "Contact{" +
                    "linkedPersons=" + linkedPersons +
                    ", linkedPersonsArray=" + Arrays.toString(linkedPersonsArray) +
                    ", personMap=" + personMap +
                    '}';
        }
    }

    public static class Person {
        private String firstName;
        private String lastName;

        //no default constructor on purpose

        private Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Person)) {
                return false;
            }

            Person person = (Person) o;

            if (firstName != null ? !firstName.equals(person.firstName) : person.firstName != null) {
                return false;
            }
            if (lastName != null ? !lastName.equals(person.lastName) : person.lastName != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = firstName != null ? firstName.hashCode() : 0;
            result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Person{" +
                    "firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    '}';
        }
    }
}