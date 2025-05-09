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
package org.apache.johnzon.jsonb.symmetry.adapter.string;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

import static org.junit.Assert.assertEquals;

/**
 * JsonbTypeAdapter on
 *  - Config
 *  - Class
 *
 *
 * Outcome:
 *  - Config wins on read
 *  - Config wins on write
 */
public class StringAdapterPrecedenceConfigClassDirectTest extends StringAdapterOnClassTest {

    @Override
    public Jsonb jsonb() {
        return JsonbBuilder.create(new JsonbConfig().withAdapters(new Adapter.Config()));
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        final String json = "\"test@domain.com\"";
        final Email actual = jsonb.fromJson(json, Email.class);
        assertEquals("test@domain.com:Config.adaptFromJson", actual.toString());
        assertEquals("Config.adaptFromJson", calls());
    }

    @Override
    public void assertWrite(final Jsonb jsonb) {
        final Email email = new Email("test", "domain.com");

        final String json = jsonb.toJson(email);
        assertEquals("\"test@domain.com:Config.adaptToJson\"", json);
        assertEquals("Config.adaptToJson", calls());
    }
}
