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
package org.apache.fleece.core;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import static org.junit.Assert.assertEquals;

public class JsonArrayBuilderImplTest {
    @Test
    public void array() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add("a").add("b");
        assertEquals("[\"a\",\"b\"]", builder.build().toString());
    }
    
    @Test
    public void emptyArray() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        assertEquals("[]", builder.build().toString());
    }
    
    @Test
    public void nullArray() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.addNull().addNull();
        assertEquals("[null,null]", builder.build().toString());
    }
    
    @Test
    public void nullJsonValueArray() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add(JsonValue.NULL).add(JsonValue.NULL);
        assertEquals("[null,null]", builder.build().toString());
    }
    
    @Test
    public void boolJsonValueArray() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add(JsonValue.TRUE).add(JsonValue.FALSE);
        assertEquals("[true,false]", builder.build().toString());
    }
    
    @Test(expected=NullPointerException.class)
    public void addStringNpeIfNull() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add((String) null);
    }
    
    @Test(expected=NullPointerException.class)
    public void addJVNpeIfNull() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add((JsonValue) null);
    }
    
    @Test(expected=NullPointerException.class)
    public void addBDNpeIfNull() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add((BigDecimal) null);
    }
    
    @Test(expected=NullPointerException.class)
    public void addBINpeIfNull() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add((BigInteger) null);
    }
    
    @Test(expected=NullPointerException.class)
    public void addJABuilderNpeIfNull() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add((JsonArrayBuilder) null);
    }
    
    @Test(expected=NullPointerException.class)
    public void addJOBuilderNpeIfNull() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add((JsonObjectBuilder) null);
    }
    
    @Test(expected=NumberFormatException.class)
    public void addDoubleNpeIfNaN() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add((double) Double.NaN);
    }
    
    @Test(expected=NumberFormatException.class)
    public void addDoubleNpeIfPosInfinite() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add((double) Double.POSITIVE_INFINITY);
    }
    
    @Test(expected=NumberFormatException.class)
    public void addDoubleNpeIfNegIfinite() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add((double) Double.NEGATIVE_INFINITY);
    }
}
