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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.json.bind.annotation.JsonbDateFormat;

public class JsonbDateConverter extends JsonbDateConverterBase<Date> {
    private static final ZoneId UTC = ZoneId.of("UTC");

    // TODO: cheap impl to avoid to rely on exceptions, better can be to parse format
    private volatile boolean hasTimezone = true;

    public JsonbDateConverter(final JsonbDateFormat dateFormat) {
        super(dateFormat);
    }

    @Override
    public String toString(final Date instance) {
        return formatter == null ?
                Long.toString(instance.getTime()) :
                toStringWithFormatter(instance);
    }

    @Override
    public Date fromString(final String text) {
        return formatter == null ?
                new Date(Long.parseLong(text)) :
                fromStringWithFormatter(text);
    }

    private Date fromStringWithFormatter(final String text) {
        final boolean hasTimezone = this.hasTimezone;
        try {
            if (hasTimezone) {
                return fromZonedDateTime(text);
            }
            return fromLocalDateTime(text);
        } catch (final DateTimeException dte) {
            this.hasTimezone = !hasTimezone;
            if (hasTimezone) {
                return fromLocalDateTime(text);
            }
            return fromZonedDateTime(text);
        }
    }

    private String toStringWithFormatter(final Date instance) {
        final boolean hasTimezone = this.hasTimezone;
        final Instant instant = Instant.ofEpochMilli(instance.getTime());
        try {
            if (hasTimezone) {
                return toStringFromZonedDateTime(instant);
            }
            return toStringFromLocalDateTime(instant);
        } catch (final DateTimeException dte) {
            this.hasTimezone = !hasTimezone;
            if (hasTimezone) {
                return toStringFromLocalDateTime(instant);
            }
            return toStringFromZonedDateTime(instant);
        }
    }

    private Date fromLocalDateTime(final String text) {
        return Date.from(LocalDateTime.parse(text, formatter).toInstant(ZoneOffset.UTC));
    }

    private Date fromZonedDateTime(final String text) {
        return Date.from(ZonedDateTime.parse(text, formatter).toInstant());
    }

    private String toStringFromLocalDateTime(final Instant instant) {
        return formatter.format(LocalDateTime.ofInstant(instant, UTC));
    }

    private String toStringFromZonedDateTime(final Instant instant) {
        return formatter.format(ZonedDateTime.ofInstant(instant, UTC));
    }
}
