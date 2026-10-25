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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.johnzon.mapper.converter.BigDecimalConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.map.LazyConverterMap;
import org.junit.Test;

public class NumberSerializationTest {
    @Test
    public void toJson() {
        final Mapper mapper = new MapperBuilder().setUseJsRange(true).build();
        final Holder holder = new Holder();
        holder.value = 1;
        assertEquals("{\"value\":1}", mapper.writeObjectAsString(holder));
        holder.value = Long.MAX_VALUE;
        assertEquals("{\"value\":\"9223372036854775807\"}", mapper.writeObjectAsString(holder));
        mapper.close();
    }

    @Test
    public void numberFromJson() {
        final Mapper mapper = new MapperBuilder().build();
        final Num num = mapper.readObject("{\"value\":0}", Num.class);
        assertTrue(BigDecimal.class.isInstance(num.value));
        assertEquals(0, num.value.intValue());
        mapper.close();
    }

    /**
     * Bug: BigDecimalConverter used toString() which produces scientific notation
     * (e.g. "7.33915E-7"). Should use toPlainString() to produce "0.000000733915".
     */
    @Test
    public void bigDecimalConverterUsesPlainNotation() {
        final BigDecimalConverter converter = new BigDecimalConverter();
        final BigDecimal smallValue = new BigDecimal("0.000000733915");
        final String result = converter.toString(smallValue);
        assertEquals("BigDecimalConverter should use plain notation, not scientific",
                "0.000000733915", result);
    }

    @Test
    public void bigDecimalConverterChecksScaleBeforePlainString() {
        final BigDecimalConverter converter = new BigDecimalConverter(2, 1000);
        assertEquals("100", converter.toString(new BigDecimal("1e2")));
        assertEquals("0.01", converter.toString(new BigDecimal("1e-2")));
        assertRejected(() -> converter.toString(new BigDecimal("1e3")));
        assertRejected(() -> converter.toString(new BigDecimal("1e-3")));
    }

    @Test
    public void bigDecimalStringAdapterChecksScaleWhenWriting() {
        try (final Mapper mapper = new MapperBuilder().setMaxBigDecimalScale(2).build()) {
            final BigDecimalHolder holder = new BigDecimalHolder();
            holder.score = new BigDecimal("1e2");
            assertEquals("{\"score\":\"100\"}", mapper.writeObjectAsString(holder));
            holder.score = new BigDecimal("1e3");
            assertRejected(() -> mapper.writeObjectAsString(holder));
        }
    }

    @Test
    public void bigIntegerFromJsonValueChecksMapperScale() {
        final org.apache.johnzon.core.JsonProviderImpl sourceProvider = new org.apache.johnzon.core.JsonProviderImpl();
        sourceProvider.setMaxBigDecimalScale(2000);
        try (final Mapper mapper = new MapperBuilder().setMaxBigDecimalScale(2)
                .setUseBigIntegerStringAdapter(false).build()) {
            assertEquals(new BigInteger("100"), mapper.readObject(
                    sourceProvider.createValue(new BigDecimal("1e2")), BigInteger.class));
            assertRejected(() -> mapper.readObject(
                    sourceProvider.createValue(new BigDecimal("1e3")), BigInteger.class));
            assertRejected(() -> mapper.readObject(sourceProvider.createObjectBuilder()
                    .add("value", new BigDecimal("1e3")).build(), BigIntegerHolder.class));
        }
    }

    /**
     * Bug fix: useBigDecimalStringAdapter and useBigIntegerStringAdapter flags
     * were swapped in LazyConverterMap. Each flag must control its own type.
     * Both default to true (string) for I-JSON (RFC 7493) interoperability.
     */
    @Test
    public void bigDecimalStringAdapterFlagControlsBigDecimal() {
        // Default: BigDecimal adapter is ON (useBigDecimalStringAdapter=true)
        final LazyConverterMap defaultAdapters = new LazyConverterMap();
        assertNotNull("BigDecimal adapter should be active by default",
                defaultAdapters.get(new AdapterKey(BigDecimal.class, String.class)));
        // Disabled: BigDecimal adapter is OFF
        final LazyConverterMap disabledAdapters = new LazyConverterMap();
        disabledAdapters.setUseBigDecimalStringAdapter(false);
        assertNull("BigDecimal adapter should be null when flag is false",
                disabledAdapters.get(new AdapterKey(BigDecimal.class, String.class)));
    }

