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

import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;

import static org.junit.Assert.assertEquals;

public class MapperEnhancedTest {

    @Test
    public void writeNull() {
        final StringWriter sw = new StringWriter();
        new MapperBuilder().build().writeObject(null, sw);
        assertEquals("null", sw.toString());
    }
    
    @Test
    public void writeReadChar() {
        CharClass charClass = new CharClass();
        charClass.setCharValue('G');
        charClass.setCharArr(new char[]{'G','O'});
        final StringWriter sw = new StringWriter();
        
        final String expectedJson = "{\"charArr\":[\"G\",\"O\"],\"charValue\":\"G\"}";
        
        final Comparator<String> attributeOrder = new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return expectedJson.indexOf(o1) - expectedJson.indexOf(o2);
            }
        };
        
        new MapperBuilder().setAttributeOrder(attributeOrder).build().writeObject(charClass, sw);
        assertEquals(expectedJson, sw.toString());
        CharClass  read = new MapperBuilder().build().readObject(new StringReader(sw.toString()), CharClass.class);
        Assert.assertNotNull(read);
        Assert.assertEquals('G', read.getCharValue());
        Assert.assertTrue(Arrays.equals(new char[]{'G','O'}, read.getCharArr()));
    }
    
    @Test
    public void writeReadSortedMap() {
        SomaClass soseClass = new SomaClass();
        soseClass.getSoma().put("key1", "val1");
        soseClass.getSoma().put("key2", "val2");
        final StringWriter sw = new StringWriter();
        
        new MapperBuilder().build().writeObject(soseClass, sw);
        assertEquals("{\"soma\":{\"key1\":\"val1\",\"key2\":\"val2\"}}", sw.toString());
        new MapperBuilder().build().readObject(new StringReader(sw.toString()), SomaClass.class);
        
    }
    
    @Test
    public void writeReadSortedSet() {
        SoseClass soseClass = new SoseClass();
        soseClass.getSose().add("string1");
        soseClass.getSose().add("string2");
        final StringWriter sw = new StringWriter();
        
        new MapperBuilder().build().writeObject(soseClass, sw);
       
        assertEquals("{\"sose\":[\"string1\",\"string2\"]}", sw.toString());
        new MapperBuilder().build().readObject(new StringReader(sw.toString()), SoseClass.class);
       
    }
    
    @Test
    public void writeReadQueue() {
        QueueClass queueClass = new QueueClass();
        queueClass.getQueue().add("string1");
        queueClass.getQueue().add("string2");
        final StringWriter sw = new StringWriter();
        
        new MapperBuilder().build().writeObject(queueClass, sw);
       
        assertEquals("{\"queue\":[\"string1\",\"string2\"]}", sw.toString());
        new MapperBuilder().build().readObject(new StringReader(sw.toString()), QueueClass.class);
       
    }

    @Test
    public void writeTestclass() {
        final StringWriter sw = new StringWriter();
        final TestClass tc2 = buildTestClassInstance();

        final String json = "{" +
            "\"bd\":-456.4567890987654321,\"string\":\"some \\t \\u0001 unicode: ÖÄÜ pppন􏿿\"," +
            "\"dates\":[]," +

            "\"inner\":{" +
                "\"bd\":-456.4567890987654321," +
                "\"string\":\"some \\t \\u0001 unicode: ÖÄÜ pppন􏿿\"," +
                "\"dates\":[]," +
                "\"sose\":[\"string1\",\"string2\"]," +
                "\"map\":{\"[{key1=-100, key11=-1002, key2=100, key22=1002}, {}]\":100}" +
            "}," +
            "\"sose\":[]," +
            "\"map\":{\"[{key1=-100, key11=-1002, key2=100, key22=1002}, {}]\":200}}";
        new MapperBuilder().setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return json.indexOf(o1) - json.indexOf(o2);
            }
        }).build().writeObject(tc2, sw);
        assertEquals(json, sw.toString());
    }

    @Test(expected = MapperException.class)
    public void needConvertersForComplexTypes() {
        final String str = "{" +
            "\"bd\":-456.4567890987654321,\"string\":\"some \\t \\u0001 unicode: ÖÄÜ pppন􏿿\"," +
            "\"dates\":[]," +
            "\"sose\":[]," +
            "\"inner\":{" +
            "\"bd\":-456.4567890987654321," +
            "\"string\":\"some \\t \\u0001 unicode: ÖÄÜ pppন􏿿\"," +
            "\"dates\":[]," +
            "\"sose\":[\"string1\",\"string2\"]," +
            "\"map\":{\"[{key1=-100, key11=-1002, key2=100, key22=1002}, {}]\":100}" +
            "}," +
            "\"map\":{\"[{key1=-100, key11=-1002, key2=100, key22=1002}, {}]\":200}}";

        new MapperBuilder().build().readObject(new StringReader(str), TestClass.class);
    }

    private TestClass buildTestClassInstance() {
        final TestClass tc1 = new TestClass(null);
        final Map<String, Integer> m = new TreeMap<String, Integer>();
        m.put("key1", -100);
        m.put("key2", +100);
        final Map<String, Integer> m2 = new TreeMap<String, Integer>();
        m.put("key11", -1002);
        m.put("key22", +1002);
        final List<Map<String, Integer>> l = new ArrayList<Map<String, Integer>>();
        l.add(m);
        l.add(m2);
        tc1.sose.add("string1");
        tc1.sose.add("string2");
        tc1.map.put(l, 100L);

        final TestClass tc2 = new TestClass(tc1);
        final Map<String, Integer> m3 = new TreeMap<String, Integer>();
        m3.put("key1", -100);
        m3.put("key2", +100);
        final Map<String, Integer> m4 = new TreeMap<String, Integer>();
        m4.put("key11", -1002);
        m4.put("key22", +1002);
        final List<Map<String, Integer>> l1 = new ArrayList<Map<String, Integer>>();
        l1.add(m);
        l1.add(m2);
        tc2.map.put(l1, 200L);
        return tc2;
    }

    public static class QueueClass {
        private Queue<String> queue = new ArrayBlockingQueue<String>(5);

        public Queue<String> getQueue() {
            return queue;
        }

        public void setQueue(Queue<String> queue) {
            this.queue = queue;
        }
    }
    
    public static class SoseClass {
        private SortedSet<String> sose = new TreeSet<String>();

        public SortedSet<String> getSose() {
            return sose;
        }

        public void setSose(SortedSet<String> sose) {
            this.sose = sose;
        }
    }
    
    public static class SomaClass {
        private SortedMap<String, String> soma = new TreeMap<String, String>();

        public SortedMap<String, String> getSoma() {
            return soma;
        }

        public void setSoma(SortedMap<String, String> soma) {
            this.soma = soma;
        }        
    }
    
    public static class CharClass {
        private char charValue;
        private char charArr[];
        
        public char[] getCharArr() {
            return charArr;
        }

        public void setCharArr(char[] charArr) {
            this.charArr = charArr;
        }

        public char getCharValue() {
            return charValue;
        }

        public void setCharValue(char charValue) {
            this.charValue = charValue;
        }           
    }
    
    public static class TestClass {
        private List<Map<String, Date>> dates = new ArrayList<Map<String, Date>>();
        private Map<List<Map<String, Integer>>, Long> map = new HashMap<List<Map<String, Integer>>, Long>();
        private TestClass inner;
        private String string = "some \t \u0001 unicode: ÖÄÜ \u0070\u0070\u0070ন\udbff\udfff";
        private BigDecimal bd = new BigDecimal("-456.4567890987654321");
        private SortedSet<String> sose = new TreeSet<String>();

        public SortedSet<String> getSose() {
            return sose;
        }

        public void setSose(SortedSet<String> sose) {
            this.sose = sose;
        }

        public TestClass(final TestClass inner) {
            super();
            this.inner = inner;
        }

        public TestClass() {
            super();
        }

        public List<Map<String, Date>> getDates() {
            return dates;
        }

        public void setDates(final List<Map<String, Date>> dates) {
            this.dates = dates;
        }

        public Map<List<Map<String, Integer>>, Long> getMap() {
            return map;
        }

        public void setMap(final Map<List<Map<String, Integer>>, Long> map) {
            this.map = map;
        }

        public TestClass getInner() {
            return inner;
        }

        public void setInner(final TestClass inner) {
            this.inner = inner;
        }

        public String getString() {
            return string;
        }

        public void setString(final String string) {
            this.string = string;
        }

        public BigDecimal getBd() {
            return bd;
        }

        public void setBd(final BigDecimal bd) {
            this.bd = bd;
        }
    }
}
