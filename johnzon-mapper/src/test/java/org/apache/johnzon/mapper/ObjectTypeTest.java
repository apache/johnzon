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


import java.lang.reflect.Type;
import java.util.Arrays;


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
                .build();

        String jsonString = "{ \"//javaType\": \"org.apache.johnzon.mapper.ObjectTypeTest$Customer\", \"firstName\":\"Bruce\", \"lastName\":\"Wayne\" }";

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
    }


    public static class TestWithTypeConverter implements ObjectConverter<Dog> {
        @Override
        public void writeJson(Dog instance, JsonbGenerator jsonGenerator) {
            jsonGenerator.getJsonGenerator().write("//javaType", instance.getClass().getName());

        }

        @Override
        public Dog fromJson(JsonbParser jsonParser, Type targetType) {
            return null;
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
        boolean hairCut = false;

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