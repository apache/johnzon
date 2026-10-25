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

import org.apache.johnzon.core.JsonProviderImpl;
import org.apache.johnzon.jsonb.api.experimental.JsonbExtension;
import org.junit.Test;

import jakarta.json.JsonNumber;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbNumberFormat;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class BigDecimalScaleTest {
    @Test
    public void defaultScaleLimitWithStringAdapter() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .setProperty("johnzon.use-bigdecimal-stringadapter", true))) {
            assertEquals(new BigDecimal("1e1000"), jsonb.fromJson("{\"value\":1e1000}", Holder.class).value);
            assertEquals(new BigDecimal("1e1000"), jsonb.fromJson("{\"value\":\"1e1000\"}", Holder.class).value);
            assertScaleRejected(jsonb, "{\"value\":1e1001}");
            assertScaleRejected(jsonb, "{\"value\":\"1e1001\"}");
        }
    }

    @Test
    public void defaultScaleLimitWithoutStringAdapter() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .setProperty("johnzon.use-bigdecimal-stringadapter", false))) {
            assertEquals(new BigDecimal("1e1000"), jsonb.fromJson("{\"value\":1e1000}", Holder.class).value);
            assertScaleRejected(jsonb, "{\"value\":1e1001}");
        }
    }

    @Test
    public void configuredScaleLimit() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .setProperty("johnzon.use-bigdecimal-stringadapter", true)
                .setProperty("johnzon.max-big-decimal-scale", 2000))) {
            assertEquals(new BigDecimal("1e2000"), jsonb.fromJson("{\"value\":1e2000}", Holder.class).value);
            assertEquals(new BigDecimal("1e2000"), jsonb.fromJson("{\"value\":\"1e2000\"}", Holder.class).value);
            assertScaleRejected(jsonb, "{\"value\":1e2001}");
            assertScaleRejected(jsonb, "{\"value\":\"1e2001\"}");
        }
    }

    @Test
    public void stricterLimitWinsWithProvider() throws Exception {
        final org.apache.johnzon.core.JsonProviderImpl provider = new org.apache.johnzon.core.JsonProviderImpl();
        provider.setMaxBigDecimalScale(100);
        try (final Jsonb jsonb = JsonbBuilder.newBuilder().withProvider(provider).build()) {
            assertEquals(new BigDecimal("1e-100"), jsonb.fromJson("{\"value\":1e-100}", Holder.class).value);
            assertScaleRejected(jsonb, "{\"value\":1e-101}");
        }
        try (final Jsonb jsonb = JsonbBuilder.newBuilder().withProvider(new org.apache.johnzon.core.JsonProviderImpl())
                .withConfig(new JsonbConfig().setProperty("johnzon.max-big-decimal-scale", 2)).build()) {
            assertScaleRejected(jsonb, "{\"value\":1e3}");
        }
    }

    @Test
    public void fromJsonValueChecksExternallySuppliedNumbers() throws Exception {
        final JsonProviderImpl permissiveProvider = new JsonProviderImpl();
        permissiveProvider.setMaxBigDecimalScale(100);
        final JsonNumber accepted = permissiveProvider.createValue(new BigDecimal("1e2"));
        final JsonNumber rejected = permissiveProvider.createValue(new BigDecimal("1e3"));
        assertEquals(new BigDecimal("1e3"), rejected.bigDecimalValue());

        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .setProperty("johnzon.max-big-decimal-scale", 2))) {
            final JsonbExtension extension = JsonbExtension.class.cast(jsonb);
            assertEquals(new BigDecimal("1e2"), extension.fromJsonValue(accepted, BigDecimal.class));
            assertEquals(new BigDecimal("1e2"), extension.fromJsonValue(accepted, Number.class));
            assertEquals(new BigInteger("100"), extension.fromJsonValue(accepted, BigInteger.class));

            assertScaleRejected(() -> extension.fromJsonValue(rejected, BigDecimal.class));
            assertScaleRejected(() -> extension.fromJsonValue(rejected, Number.class));
            assertScaleRejected(() -> extension.fromJsonValue(rejected, BigInteger.class));
        }
    }

    @Test
    public void formattedNumberChecksConfiguredScaleBeforeFormatting() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .setProperty("johnzon.max-big-decimal-scale", 3))) {
            final FormattedHolder holder = new FormattedHolder();
            holder.value = new BigDecimal("1e3");
            assertEquals("{\"value\":\"1000\"}", jsonb.toJson(holder));

            holder.value = new BigDecimal("1e4");
            assertScaleRejected(() -> jsonb.toJson(holder));
        }
    }

    private static void assertScaleRejected(final Jsonb jsonb, final String json) {
        try {
            jsonb.fromJson(json, Holder.class);
        } catch (final RuntimeException e) {
            Throwable t = e;
            while (t != null && !(t instanceof ArithmeticException)) {
                t = t.getCause();
            }
            assertNotNull("expected a scale rejection for " + json + " but got " + e, t);
            return;
        }
        fail("expected " + json + " to be rejected");
    }

    private static void assertScaleRejected(final Runnable action) {
        try {
            action.run();
        } catch (final RuntimeException e) {
            Throwable t = e;
            while (t != null && !(t instanceof ArithmeticException)) {
                t = t.getCause();
            }
            assertNotNull("expected a scale rejection but got " + e, t);
            return;
        }
        fail("expected a scale rejection");
    }

    public static class Holder {
        public BigDecimal value;
    }

    public static class FormattedHolder {
        @JsonbNumberFormat("0")
        public BigDecimal value;
    }
}
