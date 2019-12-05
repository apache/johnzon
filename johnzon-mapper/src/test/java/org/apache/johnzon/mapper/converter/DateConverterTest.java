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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class DateConverterTest {

    private TimeZone tz;
    private Locale locale;

    @Before
    public void changeConfig() {
        tz = TimeZone.getDefault();
        locale = Locale.getDefault(Locale.Category.FORMAT);
    }

    @After
    public void resetConfig() {
        TimeZone.setDefault(tz);
        Locale.setDefault(locale);
    }

    @Test
    public void testToStringWithTZ() {
        TimeZone.setDefault(TimeZone.getTimeZone("+2"));
        Locale.setDefault(Locale.GERMAN);

        Calendar calender = Calendar.getInstance();
        calender.set(2019, Calendar.NOVEMBER, 6, 16, 15, 14);
        calender.clear(Calendar.MILLISECOND);
        calender.setTimeZone(TimeZone.getTimeZone("GMT+2"));

        assertEquals("2019-11-06T14:15:14+0000", new DateConverter("yyyy-MM-dd'T'HH:mm:ssZ").toString(calender.getTime()));
    }

    @Test
    public void fromStringWithTZ() {
        TimeZone.setDefault(TimeZone.getTimeZone("+2"));
        Locale.setDefault(Locale.GERMAN);

        Calendar calender = Calendar.getInstance();
        calender.set(2018, Calendar.OCTOBER, 5, 13, 14, 13);
        calender.clear(Calendar.MILLISECOND);
        calender.setTimeZone(TimeZone.getTimeZone("GMT+0"));

        assertEquals(calender.getTime(), new DateConverter("yyyy-MM-dd'T'HH:mm:ssZ").fromString("2018-10-05T15:14:13+0200"));
    }

    @Test
    public void testToStringWithoutTZ() {
        Calendar calender = Calendar.getInstance();
        calender.set(2019, Calendar.JANUARY, 6, 16, 15, 14);
        calender.clear(Calendar.MILLISECOND);

        assertEquals("2019-01-06T16:15:14", new DateConverter("yyyy-MM-dd'T'HH:mm:ss").toString(calender.getTime()));
    }

    @Test
    public void fromStringWithoutTZ() {
        Calendar calender = Calendar.getInstance();
        calender.set(2018, Calendar.FEBRUARY, 5, 15, 14, 13);
        calender.clear(Calendar.MILLISECOND);

        assertEquals(calender.getTime(), new DateConverter("yyyy-MM-dd'T'HH:mm:ss").fromString("2018-02-05T15:14:13"));
    }
}
