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

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class InstantConverter extends Java8Converter<Instant> {

    private final DateTimeFormatter formatterOut = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneIDUTC);
    // 2007-12-03T17:15:00+03:00
    // 2007-12-03T17:15:00.0+03:00
    // 2007-12-03T17:15:00.00+03:00
    // 2007-12-03T17:15:00.000+03:00
    private final DateTimeFormatter formatterInISOLong = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // 2007-12-03T14:15
    private final DateTimeFormatter formatterInPattern1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(zoneIDUTC);

    //2007-12-03T17:15+03:00
    private final DateTimeFormatter formatterInPattern2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXXX");

    // 200712031415Z
    // 200712031115-0300
    private final DateTimeFormatter formatterInPattern3 = DateTimeFormatter.ofPattern("yyyyMMddHHmmXX");

    // 20071203141500Z
    // 20071203111500-0300
    private final DateTimeFormatter formatterInPattern4 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssXX");

    // 200712031415
    private final DateTimeFormatter formatterInPattern5 = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(zoneIDUTC);

    // 20071203141500
    private final DateTimeFormatter formatterInPattern6 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(zoneIDUTC);

    // 2007-12-03T17:15:00+0300
    private final DateTimeFormatter formatterInPattern7 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX");

    // 2007-12-03T17:15:00.0+0300
    private final DateTimeFormatter formatterInPattern8 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SXX");

    // 2007-12-03T17:15:00.00+0300
    private final DateTimeFormatter formatterInPattern9 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSXX");

    // 2007-12-03T17:15:00.000+0300
    private final DateTimeFormatter formatterInPattern10 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");

    //2007-12-03T14:15:00.000Z
    private final DateTimeFormatter formatterInPattern11 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    // 2007-12-03T17:15+0300
    private final DateTimeFormatter formatterInPattern12 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXX");

    // 20071203111500-03:00
    private final DateTimeFormatter formatterInPattern13 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssXXX");

    // 200712031115-03:00
    private final DateTimeFormatter formatterInPattern14 = DateTimeFormatter.ofPattern("yyyyMMddHHmmXXX");
    
    // 2007-12-03T14:15:00
    private final DateTimeFormatter formatterInPattern15 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(zoneIDUTC);
    
    
    /**
     * Returns the Instant in format yyyy-MM-dd'T'HH:mm:ssZ
     * @param instant
     * @return String representation
     */
    @Override
    public String toString(final Instant instant) {
        return formatterOut.format(instant);
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
    public Instant fromString(final String text) {
        try {
            if (text.contains("T")) {
                switch (text.length()) {
                    case 16:
                        // 2007-12-03T14:15
                        return Instant.from(formatterInPattern1.parse(text));
                    case 19:
                        // 2007-12-03T14:15:00
                        return Instant.from(formatterInPattern15.parse(text));
                    case 21:
                        // 2007-12-03T17:15+0300
                        return Instant.from(formatterInPattern12.parse(text));
                    case 22:
                        // 2007-12-03T17:15+03:00
                        return Instant.from(formatterInPattern2.parse(text));
                    case 24:
                        if (text.endsWith("Z")) {
                            // 2007-12-03T14:15:00.000Z
                            return Instant.from(formatterInPattern11.parse(text));
                        } else {
                            // 2007-12-03T17:15:00+0300
                            return Instant.from(formatterInPattern7.parse(text));
                        }
                    case 26:
                        // 2007-12-03T17:15:00.0+0300
                        return Instant.from(formatterInPattern8.parse(text));
                    case 27:
                        if (text.charAt(text.length() - 3) != ':') {
                            // 2007-12-03T17:15:00.00+0300
                            return Instant.from(formatterInPattern9.parse(text));
                        } else {
                            return Instant.from(formatterInISOLong.parse(text));
                        }
                    case 28:
                        if (text.charAt(text.length() - 3) != ':') {
                            // 2007-12-03T17:15:00.000+0300
                            return Instant.from(formatterInPattern10.parse(text));
                        } else {
                            return Instant.from(formatterInISOLong.parse(text));
                        }
                    default:
                        // Others can be picked up by ISO_OFFSET_DATE_TIME
                        return Instant.from(formatterInISOLong.parse(text));
                }
            } else if (text.endsWith("Z")) {
                switch (text.length()) {
                    case 13:
                        // 200712031415Z
                        return Instant.from(formatterInPattern3.parse(text));
                    default:
                        // 20071203141500Z
                        return Instant.from(formatterInPattern4.parse(text));
                }
            } else if (text.contains("-") || text.contains("+")) {
                switch (text.length()) {
                    case 17:
                        // 200712031115-0300
                        return Instant.from(formatterInPattern3.parse(text));
                    case 18:
                        // 200712031115-03:00
                        return Instant.from(formatterInPattern14.parse(text));
                    case 20:
                        // 20071203111500-03:00
                        return Instant.from(formatterInPattern13.parse(text));
                    default:
                        // 20071203111500-0300
                        return Instant.from(formatterInPattern4.parse(text));
                }
            } else {
                switch (text.length()) {
                    case 12:
                        // 200712031415
                        return Instant.from(formatterInPattern5.parse(text));
                    default:
                        // 20071203141500
                        return Instant.from(formatterInPattern6.parse(text));
                }
            }
        } catch (final DateTimeParseException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
