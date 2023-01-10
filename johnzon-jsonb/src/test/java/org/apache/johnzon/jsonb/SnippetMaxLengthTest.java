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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.johnzon.jsonb;

import org.junit.Test;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests that johnzon.snippetMaxLength works as expected
 */
public class SnippetMaxLengthTest {

    @Test
    public void testDefault() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final String s = "{ \"name\" : { \"first\":\"Charlie\", \"last\": \"Brown\" }}";
            jsonb.fromJson(s, Person.class);
            fail();
        } catch (JsonbException e) {
            assertMessage("Person property 'name' of type String cannot be mapped to json object value: " +
                    "{\"first\":\"Charlie\",\"last\":\"Brown\"}\n" +
                    "Unable to map json object value to class java.lang.String: {\"first\":\"Charlie\",\"last\":\"Brown\"}", e.getMessage());
        }
    }

    @Test
    public void testDefaultTruncated() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final String s = "{ \"name\" : { \"first\":\"Charlie\", \"last\": \"Brown\", \"age\": \"8.5\", \"dog\": \"Snoopy\" }}";
            jsonb.fromJson(s, Person.class);
            fail();
        } catch (JsonbException e) {
            assertMessage("Person property 'name' of type String cannot be mapped to json object value:" +
                    " {\"first\":\"Charlie\",\"last\":\"Brown\",\"age\":\"8.5\",\"dog...\n" +
                    "Unable to map json object value to class java.lang.String: {\"first\":\"Charlie\",\"last\":\"Brown\",\"age\":\"8.5\",\"dog...", e.getMessage());
        }
    }

    @Test
    public void testSetAsInt() throws Exception {
        final JsonbConfig config = new JsonbConfig();
        config.setProperty("johnzon.snippetMaxLength", 20);
        try (final Jsonb jsonb = JsonbBuilder.create(config)) {
            final String s = "{ \"name\" : { \"first\":\"Charlie\", \"last\": \"Brown\" }}";
            jsonb.fromJson(s, Person.class);
            fail();
        } catch (JsonbException e) {
            assertMessage("Person property 'name' of type String cannot be mapped to json object value:" +
                    " {\"first\":\"Charlie\",\"...\n" +
                    "Unable to map json object value to class java.lang.String: {\"first\":\"Charlie\",\"...", e.getMessage());
        }
    }

    private void assertMessage(final String expected, final String actual) {
        assertEquals(normalize(expected), normalize(actual));
    }

    private String normalize(final String message) {
        return message.replace("\r\n", "\n");
    }

    @Test
    public void testSetAsString() throws Exception {
        final JsonbConfig config = new JsonbConfig();
        config.setProperty("johnzon.snippetMaxLength", "20");
        try (final Jsonb jsonb = JsonbBuilder.create(config)) {
            final String s = "{ \"name\" : { \"first\":\"Charlie\", \"last\": \"Brown\" }}";
            jsonb.fromJson(s, Person.class);
            fail();
        } catch (JsonbException e) {
            assertMessage("Person property 'name' of type String cannot be mapped to json object value:" +
                    " {\"first\":\"Charlie\",\"...\n" +
                    "Unable to map json object value to class java.lang.String: {\"first\":\"Charlie\",\"...", e.getMessage());
        }
    }

    public static class Person {
        private String name;

        public Person() {
        }

        public Person(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

}
