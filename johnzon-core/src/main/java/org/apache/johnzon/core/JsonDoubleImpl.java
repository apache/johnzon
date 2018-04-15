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

final class JsonDoubleImpl implements JsonNumber, Serializable {
    private final double value;

    private Integer hashCode = null;

    JsonDoubleImpl(final double value) {
        
        if(Double.isInfinite(value) || Double.isNaN(value)) {
            throw new NumberFormatException("double value must not be NaN or Infinite");
        }
        
        this.value = value;
    }

    @Override
    public boolean isIntegral() {
        return false;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public int intValueExact() {
        checkFractionalPart();
        return intValue();
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    @Override
    public long longValueExact() {
        checkFractionalPart();
        return (long) value;
    }

    @Override
    public BigInteger bigIntegerValue() {
        return new BigDecimal(toString()).toBigInteger();
    }

    @Override
    public BigInteger bigIntegerValueExact() {
        return new BigDecimal(toString()).toBigIntegerExact();
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public BigDecimal bigDecimalValue() {
        return new BigDecimal(toString());
    }

    @Override
    public ValueType getValueType() {
        return ValueType.NUMBER;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = bigDecimalValue().hashCode();
        }

        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (JsonDoubleImpl.class.isInstance(obj)) {
            return JsonDoubleImpl.class.cast(obj).value == value;
        }
        return JsonNumber.class.isInstance(obj) && JsonNumber.class.cast(obj).doubleValue() == value;
    }

    private void checkFractionalPart() {
        if ((value % 1) != 0) {
            throw new ArithmeticException("Not an int/long, use other value readers");
        }
    }
}
