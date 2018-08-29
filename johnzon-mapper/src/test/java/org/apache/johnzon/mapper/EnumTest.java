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

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class EnumTest {

    @Test(expected = MapperException.class)
    public void testUnknownEnumValue() {
        Mapper mapper = newTestMapperBuilder().build();

        String json = "{\"myEnum\":\"UNKNOWN\"}";
        SimpleObject simpleObject = mapper.<SimpleObject>readObject(json, SimpleObject.class);
    }

    @Test
    public void testSimpleEnumAccessModeBoth() {
        testSimpleField(newTestMapperBuilder().setAccessModeName("both")
            .build());
    }

    @Test
    public void testSimpleEnumAccessModeField() {
        testSimpleField(newTestMapperBuilder().setAccessModeName("field")
            .build());
    }

    private void testSimpleField(Mapper mapper) {
        SimpleObject object = new SimpleObject(MyEnum.TWO);

        String objectAsString = mapper.writeObjectAsString(object);
        Assert.assertEquals("{\"myEnum\":\"TWO\"}", objectAsString);

        Assert.assertEquals(object.myEnum, mapper.<SimpleObject>readObject(objectAsString, SimpleObject.class).myEnum);
    }

    @Test
    public void testSimpleEnumWithCollectionAccessModeBoth() {
        testCollection(newTestMapperBuilder().setAccessModeName("both")
            .build());
    }

    @Test
    public void testSimpleEnumWithCollectionAccessModeField() {
        testCollection(newTestMapperBuilder().setAccessModeName("field")
            .build());
    }


    private void testCollection(Mapper mapper) {
        CollectionObject object = new CollectionObject(MyEnum.ONE,
            MyEnum.TWO,
            MyEnum.THREE,
            MyEnum.TWO);// duplicate entry isn't a mistake

        String jsonString = "{\"enums\":[\"ONE\",\"TWO\",\"THREE\",\"TWO\"]}";

        Assert.assertEquals(object.enums, mapper.<CollectionObject>readObject(jsonString, CollectionObject.class).enums);

        String johnzonString = mapper.writeObjectAsString(object);
        Assert.assertEquals(jsonString, johnzonString);
        Assert.assertEquals(object.enums, mapper.<CollectionObject>readObject(johnzonString, CollectionObject.class).enums);
    }


    @Test
    public void testAdvancedEnumAccessModeBoth() {
        testAdvancedEnum(newTestMapperBuilder().setAccessModeName("both")
            .build());
    }

    @Test
    public void testAdvancedEnumAccessModeField() {
        testAdvancedEnum(newTestMapperBuilder().setAccessModeName("field")
            .build());
    }

    @Test
    public void testEnumSet() {
        EnumSetObject eso = new EnumSetObject();
        eso.setEnumset(EnumSet.of(AdvancedEnum.VALUE_1, AdvancedEnum.VALUE_2));

        String json = newTestMapperBuilder().build().writeObjectAsString(eso);

        EnumSetObject eso2 = newTestMapperBuilder().build().readObject(json, EnumSetObject.class);
        Assert.assertNotNull(eso2);
        Assert.assertNotNull(eso2.getEnumset());
        Assert.assertEquals(2, eso2.getEnumset().size());
        Assert.assertTrue(eso2.getEnumset().contains(AdvancedEnum.VALUE_1));
        Assert.assertTrue(eso2.getEnumset().contains(AdvancedEnum.VALUE_2));
    }

    private void testAdvancedEnum(Mapper mapper) {
        AdvancedEnumObject object = new AdvancedEnumObject(AdvancedEnum.VALUE_1, Arrays.asList(AdvancedEnum.VALUE_2,
            AdvancedEnum.VALUE_1,
            AdvancedEnum.VALUE_1,
            AdvancedEnum.VALUE_2));

        String jsonString = "{\"advancedEnum\":\"VALUE_1\",\"advancedEnums\":[\"VALUE_2\",\"VALUE_1\",\"VALUE_1\",\"VALUE_2\"]}";

        AdvancedEnumObject johnzonObject = mapper.readObject(jsonString, AdvancedEnumObject.class);
        Assert.assertEquals(object.advancedEnum, johnzonObject.advancedEnum);
        Assert.assertEquals(object.advancedEnums, johnzonObject.advancedEnums);

        String johnzonString = mapper.writeObjectAsString(object);
        Assert.assertEquals(jsonString, johnzonString);

        johnzonObject = mapper.readObject(johnzonString, AdvancedEnumObject.class);
        Assert.assertEquals(object.advancedEnum, johnzonObject.advancedEnum);
        Assert.assertEquals(object.advancedEnums, johnzonObject.advancedEnums);
    }

    private MapperBuilder newTestMapperBuilder() {
        return new MapperBuilder().setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        });
    }

    public enum MyEnum {
        ONE,
        TWO,
        THREE
    }

    public static class SimpleObject {
        private MyEnum myEnum;

        private SimpleObject() {
        }

        private SimpleObject(MyEnum myEnum) {
            this.myEnum = myEnum;
        }
    }

    public static class CollectionObject {
        private List<MyEnum> enums;

        private CollectionObject() {
        }

        private CollectionObject(MyEnum... enums) {
            this.enums = Arrays.asList(enums);
        }
    }

    public static class EnumSetObject {
        private EnumSet<AdvancedEnum> enumset;

        public EnumSet<AdvancedEnum> getEnumset() {
            return enumset;
        }

        public void setEnumset(EnumSet<AdvancedEnum> enumset) {
            this.enumset = enumset;
        }
    }

    public enum AdvancedEnum {
        VALUE_1("one", 1),
        VALUE_2("two", 2),
        VALUE_3("three", 3);

        private String string;
        private int i;

        private AdvancedEnum(String string, int i) {
            this.string = string;
            this.i = i;
        }


        public String getString() {
            return string;
        }

        public int getI() {
            return i;
        }
    }

    public static class AdvancedEnumObject {
        private AdvancedEnum advancedEnum;
        private List<AdvancedEnum> advancedEnums;

        private AdvancedEnumObject() {
        }

        public AdvancedEnumObject(AdvancedEnum advancedEnum, List<AdvancedEnum> advancedEnums) {
            this.advancedEnum = advancedEnum;
            this.advancedEnums = advancedEnums;
        }
    }

}