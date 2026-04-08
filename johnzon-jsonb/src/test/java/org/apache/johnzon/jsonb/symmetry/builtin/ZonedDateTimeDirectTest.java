/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.jsonb.symmetry.builtin;

import jakarta.json.bind.Jsonb;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;

/**
 * JSON-B 3.0 Section 3.5 — ZonedDateTime uses ISO_ZONED_DATE_TIME format,
 * which includes the zone ID in brackets: {@code 2024-01-15T10:30:45+01:00[Europe/Paris]}.
 *
 * <p>Uses Europe/Paris (CET, UTC+1 in January) like the TCK.</p>
 *
 * @see ee.jakarta.tck.json.bind.defaultmapping.dates.DatesMappingTest#testZonedDateTimeMapping()
 */
public class ZonedDateTimeDirectTest extends BuiltInSymmetryTest {

    private static final ZonedDateTime VALUE =
            ZonedDateTime.of(2024, 1, 15, 10, 30, 45, 0, ZoneId.of("Europe/Paris"));

    private static final String JSON = "\"2024-01-15T10:30:45+01:00[Europe/Paris]\"";

    @Override
    public void assertWrite(final Jsonb jsonb) {
        assertEquals(JSON, jsonb.toJson(VALUE));
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        assertEquals(VALUE, jsonb.fromJson(JSON, ZonedDateTime.class));
    }
}
