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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;
import org.junit.Test;
import org.superbiz.Model;

public class GenericsTest {
    @Test
    public void multipleBounds() {
        final LinkedList<String> list = new LinkedList<>(
                Arrays.asList("Test 1", "Test 2"));
        final HolderWithMultipleBounds<LinkedList<String>> wrapper = new HolderWithMultipleBounds<>();
        wrapper.setInstance(new ArrayList<>());
        wrapper.getInstance().add(list);

        final Mapper mapper = new MapperBuilder().build();
        final String json = mapper.writeObjectAsString(wrapper);
        assertEquals("{\"instance\":[[\"Test 1\",\"Test 2\"]]}", json);

        final Type type = new HolderWithMultipleBounds<LinkedList<String>>() {
        }.getClass().getGenericSuperclass();
        final HolderWithMultipleBounds deserialized = mapper
                .readObject("{ \"instance\" : [[ \"Test 1\", \"Test 2\" ]] }", type);
        assertEquals(wrapper.getInstance(), deserialized.getInstance());
        mapper.close();
    }

    @Test
    public void noVariable() {
        final LinkedList<String> list = new LinkedList<>(
                Arrays.asList("Test 1", "Test 2"));
        final HolderWithMultipleBounds<LinkedList<String>> wrapper = new HolderWithMultipleBounds<>();
        wrapper.setInstance(new ArrayList<>());
        wrapper.getInstance().add(list);

        final Mapper mapper = new MapperBuilder().build();
        final String json = mapper.writeObjectAsString(wrapper);
        assertEquals("{\"instance\":[[\"Test 1\",\"Test 2\"]]}", json);

        final HolderWithMultipleBounds deserialized = mapper
                .readObject("{ \"instance\" : [[ \"Test 1\", \"Test 2\" ]] }", HolderWithMultipleBounds.class);
        assertEquals(wrapper.getInstance(), deserialized.getInstance());
        mapper.close();
    }

    @Test
    public void missingGeneric() {
        final Mapper mapper = new MapperBuilder().build();
        final ListHolder deserialized = mapper
                .readObject("{ \"instance\" : [[ \"Test 1\", \"Test 2\" ]] }", ListHolder.class);
        assertEquals(singletonList(asList("Test 1", "Test 2")), deserialized.getInstance());
        mapper.close();
    }

    @Test
    public void wildcardGeneric() {
        final Mapper mapper = new MapperBuilder().build();
        final WildcardListHolder deserialized = mapper
                .readObject("{ \"instance\" : [[ \"Test 1\", \"Test 2\" ]] }", WildcardListHolder.class);
        assertEquals(singletonList(asList("Test 1", "Test 2")), deserialized.getInstance());
        mapper.close();
    }

    @Test
    public void typeVariableMultiLevel() {
        final String input = "{\"aalist\":[{\"detail\":\"something2\",\"name\":\"Na2\"}]," +
                "\"childA\":{\"detail\":\"something\",\"name\":\"Na\"},\"childB\":{}}";
        try (final Mapper mapper = new MapperBuilder().setAttributeOrder(String::compareTo).build()) {
            final Model model = mapper.readObject(input, Model.class);
            assertNotNull(model.getChildA());
            assertNotNull(model.getChildB());
            assertNotNull(model.getAalist());
            assertEquals("something", model.getChildA().detail);
            assertEquals("Na", model.getChildA().name);
            assertEquals(1, model.getAalist().size());
            assertEquals("something2", model.getAalist().iterator().next().detail);
            assertEquals(input, mapper.writeObjectAsString(model));
        }
    }

    @Test
    public void genericClasses() {
        final String input = "{\"aalist\":[{\"name\":\"Na2\"}]}";
        try (final Mapper mapper = new MapperBuilder().setAttributeOrder(String::compareTo).build()) {
            final GenericModel<SimpleModel> model = mapper.readObject(
                    input, new JohnzonParameterizedType(GenericModel.class, SimpleModel.class));
            assertNotNull(model.aalist);
            assertEquals(1, model.aalist.size());
            assertEquals("Na2", model.aalist.get(0).name);
            assertEquals(input, mapper.writeObjectAsString(model));
        }
    }

    public static class SimpleModel {
        public String name;
    }

    public static class GenericModel<T> {
        public List<T> aalist;
    }

    public interface Holder<T> {
        T getInstance();
        void setInstance(T t);
    }

    public static class HolderWithMultipleBounds<T extends List & Queue> implements Holder<List<T>> {
        protected List<T> instance;

        @Override
        public List<T> getInstance() {
            return instance;
        }

        @Override
        public void setInstance(final List<T> instance) {
            this.instance = instance;
        }
    }

    public static class ListHolder implements Holder<List> {
        protected List instance;

        @Override
        public List getInstance() {
            return instance;
        }

        @Override
        public void setInstance(final List instance) {
            this.instance = instance;
        }
    }

    public static class WildcardListHolder implements Holder<List<?>> {
        protected List<?> instance;

        @Override
        public List<?> getInstance() {
            return instance;
        }

        @Override
        public void setInstance(final List<?> instance) {
            this.instance = instance;
        }
    }
}
