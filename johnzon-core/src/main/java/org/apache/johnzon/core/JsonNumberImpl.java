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

import javax.json.JsonNumber;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

final class JsonNumberImpl implements JsonNumber, Serializable {
    private final BigDecimal value;
    private transient Integer hashCode = null;

    JsonNumberImpl(final BigDecimal decimal) {
        if (decimal == null) {
            throw new NullPointerException("decimal must not be null");
        }

        this.value = decimal;
    }

    @Override
    public boolean isIntegral() {
        return value.scale() == 0;
    }

    @Override
    public int intValue() {
        return value.intValue();
    }

    @Override
    public int intValueExact() {
        checkFractionalPart();
        return value.intValueExact();
    }

    @Override
    public long longValue() {
        return value.longValue();
    }

    @Override
    public long longValueExact() {
        checkFractionalPart();
        return value.longValueExact();
    }

    @Override
    public BigInteger bigIntegerValue() {
        return value.toBigInteger();
    }

    @Override
    public BigInteger bigIntegerValueExact() {
        return value.toBigIntegerExact();
    }

    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.NUMBER;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public int hashCode() {
        Integer h = hashCode;
        if (h == null) {
            h = value.hashCode();
            hashCode = h;
        }
        return h;
    }

    @Override
    public boolean equals(final Object obj) {
        return JsonNumber.class.isInstance(obj) && JsonNumber.class.cast(obj).bigDecimalValue().equals(value);
    }

    private void checkFractionalPart() {
        if (value.remainder(BigDecimal.ONE).doubleValue() != 0) {
            throw new ArithmeticException("Not an int/long, use other value readers");
        }
    }
}
