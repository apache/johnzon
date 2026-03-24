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

    public static class Holder {
        public long value;
    }

    public static class Num {
        public Number value;
    }

    public static class BigDecimalHolder {
        public BigDecimal score;
    }
}
