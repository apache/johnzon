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
package org.apache.johnzon.mapper;

import org.junit.Test;
import org.apache.johnzon.mapper.converter.BigDecimalConverter;
import org.apache.johnzon.mapper.converter.BigIntegerConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;
import org.apache.johnzon.mapper.map.LazyConverterMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NumberLengthTest {
    private static final String LONG = "1".repeat(1001);
    private static final String MAX = "1".repeat(1000);

    @Test
    public void quotedValuesAboveDefaultLimit() {
        try (final Mapper mapper = new MapperBuilder().build()) {
            assertRejected(mapper, "{\"decimal\":\"" + LONG + "\"}");
            assertRejected(mapper, "{\"integer\":\"" + LONG + "\"}");
        }
    }

    @Test
    public void valuesAtDefaultLimit() {
        try (final Mapper mapper = new MapperBuilder().build()) {
            assertEquals(new BigDecimal(MAX), read(mapper, "{\"decimal\":\"" + MAX + "\"}").decimal);
            assertEquals(new BigInteger(MAX), read(mapper, "{\"integer\":\"" + MAX + "\"}").integer);
            assertEquals(new BigDecimal(MAX), read(mapper, "{\"decimal\":" + MAX + "}").decimal);
        }
    }

    @Test
    public void configuredLimitAppliesToNumbersAndQuotedValues() {
        final String value = "1".repeat(5000);
        try (final Mapper mapper = new MapperBuilder().setMaxNumberLength(5000).build()) {
            assertEquals(new BigDecimal(value), read(mapper, "{\"decimal\":" + value + "}").decimal);
            assertEquals(new BigDecimal(value), read(mapper, "{\"decimal\":\"" + value + "\"}").decimal);
            assertEquals(new BigInteger(value), read(mapper, "{\"integer\":\"" + value + "\"}").integer);
            assertRejected(mapper, "{\"decimal\":" + value + "1}");
            assertRejected(mapper, "{\"decimal\":\"" + value + "1\"}");
        }
    }

    @Test
    public void cachedBigIntegerAdapterUsesDefaultLimit() {
        final MapperBuilder builder = new MapperBuilder();
        builder.getAdapters().get(new AdapterKey(BigInteger.class, String.class));
        try (final Mapper mapper = builder.build()) {
            assertRejected(mapper, "{\"integer\":\"" + LONG + "\"}");
        }
    }

    @Test
    public void standaloneAdapterMapUsesDefaultLimit() {
        final LazyConverterMap adapters = new LazyConverterMap();
        assertAdapterLimit(adapters, BigDecimal.class);
        assertAdapterLimit(adapters, BigInteger.class);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void explicitlyRegisteredBuiltInConvertersUseDefaultLimit() {
        final MapperBuilder builder = new MapperBuilder()
                .addConverter(BigDecimal.class, new BigDecimalConverter())
                .addConverter(BigInteger.class, new BigIntegerConverter());
        try (final Mapper mapper = builder.build()) {
            assertRejected(mapper, "{\"decimal\":\"" + LONG + "\"}");
            assertRejected(mapper, "{\"integer\":\"" + LONG + "\"}");
        }
    }

    @Test
    public void annotatedBuiltInConverterUsesDefaultLimit() {
        try (final Mapper mapper = new MapperBuilder().build()) {
            assertRejected(mapper, "{\"decimal\":\"" + LONG + "\"}", AnnotatedHolder.class);
        }
    }

    @Test
    public void standaloneConvertersCanRaiseTheirLimit() {
        final String value = "1".repeat(2000);
        assertEquals(new BigDecimal(value), new BigDecimalConverter(1000, 2000).fromString(value));
        assertEquals(new BigInteger(value), new BigIntegerConverter(2000).fromString(value));
    }

    @Test
    public void reusedBuilderLowersLimitWithoutChangingEarlierMapper() {
        final String value = "1".repeat(2000);
        final String raw = "{\"unknown\":" + value + "}";
        final String decimal = "{\"decimal\":\"" + value + "\"}";
        final String integer = "{\"integer\":\"" + value + "\"}";
        final MapperBuilder builder = new MapperBuilder().setMaxNumberLength(5000);
        try (final Mapper first = builder.build()) {
            read(first, raw);
            read(first, decimal);
            read(first, integer);

            builder.setMaxNumberLength(1000);
            try (final Mapper second = builder.build()) {
                assertRejected(second, raw);
                assertRejected(second, decimal);
                assertRejected(second, integer);
                read(first, raw);
                read(first, decimal);
                read(first, integer);
            }
        }
    }

    @Test
    public void scaleChangeAfterNumberLimitChangeRefreshesProvider() {
        final MapperBuilder builder = new MapperBuilder().setMaxBigDecimalScale(2);
        try (final Mapper first = builder.build()) {
            builder.setMaxNumberLength(5000).setMaxBigDecimalScale(4);
            try (final Mapper second = builder.build()) {
                assertEquals(new BigDecimal("1e3"), read(second, "{\"decimal\":1e3}").decimal);
            }
            try {
                read(first, "{\"decimal\":1e3}");
                fail("the first mapper should retain its scale limit");
            } catch (final RuntimeException expected) {
                boolean scaleError = false;
                for (Throwable cause = expected; cause != null; cause = cause.getCause()) {
                    scaleError |= cause.getMessage() != null && cause.getMessage().contains("scale");
                }
                assertTrue("expected a scale error", scaleError);
            }
        }
    }

    private static Holder read(final Mapper mapper, final String json) {
        return mapper.readObject(json, Holder.class);
    }

    private static void assertAdapterLimit(final LazyConverterMap adapters, final Class<?> type) {
        final ConverterAdapter<?> adapter = (ConverterAdapter<?>) adapters.get(new AdapterKey(type, String.class));
        try {
            adapter.to(LONG);
            fail("expected the value to be rejected");
        } catch (final NumberFormatException expected) {
            assertTrue(expected.getMessage().contains("Number length"));
        }
    }

    private static void assertRejected(final Mapper mapper, final String json) {
        assertRejected(mapper, json, Holder.class);
    }

    private static void assertRejected(final Mapper mapper, final String json, final Class<?> type) {
        try {
            mapper.readObject(json, type);
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
        public BigDecimal decimal;
        public BigInteger integer;
    }

    public static class AnnotatedHolder {
        @JohnzonConverter(BigDecimalConverter.class)
        public BigDecimal decimal;
    }
}
