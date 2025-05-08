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
package org.apache.johnzon.jsonb.symmetry.adapter;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringAdapterPrecedenceConfigTest extends StringAdapterOnClassTest {

    @Override
    public Jsonb jsonb() {
        return JsonbBuilder.create(new JsonbConfig().withAdapters(new Adapter.Config()));
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        final String json = "{\"email\":\"test@domain.com\"}";
        final Contact actual = jsonb.fromJson(json, Contact.class);
        assertEquals("Contact{email=test@domain.com:Config.adaptFromJson}", actual.toString());
        assertEquals("Config.adaptFromJson\n" +
                "Contact.<init>", calls());
    }

    @Override
    public void assertWrite(final Jsonb jsonb) {
        final Email email = new Email("test", "domain.com");
        final Contact contact = new Contact(email);
        reset();

        final String json = jsonb.toJson(contact);
        assertEquals("{\"email\":\"test@domain.com:Config.adaptToJson\"}", json);
        assertEquals("Contact.getEmail\n" +
                "Config.adaptToJson", calls());
    }

    public static class Contact {

        private final Email email;

        @JsonbCreator
        public Contact(@JsonbProperty("email") final Email email) {
            CALLS.called();
            this.email = email;
        }

        public Email getEmail() {
            CALLS.called();
            return email;
        }

        @Override
        public String toString() {
            return String.format("Contact{email=%s}", email);
        }
    }

    /**
     * Fails as the read behavior is changed if a write is performed first.
     */
    @Test
    @Ignore
    @Override
    public void readAfterWrite() throws Exception {
        super.readAfterWrite();
    }

    /**
     * Write behavior is not symmetrical with read
     */
    @Test
    @Ignore
    @Override
    public void write() throws Exception {
        super.write();
    }

    /**
     * Write behavior is not symmetrical with read
     */
    @Test
    @Ignore
    @Override
    public void writeAfterRead() throws Exception {
        super.writeAfterRead();
    }

    /**
     * Write behavior is not symmetrical with read
     */
    @Test
    @Ignore
    @Override
    public void writeAfterWrite() throws Exception {
        super.writeAfterWrite();
    }
}
