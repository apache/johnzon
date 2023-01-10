package org.apache.johnzon.jsonb;
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


import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonString;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.json.bind.config.PropertyOrderStrategy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AdapterTest {
    public static class PathAdapter implements JsonbAdapter<Path, JsonString> {
        @Override
        public JsonString adaptToJson(final Path path) {
            return Json.createValue(path.toString());
        }

        @Override
        public Path adaptFromJson(final JsonString jsonString) {
            return Paths.get(jsonString.getString());
        }
    }

    public static class PathWrapper {
        public Path path;
    }

    @Test
    public void testSerialize() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withAdapters(new PathAdapter()))) {
            final PathWrapper wrapper = new PathWrapper();
            wrapper.path = Paths.get("/example/file.txt");

            if (System.getProperty("os.name").contains("Windows")) {
                assertEquals("{\"path\":\"\\\\example\\\\file.txt\"}", jsonb.toJson(wrapper));
                assertEquals("\"\\\\example\\\\file.txt\"", jsonb.toJson(wrapper.path));
            } else {
                assertEquals("{\"path\":\"/example/file.txt\"}", jsonb.toJson(wrapper));
                assertEquals("\"/example/file.txt\"", jsonb.toJson(wrapper.path));
            }
        }
    }

    @Test
    public void testDeserialize() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withAdapters(new PathAdapter()))) {
            final Path expected = Paths.get("/example/file.txt");
            assertEquals(expected, jsonb.fromJson("{\"path\":\"/example/file.txt\"}", PathWrapper.class).path);
            assertEquals(expected, jsonb.fromJson("\"/example/file.txt\"", Path.class));
        }
    }

    @Test
    public void adapt() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withAdapters(new BarAdapter()))) {
            final Foo foo = new Foo();
            foo.bar = new Bar();
            foo.bar.value = 1;
            foo.dummy = new Dummy();
            foo.dummy.value = 2L;
            foo.baz = new Baz();
            foo.baz.value = "3";

            final String toString = jsonb.toJson(foo);
            assertThat(toString, startsWith("{"));
            assertThat(toString, containsString("\"bar\":\"1\""));
            assertThat(toString, containsString("\"dummy\":\"2\""));
            assertThat(toString, containsString("\"baz\":\"3\""));
            assertThat(toString, endsWith("}"));
            assertEquals("{\"bar\":\"1\",\"dummy\":\"2\",\"baz\":\"3\"}".length(), toString.length());

            final Foo read = jsonb.fromJson(toString, Foo.class);
            assertEquals(foo.bar.value, read.bar.value);
            assertEquals(foo.dummy.value, read.dummy.value);
            assertEquals(foo.baz.value, read.baz.value);
        }
    }

    @Test
    public void adaptValue() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final Baz baz = new Baz();
            baz.value = "test";

            final String toString = jsonb.toJson(baz);
            assertEquals("\"test\"", toString);
        }
    }

    @Test
    public void adaptJson() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withAdapters(new Dummy2Adapter()))) {
            final Foo2 foo = new Foo2();
            foo.dummy = new Dummy2();
            foo.dummy.value = 2L;

            final String toString = jsonb.toJson(foo);
            assertEquals("{\"dummy\":\"2\"}", toString);

            final Foo2 read = jsonb.fromJson(toString, Foo2.class);
            assertEquals(foo.dummy.value, read.dummy.value);
        }
    }

    @Test
    public void notYetPloymorphism() { // we run it since it checked list/item conversion
        final Bar bar = new Bar();
        bar.value = 11;

        final Bar2 bar2 = new Bar2();
        bar2.value = 21;
        bar2.value2 = 22;

        final Polymorphism foo = new Polymorphism();
        foo.bars = new ArrayList<>(asList(bar, bar2));

        final Jsonb jsonb = JsonbBuilder.create(
                new JsonbConfig()
                        .setProperty("johnzon.readAttributeBeforeWrite", true)
                        .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL) /* assertEquals() order */);

        final String toString = jsonb.toJson(foo);
        assertEquals("{\"bars\":[" +
                "{\"type\":\"org.apache.johnzon.jsonb.AdapterTest$Bar\",\"value\":{\"value\":11}}," +
                "{\"type\":\"org.apache.johnzon.jsonb.AdapterTest$Bar2\",\"value\":{\"value\":21,\"value2\":22}}]}", toString);

        final Polymorphism read = jsonb.fromJson(toString, Polymorphism.class);
        assertEquals(2, read.bars.size());
        assertEquals(11, read.bars.get(0).value);
        assertTrue(Bar.class == read.bars.get(0).getClass());
        assertEquals(21, read.bars.get(1).value);
        assertTrue(Bar2.class == read.bars.get(1).getClass());
        assertEquals(22, Bar2.class.cast(read.bars.get(1)).value2);
    }

    @Test
    public void adaptCollectionValue() {
        final Jsonb jsonb = JsonbBuilder.create();

        Bar bar = new Bar();
        bar.value = 42;
        BarCollection barCollection = new BarCollection();
        barCollection.collection = new ArrayList<Bar>();
        barCollection.collection.add(bar);

        String barCollectionJson = jsonb.toJson(barCollection);
        assertTrue(barCollectionJson.contains("42"));

        BarCollection adaptedBarCollection = jsonb.fromJson(barCollectionJson, BarCollection.class);
        assertEquals(1, adaptedBarCollection.collection.size());
        assertEquals(42, adaptedBarCollection.collection.get(0).value);

        Baz baz = new Baz();
        baz.value = "42";
        BazCollection bazCollection = new BazCollection();
        bazCollection.collection = new ArrayList<Baz>();
        bazCollection.collection.add(baz);

        String bazCollectionJson = jsonb.toJson(bazCollection);
        assertTrue(bazCollectionJson.contains("42"));

        BazCollection adaptedBazCollection = jsonb.fromJson(bazCollectionJson, BazCollection.class);
        assertEquals(1, adaptedBazCollection.collection.size());
        assertEquals("42", adaptedBazCollection.collection.get(0).value);
    }

    public static class BarCollection {
        @JsonbTypeAdapter(BarAdapter.class)
        public List<Bar> collection;
    }

    public static class BazCollection {
        public List<Baz> collection;
    }

    public static class Polymorphism {
        @JsonbTypeAdapter(PolyBarAdapter.class)
        public List<Bar> bars;
    }


    public static class TypeInstance {
        private String type;
        private Bar value;

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
            try {
                this.value = Bar.class.cast(Thread.currentThread().getContextClassLoader().loadClass(type).newInstance());
            } catch (final InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public Bar getValue() {
            return value;
        }

        public void setValue(final Bar value) {
            this.value = value;
        }
    }


    public static class Foo2 {
        @JsonbTypeAdapter(Dummy2Adapter.class)
        public Dummy2 dummy;
    }


    public static class Foo {
        public Bar bar;

        @JsonbTypeAdapter(DummyAdapter.class)
        public Dummy dummy;

        public Baz baz;
    }

    public static class Bar2 extends Bar {
        public int value2;
    }

    public static class Bar {
        public int value;
    }

    public static class PolyBarAdapter implements JsonbAdapter<Bar, TypeInstance> {
        @Override
        public Bar adaptFromJson(final TypeInstance obj) throws Exception {
            return obj.value;
        }

        @Override
        public TypeInstance adaptToJson(final Bar obj) throws Exception {
            final TypeInstance typeInstance = new TypeInstance();
            typeInstance.type = obj.getClass().getName();
            typeInstance.value = obj;
            return typeInstance;
        }
    }

    @JsonbTypeAdapter(BazAdapter.class)
    public static class Baz {
        public String value;
    }

    public static class BazAdapter implements JsonbAdapter<Baz, String> {

        @Override
        public String adaptToJson(Baz obj) throws Exception {
            return obj.value;
        }

        @Override
        public Baz adaptFromJson(String obj) throws Exception {
            Baz baz = new Baz();
            baz.value = obj;
            return baz;
        }

    }

    public static class Dummy2 {
        public long value;
    }

    public static class Dummy {
        public long value;
    }

    public static class Dummy2Adapter implements JsonbAdapter<Dummy2, JsonString> {
        @Override
        public Dummy2 adaptFromJson(final JsonString obj) {
            final Dummy2 bar = new Dummy2();
            bar.value = Long.parseLong(obj.getString());
            return bar;
        }

        @Override
        public JsonString adaptToJson(final Dummy2 obj) {
            return Json.createValue(Long.toString(obj.value));
        }
    }

    public static class DummyAdapter implements JsonbAdapter<Dummy, String> {
        @Override
        public Dummy adaptFromJson(final String obj) throws Exception {
            final Dummy bar = new Dummy();
            bar.value = Long.parseLong(obj);
            return bar;
        }

        @Override
        public String adaptToJson(final Dummy obj) throws Exception {
            return Long.toString(obj.value);
        }
    }

    public static class BarAdapter implements JsonbAdapter<Bar, String> {
        @Override
        public String adaptToJson(final Bar obj) throws Exception {
            return Integer.toString(obj.value);
        }

        @Override
        public Bar adaptFromJson(final String obj) throws Exception {
            final Bar bar = new Bar();
            bar.value = Integer.parseInt(obj);
            return bar;
        }
    }

    @Test
    public void testAdapterOnEnum() {
        Jsonb jsonb = JsonbBuilder.newBuilder().build();

        DoorDTO door = new DoorDTO();
        door.status = DoorStatus.OPEN;
        String jsonS = jsonb.toJson(door);
        assertEquals(
                "The expected result must be a door with a status open as its enum ordinal",
                "{\"status\":0}",
                jsonS);

        DoorDTO doorDTO = jsonb.fromJson(jsonS, DoorDTO.class);

        assertEquals(
                "The expected result must be a door with a status open as an enum value",
                DoorStatus.OPEN,
                doorDTO.status);
    }

    public static class DoorDTO {
        @JsonbTypeAdapter(StatusAdapter.class)
        public DoorStatus status;
    }

    public enum DoorStatus {
        OPEN, CLOSE
    }

    public static class StatusAdapter implements JsonbAdapter<DoorStatus, Integer> {

        @Override
        public Integer adaptToJson(DoorStatus obj) {
            return obj.ordinal();
        }

        @Override
        public DoorStatus adaptFromJson(Integer obj) {
            return DoorStatus.values()[obj];
        }

    }
}
