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

import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;
import org.junit.Ignore;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// taken from the examples of the spec
// TODO: bunch of asserts
//CHECKSTYLE:OFF
public class DefaultMappingTest {
    private static final Jsonb JSONB = JsonbBuilder.create();

    @Test
    @Ignore("should it be supported")
    public void primitives() throws Exception {
        fromJsonPrimitives();
        toJsonPrimitives();
        fromJsonURLURI();
        toJsonURLURI();
    }

    @Test
    public void naming() throws Exception {
        toJsonDefaultNames();
        fromJsonDefaultNames();
    }

    @Test
    public void collections() {
        fromJsonCollections();
        toJsonCollection();
    }

    @Test
    public void arrays() {
        fromJsonArrays();
        toJsonArrays();
    }

    @Test
    public void structures() throws Exception {
        fromJsonStructures();
        toJsonStructures();
    }

    @Test
    public void pojos() throws Exception {
        fromJsonPOJOs();
        toJsonPOJOs();
    }

    @Test
    public void inheritance() throws Exception {
        fromJsonInheritance();
        toJsonInheritance();
    }

    @Test
    public void anonymous() throws Exception {
        toJsonAnonymousClass();
    }

    @Test
    public void instantiation() throws Exception {
        fromJsonInstantiation();
    }

    @Test
    public void order() throws Exception {
        toJsonAttributesOrdering();
    }

    @Test
    public void nulls() throws Exception {
        toJsonNullValues();
        fromJsonNullValues();
    }

    @Test
    public void modifiers() throws Exception {
        toJsonModifiers();
        fromJsonModifiers();
    }

    @Test
    public void optionals() throws Exception {
        toJsonOptional();
        fromJsonOptional();
    }

    @Test
    public void accessors() throws Exception {
        toJsonAccessors();
        fromJsonAccessors();
    }

    @Test
    public void simpleValues() {
        assertEquals("\"strValue\"", JSONB.toJson("\"strValue\""));
        assertEquals("true", JSONB.toJson("true"));
        assertEquals("false", JSONB.toJson("false"));
        assertEquals("null", JSONB.toJson("null"));
        assertEquals("strValue", JSONB.toJson(Optional.of("strValue")));
        assertEquals("null", JSONB.toJson(Optional.ofNullable(null)));
        assertEquals("null", JSONB.toJson(Optional.empty()));
        assertEquals("1", JSONB.toJson(OptionalInt.of(1)));
        assertEquals("null", JSONB.toJson(OptionalInt.empty()));
        assertEquals("123", JSONB.toJson(OptionalLong.of(123)));
        assertEquals("null", JSONB.toJson(OptionalLong.empty()));
        assertEquals("1.2", JSONB.toJson(OptionalDouble.of(1.2)));
        assertEquals("null", JSONB.toJson(OptionalDouble.empty()));

        //Optional
        Optional<String> stringValue = JSONB.fromJson("\"optionalString\"", new JohnzonParameterizedType(Optional.class, String.class));
        assertTrue(stringValue.isPresent());
        assertEquals("optionalString", stringValue.get());

        Optional<String> nullStringValue = JSONB.fromJson("null", new JohnzonParameterizedType(Optional.class, String.class));
        assertTrue(!nullStringValue.isPresent());

        //OptionalInt
        OptionalInt optionalInt = JSONB.fromJson("1", OptionalInt.class);
        assertTrue(optionalInt.isPresent());
        assertTrue(optionalInt.getAsInt() == 1);

        OptionalInt emptyOptionalInt = JSONB.fromJson("null", OptionalInt.class);
        assertTrue(!emptyOptionalInt.isPresent());

        //OptionalLong
        OptionalLong optionalLong = JSONB.fromJson("123", OptionalLong.class);
        assertTrue(optionalLong.isPresent());
        assertTrue(optionalLong.getAsLong() == 123L);

        OptionalLong emptyOptionalLong = JSONB.fromJson("null", OptionalLong.class);
        assertTrue(!emptyOptionalLong.isPresent());

        //OptionalDouble
        OptionalDouble optionalDouble = JSONB.fromJson("1.2", OptionalDouble.class);
        assertTrue(optionalDouble.isPresent());
        assertTrue(optionalDouble.getAsDouble() == 1.2);

        OptionalDouble emptyOptionalDouble = JSONB.fromJson("null", OptionalDouble.class);
        assertTrue(!emptyOptionalDouble.isPresent());
    }

