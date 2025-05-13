/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.jsonb.symmetry.serializer;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * JsonbTypeAdapter on
 *  - Config
 *  - Class
 *
 *
 * Outcome:
 *  - Reads fail
 *  - Config wins on write
 */
public class SerializerPrecedenceConfigClassDirectTest extends SerializerOnClassTest {

    @Override
    public Jsonb jsonb() {
        return JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new Adapter.Config())
                .withDeserializers(new Adapter.Config())
        );
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        final String json = "{\"user\":\"test\",\"domain\":\"domain.com\"}";
        final Email actual = jsonb.fromJson(json, Email.class);
        assertEquals("test@domain.com:Config.deserialize", actual.toString());
        assertEquals("Config.deserialize", calls());
    }

    @Override
    public void assertWrite(final Jsonb jsonb) {
        final Email email = new Email("test", "domain.com");

        final String json = jsonb.toJson(email);
        assertEquals("{\"user\":\"test\",\"domain\":\"domain.com\",\"call\":\"Config.serialize\"}", json);
        assertEquals("Config.serialize", calls());
    }

    /**
     * java.lang.ClassCastException: Cannot cast sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl to java.lang.Class
     *
     * 	at java.base/java.lang.Class.cast(Class.java:3889)
     * 	at org.apache.johnzon.jsonb.JsonbAccessMode.isReversedAdapter(JsonbAccessMode.java:875)
     */
    @Test
    @Ignore
    @Override
    public void read() throws Exception {
        super.read();
    }

    /**
     * java.lang.ClassCastException: Cannot cast sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl to java.lang.Class
     *
     * 	at java.base/java.lang.Class.cast(Class.java:3889)
     * 	at org.apache.johnzon.jsonb.JsonbAccessMode.isReversedAdapter(JsonbAccessMode.java:875)
     */
    @Test
    @Ignore
    @Override
    public void readAfterRead() throws Exception {
        super.readAfterRead();
    }

    /**
     * java.lang.ClassCastException: Cannot cast sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl to java.lang.Class
     *
     * 	at java.base/java.lang.Class.cast(Class.java:3889)
     * 	at org.apache.johnzon.jsonb.JsonbAccessMode.isReversedAdapter(JsonbAccessMode.java:875)
     */
    @Test
    @Ignore
    @Override
    public void readAfterWrite() throws Exception {
        super.readAfterWrite();
    }

    /**
     * java.lang.ClassCastException: Cannot cast sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl to java.lang.Class
     *
     * 	at java.base/java.lang.Class.cast(Class.java:3889)
     * 	at org.apache.johnzon.jsonb.JsonbAccessMode.isReversedAdapter(JsonbAccessMode.java:875)
     */
    @Test
    @Ignore
    @Override
    public void writeAfterRead() throws Exception {
        super.writeAfterRead();
    }
}
