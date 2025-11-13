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
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

import static org.junit.Assert.assertEquals;

/**
 * JsonbTypeAdapter on
 *  - Field
 *  - Constructor
 *  - Setter
 *  - Config
 *  - Class
 *
 * Still has a getter
 *
 * Outcome
 *  - Setter wins on read
 *  - Field wins on write
 *  - Constructor adapter is called, but overwritten
 */
public class ArrayAdapterPrecedenceConfigClassSetterFieldConstructorHasGetterTest extends ArrayAdapterOnClassTest {

    @Override
    public Jsonb jsonb() {
        return JsonbBuilder.create(new JsonbConfig().withAdapters(new Adapter.Config()));
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        final String json = "{\"email\":[\"test\",\"domain.com\",\"Field.adaptToJson\"]}";
        final Contact actual = jsonb.fromJson(json, Contact.class);
        assertEquals("Contact{email=test@domain.com:Setter.adaptFromJson}", actual.toString());
        assertEquals("Constructor.adaptFromJson\n" +
                "Contact.<init>\n" +
                "Setter.adaptFromJson\n" +
                "Contact.setEmail", calls());
    }

    @Override
    public void assertWrite(final Jsonb jsonb) {
        final Email email = new Email("test", "domain.com");
        final Contact contact = new Contact(email);
        reset();

        final String json = jsonb.toJson(contact);
        assertEquals("{\"email\":[\"test\",\"domain.com\",\"Field.adaptToJson\"]}", json);
        assertEquals("Contact.getEmail\n" +
                "Field.adaptToJson", calls());
    }

    public static class Contact {

        @JsonbTypeAdapter(Adapter.Field.class)
        private Email email;

        @JsonbCreator
        public Contact(@JsonbProperty("email") @JsonbTypeAdapter(Adapter.Constructor.class) final Email email) {
            CALLS.called();
            this.email = email;
        }

        public Email getEmail() {
            CALLS.called();
            return email;
        }

        @JsonbTypeAdapter(Adapter.Setter.class)
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
