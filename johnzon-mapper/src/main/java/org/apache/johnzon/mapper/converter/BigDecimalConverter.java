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
package org.apache.johnzon.mapper.converter;

import org.apache.johnzon.mapper.Converter;

import java.math.BigDecimal;

public class BigDecimalConverter implements Converter<BigDecimal> {
    public static final int DEFAULT_MAX_SCALE = Integer.getInteger("johnzon.max-big-decimal-scale", 1000);
    public static final int DEFAULT_MAX_NUMBER_LENGTH = Integer.getInteger("org.apache.johnzon.max-number-length", 1000);

    private final int maxScale;
    private final int maxLength;

    public BigDecimalConverter() {
        this(DEFAULT_MAX_SCALE, DEFAULT_MAX_NUMBER_LENGTH);
    }

    public BigDecimalConverter(final int maxScale, final int maxLength) {
        this.maxScale = maxScale;
        this.maxLength = maxLength;
    }

    @Override
    public String toString(final BigDecimal instance) {
        // when using the converter, user expects the decimal notation
        // otherwise, JsonNumber will give the E (scientific) notation
        checkScale(instance);
        return instance.toPlainString();
    }

    @Override
    public BigDecimal fromString(final String text) {
        if (text.length() > maxLength) {
            throw new NumberFormatException(String.format(
                    "Number length (%d) exceeds maximum allowed (%d)", text.length(), maxLength));
        }
        final BigDecimal value = new BigDecimal(text);
        checkScale(value);
        return value;
    }

    /**
     * Checks a decimal obtained without parsing a string, for example from a JSON-P provider.
     * @param value the decimal to validate
     * @throws ArithmeticException if its absolute scale exceeds this converter's limit
     */
    public void checkScale(final BigDecimal value) {
        checkScale(value, maxScale);
    }

    /**
     * @param value the decimal to validate
     * @param maxScale the maximum absolute scale
     * @throws ArithmeticException if the absolute scale of the value exceeds maxScale
     */
    public static void checkScale(final BigDecimal value, final int maxScale) {
        if (Math.abs((long) value.scale()) > maxScale) {
            throw new ArithmeticException(String.format(
                    "BigDecimal scale (%d) limit exceeds maximum allowed (%d)",
                    value.scale(), maxScale));
        }
    }
}
