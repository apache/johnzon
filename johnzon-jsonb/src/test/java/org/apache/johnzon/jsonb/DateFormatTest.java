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
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbDateFormat;

import org.apache.johnzon.jsonb.model.Holder;
import org.apache.johnzon.jsonb.model.packageformat.FormatOnClassModel;
import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

public class DateFormatTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    @Test
    public void dateRoundTrip() {
        final Date date = new Date(1);
        final String str = jsonb.toJson(date);
        final Date date1 = jsonb.fromJson(str, Date.class);
        final String dateReser = jsonb.toJson(date1);
        final Date reDeser = jsonb.fromJson(dateReser, Date.class);
        assertEquals(date.getTime(), reDeser.getTime());
    }

    @Test
    public void calendarCanBeParsed() {
        Stream.of("{\"instance\":\"1970-01-01T00:00+01:00[Europe/Paris]\"}",
                "{\"instance\":\"1970-01-01\"}")
                .forEach(json -> {
                    final Calendar cal = jsonb.fromJson(json, CalendarHolder.class).getInstance();
                    assertNotNull(cal);
                    // todo: assert value, fixed bug was that it was not even parseable
                });
    }

    @Test
    public void dateCanBeParsed() {
        final Date date = new Date(70, 0, 1);

        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        final DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
        final String value = dtf.format(calendar.toZonedDateTime()).replace("]", "\\]")
                .replace("[", "\\[").replace("+", "\\+");

        Stream.of("{\"instance\":\"" + value + "\"}")
                .forEach(json -> {
                    final Date unmarshalled = jsonb.fromJson(json, DateHolder.class).getInstance();
                    assertNotNull(unmarshalled);
                    // todo: assert value, fixed bug was that it was not even parseable
                });
    }

    @Test
    public void dateFormatMethods() {
        final Date instance = new Date(0);
        {
            final String json = jsonb.toJson(new DateHolder() {{ setInstance(instance); }});
            final String expected = "{\"instance\":\"" +
                DateTimeFormatter.ofPattern("E DD MMM yyyy HH:mm:ss z")
                        .withLocale(new Locale.Builder().setLanguage("it").build())
                        .format(ZonedDateTime.ofInstant(instance.toInstant(), ZoneId.of("UTC"))) + "\"}";
            assertEquals(expected, json);
        }
        {
            final Calendar c = Calendar.getInstance(Locale.GERMAN);
            c.set(Calendar.YEAR, Integer.parseInt("19700001".substring(0, 4)));
            c.set(Calendar.MONTH, Integer.parseInt("19700001".substring(4, 6)));
            c.set(Calendar.DATE, Integer.parseInt("19700001".substring(6, 8)));
            final String json = String.format("{\"instance\":\"%s 1970 01:00:00 MEZ\"}",
                    new SimpleDateFormat("EEE' 'dd' 'MMM", Locale.GERMAN).format(c.getTime()));
            final DateHolder holder = jsonb.fromJson(json, DateHolder.class);
            assertEquals(instance, holder.getInstance());
        }
    }

    @Test
    public void packageConfigOverridenByClass() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withDateFormat("E DD MMM yyyy HH:mm:ss z", Locale.CANADA))) {

            final Date instance = new Date(0);
            final Locale locale = new Locale.Builder().setLanguage("de").build();

            {
                final String json = jsonb.toJson(new FormatOnClassModel() {{ setInstance(instance); }});
                final String expected = "{\"instance\":\""
                        + DateTimeFormatter.ofPattern("E DD MMM yyyy HH:mm:ss")
                                .withLocale(locale)
                                .format(ZonedDateTime.ofInstant(instance.toInstant(), ZoneId.of("UTC")))
                        +  "\"}";
                assertEquals(expected, json);
            }
        }
    }

    public static class CalendarHolder implements Holder<Calendar> {
        private Calendar instance;

        @Override
        public Calendar getInstance() {
            return instance;
        }

        @Override
        public void setInstance(final Calendar instance) {
            this.instance = instance;
        }
    }

    public static class DateHolder implements Holder<Date> {
        private Date instance;

        @Override
        @JsonbDateFormat(value = "E DD MMM yyyy HH:mm:ss z", locale = "it")
        public Date getInstance() {
            return instance;
        }

        @Override
        @JsonbDateFormat(value = "E DD MMM yyyy HH:mm:ss z", locale = "de")
        public void setInstance(final Date instance) {
            this.instance = instance;
        }
    }
}