    public static void fromJsonPrimitives() {
        //String
        String str = JSONB.fromJson("\"some_string\"", String.class);

        //String escaping
        String escapedString = JSONB.fromJson("\" \\\" \\\\ \\/ \\b \\f \\n \\r \\t \\u0039\"", String.class);
        assertEquals(" \" \\ / \b \f \n \r \t 9", escapedString);

        //Character
        Character ch = JSONB.fromJson("\"\uFFFF\"", Character.class);

        //Byte
        Byte byte1 = JSONB.fromJson("1", Byte.class);

        //Short
        Short short1 = JSONB.fromJson("1", Short.class);

        //Integer
        Integer int1 = JSONB.fromJson("1", Integer.class);

        //Long
        Long long1 = JSONB.fromJson("1", Long.class);

        //Float
        Float float1 = JSONB.fromJson("1.2", Float.class);

        //Double
        Double double1 = JSONB.fromJson("1.2", Double.class);

        //BigInteger
        BigInteger bigInteger = JSONB.fromJson("1", BigInteger.class);

        //BigDecimal
        BigDecimal bigDecimal = JSONB.fromJson("1.2", BigDecimal.class);

        //Number
        Number number = JSONB.fromJson("1.2", Number.class);

        //Boolean
        Boolean trueValue = JSONB.fromJson("true", Boolean.class);

        //Boolean
        Boolean falseValue = JSONB.fromJson("false", Boolean.class);

        //null
        Object nullValue = JSONB.fromJson("null", Object.class);

        assertTrue(nullValue == null);
    }

    public static void exceptions() {
        //Exception
        //fail fast strategy by default

        //incompatible types
        try {
            JSONB.fromJson("not_a_number", Integer.class);
            assertTrue(false);
        } catch (JsonbException e) {
        }

        //incompatible types
        try {
            JSONB.fromJson("[null,1]", int[].class);
            assertTrue(false);
        } catch (JsonbException e) {
        }

        //bad structure
        try {
            JSONB.fromJson("[1,2", int[].class);
            assertTrue(false);
        } catch (JsonbException e) {
        }

        //overflow - Value out of range
        try {
            JSONB.fromJson("" + new Integer(Byte.MAX_VALUE + 1) + "", Byte.class);
            assertTrue(false);
        } catch (JsonbException e) {
        }

        //underflow - Value out of range
        try {
            JSONB.fromJson("" + new Integer(Byte.MIN_VALUE - 1) + "", Byte.class);
            assertTrue(false);
        } catch (JsonbException e) {
        }
    }

    public static void toJsonPrimitives() {

        //String
        assertEquals("\"some_string\"", JSONB.toJson("some_string"));

        //escaped String
        assertEquals("\" \\\\ \\\" / \\b \\f \\n \\r \\t 9\"", JSONB.toJson(" \\ \" / \b \f \n \r \t \u0039"));

        //Character
        assertEquals("\"\uFFFF\"", JSONB.toJson('\uFFFF'));

        //Byte
        assertEquals("1", JSONB.toJson((byte) 1));

        //Short
        assertEquals("1", JSONB.toJson((short) 1));

        //Integer
        assertEquals("1", JSONB.toJson(1));

        //Long
        assertEquals("5", JSONB.toJson(5L));

        //Float
        assertEquals("1.2", JSONB.toJson(1.2f));

        //Double
        assertEquals("1.2", JSONB.toJson(1.2));

        //BigInteger
        assertEquals("1", JSONB.toJson(new BigInteger("1")));

        //BigDecimal
        assertEquals("1.2", JSONB.toJson(new BigDecimal("1.2")));

        //Number
        assertEquals("1.2", JSONB.toJson((java.lang.Number) 1.2));

        //Boolean true
        assertEquals("true", JSONB.toJson(true));

        //Boolean false
        assertEquals("false", JSONB.toJson(false));

        //null
        assertEquals("null", JSONB.toJson(null));
    }

    public static void fromJsonStructures() {

        //Map
        Map<String, Object> map = (Map<String, Object>) JSONB.fromJson("{\"name\":\"unknown object\"}", Object.class);

        //mapping for number  -> Integer, Long, Double
        Map<String, Object> mapWithBigDecimal = (Map<String, Object>) JSONB.fromJson("{\"intValue\":5,\"longValue\":17179869184,\"doubleValue\":1.2}", Object.class);
        assertTrue(mapWithBigDecimal.get("intValue") instanceof Integer);
        assertTrue(mapWithBigDecimal.get("longValue") instanceof Long);
        assertTrue(mapWithBigDecimal.get("doubleValue") instanceof Double);

        //Collection
        /* why collection and not array or sthg else?
        Collection<Object> collection = (Collection<Object>) JSONB.fromJson("[{\"value\":\"first\"}, {\"value\":\"second\"}]", Object.class);
        */

        //JsonStructure
        assertNotNull(JSONB.fromJson("{\"name\":\"unknown object\"}", JsonStructure.class));

        //JsonObject
        assertNotNull(JSONB.fromJson("{\"name\":\"unknown object\"}", JsonObject.class));

        //JsonArray
        assertNotNull(JSONB.fromJson("[{\"value\":\"first\"},{\"value\":\"second\"}]", JsonArray.class));

        //JsonValue
        assertNotNull(JSONB.fromJson("1", JsonValue.class));
    }

