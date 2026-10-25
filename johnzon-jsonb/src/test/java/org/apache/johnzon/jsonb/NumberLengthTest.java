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
package org.apache.johnzon.jsonb;

import org.junit.Test;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class NumberLengthTest {
    @Test
    public void defaultLimit() throws Exception {
        final String value = "1".repeat(1001);
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            assertRejected(jsonb, "{\"value\":" + value + "}");
            assertRejected(jsonb, "{\"value\":\"" + value + "\"}");
        }
    }

    @Test
    public void configuredLimit() throws Exception {
        final String value = "1".repeat(5000);
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .setProperty("org.apache.johnzon.max-number-length", 5000))) {
            assertEquals(new BigDecimal(value), jsonb.fromJson("{\"value\":" + value + "}", Holder.class).value);
            assertEquals(new BigDecimal(value), jsonb.fromJson("{\"value\":\"" + value + "\"}", Holder.class).value);
            assertRejected(jsonb, "{\"value\":" + value + "1}");
            assertRejected(jsonb, "{\"value\":\"" + value + "1\"}");
        }
    }

    @Test
    public void reusedBuilderResetsToDefaultLimit() throws Exception {
        final String value = "1".repeat(2000);
        final String quoted = "{\"value\":\"" + value + "\"}";
        final JohnzonBuilder builder = new JohnzonBuilder();
        builder.withConfig(new JsonbConfig().setProperty("org.apache.johnzon.max-number-length", 5000));
        try (final Jsonb first = builder.build()) {
            assertEquals(new BigDecimal(value), first.fromJson(quoted, Holder.class).value);
            builder.withConfig(new JsonbConfig());
            try (final Jsonb second = builder.build()) {
                assertRejected(second, quoted);
                assertEquals(new BigDecimal(value), first.fromJson(quoted, Holder.class).value);
            }
        }
    }

    private static void assertRejected(final Jsonb jsonb, final String json) {
        try {
            jsonb.fromJson(json, Holder.class);
        } catch (final RuntimeException e) {
            for (Throwable t = e; t != null; t = t.getCause()) {
                if (t.getMessage() != null && t.getMessage().toLowerCase(Locale.ROOT).contains("number length")) {
                    return;
                }
            }
            throw new AssertionError("unexpected error for a value of length " + json.length(), e);
        }
        fail("expected a value of length " + json.length() + " to be rejected");
    }

    public static class Holder {
        public BigDecimal value;
    }
}