    @Test
    public void bigIntegerLongExponent() {
        String poc = "1e200000000";
        try (Mapper mapper = new MapperBuilder().build()) {
            assertRejected(() -> mapper.readObject(new StringReader(poc), byte.class));
            assertRejected(() -> mapper.readObject(new StringReader(poc), long.class));
            assertRejected(() -> mapper.readObject(new StringReader(poc), int.class));
            assertRejected(() -> mapper.readObject(new StringReader(poc), BigInteger.class));
            assertRejected(() -> mapper.readObject(new StringReader(poc), BigDecimal.class));
        }
    }

    @Test
    public void bigIntegerStringAdapterFlagControlsBigInteger() {
        // Default: BigInteger adapter is ON (useBigIntegerStringAdapter=true)
        final LazyConverterMap adapters = new LazyConverterMap();
        assertNotNull("BigInteger adapter should be active by default",
                adapters.get(new AdapterKey(BigInteger.class, String.class)));
        // Disabled: BigInteger adapter is OFF
        final LazyConverterMap adapters2 = new LazyConverterMap();
        adapters2.setUseBigIntegerStringAdapter(false);
        assertNull("BigInteger adapter should be null when flag is false",
                adapters2.get(new AdapterKey(BigInteger.class, String.class)));
    }

    /**
     * With useBigDecimalStringAdapter=true (default), BigDecimal fields
     * should be serialized as JSON strings using plain notation.
     */
    @Test
    public void bigDecimalDefaultSerializesAsString() {
        try (final Mapper mapper = new MapperBuilder().build()) {
            final BigDecimalHolder holder = new BigDecimalHolder();
            holder.score = new BigDecimal("0.000000733915");
            final String json = mapper.writeObjectAsString(holder);
            // Default: BigDecimal as string with plain notation (I-JSON interoperability)
            assertEquals("{\"score\":\"0.000000733915\"}", json);
        }
    }

    /**
     * With useBigDecimalStringAdapter=false, BigDecimal fields should be
     * serialized as JSON numbers (strict JSON-B 3.0 / TCK compliance).
     */
    @Test
    public void bigDecimalWithAdapterDisabledSerializesAsNumber() {
        try (final Mapper mapper = new MapperBuilder()
                .setUseBigDecimalStringAdapter(false)
                .build()) {
            final BigDecimalHolder holder = new BigDecimalHolder();
            holder.score = new BigDecimal("0.000000733915");
            final String json = mapper.writeObjectAsString(holder);
            // Adapter disabled: BigDecimal as JSON number (scientific notation is valid per RFC 8259)
            assertEquals("{\"score\":7.33915E-7}", json);
        }
    }

    @Test
    public void bigDecimalNumberPropertyRejectsScaleAboveDefaultLimit() {
        try (final Mapper mapper = new MapperBuilder().build()) {
            assertScaleRejected(mapper, "{\"score\":1e1001}");
            assertScaleRejected(mapper, "{\"score\":1e-1001}");
        }
    }

    @Test
    public void bigDecimalStringPropertyRejectsScaleAboveDefaultLimit() {
        try (final Mapper mapper = new MapperBuilder().build()) {
            assertScaleRejected(mapper, "{\"score\":\"1e1001\"}");
            assertScaleRejected(mapper, "{\"score\":\"1E+1001\"}");
        }
    }

    @Test
    public void bigDecimalNumberPropertyRejectsScaleWithoutAdapter() {
        try (final Mapper mapper = new MapperBuilder().setUseBigDecimalStringAdapter(false).build()) {
            assertScaleRejected(mapper, "{\"score\":1e1001}");
        }
    }

    @Test
    public void bigDecimalPropertyWithinScale() {
        try (final Mapper mapper = new MapperBuilder().build()) {
            assertEquals(new BigDecimal("1.25"), readScore(mapper, "{\"score\":1.25}"));
            assertEquals(new BigDecimal("1.25"), readScore(mapper, "{\"score\":\"1.25\"}"));
            assertEquals(new BigDecimal("1e1000"), readScore(mapper, "{\"score\":1e1000}"));
            assertEquals(new BigDecimal("1e1000"), readScore(mapper, "{\"score\":\"1e1000\"}"));
        }
    }