    public static void toJsonStructures() {

        JsonBuilderFactory factory = Json.createBuilderFactory(Collections.emptyMap());
        JsonObject jsonObject = factory.createObjectBuilder().
            add("name", "home").
            add("city", "Prague")
            .build();

        //JsonObject
        assertEquals("{\"name\":\"home\",\"city\":\"Prague\"}", JSONB.toJson(jsonObject));

        JsonArray jsonArray = factory.createArrayBuilder().add(jsonObject).add(jsonObject).build();

        //JsonArray
        assertEquals("[{\"name\":\"home\",\"city\":\"Prague\"},{\"name\":\"home\",\"city\":\"Prague\"}]", JSONB.toJson(jsonArray));

        //JsonStructure
        assertEquals("[{\"name\":\"home\",\"city\":\"Prague\"},{\"name\":\"home\",\"city\":\"Prague\"}]", JSONB.toJson((JsonStructure) jsonArray));

        //JsonValue
        assertEquals("true", JSONB.toJson(JsonValue.TRUE));

        //Map
        Map<String, Object> commonMap = new LinkedHashMap<>();
        commonMap.put("first", 1);
        commonMap.put("second", 2);

        assertEquals("{\"first\":1,\"second\":2}", JSONB.toJson(commonMap));

        //Collection
        Collection<Object> commonList = new ArrayList<>();
        commonList.add(1);
        commonList.add(2);

        assertEquals("[1,2]", JSONB.toJson(commonList));
    }

    public static void fromJsonCollections() {

        //support deserialization of java.util.Collection and java.util.Map and its subinterfaces and implementing (sub)classes

        //Collection, Map

        //Set, HashSet, NavigableSet, SortedSet, TreeSet, LinkedHashSet, TreeHashSet

        //HashMap, NavigableMap, SortedMap, TreeMap, LinkedHashMap, TreeHashMap

        //List, ArrayList, LinkedList

        //Deque, ArrayDeque, Queue, PriorityQueue

        Collection<Object> collection = JSONB.fromJson("[\"first\",\"second\"]", Collection.class);

        Map<String, Object> map = JSONB.fromJson("{\"first\":\"second\"}", Map.class);

        //concrete implementation of Map
        HashMap<String, Object> hashMap = JSONB.fromJson("{\"first\":\"second\"}", HashMap.class);

        //concrete implementation of Collection
        ArrayList<Object> arrayList = JSONB.fromJson("[\"first\",\"second\"]", ArrayList.class);

        //deque
        Deque<String> dequeList = JSONB.fromJson("[\"first\",\"second\"]", Deque.class);
        assertEquals(2, dequeList.size());
        assertEquals("first", dequeList.getFirst());
        assertEquals("second", dequeList.getLast());

        //JSON Binding supports default deserialization of the following interfaces
        //syntax: interface -> default implementation

        //Collection -> ArrayList
        //Set -> HashSet
        //NavigableSet -> TreeSet
        //SortedSet -> TreeSet
        //Map -> HashMap
        //SortedMap -> TreeMap
        //NavigableMap -> TreeMap
        //Deque -> ArrayDeque
        //Queue -> ArrayDeque

        //any implementation of Collection and Map with public default constructor is deserializable

    }

    public static void toJsonCollection() {
        Collection<Integer> collection = Arrays.asList(1, 2, 3);

        assertEquals("[1,2,3]", JSONB.toJson(collection));

        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);

        assertEquals("{\"1\":1,\"2\":2,\"3\":3}", JSONB.toJson(map));

        //any implementation of Collection and Map is serializable

        //deque
        Deque<String> deque = new ArrayDeque<>();
        deque.add("first");
        deque.add("second");

