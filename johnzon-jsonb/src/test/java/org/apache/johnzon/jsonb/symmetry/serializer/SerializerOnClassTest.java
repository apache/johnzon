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

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import org.apache.johnzon.jsonb.symmetry.Calls;
import org.apache.johnzon.jsonb.symmetry.SymmetryTest;
import org.junit.Before;

import java.lang.reflect.Type;

public abstract class SerializerOnClassTest extends SymmetryTest {

    protected static final Calls CALLS = new Calls();

    @Before
    public void reset() {
        CALLS.reset();
    }

    public static String calls() {
        return CALLS.get();
    }

    @JsonbTypeDeserializer(Adapter.EmailClass.class)
    @JsonbTypeSerializer(Adapter.EmailClass.class)
    public static class Email {
        final String user;
        final String domain;
        final String call;

        public Email(final String user, final String domain) {
            this(user, domain, null);
        }

        public Email(final String user, final String domain, final String call) {
            this.user = user;
            this.domain = domain;
            this.call = call;
        }

        @Override
        public String toString() {
            if (call == null) {
                return user + "@" + domain;
            } else {
                return user + "@" + domain + ":" + call;
            }
        }
    }

    public abstract static class Adapter implements JsonbSerializer<Email>, JsonbDeserializer<Email> {

        @Override
        public void serialize(final Email obj, final JsonGenerator generator, final SerializationContext ctx) {
            final String call = CALLS.called(this);
            generator.writeStartObject();
            generator.write("user", obj.user);
            generator.write("domain", obj.domain);
            generator.write("call", call);
            generator.writeEnd();
        }

        @Override
        public Email deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
            String user = null;
            String domain = null;

            while (parser.hasNext()) {
                final JsonParser.Event event = parser.next();
                if (event == JsonParser.Event.KEY_NAME) {
                    final String key = parser.getString();
                    parser.next();
                    switch (key) {
                        case "user":
                            user = parser.getString();
                            break;
                        case "domain":
                            domain = parser.getString();
                            break;
                        // skip "call"
                    }
                } else if (event == JsonParser.Event.END_OBJECT) {
                    break;
                }
            }

            final String call = CALLS.called(this);
            return new Email(user, domain, call);
        }

        public static final class Getter extends Adapter {
        }

        public static final class Setter extends Adapter {
        }

        public static final class Field extends Adapter {
        }

        public static final class Constructor extends Adapter {
        }

        public static final class Config extends Adapter {
        }

        public static final class EmailClass extends Adapter {
        }
    }
}