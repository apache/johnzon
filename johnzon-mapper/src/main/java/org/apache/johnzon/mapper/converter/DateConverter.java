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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

public class DateConverter implements Converter<Date> {
    private final DateTimeFormatter formatter;

    public DateConverter(final String pattern) {
        formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public String toString(final Date instance) {
        return formatter.format(instance.toInstant().atZone(ZoneId.systemDefault()));
    }

    @Override
    public Date fromString(final String text) {
        try {
            TemporalAccessor parsedValue = formatter.parse(text);

            if (parsedValue.isSupported(ChronoField.OFFSET_SECONDS)) {
                // if "text" includes a timezone, just create an instant from it
                return Date.from(Instant.from(parsedValue));
            } else {
                // otherwise, create a timezone-less localdatetime first, add the systemdefault timezone, and finally get the instant
                return Date.from(LocalDateTime.from(parsedValue).atZone(ZoneId.systemDefault()).toInstant());
            }
        } catch (final DateTimeParseException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
