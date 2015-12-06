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
package org.apache.johnzon.jsonb.converter;

import javax.json.bind.annotation.JsonbNumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;
import java.util.function.Supplier;

public class JsonbNumberConverter extends JsonbLocaleParserConverterBase<Number> {
    private final Supplier<NumberFormat> delegateFactory; // NumberFormat is not thread safe
    private final Queue<NumberFormat> pool = new ArrayBlockingQueue<>(30); // configurable?

    public JsonbNumberConverter(final JsonbNumberFormat numberFormat) {
        final String locale = numberFormat.locale();
        final String format = numberFormat.value();
        final boolean customLocale = !JsonbNumberFormat.DEFAULT_LOCALE.equals(locale);
        if (format.isEmpty() && customLocale) {
            delegateFactory = () -> NumberFormat.getInstance(newLocale(locale));
        } else if (format.isEmpty()) {
            delegateFactory = NumberFormat::getInstance;
        } else if (customLocale) {
            delegateFactory = () -> new DecimalFormat(format, DecimalFormatSymbols.getInstance(newLocale(locale)));
        } else {
            delegateFactory = () -> new DecimalFormat(format);
        }
    }

    @Override
    public String toString(final Number instance) {
        return execute(f -> f.format(instance));
    }

    @Override
    public Number fromString(final String text) {
        return execute(f -> {
            try {
                return f.parse(text);
            } catch (final ParseException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    private <T> T execute(final Function<NumberFormat, T> function) {
        NumberFormat format = pool.poll();
        if (format == null) {
            format = delegateFactory.get();
        }
        try {
            return function.apply(format);
        } finally {
            pool.add(format);
        }
    }
}
