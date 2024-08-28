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
import org.apache.johnzon.mapper.util.DateUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

public class DateConverter implements Converter<Date> {
    // Almost ISO 8601 basic, but zone is defined by generic name instead of zone-offset
    public static final DateConverter ISO_8601_SHORT = new DateConverter("yyyyMMddHHmmssv");

    private static final ZoneId UTC = ZoneId.of("UTC");

    private final DateTimeFormatter formatter;

    public DateConverter(final String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern, Locale.ROOT);
    }

    @Override
    public String toString(final Date instance) {
        return formatter.format(ZonedDateTime.ofInstant(instance.toInstant(), UTC));
    }

    @Override
    public Date fromString(final String text) {
        try {
            return Date.from(DateUtil.parseZonedDateTime(text, formatter, UTC).toInstant());
        } catch (final DateTimeParseException dpe) {
            return Date.from(LocalDateTime.parse(text).toInstant(ZoneOffset.UTC));
        }
    }
}
