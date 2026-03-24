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
     * Both default to false (JSON number).
     */
    @Test
    public void bigDecimalStringAdapterFlagControlsBigDecimal() {
        // Default: BigDecimal adapter is OFF (useBigDecimalStringAdapter=false)
        final LazyConverterMap defaultAdapters = new LazyConverterMap();
        assertNull("BigDecimal adapter should be null by default",
                defaultAdapters.get(new AdapterKey(BigDecimal.class, String.class)));
        // Enabled: BigDecimal adapter is ON (fresh instance to avoid NO_ADAPTER cache)
        final LazyConverterMap enabledAdapters = new LazyConverterMap();
        enabledAdapters.setUseBigDecimalStringAdapter(true);
        assertNotNull("BigDecimal adapter should be active when flag is true",
                enabledAdapters.get(new AdapterKey(BigDecimal.class, String.class)));
    }

    @Test
    public void bigIntegerStringAdapterFlagControlsBigInteger() {
        final LazyConverterMap adapters = new LazyConverterMap();
        // Default: BigInteger adapter is OFF (useBigIntegerStringAdapter=false)
        assertNull("BigInteger adapter should be null by default",
                adapters.get(new AdapterKey(BigInteger.class, String.class)));
        // Enable it explicitly
        final LazyConverterMap adapters2 = new LazyConverterMap();
        adapters2.setUseBigIntegerStringAdapter(true);
        assertNotNull("BigInteger adapter should be active when flag is true",
                adapters2.get(new AdapterKey(BigInteger.class, String.class)));
    }

    /**
     * With useBigDecimalStringAdapter=false (default), BigDecimal fields
     * should be serialized as JSON numbers in the mapper.
     */
    @Test
    public void bigDecimalDefaultSerializesAsNumber() {
        try (final Mapper mapper = new MapperBuilder().build()) {
            final BigDecimalHolder holder = new BigDecimalHolder();
            holder.score = new BigDecimal("0.000000733915");
            final String json = mapper.writeObjectAsString(holder);
            // Default: BigDecimal as JSON number (per JSON-B spec and RFC 8259)
            assertEquals("{\"score\":7.33915E-7}", json);
        }
    }

    /**
     * With useBigDecimalStringAdapter=true, BigDecimal fields should be
     * serialized as JSON strings using plain notation (symmetric with deserialization).
     */
    @Test
    public void bigDecimalWithStringAdapterSerializesAsString() {
        try (final Mapper mapper = new MapperBuilder()
                .setUseBigDecimalStringAdapter(true)
                .build()) {
            final BigDecimalHolder holder = new BigDecimalHolder();
            holder.score = new BigDecimal("0.000000733915");
            final String json = mapper.writeObjectAsString(holder);
            // String adapter uses toPlainString(), no scientific notation
            assertEquals("{\"score\":\"0.000000733915\"}", json);
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
