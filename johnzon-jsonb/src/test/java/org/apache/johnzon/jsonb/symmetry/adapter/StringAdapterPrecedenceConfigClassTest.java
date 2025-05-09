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

import static org.junit.Assert.assertEquals;

/**
 * JsonbTypeAdapter on
 *  - Config
 *  - Class
 *
 * Has
 *  - Getter
 *  - Final Field
 *
 * Outcome:
 *  - EmailClass wins on read
 *  - EmailClass wins on write
 *
 * Question:
 *  - Should Config win on read and write?
 *    Adapters on the target type itself (Email) are effectively a hardcoded default adapter
 *    If a user wishes to alter this behavior for a specific operation via the config, why
 *    not let them?  This would be the most (only?) convenient way to change behavior without
 *    sweeping code change.
 */
public class StringAdapterPrecedenceConfigClassTest extends StringAdapterOnClassTest {

    @Override
    public Jsonb jsonb() {
        return JsonbBuilder.create(new JsonbConfig().withAdapters(new Adapter.Config()));
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        final String json = "{\"email\":\"test@domain.com\"}";
        final Contact actual = jsonb.fromJson(json, Contact.class);
        assertEquals("Contact{email=test@domain.com:EmailClass.adaptFromJson}", actual.toString());
        assertEquals("Contact.<init>\n" +
                "EmailClass.adaptFromJson\n" +
                "Contact.setEmail", calls());
    }

    @Override
    public void assertWrite(final Jsonb jsonb) {
        final Email email = new Email("test", "domain.com");
        final Contact contact = new Contact();
        contact.setEmail(email);
        reset();

        final String json = jsonb.toJson(contact);
        assertEquals("{\"email\":\"test@domain.com:EmailClass.adaptToJson\"}", json);
        assertEquals("Contact.getEmail\n" +
                "EmailClass.adaptToJson", calls());
    }

    public static class Contact {

        private Email email;

        public Contact() {
            CALLS.called();
        }

        public Email getEmail() {
            CALLS.called();
            return email;
        }

        public void setEmail(final Email email) {
            CALLS.called();
            this.email = email;
        }

        @Override
        public String toString() {
            return String.format("Contact{email=%s}", email);
        }
    }
}
