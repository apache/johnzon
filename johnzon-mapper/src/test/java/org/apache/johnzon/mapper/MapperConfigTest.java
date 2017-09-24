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

import org.apache.johnzon.mapper.access.FieldAccessMode;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.junit.Assert;
import org.junit.Test;

import javax.json.JsonObject;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapperConfigTest {

    @Test
    public void testFindObjectConverterConverterForSpecificClass() {

        ObjectConverter.Codec<ClassWithoutSupertypes> theConverter = new TheConverter<ClassWithoutSupertypes>();

        Map<Class<?>, ObjectConverter.Codec<?>> converterMap = new HashMap<Class<?>, ObjectConverter.Codec<?>>(1);
        converterMap.put(ClassWithoutSupertypes.class, theConverter);

        MapperConfig config = createConfig(converterMap);

        ObjectConverter.Reader converter = config.findObjectConverterReader(ClassWithoutSupertypes.class);
        Assert.assertNotNull(converter);
        Assert.assertEquals(theConverter, converter);
    }

    @Test
    public void testFindObjectConverterConverterForInterface() {

        ObjectConverter.Codec<TheInterface> theConverter = new TheConverter<TheInterface>();

        MapperConfig config = createConfig(Collections.<Class<?>, ObjectConverter.Codec<?>>singletonMap(TheInterface.class, theConverter));

        ObjectConverter.Writer converter = config.findObjectConverterWriter(ClassForTheInterface.class);
        Assert.assertNotNull(converter);
        Assert.assertEquals(theConverter, converter);
    }

    @Test
    public void testFindObjectConverterConverterOnlyForSuperclass() {

        ObjectConverter.Codec<ClassForTheInterface> theConverter = new TheConverter<ClassForTheInterface>();

        MapperConfig config = createConfig(Collections.<Class<?>, ObjectConverter.Codec<?>>singletonMap(ClassForTheInterface.class, theConverter));

        ObjectConverter.Reader converter = config.findObjectConverterReader(ExtendingClassForTheInterface.class);
        Assert.assertNotNull(converter);
        Assert.assertEquals(theConverter, converter);
    }

    @Test
    public void testFindObjectConverterConverterForInterfaceAndClass() {

        ObjectConverter.Codec<TheInterface> interfaceConverter = new TheConverter<TheInterface>();
        ObjectConverter.Codec<ClassForTheInterface> theConverter = new TheConverter<ClassForTheInterface>();

        Map<Class<?>, ObjectConverter.Codec<?>> converterMap = new HashMap<Class<?>, ObjectConverter.Codec<?>>(2);
        converterMap.put(TheInterface.class, interfaceConverter);
        converterMap.put(ClassForTheInterface.class, theConverter);

        MapperConfig config = createConfig(converterMap);

        {
            ObjectConverter.Reader converter = config.findObjectConverterReader(ClassForTheInterface.class);
            Assert.assertNotNull(converter);
            Assert.assertEquals(theConverter, converter);
        }

        {
            ObjectConverter.Writer converter = config.findObjectConverterWriter(ExtendingClassForTheInterface.class);
            Assert.assertNotNull(converter);
            Assert.assertEquals(theConverter, converter);
        }
    }

    @Test
    public void testFindObjectConverterConverterForMoreInterfaces() {

        ObjectConverter.Codec<TheInterface> firstConverter = new TheConverter<TheInterface>();
        ObjectConverter.Codec<TheSecondInterface> secondConverter = new TheConverter<TheSecondInterface>();

        Map<Class<?>, ObjectConverter.Codec<?>> converterMap = new HashMap<Class<?>, ObjectConverter.Codec<?>>(2);
        converterMap.put(TheInterface.class, firstConverter);
        converterMap.put(TheSecondInterface.class, secondConverter);
        MapperConfig config = createConfig(converterMap);

        ObjectConverter.Writer converter = config.findObjectConverterWriter(ClassWithTwoInterfaces.class);
        Assert.assertNotNull(converter);
        Assert.assertEquals(converterMap.get(ClassWithTwoInterfaces.class.getInterfaces()[0]), converter);
    }

    @Test
    public void testFindObjectConverterConverterForInterfaceAndClassConverterSubclasses() {

        TheAbstractConverter<ClassForTheInterface> theConverter = new TheAbstractConverter<ClassForTheInterface>() {};

        MapperConfig config = createConfig(Collections.<Class<?>, ObjectConverter.Codec<?>>singletonMap(ClassForTheInterface.class, theConverter));

        {
            ObjectConverter.Writer converter = config.findObjectConverterWriter(ClassForTheInterface.class);
            Assert.assertNotNull(converter);
            Assert.assertEquals(theConverter, converter);
        }

        {
            ObjectConverter.Writer converter = config.findObjectConverterWriter(ExtendingClassForTheInterface.class);
            Assert.assertNotNull(converter);
            Assert.assertEquals(theConverter, converter);
        }
    }

    @Test
    public void testfindObjectConverterConverterForObject() {
        TheConverter<Object> theConverter = new TheConverter<Object>();

        MapperConfig config = createConfig(Collections.<Class<?>, ObjectConverter.Codec<?>>singletonMap(Object.class, theConverter));

        ObjectConverter.Reader converter = config.findObjectConverterReader(ClassForTheInterface.class);
        Assert.assertNotNull(converter);
        Assert.assertEquals(theConverter, converter);

        converter = config.findObjectConverterReader(TheInterface.class);
        Assert.assertNotNull(converter);
        Assert.assertEquals(theConverter, converter);

        converter = config.findObjectConverterReader(InterfaceExtendingTwoInterfaces.class);
        Assert.assertNotNull(converter);
        Assert.assertEquals(theConverter, converter);
    }


    private MapperConfig createConfig(Map<Class<?>, ObjectConverter.Codec<?>> converter) {
        return new MapperConfig(new ConcurrentHashMap<AdapterKey, Adapter<?, ?>>(0),
                                Map.class.cast(converter), Map.class.cast(converter),
                                -1,
                                true,
                                true,
                                true,
                                false,
                                false,
                                false,
                                new FieldAccessMode(true, true),
                                Charset.forName("UTF-8"),
                                null,
                                false, false, null, false, false);
    }


    private static final class ClassWithoutSupertypes {}

    private interface TheInterface {}
    private static class ClassForTheInterface implements TheInterface {}
    private static class ExtendingClassForTheInterface extends ClassForTheInterface {}

    private interface TheSecondInterface {}
    private static class ClassWithTwoInterfaces implements TheInterface, TheSecondInterface {}

    private interface InterfaceExtendingTwoInterfaces extends TheInterface, TheSecondInterface {}


    private static class TheConverter<T> implements ObjectConverter.Codec<T>{
        @Override
        public void writeJson(T instance, MappingGenerator jsonbGenerator) {
            // dummy
        }

        @Override
        public T fromJson(JsonObject jsonObject, Type targetType, MappingParser parser) {
            // dummy
            return null;
        }
    }

    private static abstract class TheAbstractConverter<T extends TheInterface> implements ObjectConverter.Codec<T> {
        @Override
        public void writeJson(T instance, MappingGenerator jsonbGenerator) {
            // dummy
        }

        @Override
        public T fromJson(JsonObject jsonObject, Type targetType, MappingParser parser) {
            // dummy
            return null;
        }
    }
}
