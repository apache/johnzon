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
import org.apache.johnzon.mapper.reflection.JohnzonCollectionType;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;
import org.junit.Test;

import java.beans.ConstructorProperties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MapperTest {
    private static final String BIG_OBJECT_STR = "{" + "\"name\":\"the string\"," + "\"integer\":56," + "\"longnumber\":118,"
            + "\"bool\":true," + "\"nested\":{" + "\"name\":\"another value\"," + "\"integer\":97," + "\"longnumber\":34" + "},"
            + "\"array\":[" + "{" + "\"name\":\"a1\"," + "\"integer\":1," + "\"longnumber\":2" + "}," + "{" + "\"name\":\"a2\","
            + "\"integer\":3," + "\"longnumber\":4" + "}" + "]," + "\"list\":[" + "{" + "\"name\":\"a3\"," + "\"integer\":5,"
            + "\"longnumber\":6" + "}," + "{" + "\"name\":\"a4\"," + "\"integer\":7," + "\"longnumber\":8" + "}" + "],"
            + "\"primitives\":[1,2,3,4,5]," + "\"collectionWrapper\":[1,2,3,4,5]," + "\"map\":{\"uno\":true,\"duos\":false}" + "}";

    @Test
    public void writeEmptyObject() {
        final StringWriter writer = new StringWriter();
        new MapperBuilder().build().writeObject(null, writer);
        assertEquals("null", writer.toString());
    }

    @Test
    public void readEmptyObject() {
        final TheObject object = new MapperBuilder().build().readObject(new ByteArrayInputStream("{}".getBytes()), TheObject.class);
        assertNotNull(object);
        assertNull(object.name);
    }

    @Test
    public void readEmptyArray() {
        final TheObject[] object = new MapperBuilder().build().readArray(new ByteArrayInputStream("[]".getBytes()), TheObject.class);
        assertNotNull(object);
        assertEquals(0, object.length);
    }

    @Test
    public void readCollection() { // mainly API test
        final Collection<TheObject> object = new MapperBuilder().build()
                .readCollection(new ByteArrayInputStream("[{}]".getBytes()),
                        new JohnzonParameterizedType(List.class, TheObject.class));
        assertNotNull(object);
        assertEquals(1, object.size());
        final Collection<TheObject> object2 = new MapperBuilder().build()
                .readJohnzonCollection(new ByteArrayInputStream("[{}]".getBytes()),
                    new JohnzonCollectionType<List<TheObject>>() {
                    });
        assertNotNull(object2);
        assertEquals(1, object2.size());
    }

    @Test
    public void testShouldMapACollection() throws Exception {
        final Mapper mapper = new MapperBuilder().setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        }).build();
        final String json = "[" +
            "{\"name\":\"addKey\"}," +
            "{\"action\":\"REMOVE\",\"name\":\"removeKey\"}]";

        final ParameterizedType type = new JohnzonParameterizedType(List.class, Command.class);
        final List<Command> properties = new ArrayList(mapper.readCollection(new StringReader(json), type));

        assertNotNull(properties);
        assertEquals(2, properties.size());
        assertEquals("addKey", properties.get(0).getName());
        assertEquals("removeKey", properties.get(1).getName());
        assertEquals(Command.Action.REMOVE, properties.get(1).getAction());
        assertEquals(json, mapper.writeArrayAsString(properties));
    }

    @Test
    public void enumCollection() throws Exception {
        final Mapper mapper = new MapperBuilder().build();
        final String json = "[\"REMOVE\",\"ADD\"]";

        final ParameterizedType type = new JohnzonParameterizedType(List.class, Command.Action.class);
        final List<Command.Action> properties = new ArrayList(mapper.readCollection(new StringReader(json), type));

        assertNotNull(properties);
        assertEquals(2, properties.size());
        assertEquals(Command.Action.ADD, properties.get(1));
        assertEquals(Command.Action.REMOVE, properties.get(0));
        assertEquals(json, mapper.writeArrayAsString(properties));
    }

    @Test
    public void primitiveCollection() throws Exception {
        final Mapper mapper = new MapperBuilder().build();
        final String json = "[1,2]";

        final ParameterizedType type = new JohnzonParameterizedType(List.class, Integer.class);
        final List<Integer> properties = new ArrayList(mapper.readCollection(new StringReader(json), type));

        assertNotNull(properties);
        assertEquals(2, properties.size());
        assertEquals(2, properties.get(1).intValue());
        assertEquals(1, properties.get(0).intValue());
        assertEquals(json, mapper.writeArrayAsString(properties));
    }

    @Test
    public void readMapObject() {
        final Map<String, Object> data = new MapperBuilder().build()
                .readObject(new ByteArrayInputStream(("{\"a\":1,\"b\":true,\"c\":null,\"d\":[1, 2], " +
                                "\"e\":[\"i\", \"j\"],\"k\":{\"a\":1,\"b\":true,\"c\":null,\"d\":[1, 2], \"e\":[\"i\", \"j\"]}}").getBytes()),
                        new JohnzonParameterizedType(Map.class, String.class, Object.class));
        assertOneDimension(data, 6);

        final Map<String, Object> k = (Map<String, Object>) data.get("k");
        assertNotNull(k);
        assertOneDimension(k, 5);
    }

    @Test
    public void sortedMap() {
        final Mapper sortedMapper = new MapperBuilder().setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o2.compareTo(o1);
            }
        }).build();
        final Map<String, String> sorted = new TreeMap<String, String>(new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        });
        sorted.put("a", "1");
        sorted.put("b", "2");
        sorted.put("c", "3");
        assertEquals("{\"a\":\"1\",\"b\":\"2\",\"c\":\"3\"}", sortedMapper.writeObjectAsString(sorted));
        assertEquals(asList("c", "b", "a"), new ArrayList<Object>(Map.class.cast(
                sortedMapper.readObject("{\"a\":\"1\",\"b\":\"2\",\"c\":\"3\"}",
                        new JohnzonParameterizedType(SortedMap.class, String.class, String.class))).keySet()));
    }

    @Test
    public void justObjectAsModel() {
        final Mapper encodingAwareMapper = new MapperBuilder().setEncoding("UTF-8" /*otherwise guess algo fails for too small string*/).build();
        final Mapper simpleMapper = new MapperBuilder().build();
        final Mapper enforcedQuotes = new MapperBuilder().setEnforceQuoteString().build();
        { // object
            final String object = "{\"a\":1,\"b\":true,\"c\":null,\"d\":[1,2]," +
                    "\"e\":[\"i\",\"j\"],\"k\":{\"a\":1,\"b\":true,\"c\":null,\"d\":[1,2],\"e\":[\"i\",\"j\"]}}";
            final Mapper mapper = simpleMapper;
            final Object raw = mapper.readObject(new ByteArrayInputStream(object.getBytes()), Object.class);
            final Map<String, Object> data = Map.class.cast(raw);
            assertOneDimension(data, 6);

            final Map<String, Object> k = (Map<String, Object>) data.get("k");
            assertNotNull(k);
            assertOneDimension(k, 5);

            final Map<String, Object> sorted = new TreeMap<String, Object>(data);
            sorted.put("k", new TreeMap((Map) sorted.get("k")));
            assertEquals(object.replace(",\"c\":null", ""), mapper.writeObjectAsString(sorted));
        }
        { // primitives
            // read
            assertEquals(Boolean.TRUE, simpleMapper.readObject(new ByteArrayInputStream("true".getBytes()), Object.class));
            assertEquals(Boolean.FALSE, simpleMapper.readObject(new ByteArrayInputStream("false".getBytes()), Object.class));
            assertEquals(1.,
                    (Double) encodingAwareMapper.readObject(new ByteArrayInputStream("1".getBytes()), Object.class),
                         0.1);
            assertEquals("val", simpleMapper.readObject(new ByteArrayInputStream("\"val\"".getBytes()), Object.class));
            assertEquals(asList("val1", "val2"), simpleMapper.readObject(new ByteArrayInputStream("[\"val1\", \"val2\"]".getBytes()), Object.class));
            assertEquals(new HashMap<String, Object>() {{
                put("a", "val");
                put("b", true);
                put("c", 1);
                put("d", true);
            }}, simpleMapper.readObject(new ByteArrayInputStream("{\"a\":\"val\", \"b\": true, \"c\": 1, \"d\": true}".getBytes()), Object.class));

            // write
            assertEquals("true", simpleMapper.writeObjectAsString(true));
            assertEquals("false", simpleMapper.writeObjectAsString(false));
            assertEquals("1", simpleMapper.writeObjectAsString(1));
            assertEquals("\"val\"", enforcedQuotes.writeObjectAsString("val"));
            assertEquals("[\"val1\",\"val2\"]", simpleMapper.writeObjectAsString(asList("val1", "val2")));
            assertEquals("{\"a\":\"val\",\"b\":true,\"c\":1,\"d\":true}", simpleMapper.writeObjectAsString(new TreeMap<String, Object>() {{
                put("a", "val");
                put("b", true);
                put("c", 1);
                put("d", true);
            }}));
        }
        { // in model
            PrimitiveObject p = new PrimitiveObject();
            p.bool = true;
            final Mapper fieldMapper = new MapperBuilder().setAccessModeName("field").build();
            assertEquals("{\"bool\":true}", fieldMapper.writeObjectAsString(p));
            assertEquals(Boolean.TRUE, PrimitiveObject.class.cast(fieldMapper.readObject(new StringReader("{\"bool\":true}"), PrimitiveObject.class)).bool);
        }
    }

    private void assertOneDimension(final Map<String, Object> data, final int size) {
        assertEquals(size, data.size());
        assertEquals(1, data.get("a"));
        assertEquals(true, data.get("b"));
        assertNull(data.get("c"));
        assertEquals(asList(1, 2), data.get("d"));
        assertEquals(asList("i", "j"), data.get("e"));
    }

    @Test
    public void writeMap() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new MapperBuilder().build().writeObject(new LinkedHashMap<String, Integer>() {
            {
                put("a", 1);
                put("b", 2);
            }
        }, baos);
        assertEquals("{\"a\":1,\"b\":2}", new String(baos.toByteArray()));
    }

    @Test
    public void writeArrayOfArray() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new MapperBuilder().build().writeArray(new String[][]{new String[]{"a", "b"}, new String[]{"c", "d"}}, baos);
        assertEquals("[[\"a\",\"b\"],[\"c\",\"d\"]]", new String(baos.toByteArray()));
    }

    @Test
    public void writeListOfList() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new MapperBuilder().build().writeArray(new ArrayList<List<String>>(){{
            add(new ArrayList<String>(){{ add("a");add("b"); }});
            add(new ArrayList<String>(){{ add("c");add("d"); }});
        }}, baos);
        assertEquals("[[\"a\",\"b\"],[\"c\",\"d\"]]", new String(baos.toByteArray()));
    }

    @Test
    public void writeObject() {
        final TheObject instance = new MapperBuilder().build().readObject(new ByteArrayInputStream(BIG_OBJECT_STR.getBytes()),
                TheObject.class); // suppose reader writes but this is tested
        final StringWriter writer = new StringWriter();
        new MapperBuilder().build().writeObject(instance, writer);
        final String serialized = writer.toString();
        assertTrue(serialized.contains("\"primitives\":[1,2,3,4,5]"));
        assertTrue(serialized.contains("\"collectionWrapper\":[1,2,3,4,5]"));
        assertTrue(serialized.contains("\"bool\":true"));

        //Assert fail with oracle java 1.7.0_45, works well with apple java 1.6.0_65
        //assertTrue(serialized.contains("\"map\":{\"uno\":true,\"duos\":false}"));
        assertTrue(serialized.contains("\"map\":{"));
        assertTrue(serialized.contains("\"uno\":true"));
        assertTrue(serialized.contains("\"duos\":false"));

        final TheObject instance2 = new MapperBuilder().build()
                .readObject(new ByteArrayInputStream(serialized.getBytes()), TheObject.class); // suppose reader writes but this is tested

        assertEquals(instance, instance2);
    }
    
    
   @Test
   public void writeShortArray() {
        StringWriter writer = new StringWriter();
        new MapperBuilder().build().writeArray(new Short[]{(short)1,(short)2,(short)3},writer);
        assertEquals("[1,2,3]", writer.toString());
   }
   
   @Test
   public void writeByteArray() {
       StringWriter writer = new StringWriter();
       new MapperBuilder().build().writeArray(new Byte[]{(byte)1,(byte)2,(byte)3},writer);
       assertEquals("[1,2,3]", writer.toString());
   }
   
   
   @Test
   public void shortAndByte() {
       ByteShort bs = new ByteShort();
       bs.setNumByte((byte) 6);
       bs.setNumShort((short) -1);
       bs.setNumByteA(new byte[]{(byte) 1, (byte) -1, (byte) 2});
       bs.setNumShortA(new short[]{(short) 4, (short) -2});
       
       bs.setByteW(new Byte((byte)7));
       bs.setShortW(new Short((short)22));
       bs.setByteWA(new Byte[]{new Byte((byte) 4), new Byte((byte) -12), new Byte((byte) 2)});
       bs.setShortWA(new Short[]{new Short((short) 7), new Short((short) -2)});
       
       final String expectedJson = "{\"byteW\":7,\"byteWA\":[4,-12,2],\"numByte\":6,\"numByteA\":[1,-1,2],\"numShort\":-1,\"numShortA\":[4,-2],\"shortW\":22,\"shortWA\":[7,-2]}";
       final Comparator<String> attributeOrder = new Comparator<String>() {
           @Override
           public int compare(final String o1, final String o2) {
               return o1.compareTo(o2);
           }
       };
       
       Mapper mapper = new MapperBuilder().setAttributeOrder(attributeOrder).build();

       StringWriter writer = new StringWriter();
       mapper.writeObject(bs, writer);
       assertEquals(expectedJson, writer.toString());
   
       ByteShort bsr = mapper.readObject(new StringReader(expectedJson), ByteShort.class);
       
       writer = new StringWriter();
       mapper.writeObject(bsr, writer);
       assertEquals(expectedJson, writer.toString());
   }
   
   @Test
   public void shortAndByteBase64() {
       ByteShort bs = new ByteShort();
       bs.setNumByte((byte) 6);
       bs.setNumShort((short) -1);
       bs.setNumByteA(new byte[]{(byte) 1, (byte) -1, (byte) 2});
       bs.setNumShortA(new short[]{(short) 4, (short) -2});
       
       bs.setByteW(new Byte((byte)7));
       bs.setShortW(new Short((short)22));
       bs.setByteWA(new Byte[]{new Byte((byte) 4), new Byte((byte) -12), new Byte((byte) 2)});
       bs.setShortWA(new Short[]{new Short((short) 7), new Short((short) -2)});
       
       final String expectedJson = "{\"byteW\":7,\"byteWA\":[4,-12,2],\"numByte\":6,\"numByteA\":\"Af8C\",\"numShort\":-1,\"numShortA\":[4,-2],\"shortW\":22,\"shortWA\":[7,-2]}";
       
       
       final Comparator<String> attributeOrder = new Comparator<String>() {
           @Override
           public int compare(final String o1, final String o2) {
               return o1.compareTo(o2);
           }
       };
       
       Mapper mapper = new MapperBuilder().setAttributeOrder(attributeOrder).setTreatByteArrayAsBase64(true).build();
       
       StringWriter writer = new StringWriter();
       mapper.writeObject(bs, writer);
       assertEquals(expectedJson, writer.toString());
   
       ByteShort bsr = mapper.readObject(new StringReader(expectedJson), ByteShort.class);
       
       writer = new StringWriter();
       mapper.writeObject(bsr, writer);
       assertEquals(expectedJson, writer.toString());
   }
   
   /*@Test
   public void byteArrayBase64Converter() {
       
       Mapper mapper = new MapperBuilder().setTreatByteArrayAsBase64(false).build();
       
       ByteArray ba = new ByteArray();
       ba.setByteArray(new byte[]{(byte) 1,(byte) 1,(byte) 1 });
       
       final String expectedJson = "{\"shortW\":22,\"shortWA\":[7,-2],\"byteW\":7,\"numShortA\":[4,-2],\"numByteA\":\"Af8C\",\"byteWA\":[4,-12,2],\"numByte\":6,\"numShort\":-1}";
       
       StringWriter writer = new StringWriter();
       mapper.writeObject(ba, writer);
       assertEquals(expectedJson, writer.toString());
   
       ByteShort bsr = mapper.readObject(new StringReader(expectedJson), ByteArray.class);
       
       writer = new StringWriter();
       mapper.writeObject(bsr, writer);
       assertEquals(expectedJson, writer.toString());
   }*/

    static class Bool {
        private boolean bool;

        public boolean isBool() {
            return bool;
        }

        public void setBool(final boolean bool) {
            this.bool = bool;
        }

    }

    static class Bool2 {
        private Map<String, Boolean> map;

        public Map<String, Boolean> getMap() {
            return map;
        }

        public void setMap(final Map<String, Boolean> map) {
            this.map = map;
        }

    }

    @Test
    public void literal() {

        final Bool instance = new MapperBuilder().build().readObject(new ByteArrayInputStream("{\"bool\":true}".getBytes()), Bool.class);

        assertTrue(instance.bool);

        final StringWriter writer = new StringWriter();
        new MapperBuilder().build().writeObject(instance, writer);
        final String serialized = writer.toString();
        assertEquals("{\"bool\":true}", serialized);

    }

    @Test(expected = MapperException.class)
    public void literalFail() {

        final Bool instance = new MapperBuilder().build()
                .readObject(new ByteArrayInputStream("{\"bool\":\"true\"}".getBytes()), Bool.class);

        assertTrue(instance.bool);

    }

    @Test(expected = MapperException.class)
    public void literalFail2() {
        new MapperBuilder().build().readObject(new ByteArrayInputStream("{\"map\":{\"key\":\"true\"}}".getBytes()), Bool2.class);
    }

    @Test
    public void writeArray() {
        // integer
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new MapperBuilder().build().writeArray(new Integer[] { 1, 2 }, baos);
        assertEquals("[1,2]", new String(baos.toByteArray()));

        // object
        baos = new ByteArrayOutputStream();
        new MapperBuilder().build().writeArray(new Pair[] { new Pair(1, "a"), new Pair(2, "b") }, baos);
        assertEquals("[{\"s\":\"a\",\"i\":1},{\"s\":\"b\",\"i\":2}]", new String(baos.toByteArray()));
    }

    @Test
    public void readObject() {
        final TheObject object = new MapperBuilder().build().readObject(new ByteArrayInputStream(BIG_OBJECT_STR.getBytes()),
                TheObject.class);

        assertNotNull(object);
        assertEquals("the string", object.name);
        assertEquals(56, object.integer);
        assertEquals(118, object.longnumber);
        assertTrue(object.bool);
        assertEquals("another value", object.nested.name);
        assertEquals(97, object.nested.integer);
        assertEquals(34, object.nested.longnumber);
        assertFalse(object.nested.bool);
        assertNotNull(object.array);
        assertEquals(2, object.array.length);
        assertEquals("a1", object.array[0].name);
        assertEquals(1, object.array[0].integer);
        assertEquals(2, object.array[0].longnumber);
        assertEquals("a2", object.array[1].name);
        assertEquals(3, object.array[1].integer);
        assertEquals(4, object.array[1].longnumber);
        assertEquals("a3", object.list.get(0).name);
        assertEquals(5, object.list.get(0).integer);
        assertEquals(6, object.list.get(0).longnumber);
        assertEquals("a4", object.list.get(1).name);
        assertEquals(7, object.list.get(1).integer);
        assertEquals(8, object.list.get(1).longnumber);
        assertEquals(5, object.primitives.length);
        for (int i = 0; i < object.primitives.length; i++) {
            assertEquals(i + 1, object.primitives[i]);
        }
        assertNotNull(object.collectionWrapper);
        assertEquals(5, object.collectionWrapper.size());
        for (int i = 0; i < object.collectionWrapper.size(); i++) {
            assertEquals(i + 1, object.collectionWrapper.get(i).intValue());
        }
        assertNotNull(object.map);
        assertEquals(2, object.map.size());
        assertTrue(object.map.containsKey("uno"));
        assertTrue(object.map.containsKey("duos"));
        assertTrue(object.map.get("uno"));
        assertFalse(object.map.get("duos"));
    }

    @Test
    public void readArray() {
        final TheObject[] object = new MapperBuilder().build().readArray(
                new ByteArrayInputStream(("[" + "{" + "\"name\":\"a3\"," + "\"integer\":5," + "\"longnumber\":6" + "}," + "{"
                        + "\"name\":\"a4\"," + "\"integer\":7," + "\"longnumber\":8" + "}" + "]").getBytes()), TheObject.class);
        assertNotNull(object);
        assertEquals(2, object.length);
        assertEquals("a3", object[0].name);
        assertEquals(5, object[0].integer);
        assertEquals(6, object[0].longnumber);
        assertEquals("a4", object[1].name);
        assertEquals(7, object[1].integer);
        assertEquals(8, object[1].longnumber);
    }

    @Test
    public void converters() {
        final String json = "{\"s\":\"noznhoj\"}";
        final Converted v = new MapperBuilder().build().readObject(new ByteArrayInputStream(json.getBytes()), Converted.class);
        assertEquals("johnzon", v.getS());
        final StringWriter stream = new StringWriter();
        new MapperBuilder().build().writeObject(v, stream);
        assertEquals(json, stream.toString());
    }

    @Test
    public void privateConstructor() {
        final HiddenConstructor value = new MapperBuilder().setSupportHiddenAccess(true).build()
                .readObject(new ByteArrayInputStream("{\"value\":1}".getBytes()), HiddenConstructor.class);
        assertEquals(1, value.value);
    }

    @Test
    public void fieldAccess() {
        final FieldAccess value = new MapperBuilder().setAccessModeName("field").build()
                .readObject(new ByteArrayInputStream("{\"value\":1}".getBytes()), FieldAccess.class);
        assertEquals(1, value.value);
    }


    @Test
    public void nan() {
        final String value = new MapperBuilder().build()
                .writeObjectAsString(new NanHolder());
        assertEquals("{}", value);
    }


    @Test
    public void noSetterCollection() {
        final NoSetterCollection value = new MapperBuilder().setSupportGetterForCollections(true).build()
                .readObject(new ByteArrayInputStream("{\"theCollection\":[\"a\",\"b\"]}".getBytes()), NoSetterCollection.class);
        assertEquals(asList("a", "b"), value.getTheCollection());
    }


    @Test
    public void constructor() {
        final ConstructorUsage value = new MapperBuilder().setSupportConstructors(true).build()
                .readObject(
                    new ByteArrayInputStream(
                        "{\"converted\":\"yeah\",\"value\":\"test\",\"collection\":[\"a\",\"b\"]}".getBytes()),
                        ConstructorUsage.class);
        assertEquals("test", value.aValue);
        assertEquals(asList("a", "b"), value.theCollection);
    }

    @Test
    public void aliases() {
        {
            final Aliases aliases = new MapperBuilder().build().readObject(
                    new ByteArrayInputStream("{\"super_long_property\":\"ok\"}".getBytes()), Aliases.class);
            assertEquals("ok", aliases.superLongProperty);
        }
        {
            final Aliases aliases = new Aliases();
            aliases.setSuperLongProperty("ok");
            assertEquals("{\"super_long_property\":\"ok\"}", new MapperBuilder().build().writeObjectAsString(aliases));
        }
        {
            final AliasesOnField aliases = new MapperBuilder().setAccessModeName("field").build().readObject(
                    new ByteArrayInputStream("{\"super_long_property\":\"ok\"}".getBytes()), AliasesOnField.class);
            assertEquals("ok", aliases.superLongProperty);
        }
        {
            final AliasesOnField aliases = new AliasesOnField();
            aliases.setSuperLongProperty("ok");
            assertEquals("{\"super_long_property\":\"ok\"}", new MapperBuilder().setAccessModeName("field").build().writeObjectAsString(aliases));
        }
    }

    @Test
    public void fakedObject() {
        final ChildOfFakedObject source = new ChildOfFakedObject();
        source.a = 1;
        source.b = 2;
        source.c = new String[] { "3", "4" };
        source.children = asList("5", "6");

        final Mapper mapper = new MapperBuilder().setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        }).setAccessMode(new FieldAccessMode(true, false)).build();

        final String asString = mapper.writeObjectAsString(source);
        assertEquals("{\"children\":[\"5\",\"6\"],\"nested\":{\"b\":2,\"sub\":{\"a\":1,\"c\":[\"3\",\"4\"]}}}", asString);

        final ChildOfFakedObject childOfFakedObject = mapper.readObject(asString, ChildOfFakedObject.class);
        assertEquals(source.a, childOfFakedObject.a);
        assertEquals(source.b, childOfFakedObject.b);
        assertArrayEquals(source.c, childOfFakedObject.c);
        assertEquals(source.children, childOfFakedObject.children);
    }

    @Test
    public void encodingTest() {
        final ByteArrayOutputStream utf8 = new ByteArrayOutputStream();
        final ByteArrayOutputStream latin = new ByteArrayOutputStream();

        new MapperBuilder().setEncoding("UTF-8").build().writeObject(new StringHolder("摩"), utf8);
        new MapperBuilder().setEncoding("Latin1").build().writeObject(new StringHolder("摩"), latin);
        assertNotEquals(utf8, latin); // means encoding was considered, we don't need more here
    }

    public static class NanHolder {
        private Double nan = Double.NaN;

        public Double getNan() {
            return nan;
        }

        public void setNan(Double nan) {
            this.nan = nan;
        }
    }

    public static class TheObject {
        private String name;
        private int integer;
        private long longnumber;
        private boolean bool;
        private TheObject nested;
        private TheObject[] array;
        private List<TheObject> list;
        private int[] primitives;
        private List<Integer> collectionWrapper;
        private Map<String, Boolean> map;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getInteger() {
            return integer;
        }

        public void setInteger(final int integer) {
            this.integer = integer;
        }

        public long getLongnumber() {
            return longnumber;
        }

        public void setLongnumber(final long longnumber) {
            this.longnumber = longnumber;
        }

        public boolean isBool() {
            return bool;
        }

        public void setBool(final boolean bool) {
            this.bool = bool;
        }

        public TheObject getNested() {
            return nested;
        }

        public void setNested(final TheObject nested) {
            this.nested = nested;
        }

        public TheObject[] getArray() {
            return array;
        }

        public void setArray(final TheObject[] array) {
            this.array = array;
        }

        public List<TheObject> getList() {
            return list;
        }

        public void setList(final List<TheObject> list) {
            this.list = list;
        }

        public int[] getPrimitives() {
            return primitives;
        }

        public void setPrimitives(final int[] primitives) {
            this.primitives = primitives;
        }

        public List<Integer> getCollectionWrapper() {
            return collectionWrapper;
        }

        public void setCollectionWrapper(final List<Integer> collectionWrapper) {
            this.collectionWrapper = collectionWrapper;
        }

        public Map<String, Boolean> getMap() {
            return map;
        }

        public void setMap(final Map<String, Boolean> map) {
            this.map = map;
        }

        @Override
        public String toString() {
            return "TheObject [name=" + name + ", integer=" + integer + ", longnumber=" + longnumber + ", bool=" + bool + ", nested="
                    + nested + ", array=" + Arrays.toString(array) + ", list=" + list + ", primitives=" + Arrays.toString(primitives)
                    + ", collectionWrapper=" + collectionWrapper + ", map=" + map + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(array);
            result = prime * result + (bool ? 1231 : 1237);
            result = prime * result + ((collectionWrapper == null) ? 0 : collectionWrapper.hashCode());
            result = prime * result + integer;
            result = prime * result + ((list == null) ? 0 : list.hashCode());
            result = prime * result + (int) (longnumber ^ (longnumber >>> 32));
            result = prime * result + ((map == null) ? 0 : map.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((nested == null) ? 0 : nested.hashCode());
            result = prime * result + Arrays.hashCode(primitives);
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TheObject other = (TheObject) obj;
            if (!Arrays.equals(array, other.array)) {
                return false;
            }
            if (bool != other.bool) {
                return false;
            }
            if (collectionWrapper == null) {
                if (other.collectionWrapper != null) {
                    return false;
                }
            } else if (!collectionWrapper.equals(other.collectionWrapper)) {
                return false;
            }
            if (integer != other.integer) {
                return false;
            }
            if (list == null) {
                if (other.list != null) {
                    return false;
                }
            } else if (!list.equals(other.list)) {
                return false;
            }
            if (longnumber != other.longnumber) {
                return false;
            }
            if (map == null) {
                if (other.map != null) {
                    return false;
                }
            } else if (!map.equals(other.map)) {
                return false;
            }
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (nested == null) {
                if (other.nested != null) {
                    return false;
                }
            } else if (!nested.equals(other.nested)) {
                return false;
            }
            if (!Arrays.equals(primitives, other.primitives)) {
                return false;
            }
            return true;
        }

    }

    public static class Pair {
        private final int i;
        private final String s;

        public Pair(final int i, final String s) {
            this.i = i;
            this.s = s;
        }

        public int getI() {
            return i;
        }

        public String getS() {
            return s;
        }
    }

    public static class Converted {
        private String s;

        @JohnzonConverter(ReverseConverter.class)
        public String getS() {
            return s;
        }

        @JohnzonConverter(ReverseConverter.class)
        public void setS(final String v) {
            s = v;
        }
    }

    public static class ReverseConverter implements Converter<String> {
        @Override
        public String toString(final String instance) {
            return new StringBuilder(instance).reverse().toString();
        }

        @Override
        public String fromString(final String text) {
            return toString(text);
        }
    }

    public static class HiddenConstructor {
        private int value;

        private HiddenConstructor() {
            // no-op
        }

        public void setValue(final int value) {
            this.value = value;
        }
    }


    public static class FieldAccess {
        private int value;
    }
    
    public static class ByteShort {
        
        private byte numByte;
        private short numShort;
        
        private byte[] numByteA;
        private short[] numShortA;
        
        private Byte byteW;
        private Short shortW;
        
        private Byte[] byteWA;
        private Short[] shortWA;
        
        public byte[] getNumByteA() {
            return numByteA;
        }
        public void setNumByteA(byte[] numByteA) {
            this.numByteA = numByteA;
        }
        public short[] getNumShortA() {
            return numShortA;
        }
        public void setNumShortA(short[] numShortA) {
            this.numShortA = numShortA;
        }
        public byte getNumByte() {
            return numByte;
        }
        public void setNumByte(byte numByte) {
            this.numByte = numByte;
        }
        public Byte getByteW() {
            return byteW;
        }
        public void setByteW(Byte byteW) {
            this.byteW = byteW;
        }
        public Short getShortW() {
            return shortW;
        }
        public void setShortW(Short shortW) {
            this.shortW = shortW;
        }
        public Byte[] getByteWA() {
            return byteWA;
        }
        public void setByteWA(Byte[] byteWA) {
            this.byteWA = byteWA;
        }
        public Short[] getShortWA() {
            return shortWA;
        }
        public void setShortWA(Short[] shortWA) {
            this.shortWA = shortWA;
        }
        public short getNumShort() {
            return numShort;
        }
        public void setNumShort(short numShort) {
            this.numShort = numShort;
        }
        
        
        
    }

    public static class Aliases {
        private String superLongProperty;

        @JohnzonProperty("super_long_property")
        public String getSuperLongProperty() {
            return superLongProperty;
        }

        public void setSuperLongProperty(final String superLongProperty) {
            this.superLongProperty = superLongProperty;
        }
    }

    public static class AliasesOnField {
        @JohnzonProperty("super_long_property")
        private String superLongProperty;

        public String getSuperLongProperty() {
            return superLongProperty;
        }

        public void setSuperLongProperty(final String superLongProperty) {
            this.superLongProperty = superLongProperty;
        }
    }

    public static class NoSetterCollection {
        private Collection<String> theCollection;

        public Collection<String> getTheCollection() {
            if (theCollection == null) {
                theCollection = new LinkedList<String>();
            }
            return theCollection;
        }
    }

    public static class ConstructorUsage {
        private final String foo;
        private final String aValue;
        private final Collection<String> theCollection;

        @ConstructorProperties({ "value", "collection", "converted" })
        public ConstructorUsage(final String aValue, final Collection<String> theCollection, @JohnzonConverter(YeahConverter.class) final String foo) {
            this.aValue = aValue;
            this.foo = foo;
            this.theCollection = theCollection;
        }

        public static class YeahConverter implements Converter<String> {
            @Override
            public String toString(final String instance) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String fromString(final String text) {
                return "yeah";
            }
        }
    }

    public static class FakeNestedObject {
        protected int a;
        protected int b;
        protected String[] c;
    }

    @JohnzonVirtualObjects({
            @JohnzonVirtualObject(
                    path = "nested",
                    fields = @JohnzonVirtualObject.Field("b")
            ),
            @JohnzonVirtualObject(
                    path = { "nested", "sub" },
                    fields = {
                            @JohnzonVirtualObject.Field("a"), @JohnzonVirtualObject.Field("c")
                    }
            )
    })
    public static class ChildOfFakedObject extends FakeNestedObject {
        protected List<String> children;
    }

    public static class StringHolder {
        private String value;

        public StringHolder(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public static class Command {
        public enum Action { ADD, REMOVE }

        private Action action;
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Action getAction() {
            return action;
        }

        public void setAction(final Action action) {
            this.action = action;
        }
    }

    public static class PrimitiveObject {
        public Object bool;
    }
}