    @Test
    public void bigDecimalPropertyConfiguredScale() {
        try (final Mapper mapper = new MapperBuilder().setMaxBigDecimalScale(2000).build()) {
            assertEquals(new BigDecimal("1e2000"), readScore(mapper, "{\"score\":1e2000}"));
            assertEquals(new BigDecimal("1e2000"), readScore(mapper, "{\"score\":\"1e2000\"}"));
            assertScaleRejected(mapper, "{\"score\":\"1e2001\"}");
        }
    }

    @Test
    public void minimumBigDecimalScaleRejected() {
        final BigDecimal value = new BigDecimal(BigInteger.ONE, Integer.MIN_VALUE);
        try {
            new BigDecimalConverter(1000, 1000).checkScale(value);
            fail("expected the minimum scale to be rejected");
        } catch (final ArithmeticException expected) {
            // The scale's absolute value must not overflow an int.
        }
    }

    @Test
    public void numericBigDecimalHonorsConverterLimit() {
        try (final Mapper mapper = new MapperBuilder()
                .addConverter(BigDecimal.class, new BigDecimalConverter(2, 1000)).build()) {
            assertEquals(new BigDecimal("1e2"), readScore(mapper, "{\"score\":1e2}"));
            assertEquals(new BigDecimal("1e-2"), readScore(mapper, "{\"score\":1e-2}"));
            assertScaleRejected(mapper, "{\"score\":1e3}");
            assertScaleRejected(mapper, "{\"score\":1e-3}");
            assertScaleRejected(mapper, "{\"score\":\"1e3\"}");
        }
    }

    @Test
    public void numericBigDecimalHonorsMapperLimitWithExplicitProvider() {
        final org.apache.johnzon.core.JsonProviderImpl provider = new org.apache.johnzon.core.JsonProviderImpl();
        try (final Mapper mapper = new MapperBuilder().setProvider(provider).setMaxBigDecimalScale(2).build()) {
            assertScaleRejected(mapper, "{\"score\":1e3}");
            assertScaleRejected(mapper, "{\"score\":\"1e3\"}");
            assertEquals(1000, provider.getMaxBigDecimalScale());
        }
    }

    @Test
    public void cachedBigDecimalAdapterHonorsScaleChanges() {
        final MapperBuilder builder = new MapperBuilder().setMaxBigDecimalScale(4);
        builder.getAdapters().get(new AdapterKey(BigDecimal.class, String.class));
        try (final Mapper mapper = builder.setMaxBigDecimalScale(2).build()) {
            assertScaleRejected(mapper, "{\"score\":\"1e3\"}");
        }
        try (final Mapper mapper = builder.setMaxBigDecimalScale(4).build()) {
            assertEquals(new BigDecimal("1e3"), readScore(mapper, "{\"score\":\"1e3\"}"));
        }
        try (final Mapper mapper = builder.setMaxBigDecimalScale(2).build()) {
            assertScaleRejected(mapper, "{\"score\":\"1e3\"}");
        }
    }

    @Test
    public void earlyBigDecimalAdapterHasDefaultLimit() {
        final MapperBuilder builder = new MapperBuilder();
        final Adapter<?, ?> adapter = builder.getAdapters().get(new AdapterKey(BigDecimal.class, String.class));
        try {
            ((Adapter<BigDecimal, String>) adapter).to("1e1001");
            fail("expected the default scale limit before build");
        } catch (final ArithmeticException expected) {
            // An early lookup must not create an unlimited converter.
        }
        try (final Mapper mapper = builder.build()) {
            assertScaleRejected(mapper, "{\"score\":\"1e1001\"}");
        }
    }

    @Test
    public void scaleChangesPreserveRegisteredConverter() {
        try (final Mapper mapper = new MapperBuilder()
                .addConverter(BigDecimal.class, new BigDecimalConverter(4, 1000)).setMaxBigDecimalScale(2).build()) {
            assertEquals(new BigDecimal("1e3"), readScore(mapper, "{\"score\":\"1e3\"}"));
            assertScaleRejected(mapper, "{\"score\":\"1e5\"}");
        }
    }

    @Test
    public void numericBigDecimalHonorsStricterProviderLimit() {
        final org.apache.johnzon.core.JsonProviderImpl provider = new org.apache.johnzon.core.JsonProviderImpl();
        provider.setMaxBigDecimalScale(100);
        try (final Mapper mapper = new MapperBuilder().setProvider(provider).setMaxBigDecimalScale(1000).build()) {
            assertEquals(new BigDecimal("1e-100"), readScore(mapper, "{\"score\":1e-100}"));
            assertScaleRejected(mapper, "{\"score\":1e-101}");
            assertRejected(() -> mapper.readObject("{\"scores\":[1e-101]}", BigDecimalListHolder.class));
        }
    }

