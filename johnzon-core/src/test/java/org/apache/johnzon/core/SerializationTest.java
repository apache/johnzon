/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.apache.johnzon.core;

import org.junit.Test;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class SerializationTest {
    @Test
    public void jsonString() throws IOException, ClassNotFoundException {
        final JsonString source = new JsonStringImpl("test");
        final JsonString string = serialDeser(source);
        assertNotSame(source, string);
        assertEquals("test", string.getString());
        assertEquals("\"test\"", string.toString());
    }

    @Test
    public void jsonNumber() throws IOException, ClassNotFoundException {
        final JsonNumber source = new JsonNumberImpl(new BigDecimal("1.0"));
        final JsonNumber deserialized = serialDeser(source);
        assertNotSame(source, deserialized);
        assertEquals(1.0, deserialized.doubleValue(), 0.);
        assertEquals("1.0", deserialized.toString());
    }

    @Test
    public void jsonLong() throws IOException, ClassNotFoundException {
        final JsonNumber source = new JsonLongImpl(1);
        final JsonNumber string = serialDeser(source);
        assertNotSame(source, string);
        assertEquals(1, string.longValue());
        assertEquals("1", string.toString());
    }

    @Test
    public void jsonDouble() throws IOException, ClassNotFoundException {
        final JsonNumber source = new JsonDoubleImpl(1.5);
        final JsonNumber string = serialDeser(source);
        assertNotSame(source, string);
        assertEquals(1.5, string.doubleValue(), 0.);
        assertEquals("1.5", string.toString());
    }

    @Test
    public void jsonObject() throws IOException, ClassNotFoundException {
        final Map<String, JsonValue> map = new LinkedHashMap<String, JsonValue>();
        map.put("test", new JsonStringImpl("val"));
        map.put("test2", JsonValue.TRUE);
        final JsonObject source = new JsonObjectImpl(Collections.unmodifiableMap(map));
        final JsonObject serialization = serialDeser(source);
        assertNotSame(source, serialization);
        assertTrue(serialization.containsKey("test"));
        assertEquals("val", serialization.getString("test"));
        assertEquals(true, serialization.getBoolean("test2"));
        assertEquals(2, serialization.size());
    }

    @Test
    public void jsonArray() throws IOException, ClassNotFoundException {
        final List<JsonValue> list = new ArrayList<JsonValue>();
        list.add(new JsonStringImpl("test"));
        list.add(JsonValue.TRUE); // not ser but we should be able to handle that
        final JsonArray source = new JsonArrayImpl(Collections.unmodifiableList(list));
        final JsonArray serialization = serialDeser(source);
        assertNotSame(source, serialization);
        assertEquals(2, serialization.size());
        final Iterator<JsonValue> iterator = serialization.iterator();
        assertEquals("test", JsonString.class.cast(iterator.next()).getString());
        assertEquals(JsonValue.TRUE, iterator.next());
    }

    @Test
    public void primitiveInObject() throws IOException, ClassNotFoundException {
        assertTrue(serialDeser(JsonProviderImpl.provider().createObjectBuilder()
                .add("bool", true)
                .build())
                .getBoolean("bool"));
    }

    private static <T> T serialDeser(final T instance) throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(instance);
        oos.close();
        final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        try {
            return (T) in.readObject();
        } finally {
            in.close();
        }
    }
}
