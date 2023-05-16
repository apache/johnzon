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
package org.apache.johnzon.core;

import org.junit.Assert;
import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;


public class JsonNumberTest {
    @Test
    public void nonZeroFractional() {
        final JsonNumber number = Json.createArrayBuilder()
                .add(12345.6489)
                .build()
                .getJsonNumber(0);
        try {
            number.intValueExact();
            fail();
        } catch (final ArithmeticException ae) {
            // ok
        }
        try {
            number.longValueExact();
            fail();
        } catch (final ArithmeticException ae) {
            // ok
        }
    }
    @Test
    public void equals() {
        final JsonNumber a = Json.createObjectBuilder().add("a", 1).build().getJsonNumber("a");
        final JsonNumber b = Json.createObjectBuilder().add("b", 1.1).build().getJsonNumber("b");
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
    }
    
    @Test(expected=ArithmeticException.class)
    public void testBigIntegerExact() {
        JsonArray array = Json.createArrayBuilder().add(100.0200).build();
        array.getJsonNumber(0).bigIntegerValueExact();
    }

    @Test
    public void testBigInteger() {
        JsonArray array = Json.createArrayBuilder().add(100.0200).build();
        Assert.assertEquals(new BigInteger("100"), array.getJsonNumber(0).bigIntegerValue());
    }

    @Test
    public void testSlowBigIntegerConversion() {
        JsonArray array = Json.createArrayBuilder()
                              .add(new BigDecimal("1e1000")) // 1e20000000 --> lost of damage
                              .add(Double.MAX_VALUE)
                              .build();

        { // for Double
            long start = System.nanoTime();
            for (int i = 1; i < 5; i++) {
                // if it takes a few seconds in any machine, that's already too much
                if (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) > 1) {
                    fail("took too long: " + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) + " s" +
                         " to compute " + i + " conversions toBigInteger");
                }

                array.getJsonNumber(1).bigIntegerValue();
            }
            long end = System.nanoTime();
            System.out.println("took: " + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms");
        }

        { // for Number
            long start = System.nanoTime();
            for (int i = 1; i < 100; i++) {
                // if it takes a second in any machine, that's already too much
                // depends on the allowed scale in JsonNumberImpl#checkBigDecimalScale
                if (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) > 1) {
                    fail("took too long: " + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) + " s" +
                         " to compute " + i + " conversions toBigInteger");
                }

                array.getJsonNumber(0).bigIntegerValue();
            }
            long end = System.nanoTime();
            System.out.println("took: " + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms");
        }
    }

    @Test(expected = ArithmeticException.class)
    public void testBigIntegerConversionLimit() {
        JsonArray array = Json.createArrayBuilder()
                              .add(new BigDecimal("1e1001")) // limit is 1000 by default
                              .build();

        array.getJsonNumber(0).bigIntegerValue();
    }

    @Test
    public void testBigIntegerButFromJustALongTooLong() {
        final StringWriter writer = new StringWriter();
        Json.createGenerator(writer).writeStartObject().write("value", new BigInteger("10002000000000000000")).writeEnd().close();
        final String asJson = writer.toString();
        final JsonNumber jsonNumber = Json.createReader(new StringReader(asJson)).readObject().getJsonNumber("value");
        Assert.assertEquals(new BigInteger("10002000000000000000"), jsonNumber.bigIntegerValue());
    }

    @Test
    public void testHashCode() {
        JsonNumber a = Json.createObjectBuilder().add("a", 1).build().getJsonNumber("a");
        JsonNumber b = Json.createObjectBuilder().add("b", 1.1).build().getJsonNumber("b");

        Assert.assertEquals(a.hashCode(), a.bigDecimalValue().hashCode());
        Assert.assertEquals(b.hashCode(), b.bigDecimalValue().hashCode());
    }
}
