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

import jakarta.json.JsonNumber;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class JsonLongImpl implements JsonNumber, Serializable {
    private final long value;
    private Integer hashCode = null;

    JsonLongImpl(final long value) {
        this.value = value;
    }

    @Override
    public Number numberValue() {
        return value;
    }

    @Override
    public boolean isIntegral() {
        return true;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public int intValueExact() {
        int intVal =  intValue();
        if (intVal != value) {
            throw new java.lang.ArithmeticException("Overflow");
        }
        return intVal;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public long longValueExact() {
        return value;
    }

    @Override
    public BigInteger bigIntegerValue() {
        return new BigInteger(toString());
    }

    @Override
    public BigInteger bigIntegerValueExact() {
        return bigIntegerValue();
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
        return Long.toString(value);
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
        if (JsonLongImpl.class.isInstance(obj)) {
            return JsonLongImpl.class.cast(obj).value == value;
        }
        return JsonNumber.class.isInstance(obj) && JsonNumber.class.cast(obj).bigDecimalValue().equals(bigDecimalValue());
    }
}
