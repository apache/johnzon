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

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonValue;

import org.junit.Assert;
import org.junit.Test;

public class JsonProviderTest {

    @Test
    public void testJsonCreateValueString() {
        JsonValue val = Json.createValue("hello");
        Assert.assertNotNull(val);
        Assert.assertEquals(JsonValue.ValueType.STRING, val.getValueType());
        Assert.assertEquals("\"hello\"", val.toString());
    }

    @Test
    public void testJsonCreateValueInt() {
        JsonValue val = Json.createValue(4711);
        Assert.assertNotNull(val);
        Assert.assertEquals(JsonValue.ValueType.NUMBER, val.getValueType());
        Assert.assertEquals("4711", val.toString());
    }

    @Test
    public void testJsonCreateValueLong() {
        JsonValue val = Json.createValue(1234567890L);
        Assert.assertNotNull(val);
        Assert.assertEquals(JsonValue.ValueType.NUMBER, val.getValueType());
        Assert.assertEquals("1234567890", val.toString());
    }

    @Test
    public void testJsonCreateValueDouble() {
        double d = 1234567890.12345d;
        JsonNumber val = Json.createValue(d);
        Assert.assertNotNull(val);
        Assert.assertEquals(JsonValue.ValueType.NUMBER, val.getValueType());

        Assert.assertEquals(d, val.doubleValue(), 0.01f);
    }

    @Test
    public void testJsonCreateValueBigDecimal() {
        BigDecimal bd = new BigDecimal(1234567890.12345d);
        JsonNumber val = Json.createValue(bd);
        Assert.assertNotNull(val);
        Assert.assertEquals(JsonValue.ValueType.NUMBER, val.getValueType());

        Assert.assertEquals(bd, val.bigDecimalValue());
    }

    @Test
    public void testJsonCreateValueBigInteger() {
        BigInteger bi = BigInteger.valueOf(1234567890L);
        JsonNumber val = Json.createValue(bi);
        Assert.assertNotNull(val);
        Assert.assertEquals(JsonValue.ValueType.NUMBER, val.getValueType());

        Assert.assertEquals(bi, val.bigIntegerValue());
    }

    @Test
    public void testJsonCreateValueNumber() {
        Number someNumber = 42;
        JsonNumber val = Json.createValue(someNumber);

        Assert.assertNotNull(val);
        Assert.assertEquals(JsonValue.ValueType.NUMBER, val.getValueType());
        Assert.assertEquals(someNumber, val.intValue());
    }
}
