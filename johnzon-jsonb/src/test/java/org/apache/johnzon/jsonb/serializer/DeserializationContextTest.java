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
package org.apache.johnzon.jsonb.serializer;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Type;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;

import org.junit.Test;

public class DeserializationContextTest {

    /**
     * Test Case for <a href="https://issues.apache.org/jira/browse/JOHNZON-277">JOHNZON-277</a>
     *
     * According to https://github.com/eclipse-ee4j/jsonb-api/blob/master/api/src/main/java/javax/json/bind/serializer/DeserializationContext.java#L34-L35 a
     * deserialization context must be able to deserialize an object EVEN IN CASE the parser cursor is at {@code START_OBJECT} or {@code START_ARRAY}.
     *
     * Quote: "JsonParser cursor have to be at KEY_NAME before START_OBJECT / START_ARRAY, or at START_OBJECT / START_ARRAY 35 * to call this method."
     *
     * The latter case, "...or at START_OBJECT..." seems to fail in case the inner JSON to parse is an empty block "{}" (also for empty arrays).
     */
    @Test
    public void issue277() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            // CAN create Bar and Bar[] instance from empty JSON inside of Foo
            final Foo foo = jsonb.fromJson("{\"bar\":{},\"bars\":[]}", Foo.class);
            assertNotNull(foo);
            assertNotNull(foo.bar);
            assertNotNull(foo.bars);
        }

        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withDeserializers(new CustomDeserializer()))) {
            // CANNOT create Bar nor Bar[] instance from empty JSON when parser cursor is at START_OBJECT => Bug in Johnzon
            final Foo foo = jsonb.fromJson("{\"bar\":{},\"bars\":[]}", Foo.class); // throws exception "Unknown structure: END_OBJECT / END_ARRAY"
            assertNotNull(foo);
            assertNotNull(foo.bar);
            assertNotNull(foo.bars);
        }
    }

    public static class CustomDeserializer implements JsonbDeserializer<Foo> {
        @Override
        public Foo deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
            parser.next(); // now cursor is at START_OBJECT of outer Foo, so let's create empty Foo...
            final Foo foo = new Foo();
            parser.next(); // now cursor is at KEY_NAME "bar"
            parser.next(); // now cursor is at START_OBJECT of inner Bar, so let's fill Foo.bar...
            foo.bar = ctx.deserialize(Bar.class, parser);
            parser.next(); // now cursor is at KEY_NAME "bars"
            parser.next(); // now cursor is at START_ARRAY of inner Bar[], so let's fill Foo.bars...
            foo.bars = ctx.deserialize(Bar[].class, parser);
            return foo;
        }
    }

    public static class Foo {
        public Bar bar;
        public Bar[] bars;
    }

    public static class Bar {
        // intentionally left blank
    }

}
