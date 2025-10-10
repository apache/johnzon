/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.mapper.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;

public final class DateUtil {
    private DateUtil() {

    }

    public static ZonedDateTime parseZonedDateTime(final String text, final DateTimeFormatter formatter, ZoneId defaultZone) {
        final TemporalAccessor parse = formatter.parse(text);
        ZoneId zone = parse.query(TemporalQueries.zone());
        if (zone == null) {
            zone = defaultZone;
        }

        return ZonedDateTime.of(
                ifSupported(parse, ChronoField.YEAR),
                ifSupported(parse, ChronoField.MONTH_OF_YEAR),
                ifSupported(parse, ChronoField.DAY_OF_MONTH),
                ifSupported(parse, ChronoField.HOUR_OF_DAY),
                ifSupported(parse, ChronoField.MINUTE_OF_HOUR),
                ifSupported(parse, ChronoField.SECOND_OF_MINUTE),
                ifSupported(parse, ChronoField.MILLI_OF_SECOND),
                zone);
    }

    public static int ifSupported(TemporalAccessor temporal, ChronoField unit) {
        if (temporal.isSupported(unit)) {
            return temporal.get(unit);
        }

        return 0;
    }
}