        assertEquals("[\"first\",\"second\"]", JSONB.toJson(deque));
    }

    public static void fromJsonArrays() {

        //support of arrays of types that JSON Binding is able to deserialize
        //Byte[], Short[], Integer[] Long[], Float[], Double[], BigInteger[], BigDecimal[], Number[]
        //Object[], JsonArray[], JsonObject[], JsonStructure[]
        //String[], Character[]
        //byte[], short[], int[], long[], float[], double[], char[], boolean[]
        //java.net.URL[], java.net.URI[]
        //Map[], Collection[], other collections ...
        //enum, EnumSet, EnumMap
        //support of multidimensional arrays


        //Several examples

        //Byte arrays
        Byte[] byteArray = JSONB.fromJson("[1,2]", Byte[].class);

        //Integer array
        Integer[] integerArray = JSONB.fromJson("[1,2]", Integer[].class);

        //int array
        int[] intArray = JSONB.fromJson("[1,2]", int[].class);

        //String arrays
        String[] stringArray = JSONB.fromJson("[\"first\",\"second\"]", String[].class);

        //multidimensional arrays
        String[][] stringMultiArray = JSONB.fromJson("[[\"first\", \"second\"], [\"third\" , \"fourth\"]]", String[][].class);

        //default mapping should handle multidimensional arrays of types supported by default mapping, e.g. Map
        Map<String, Object>[][] mapMultiArray = JSONB.fromJson("[[{\"1\":2}, {\"3\":4}],[{\"5\":6},{\"7\":8}]]", Map[][].class);
    }

    public static void toJsonArrays() {

        //support of arrays of types that JSON Binding is able to serialize
        //Byte[], Short[], Integer[] Long[], Float[], Double[], BigInteger[], BigDecimal[], Number[]
        //Object[], JsonArray[], JsonObject[], JsonStructure[]
        //String[], Character[]
        //byte[], short[], int[], long[], float[], double[], char[], boolean[]
        //java.net.URL[], java.net.URI[]
        //Map[], Collection[], other collections ...
        //enum, EnumSet, EnumMap
        //support of multidimensional arrays


        //Several examples

        Byte[] byteArray = {1, 2, 3};

        assertEquals("[1,2,3]", JSONB.toJson(byteArray));

        Integer[] integerArray = {1, 2, 3};

        assertEquals("[1,2,3]", JSONB.toJson(integerArray));

        int[] intArray = {1, 2, 3};

        assertEquals("[1,2,3]", JSONB.toJson(intArray));

        String[] stringArray = {"first", "second", "third"};

        assertEquals("[\"first\",\"second\",\"third\"]", JSONB.toJson(stringArray));

        String[][] stringMultiArray = {{"first", "second"}, {"third", "fourth"}};

        assertEquals("[[\"first\",\"second\"],[\"third\",\"fourth\"]]", JSONB.toJson(stringMultiArray));

        Map<String, Object>[][] mapMultiArray = new LinkedHashMap[2][2];

        mapMultiArray[0][0] = new LinkedHashMap<>(1);
        mapMultiArray[0][0].put("0", 0);
        mapMultiArray[0][1] = new LinkedHashMap<>(1);
        mapMultiArray[0][1].put("0", 1);
        mapMultiArray[1][0] = new LinkedHashMap<>(1);
        mapMultiArray[1][0].put("1", 0);
        mapMultiArray[1][1] = new LinkedHashMap<>(1);
        mapMultiArray[1][1].put("1", 1);

        assertEquals("[[{\"0\":0},{\"0\":1}],[{\"1\":0},{\"1\":1}]]", JSONB.toJson(mapMultiArray));
    }

    public EnumSet<Language> languageEnumSet = EnumSet.of(Language.Czech);
    public EnumMap<Language, String> languageEnumMap = new EnumMap<>(Language.class);

    public static void fromJsonEnums() throws Exception {

        EnumSet<Language> languageEnumSet = JSONB.fromJson("[\"Slovak\", \"English\"]", DefaultMappingTest.class.getField("languageEnumSet").getGenericType());

        EnumMap<Language, String> languageEnumMap = JSONB.fromJson("[\"Slovak\" : \"sk\", \"Czech\" : \"cz\"]", DefaultMappingTest.class.getField("languageEnumMap").getGenericType());
    }

    public static void toJsonEnums() {

        Language language = Language.Slovak;

        assertEquals("\"Slovak\"", JSONB.toJson(language));

        EnumSet<Language> languageEnumSet = EnumSet.of(Language.Czech, Language.Slovak);

        assertEquals("\"Czech\",\"Slovak\"", JSONB.toJson(languageEnumSet));

        EnumMap<Language, String> languageEnumMap = new EnumMap<>(Language.class);
        languageEnumMap.put(Language.Czech, "cz");
        languageEnumMap.put(Language.English, "en");

        assertEquals("{\"Czech\":\"cz\",\"English\":\"en\"}", languageEnumMap);
    }

    private enum Language {
        English, Slovak, Czech
    }

    public static void fromJsonPOJOs() {

        POJO pojo = JSONB.fromJson("{\"id\":1, \"name\":\"pojoName\"}", POJO.class);

        POJO nullPOJO = JSONB.fromJson("{\"id\":1, \"name\":null}", POJO.class);
        assertTrue(null == nullPOJO.name);

        //just public nested class
        try {
            POJOWithNestedClass pojoWithNestedClass = JSONB.fromJson("{\"id\":1, \"name\":\"pojo_name\", \"nestedClass\" : {\"nestedId\":2, \"nestedName\" : \"nestedPojoName\"}}", POJOWithNestedClass.class);
            fail();
        } catch (final JsonbException e) {
            // ok
        }

        //just public nested class
        try {
            POJOWithNestedClass.NestedClass nestedClass = JSONB.fromJson("{\"nestedId\":2, \"nestedName\" : \"nestedPojoName\"}", POJOWithNestedClass.NestedClass.class);
            fail();
        } catch (final JsonbException e) {
            // ok
        }

        POJOWithStaticNestedClass pojoWithStaticNestedClass = JSONB.fromJson("{\"id\":1, \"name\":\"pojoName\"}", POJOWithStaticNestedClass.class);

        POJOWithStaticNestedClass.StaticNestedClass staticNestedClass = JSONB.fromJson("{\"nestedId\":2, \"nestedName\" : \"nestedPojoName\"}", POJOWithStaticNestedClass.StaticNestedClass.class);

        POJOWithMixedFieldAccess pojoWithMixedFieldAccess =
            JSONB.fromJson("{\"id\":5, \"name\":\"new_name\", \"active\":true}"/*, \"valid\":true}"*/, POJOWithMixedFieldAccess.class);

        assertTrue(pojoWithMixedFieldAccess.id.intValue() == 10);
        assertTrue(pojoWithMixedFieldAccess.name.equals("new_name"));
        assertTrue(pojoWithMixedFieldAccess.active);
        assertNull(pojoWithMixedFieldAccess.valid);

        //composite class
        CompositePOJO compositePOJO = JSONB.fromJson(
            "{\"compositeId\":\"13\"," +
                "\"stringArray\":[\"first\",\"second\"],\"stringList\":[\"one\",\"two\"]}", CompositePOJO.class);
    }

    public static void toJsonPOJOs() {

        POJO pojo = new POJO();
        pojo.setId(1);
        pojo.setName("pojoName");

        assertEquals("{\"id\":1,\"name\":\"pojoName\"}", JSONB.toJson(pojo));

        //pojo with nested class
        POJOWithNestedClass pojoWithNestedClass = new POJOWithNestedClass();
        pojoWithNestedClass.setName("pojoName");
        pojoWithNestedClass.setId(1);

        POJOWithNestedClass.NestedClass nestedClass = pojoWithNestedClass.new NestedClass();
        nestedClass.setNestedId(2);
        nestedClass.setNestedName("nestedPojoName");

        pojoWithNestedClass.setNestedClass(nestedClass);

        assertEquals("{\"id\":1,\"name\":\"pojoName\",\"nestedClass\":{\"nestedId\":2,\"nestedName\":\"nestedPojoName\"}}", JSONB.toJson(pojoWithNestedClass));

        //nested class
        assertEquals("{\"nestedId\":2,\"nestedName\":\"nestedPojoName\"}", JSONB.toJson(nestedClass));

        //pojo with static nested class
        POJOWithStaticNestedClass pojoWithStaticNestedClass = new POJOWithStaticNestedClass();
        pojoWithStaticNestedClass.setId(1);
        pojoWithStaticNestedClass.setName("pojoName");

        assertEquals("{\"id\":1,\"name\":\"pojoName\"}", JSONB.toJson(pojoWithStaticNestedClass));

        //static nested class
        POJOWithStaticNestedClass.StaticNestedClass staticNestedClass = new POJOWithStaticNestedClass.StaticNestedClass();
        staticNestedClass.setNestedId(2);
        staticNestedClass.setNestedName("nestedPojoName");

        assertEquals("{\"nestedId\":2,\"nestedName\":\"nestedPojoName\"}", JSONB.toJson(staticNestedClass));

        POJOWithMixedFieldAccess pojoWithMixedFieldAccess = new POJOWithMixedFieldAccess();

        assertEquals("{\"active\":true,\"id\":2,\"name\":\"pojoName\"}"/*,\"valid\":false}"*/, JSONB.toJson(pojoWithMixedFieldAccess));

        //composite class
        CompositePOJO compositePOJO = new CompositePOJO();
        compositePOJO.setCompositeId(13);
        compositePOJO.setStringArray(new String[]{"first", "second"});
        compositePOJO.setStringList(Arrays.asList("one", "two"));
        POJO innerPOJO = new POJO();
        innerPOJO.setId(4);
        innerPOJO.setName("innerPOJO");
        compositePOJO.setInner(innerPOJO);

        assertEquals(
            "{\"compositeId\":13,\"inner\":{\"id\":4,\"name\":\"innerPOJO\"}," +
                "\"stringArray\":[\"first\",\"second\"],\"stringList\":[\"one\",\"two\"]}",
            JSONB.toJson(compositePOJO));

    }

    static class CompositePOJO {
        private Integer compositeId;
        private POJO inner;
        private List<String> stringList;
        private String[] stringArray;

        public CompositePOJO() {
        }

        public Integer getCompositeId() {
            return compositeId;
        }

        public void setCompositeId(Integer compositeId) {
            this.compositeId = compositeId;
        }

        public POJO getInner() {
            return inner;
        }

        public void setInner(POJO inner) {
            this.inner = inner;
        }

        public List<String> getStringList() {
            return stringList;
        }

        public void setStringList(List<String> stringList) {
            this.stringList = stringList;
        }

        public String[] getStringArray() {
            return stringArray;
        }

        public void setStringArray(String[] stringArray) {
            this.stringArray = stringArray;
        }
    }

    private static class POJO {
        private Integer id;
        private String name;

        public POJO() {
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        //other supported attributes
    }

    private static class POJOWithNestedClass {
        private Integer id;
        private String name;
        private NestedClass nestedClass;

        public POJOWithNestedClass() {
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public NestedClass getNestedClass() {
            return nestedClass;
        }

        public void setNestedClass(NestedClass nestedClass) {
            this.nestedClass = nestedClass;
        }

        //other supported attributes

        public class NestedClass {
            private Integer nestedId;
            private String nestedName;

            public NestedClass() {
            }

            public Integer getNestedId() {
                return nestedId;
            }

            public void setNestedId(Integer nestedId) {
                this.nestedId = nestedId;
            }

            public String getNestedName() {
                return nestedName;
            }

            public void setNestedName(String nestedName) {
                this.nestedName = nestedName;
            }
        }
    }

    private static class POJOWithStaticNestedClass {
        private Integer id;
        private String name;

        public POJOWithStaticNestedClass() {
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        //other supported attributes

        public static class StaticNestedClass {
            private Integer nestedId;
            private String nestedName;

            public StaticNestedClass() {
            }

            public Integer getNestedId() {
                return nestedId;
            }

            public void setNestedId(Integer nestedId) {
                this.nestedId = nestedId;
            }

            public String getNestedName() {
                return nestedName;
            }

            public void setNestedName(String nestedName) {
                this.nestedName = nestedName;
            }
        }
    }

    private static class POJOWithMixedFieldAccess {
        public Integer id = 1;
        public String name = "pojoName";
        public Boolean active = false;
        public Boolean valid = null;

        public Integer getId() {
            return 2;
        }

        public void setId(Integer id) {
            this.id = id * 2;
        }

        public Boolean getActive() {
            return true;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }

        public Boolean isValid() {
            return false;
        }

        public void setValid(Boolean valid) {
            this.valid = valid;
        }
    }

    private static void fromJsonInheritance() {
        //we need public constructor
        Dog animal = JSONB.fromJson("{\"age\":5, \"name\":\"Rex\"}", Dog.class);
    }

    public static void toJsonInheritance() {

        Dog dog = new Dog();
        dog.setAge(5);
        dog.setName("Rex");

        assertEquals("{\"age\":5,\"name\":\"Rex\"}", JSONB.toJson(dog), JSONB.toJson(dog));
    }

    public static class Animal {
        int age;

        public Animal() {
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class Dog extends Animal {
        private String name;

        public Dog() {
            super();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static void toJsonAnonymousClass() {
        assertEquals("{\"id\":1,\"name\":\"pojoName\"}", JSONB.toJson(new POJO() {
            @Override
            public Integer getId() {
                return 1;
            }

            @Override
            public String getName() {
                return "pojoName";
            }
        }));

    }

    public static void fromJsonURLURI() {
        java.net.URL url = JSONB.fromJson("\"https://www.jcp.org/en/jsr/detail?id=367#3\"", java.net.URL.class);

        java.net.URI uri = JSONB.fromJson("\"mailto:users@JSONB-spec.java.net\"", java.net.URI.class);
    }

    public static void toJsonURLURI() throws Exception {

        java.net.URL url = new java.net.URL("https://www.jcp.org/en/jsr/detail?id=367#3");

        assertEquals("\"https://www.jcp.org/en/jsr/detail?id=367#3\"", JSONB.toJson(url));

        java.net.URI uri = new java.net.URI("mailto:users@JSONB-spec.java.net");

        assertEquals("\"mailto:users@JSONB-spec.java.net\"", JSONB.toJson(uri));
    }

    static class POJOWithoutDefaultArgConstructor {
        public String id;

        public POJOWithoutDefaultArgConstructor(String id) {
            this.id = id;
        }
    }

    static class POJOWithPrivateConstructor {
        public String id;

        private POJOWithPrivateConstructor() {
        }
    }

    public static void fromJsonInstantiation() {

        //public or protected constructor must be present

        try {
            POJOWithoutDefaultArgConstructor pojo = JSONB.fromJson("{\"id\":\"1\"}", POJOWithoutDefaultArgConstructor.class);
            fail();
        } catch (JsonbException e) {
        }

        /* TBD: protected or private is the same
        try {
            JSONB.fromJson("{\"id\":\"1\"}", POJOWithPrivateConstructor.class);
            fail();
        } catch (JsonbException e) {
        }
        */
    }

    public static void toJsonDefaultNames() {
        DefaultTestNames defaultTestNames = new DefaultTestNames();
        String result = JSONB.toJson(defaultTestNames);
        assertEquals("{\"A\":\"A\",\"ABC\":\"ABC\",\"A_Bc\":\"A_Bc\",\"DdB_ee\":\"DdB_ee\",\"_12ac\":\"_12ac\"," +
            "\"_23_45_a\":\"_23_45_a\",\"_AB\":\"_AB\",\"_ABc\":\"_ABc\",\"_Ab\":\"_Ab\"," +
            "\"a\":\"a\",\"a_bC\":\"a_bC\",\"abc\":\"abc\",\"okNotOk\":\"okNotOk\"," +
            "\"okNot_Ok\":\"okNot_Ok\",\"okNot_ok\":\"okNot_ok\"}", result);
    }

    public static void fromJsonDefaultNames() {
        DefaultNames defaultNames = JSONB.fromJson("{\"defaultName\":\"newName\"}", DefaultNames.class);
        assertEquals("newName", defaultNames.defaultName);
        assertNull(JSONB.fromJson("{\"defaultNAME\":\"newName\"}", DefaultNames.class).defaultName);
    }

    static class DefaultNames {
        public String defaultName;

        public DefaultNames() {
        }
    }

    static class DefaultTestNames {
        public String a = "a";
        public String A = "A";
        public String ABC = "ABC";
        public String abc = "abc";
        public String a_bC = "a_bC";
        public String A_Bc = "A_Bc";
        public String _12ac = "_12ac";
        public String _Ab = "_Ab";
        public String _AB = "_AB";
        public String _ABc = "_ABc";
        public String okNotOk = "okNotOk";
        public String okNot_Ok = "okNot_Ok";
        public String okNot_ok = "okNot_ok";
        public String DdB_ee = "DdB_ee";
        public String _23_45_a = "_23_45_a";
    }

    public static void toJsonAttributesOrdering() {
        //lexicographical order
        AttributesOrderingClass attributesOrderingClass = new AttributesOrderingClass();
        attributesOrderingClass.aField = "text";
        attributesOrderingClass.cField = "text";
        attributesOrderingClass.bField = "text";

        assertEquals("{\"aField\":\"text\",\"bField\":\"text\",\"cField\":\"text\"}", JSONB.toJson(attributesOrderingClass));

        AttributesOrderingWithInheritance attributesOrderingWithInheritance = new AttributesOrderingWithInheritance();
        attributesOrderingWithInheritance.aField = "aField";
        attributesOrderingWithInheritance.cField = "cField";
        attributesOrderingWithInheritance.bField = "bField";
        attributesOrderingWithInheritance.aa = "aa";
        attributesOrderingWithInheritance.cc = "cc";
        attributesOrderingWithInheritance.bb = "bb";

        assertEquals("{\"aField\":\"aField\",\"aa\":\"aa\",\"bField\":\"bField\",\"bb\":\"bb\",\"cField\":\"cField\",\"cc\":\"cc\"}",
            JSONB.toJson(attributesOrderingWithInheritance));

        AttributesOrderingWithCounterClass attributesOrderingWithCounterClass = JSONB.fromJson("{\"second\":\"a\",\"third\":\"b\",\"first\":\"c\"}", AttributesOrderingWithCounterClass.class);
        assertEquals("a1", attributesOrderingWithCounterClass.second);
        assertEquals("b2", attributesOrderingWithCounterClass.third);
        assertEquals("c0", attributesOrderingWithCounterClass.first);
    }

    public static void toJsonNullValues() {
        //array
        List<String> stringList = new ArrayList<>();
        stringList.add("value1");
        stringList.add(null);
        stringList.add("value3");

        assertEquals("[\"value1\",null,\"value3\"]", JSONB.toJson(stringList));

        //java object
        POJO pojo = new POJO();
        pojo.id = 1;
        pojo.name = null;

        assertEquals("{\"id\":1}", JSONB.toJson(pojo));
    }

    public static void fromJsonNullValues() {
        //array
        ArrayList<Object> stringList = JSONB.fromJson("[\"value1\",null,\"value3\"]", ArrayList.class);
        assertTrue(stringList.size() == 3);
        Iterator<Object> iterator = stringList.iterator();
        assertEquals("value1", iterator.next());
        assertTrue(null == iterator.next());
        assertEquals("value3", iterator.next());

        //java object
        POJOWithInitialValue pojoWithInitialValue = JSONB.fromJson("{\"name\":\"newName\"}", POJOWithInitialValue.class);
        assertTrue(pojoWithInitialValue.id.intValue() == 4);
        assertEquals("newName", pojoWithInitialValue.name);

        POJOWithInitialValue pojoWithNullValue = JSONB.fromJson("{\"name\":\"newName\",\"id\":null}", POJOWithInitialValue.class);
        assertTrue(pojoWithNullValue.id == null);
        assertEquals("newName", pojoWithNullValue.name);
    }

    public static void toJsonModifiers() {
        ModifiersClass modifiersClass = new ModifiersClass();
        assertEquals("{\"finalField\":\"finalValue\",\"regularField\":\"regularValue\"}", JSONB.toJson(modifiersClass));
    }

    public static void fromJsonModifiers() {
        //deserialization of final field is ignored
        ModifiersClass modifiersClass = JSONB.fromJson("{\"finalField\":\"newFinalValue\",\"regularField\":\"newRegularValue\"}", ModifiersClass.class);
        assertEquals("finalValue", modifiersClass.finalField);
        assertEquals("newRegularValue", modifiersClass.regularField);

        //deserialization of static field is ignored
        modifiersClass = JSONB.fromJson("{\"staticField\":\"newStaticValue\",\"regularField\":\"newRegularValue\"}", ModifiersClass.class);
        assertEquals("staticValue", modifiersClass.staticField);
        assertEquals("newRegularValue", modifiersClass.regularField);

        //deserialization of transient field is ignored
        modifiersClass = JSONB.fromJson("{\"transientField\":\"newTransientValue\",\"regularField\":\"newRegularValue\"}", ModifiersClass.class);
        assertEquals("transientValue", modifiersClass.transientField);
        assertEquals("newRegularValue", modifiersClass.regularField);

        //deserialization of unknown field is ignored
        modifiersClass = JSONB.fromJson("{\"unknownField\":\"newUnknownValue\",\"regularField\":\"newRegularValue\"}", ModifiersClass.class);
        assertEquals("newRegularValue", modifiersClass.regularField);
    }

    public static void toJsonOptional() {
        final OptionalClass object = new OptionalClass();
        object.optionalField = Optional.of("test");
        assertEquals("{\"optionalField\":\"test\"}", JSONB.toJson(object));

        OptionalClass optionalClass = new OptionalClass();
        optionalClass.optionalField = Optional.of("value");

        assertEquals("{\"optionalField\":\"value\"}", JSONB.toJson(optionalClass));

        OptionalClass nullOptionalField = new OptionalClass();
        nullOptionalField.optionalField = null;

        assertEquals("{}", JSONB.toJson(nullOptionalField));
    }

    public static void fromJsonOptional() {
        OptionalClass optionalClass = JSONB.fromJson("{\"optionalField\":\"value\"}", OptionalClass.class);
        assertTrue(optionalClass.optionalField.isPresent());
        assertEquals("value", optionalClass.optionalField.get());

        OptionalClass emptyOptionalClass = JSONB.fromJson("{}", OptionalClass.class);
        assertTrue(!emptyOptionalClass.optionalField.isPresent());

        OptionalClass nullOptionalClass = JSONB.fromJson("{\"optionalField\":null}", OptionalClass.class);
        assertFalse(nullOptionalClass.optionalField.isPresent());
    }

    public static void toJsonAccessors() {

        AccessorsClass accessorsClass = new AccessorsClass();
        accessorsClass.setPrivateFieldWithPrivateAccessors(1);
        accessorsClass.setPrivateFieldWithPublicAccessors(2);
        accessorsClass.publicField = 3;

        assertEquals("{\"privateFieldWithPublicAccessors\":2,\"publicField\":3,\"valueWithoutField\":1}", JSONB.toJson(accessorsClass));
    }

    public static void fromJsonAccessors() {
        AccessorsClass accessorsClass = JSONB.fromJson(
            "{\"privateFieldWithPrivateAccessors\":5,\"valueWithoutField\":7," +
                "\"unknownValue\":11,\"transientValue\":9,\"privateFieldWithPublicAccessors\":4,\"publicField\":9," +
                "\"protectedField\":8,\"defaultField\":13,\"privateField\":17}",
            AccessorsClass.class);

        assertEquals(7, accessorsClass.transientValue.intValue());
        assertEquals(4, accessorsClass.privateFieldWithPublicAccessors.intValue());
        assertEquals(9, accessorsClass.publicField.intValue());
        assertNull(accessorsClass.privateFieldWithPrivateAccessors);
        assertNull(accessorsClass.privateField);
        assertNull(accessorsClass.defaultField);
        assertNull(accessorsClass.protectedField);
    }

    static class AccessorsClass {
        private Integer privateField;

        protected Integer protectedField;

        Integer defaultField;

        public Integer publicField;

        public transient Integer transientValue;

        private Integer privateFieldWithPrivateAccessors;

        private Integer privateFieldWithPublicAccessors;

        private Integer getPrivateFieldWithPrivateAccessors() {
            return privateFieldWithPrivateAccessors;
        }

        private void setPrivateFieldWithPrivateAccessors(Integer privateFieldWithPrivateAccessors) {
            this.privateFieldWithPrivateAccessors = privateFieldWithPrivateAccessors;
        }

        public Integer getPrivateFieldWithPublicAccessors() {
            return privateFieldWithPublicAccessors;
        }

        public void setPrivateFieldWithPublicAccessors(Integer privateFieldWithPublicAccessors) {
            this.privateFieldWithPublicAccessors = privateFieldWithPublicAccessors;
        }

        public Integer getValueWithoutField() {
            return 1;
        }

        public void setValueWithoutField(Integer valueWithoutField) {
            transientValue = valueWithoutField;
        }
    }

    static class OptionalClass {
        public Optional<String> optionalField = Optional.empty();

        public OptionalClass() {
        }
    }

    static class ModifiersClass {
        public final String finalField = "finalValue";
        public static String staticField = "staticValue";
        public transient String transientField = "transientValue";
        public String regularField = "regularValue";

        public ModifiersClass() {
        }
    }

    static class POJOWithInitialValue {
        public Integer id = 4;
        public String name;

        public POJOWithInitialValue() {
        }
    }

    static class AttributesOrderingClass {
        public String aField;
        public String cField;
        public String bField;

        public AttributesOrderingClass() {
        }
    }

    static class AttributesOrderingWithInheritance extends AttributesOrderingClass {
        public String aa;
        public String cc;
        public String bb;

        public AttributesOrderingWithInheritance() {
        }
    }

    static class AttributesOrderingWithCounterClass {
        private transient int counter = 0;

        private String first = "first";

        private String second = "second";

        private String third = "third";

        public AttributesOrderingWithCounterClass() {
        }

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first + (counter++);
        }

        public String getSecond() {
            return second;
        }

        public void setSecond(String second) {
            this.second = second + (counter++);
        }

        public String getThird() {
            return third;
        }

        public void setThird(String third) {
            this.third = third + (counter++);
        }
    }
//CHECKSTYLE:ON
}
