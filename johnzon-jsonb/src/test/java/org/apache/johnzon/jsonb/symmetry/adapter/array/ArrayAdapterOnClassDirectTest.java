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
package org.apache.johnzon.jsonb.symmetry.adapter.array;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArrayAdapterOnClassDirectTest extends ArrayAdapterOnClassTest {

    public Jsonb jsonb() {
        return JsonbBuilder.create();
    }

    public void assertWrite(final Jsonb jsonb) {
        final Email email = new Email("test", "domain.com");
        final String json = jsonb.toJson(email);
        assertEquals("[\"test\",\"domain.com\",\"EmailClass.adaptToJson\"]", json);
        assertEquals("EmailClass.adaptToJson", calls());
    }

    public void assertRead(final Jsonb jsonb) {
        final String json = "[\"test\",\"domain.com\"]";
        final Email email = jsonb.fromJson(json, Email.class);
        assertEquals("test@domain.com:EmailClass.adaptFromJson", email.toString());
        assertEquals("EmailClass.adaptFromJson", calls());
    }

    /**
     * Fails as the adapter is not found
     */
    @Test
    @Ignore()
    @Override
    public void read() throws Exception {
        super.read();
    }

    /**
     * Fails as the adapter is not found
     */
    @Test
    @Ignore()
    @Override
    public void readAfterRead() throws Exception {
        super.readAfterRead();
    }

    /**
     * Fails as the adapter is not found on the first read
     */
    @Test
    @Ignore()
    @Override
    public void writeAfterRead() throws Exception {
        super.writeAfterRead();
    }

    @Test
    @Ignore()
    @Override
    public void readAfterWrite() throws Exception {
        super.readAfterWrite();
    }
}
