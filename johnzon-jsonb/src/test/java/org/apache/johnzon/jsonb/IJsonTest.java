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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;

import org.apache.johnzon.jsonb.model.Holder;
import org.junit.Test;

public class IJsonTest {
    @Test
    public void binary() throws Exception {
        final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withStrictIJSON(true));
        final String jsonString = jsonb.toJson(new Bytes());
        assertEquals("{\"data\":\"VGVzdCBTdHJpbmc=\"}", jsonString);
        jsonb.close();
    }

    @Test
    public void date() throws Exception {
        final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withStrictIJSON(true));
        final Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1970, 0, 1);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String json = jsonb.toJson(new DateHolder() {{ setInstance(cal.getTime()); }});
        assertEquals("{\"instance\":\"1970-01-01T00:00:00Z+00:00\"}", json);
        jsonb.close();
    }

    @Test
    public void calendar() throws Exception {
        final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withStrictIJSON(true));

        final Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1970, Calendar.JANUARY, 1);
        cal.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));

        final String json = jsonb.toJson(new CalendarHolder() {{ setInstance(cal); }});
        assertEquals("{\"instance\":\"1970-01-01T00:00:00Z+01:00\"}", json);
        jsonb.close();
    }

    @Test
    public void onlyObjectAndArrayCanBeRoot() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withStrictIJSON(true))) {
            try {
                jsonb.toJson("Test String");
                fail();
            } catch (final JsonbException e) {
                // ok
            }
        }
    }

    public class DateHolder implements Holder<Date> {
        private Date instance;

        @Override
        public Date getInstance() {
            return instance;
        }

        @Override
        public void setInstance(final Date instance) {
            this.instance = instance;
        }
    }

    public class CalendarHolder implements Holder<Calendar> {
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

    public static class Bytes {
        private byte[] data = "Test String".getBytes();

        public byte[] getData() {
            return data;
        }

        public void setData(final byte[] data) {
            this.data = data;
        }
    }
}
