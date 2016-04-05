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
import java.util.Arrays;


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
                .build();

        String expectedJsonString = "{\"//javaType\":\"org.apache.johnzon.mapper.ObjectTypeTest$Mutt\"," +
                "\"mother\":{\"//javaType\":\"org.apache.johnzon.mapper.ObjectTypeTest$Poodle\",\"name\":\"Rosa\",\"hairCut\":true}," +
                "\"father\":{\"//javaType\":\"org.apache.johnzon.mapper.ObjectTypeTest$Beagle\"," +
                "\"father\":{\"//javaType\":\"org.apache.johnzon.mapper.ObjectTypeTest$Beagle\",\"name\":\"Wuffi\"},\"name\":\"Gnarl\"},\"name\":\"Snoopie\"}";

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

        String json = mapper.writeObjectAsString(snoopie);
        Assert.assertNotNull(json);
        //X TODO Assert.assertEquals(expectedJsonString, json);
    }


    public static class TestWithTypeConverter implements ObjectConverter<Dog> {
        @Override
        public void writeJson(Dog instance, MappingGenerator mappingGenerator) {
            mappingGenerator.getJsonGenerator().write("//javaType", instance.getClass().getName());
            mappingGenerator.writeObject(instance);
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
}