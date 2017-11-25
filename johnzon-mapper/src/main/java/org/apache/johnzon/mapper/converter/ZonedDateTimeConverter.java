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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import static org.apache.johnzon.mapper.converter.Java8Converter.ZONE_ID_UTC;

public class ZonedDateTimeConverter extends Java8Converter<ZonedDateTime> {

    
    private static final DateTimeFormatter FORMATTER_OUT = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZONE_ID_UTC);
    // 2007-12-03T17:15:00+03:00
    // 2007-12-03T17:15:00.0+03:00
    // 2007-12-03T17:15:00.00+03:00
    // 2007-12-03T17:15:00.000+03:00
    private static final DateTimeFormatter FORMATTER_IN_ISO_LONG = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // 2007-12-03T14:15
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(ZONE_ID_UTC);

    //2007-12-03T17:15+03:00
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXXX");

    // 200712031415Z
    // 200712031115-0300
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_3 = DateTimeFormatter.ofPattern("yyyyMMddHHmmXX");

    // 20071203141500Z
    // 20071203111500-0300
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_4 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssXX");

    // 200712031415
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_5 = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZONE_ID_UTC);

    // 20071203141500
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_6 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZONE_ID_UTC);

    // 2007-12-03T17:15:00+0300
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_7 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX");

    // 2007-12-03T17:15:00.0+0300
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_8 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SXX");

    // 2007-12-03T17:15:00.00+0300
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_9 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSXX");

    // 2007-12-03T17:15:00.000+0300
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_10 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");

    //2007-12-03T14:15:00.000Z
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_11 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    // 2007-12-03T17:15+0300
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_12 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXX");

    // 20071203111500-03:00
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_13 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssXXX");

    // 200712031115-03:00
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_14 = DateTimeFormatter.ofPattern("yyyyMMddHHmmXXX");
    
    // 2007-12-03T14:15:00
    private static final DateTimeFormatter FORMATTER_IN_PATTERN_15 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZONE_ID_UTC);
    
    
    /**
     * Returns the Instant in format yyyy-MM-dd'T'HH:mm:ssZ
     * @param zdt
     * @return String representation
     */
    @Override
    public String toString(final ZonedDateTime zdt) {
        return zdt.toString();
    }

    /**
     * Parse string that could be in a number of different formats into an Instant.
     * A missing time zone assumes UTC.
     * Examples of what needs to be coped with (all are representing the same instant in time):
     *
     * 2007-12-03T17:15:00+03:00
     * 2007-12-03T17:15:00.0+03:00
     * 2007-12-03T17:15:00.00+03:00
     * 2007-12-03T17:15:00.000+03:00
     * 2007-12-03T17:15+03:00
     * 2007-12-03T14:15
     *
     * 20071203141500Z
     * 200712031415Z
     *
     * 20071203111500-0300
     * 200712031115-0300
     *
     * 200712031415
     * 20071203141500
     * @param text
     * @return Instant in time
     */
    @Override
    public ZonedDateTime fromString(final String text) {
        try {
            if (text.contains("T")) {
                switch (text.length()) {
                    case 16:
                        // 2007-12-03T14:15
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_1.parse(text));
                    case 19:
                        // 2007-12-03T14:15:00
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_15.parse(text));
                    case 21:
                        // 2007-12-03T17:15+0300
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_12.parse(text));
                    case 22:
                        // 2007-12-03T17:15+03:00
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_2.parse(text));
                    case 24:
                        if (text.endsWith("Z")) {
                            // 2007-12-03T14:15:00.000Z
                            return ZonedDateTime.from(FORMATTER_IN_PATTERN_11.parse(text));
                        } else {
                            // 2007-12-03T17:15:00+0300
                            return ZonedDateTime.from(FORMATTER_IN_PATTERN_7.parse(text));
                        }
                    case 26:
                        // 2007-12-03T17:15:00.0+0300
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_8.parse(text));
                    case 27:
                        if (text.charAt(text.length() - 3) != ':') {
                            // 2007-12-03T17:15:00.00+0300
                            return ZonedDateTime.from(FORMATTER_IN_PATTERN_9.parse(text));
                        } else {
                            return ZonedDateTime.from(FORMATTER_IN_ISO_LONG.parse(text));
                        }
                    case 28:
                        if (text.charAt(text.length() - 3) != ':') {
                            // 2007-12-03T17:15:00.000+0300
                            return ZonedDateTime.from(FORMATTER_IN_PATTERN_10.parse(text));
                        } else {
                            return ZonedDateTime.from(FORMATTER_IN_ISO_LONG.parse(text));
                        }
                    default:
                        // Others can be picked up by ISO_OFFSET_DATE_TIME
                        return ZonedDateTime.from(FORMATTER_IN_ISO_LONG.parse(text));
                }
            } else if (text.endsWith("Z")) {
                switch (text.length()) {
                    case 13:
                        // 200712031415Z
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_3.parse(text));
                    default:
                        // 20071203141500Z
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_4.parse(text));
                }
            } else if (text.contains("-") || text.contains("+")) {
                switch (text.length()) {
                    case 17:
                        // 200712031115-0300
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_3.parse(text));
                    case 18:
                        // 200712031115-03:00
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_14.parse(text));
                    case 20:
                        // 20071203111500-03:00
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_13.parse(text));
                    default:
                        // 20071203111500-0300
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_4.parse(text));
                }
            } else {
                switch (text.length()) {
                    case 12:
                        // 200712031415
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_5.parse(text));
                    default:
                        // 20071203141500
                        return ZonedDateTime.from(FORMATTER_IN_PATTERN_6.parse(text));
                }
            }
        } catch (final DateTimeParseException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

