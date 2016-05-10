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


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ObjectTypeTest {


    @Parameterized.Parameter
    public String accessMode;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<String> modes() {
        return Arrays.asList("field", "method", "both", "strict-method");
    }


    @Test
    public void testObjectConverterMapper() {
        Mapper mapper = new MapperBuilder()
                .setAccessModeName(accessMode)
                .addObjectConverter(Dog.class, new TestWithTypeConverter())
                .setAttributeOrder(String.CASE_INSENSITIVE_ORDER)
                .build();

        String expectedJsonString = getJson();
        Mutt snoopie = getJavaObject();

        String json = mapper.writeObjectAsString(snoopie);
        Assert.assertNotNull(json);
        Assert.assertEquals(expectedJsonString, json);
    }

    @Test
    public void testReadWithObjectConverter() {

        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .addObjectConverter(Dog.class, new TestWithTypeConverter())
                                           .build();

        Dog dog = mapper.readObject(getJson(), Dog.class);
        Assert.assertNotNull(dog);
        Assert.assertEquals(getJavaObject(), dog);
    }

    @Test
    public void testWriteWithAdvancedObjectConverter() {

        String expectedJson = "[{\"poodleName\":\"Poodle1\"},{\"poodleName\":\"Poodle2\"}]";

        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .addObjectConverter(Poodle.class, new DBAccessPoodleConverter())
                                           .build();

        String json = mapper.writeObjectAsString(new ArrayList<Poodle>(DBAccessPoodleConverter.POODLES.values()));
        Assert.assertNotNull(json);
        Assert.assertEquals(expectedJson, json);
    }

    @Test
    public void testReadWithAdvancedObjectConverter() {

        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .addObjectConverter(Poodle.class, new DBAccessPoodleConverter())
                                           .build();

        List<Poodle> poodles = mapper.readObject("[{\"poodleName\":\"Poodle1\"},{\"poodleName\":\"Poodle2\"}]", new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{Poodle.class};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        });

        Assert.assertNotNull(poodles);
        Assert.assertEquals(2, poodles.size());
        for (Poodle poodle : poodles) {
            Assert.assertEquals(DBAccessPoodleConverter.POODLES.get(poodle.getName()), poodle);
        }
    }


    private Mutt getJavaObject() {
        Poodle mum = new Poodle();
        mum.setName("Rosa");
        mum.setHairCut(true);

        Beagle dad = new Beagle();
        dad.setName("Gnarl");

        Beagle grandPa = new Beagle();
        grandPa.setName("Wuffi");
        dad.setFather(grandPa);

        Mutt snoopie = new Mutt();
        snoopie.setName("Snoopie");
        snoopie.setFather(dad);
        snoopie.setMother(mum);
        return snoopie;
    }

    private String getJson() {
        return "{" +
                                          "\"//javaType\":\"org.apache.johnzon.mapper.ObjectTypeTest$Mutt\"," +
                                          "\"father\":{" +
                                            "\"//javaType\":\"org.apache.johnzon.mapper.ObjectTypeTest$Beagle\"," +
                                              "\"father\":{" +
                                                "\"//javaType\":\"org.apache.johnzon.mapper.ObjectTypeTest$Beagle\"," +
                                                "\"name\":\"Wuffi\"" +
                                              "}," +
                                              "\"name\":\"Gnarl\"}," +
                                          "\"mother\":{" +
                                            "\"//javaType\":\"org.apache.johnzon.mapper.ObjectTypeTest$Poodle\"," +
                                            "\"hairCut\":true," +
                                            "\"name\":\"Rosa\"}," +
                                          "\"name\":\"Snoopie\"" +
                                        "}";
    }


    public static class TestWithTypeConverter implements ObjectConverter.Codec<Dog> {
        @Override
        public void writeJson(Dog instance, MappingGenerator mappingGenerator) {
            mappingGenerator.getJsonGenerator().write("//javaType", instance.getClass().getName());
            mappingGenerator.writeObject(instance, mappingGenerator.getJsonGenerator());
        }

        @Override
        public Dog fromJson(JsonObject jsonObject, Type targetType, MappingParser parser) {
            String javaType = jsonObject.getString("//javaType");
            Class targetClass = javaType != null ? getSubClass(targetType, javaType) : (Class) targetType;

            return parser.readObject(jsonObject, targetClass);
        }



        /**
         * Helper method to check that javaType is really a subclass of targetType.
         * Might get moved to a utility class
         */
        private Class getSubClass(Type targetType, String javaType) {
            if (javaType != null) {
                // the following should get extracted in a utility class.
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = targetType.getClass().getClassLoader();
                }

                try {
                    Class subClass = cl.loadClass(javaType);
                    Class targetClass = null;
                    if (targetType instanceof Class) {
                        targetClass = (Class) targetType;
                    } else if (targetType instanceof ParameterizedType) {
                        targetClass = (Class)((ParameterizedType) targetType).getRawType();
                    }
                    if (targetClass != null && targetClass.isAssignableFrom(subClass)) {
                        return subClass;
                    }
                } catch (ClassNotFoundException e) {
                    // continue without better class match
                }
            }

            return (Class) targetType;
        }
    }

    public static class Dog {
        private String name;
        private Dog father;
        private Dog mother;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Dog getFather() {
            return father;
        }

        public void setFather(Dog father) {
            this.father = father;
        }

        public Dog getMother() {
            return mother;
        }

        public void setMother(Dog mother) {
            this.mother = mother;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Dog dog = (Dog) o;

            if (name != null ? !name.equals(dog.name) : dog.name != null) {
                return false;
            }
            if (father != null ? !father.equals(dog.father) : dog.father != null) {
                return false;
            }
            return mother != null ? mother.equals(dog.mother) : dog.mother == null;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (father != null ? father.hashCode() : 0);
            result = 31 * result + (mother != null ? mother.hashCode() : 0);
            return result;
        }
    }

    public static class Beagle extends Dog {
    }

    public static class Poodle extends Dog {
        private boolean hairCut = false;

        public boolean isHairCut() {
            return hairCut;
        }

        public void setHairCut(boolean hairCut) {
            this.hairCut = hairCut;
        }
    }

    public static class Mutt extends Dog {
    }


    public static class DBAccessPoodleConverter implements ObjectConverter.Codec<Poodle> {

        public static final String POODLE_1_NAME = "Poodle1";
        public static final String POODLE_2_NAME = "Poodle2";

        public static final Map<String, Poodle> POODLES = new LinkedHashMap<String, Poodle>(2);
        static {
            Poodle poodle1 = new Poodle();
            poodle1.setHairCut(true);
            poodle1.setName(POODLE_1_NAME);

            Poodle poodle2 = new Poodle();
            poodle2.setHairCut(false);
            poodle2.setName(POODLE_2_NAME);

            POODLES.put(poodle1.getName(), poodle1);
            POODLES.put(poodle2.getName(), poodle2);
        }

        @Override
        public void writeJson(Poodle instance, MappingGenerator jsonbGenerator) {
            jsonbGenerator.getJsonGenerator().write("poodleName", instance.getName());
        }

        @Override
        public Poodle fromJson(JsonObject jsonObject, Type targetType, MappingParser parser) {
            return POODLES.get(jsonObject.getString("poodleName"));
        }
    }
}