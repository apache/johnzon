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

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.annotation.JsonbDateFormat;

import org.apache.johnzon.jsonb.model.Holder;
import org.apache.johnzon.jsonb.model.packageformat.FormatOnClassModel;
import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

public class DateFormatTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

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

    public static class DateHolder implements Holder<Date> {
        private Date instance;

        @Override
        @JsonbDateFormat(value = "E DD MMM yyyy HH:mm:ss z", locale = "it")
        public Date getInstance() {
            return instance;
        }

        @Override
        @JsonbDateFormat(value = "E DD MMM yyyy HH:mm:ss z", locale = "de")
        public void setInstance(Date instance) {
            this.instance = instance;
        }
    }
}
