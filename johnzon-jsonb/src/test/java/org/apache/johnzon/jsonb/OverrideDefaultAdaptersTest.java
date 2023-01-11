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
package org.apache.johnzon.jsonb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.adapter.JsonbAdapter;

import org.junit.Test;

public class OverrideDefaultAdaptersTest {
    @Test
    public void run() throws Exception {
        final ZonedDateTime zdtString = ZonedDateTime.ofInstant(new Date(0).toInstant(), ZoneId.of("UTC"));
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withAdapters(new ZonedDateTimeWithFallback()))) {
            final DateHolder holder = jsonb.fromJson("{\"date\":\"" + zdtString + "i\"}", DateHolder.class);
            assertEquals(new Date(0).getTime(), holder.date.getTime());
        }
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            jsonb.fromJson("{\"date\":\"" + zdtString + "i\"}", DateHolder.class);
            fail();
        } catch (final JsonbException je) {
            // expected
        }
    }

    public static class DateHolder {

        public Date date;
    }

    public static class ZonedDateTimeWithFallback implements JsonbAdapter<Date, String> {
        private static final ZoneId UTC = ZoneId.of("UTC");

        @Override
        public Date adaptFromJson(final String obj) {
            try {
                return Date.from(LocalDateTime.parse(obj).toInstant(ZoneOffset.UTC));
            } catch (final DateTimeParseException pe) {
                return new Date(ZonedDateTime.parse(obj.substring(0, obj.length() - 1)).toInstant().toEpochMilli());
            }
        }

        @Override
        public String adaptToJson(final Date obj) {
            return LocalDateTime.ofInstant(obj.toInstant(), UTC).toString();
        }
    }
}