    @Test
    public void builtMapperKeepsScaleWhenBuilderChanges() {
        final MapperBuilder builder = new MapperBuilder().setMaxBigDecimalScale(2);
        try (final Mapper first = builder.build()) {
            try (final Mapper second = builder.setMaxBigDecimalScale(4).build()) {
                assertScaleRejected(first, "{\"score\":\"1e3\"}");
                assertScaleRejected(first, "{\"score\":1e3}");
                assertEquals(new BigDecimal("1e3"), readScore(second, "{\"score\":\"1e3\"}"));
                assertEquals(new BigDecimal("1e3"), readScore(second, "{\"score\":1e3}"));
            }
        }
    }

    @Test
    public void nestedBigDecimalsHonorMapperLimit() {
        try (final Mapper mapper = new MapperBuilder().setProvider(new org.apache.johnzon.core.JsonProviderImpl())
                .setMaxBigDecimalScale(2).setUseBigDecimalForObjectNumbers(true).build()) {
            final BigDecimalListHolder list = mapper.readObject("{\"scores\":[1e2]}", BigDecimalListHolder.class);
            assertEquals(new BigDecimal("1e2"), list.scores.get(0));
            assertRejected(() -> mapper.readObject("{\"scores\":[1e3]}", BigDecimalListHolder.class));
            assertRejected(() -> mapper.readObject("{\"scores\":[1e3]}", BigDecimalArrayHolder.class));
            assertRejected(() -> mapper.readObject("{\"scores\":{\"a\":1e3}}", BigDecimalMapHolder.class));
            assertRejected(() -> mapper.readObject("{\"scores\":{\"a\":1e3}}", ObjectMapHolder.class));
            assertRejected(() -> mapper.readObject("1e3", BigDecimal.class));
        }
        try (final Mapper mapper = new MapperBuilder().setMaxBigDecimalScale(2).setUseBigDecimalStringAdapter(false).build()) {
            assertScaleRejected(mapper, "{\"score\":1e3}");
        }
    }

    @Test
    public void defaultBigDecimalConverterIsBounded() {
        try (final Mapper mapper = new MapperBuilder().addConverter(BigDecimal.class, new BigDecimalConverter()).build()) {
            assertEquals(new BigDecimal("1e1000"), readScore(mapper, "{\"score\":\"1e1000\"}"));
            assertScaleRejected(mapper, "{\"score\":\"1e1001\"}");
        }
    }

    private static void assertRejected(final Runnable read) {
        try {
            read.run();
        } catch (final RuntimeException e) {
            Throwable t = e;
            while (t != null && !ArithmeticException.class.isInstance(t)) {
                t = t.getCause();
            }
            assertNotNull("expected a scale rejection but got " + e, t);
            return;
        }
        fail("expected a scale rejection");
    }

    private static BigDecimal readScore(final Mapper mapper, final String json) {
        final BigDecimalHolder holder = mapper.readObject(json, BigDecimalHolder.class);
        return holder.score;
    }

    private static void assertScaleRejected(final Mapper mapper, final String json) {
        try {
            mapper.readObject(json, BigDecimalHolder.class);
        } catch (final RuntimeException e) {
            Throwable t = e;
            while (t != null && !ArithmeticException.class.isInstance(t)) {
                t = t.getCause();
            }
            assertNotNull("expected a scale rejection for " + json + " but got " + e, t);
            return;
        }
        fail("expected " + json + " to be rejected");
    }

    public static class Holder {
        public long value;
    }

    public static class Num {
        public Number value;
    }

    public static class BigDecimalHolder {
        public BigDecimal score;
    }

    public static class BigIntegerHolder {
        public BigInteger value;
    }

    public static class BigDecimalListHolder {
        public java.util.List<BigDecimal> scores;
    }

    public static class BigDecimalArrayHolder {
        public BigDecimal[] scores;
    }

    public static class BigDecimalMapHolder {
        public java.util.Map<String, BigDecimal> scores;
    }

    public static class ObjectMapHolder {
        public java.util.Map<String, Object> scores;
    }
}
