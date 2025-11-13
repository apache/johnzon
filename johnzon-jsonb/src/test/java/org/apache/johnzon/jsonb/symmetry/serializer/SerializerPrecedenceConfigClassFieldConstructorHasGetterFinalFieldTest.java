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
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;

import static org.junit.Assert.assertEquals;

/**
 * Adapter on
 *  - Field
 *  - Constructor
 *  - Config
 *  - Class
 *
 * Has
 *  - Getter
 *  - Final Field
 *
 *  Outcome:
 *   - Constructor wins on read
 *   - Field wins on write
 */
public class SerializerPrecedenceConfigClassFieldConstructorHasGetterFinalFieldTest extends SerializerOnClassTest {

    @Override
    public Jsonb jsonb() {
        return JsonbBuilder.create(new JsonbConfig()
                .withSerializers(new Adapter.Config())
                .withDeserializers(new Adapter.Config())
        );
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        final String json = "{\"email\":{\"user\":\"test\",\"domain\":\"domain.com\"}}";
        final Contact actual = jsonb.fromJson(json, Contact.class);
        assertEquals("Contact{email=test@domain.com:Constructor.deserialize}", actual.toString());
        assertEquals("Constructor.deserialize\n" +
                "Contact.<init>", calls());
    }

    @Override
    public void assertWrite(final Jsonb jsonb) {
        final Email email = new Email("test", "domain.com");
        final Contact contact = new Contact(email);
        reset();

        final String json = jsonb.toJson(contact);
        assertEquals("{\"email\":{\"user\":\"test\",\"domain\":\"domain.com\",\"call\":\"Field.serialize\"}}", json);
        assertEquals("Contact.getEmail\n" +
                "Field.serialize", calls());
    }

    public static class Contact {

        @JsonbTypeDeserializer(Adapter.Field.class)
        @JsonbTypeSerializer(Adapter.Field.class)
        private final Email email;

        @JsonbCreator
        public Contact(@JsonbProperty("email") @JsonbTypeDeserializer(Adapter.Constructor.class) final Email email) {
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
}
