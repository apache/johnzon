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

import org.junit.Test;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbConfig;
import javax.json.bind.spi.JsonbProvider;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.cxf.common.util.StringUtils;

import static org.junit.Assert.assertEquals;

public class JsonbTypesTest {
    @Test
    public void readAndWrite() {
        final LocalDate localDate = LocalDate.of(2015, 1, 1);
        final LocalDateTime localDateTime = LocalDateTime.of(2015, 1, 1, 1, 1);
        final String dateTime = localDateTime.toString();
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
        final String expected = "{" +
            "\"calendar\":\"" + zonedDateTime.toString() + "\"," +
            "\"date\":\"" + localDateTime.toString() + "\"," +
            "\"duration\":\"PT30S\"," +
            "\"gregorianCalendar\":\"" + zonedDateTime.toString() + "\"," +
            "\"instant\":\"" + Instant.ofEpochMilli(TimeUnit.DAYS.toMillis(localDate.toEpochDay())).toString() + "\"," +
            "\"localDate\":\"" + localDate.toString() + "\"," +
            "\"localDateTime\":\"" + dateTime + "\"," +
            "\"offsetDateTime\":\"" + OffsetDateTime.of(localDateTime, ZoneOffset.UTC).toString() + "\"," +
            "\"offsetTime\":\"" + OffsetTime.of(localDateTime.toLocalTime(), ZoneOffset.UTC).toString() + "\"," +
            "\"optionalDouble\":3.4," +
            "\"optionalInt\":1," +
            "\"optionalLong\":2," +
            "\"optionalString\":\"yes\"," +
            "\"period\":\"P1M10D\"," +
            "\"simpleTimeZone\":\"UTC\"," +
            "\"timeZone\":\"UTC\"," +
            "\"uri\":\"http://localhost:2222\"," +
            "\"url\":\"http://localhost:1111\"," +
            "\"zoneId\":\"UTC\"," +
            "\"zoneOffset\":\"Z\"" +
            "}";

        final Jsonb jsonb = newJsonb();

        final Types types = jsonb.fromJson(new StringReader(expected), Types.class);
        assertEquals("http://localhost:1111", types.url.toExternalForm());
        assertEquals("http://localhost:2222", types.uri.toASCIIString());
        assertEquals(Optional.of("yes"), types.optionalString);
        assertEquals(1, types.optionalInt.getAsInt());
        assertEquals(2, types.optionalLong.getAsLong());
        assertEquals(3.4, types.optionalDouble.getAsDouble(), 0.);
        assertEquals(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli(), types.date.getTime());
        assertEquals(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli(), types.calendar.getTime().getTime());
        assertEquals(localDateTime, types.localDateTime);
        assertEquals(localDate, types.localDate);
        assertEquals(OffsetDateTime.of(localDateTime, ZoneOffset.UTC), types.offsetDateTime);
        assertEquals(OffsetTime.of(localDateTime.toLocalTime(), ZoneOffset.UTC), types.offsetTime);
        assertEquals(TimeZone.getTimeZone("UTC"), types.timeZone);
        assertEquals(ZoneId.of("UTC"), types.zoneId);
        assertEquals(ZoneOffset.UTC, types.zoneOffset);
        assertEquals("UTC", types.simpleTimeZone.getID());
        assertEquals(0, types.simpleTimeZone.getRawOffset());
        assertEquals(TimeUnit.DAYS.toMillis(localDate.toEpochDay()), types.instant.toEpochMilli());
        assertEquals(Duration.of(30, ChronoUnit.SECONDS), types.duration);
        assertEquals(Period.of(0, 1, 10), types.period);

        assertEquals(expected, jsonb.toJson(types));
    }

    @Test
    public void readAndWriteWithDateFormats() {
        readAndWriteWithDateFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"), "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        readAndWriteWithDateFormat(DateTimeFormatter.ofPattern("yyyyMMdd+HHmmssZ"), "yyyyMMdd+HHmmssZ");
        readAndWriteWithDateFormat(DateTimeFormatter.ofPattern("yyyy-MM-dd"), "yyyy-MM-dd");
    }
    
    private void readAndWriteWithDateFormat(DateTimeFormatter dateTimeFormatter, String dateFormat) {
        final LocalDate localDate = LocalDate.of(2015, 1, 1);
        final LocalDateTime localDateTime = LocalDateTime.of(2015, 1, 1, 1, 1);
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
        final String expected = "{" +
            "\"calendar\":\"" + dateTimeFormatter.format(zonedDateTime) + "\"," +
            "\"date\":\"" + dateTimeFormatter.format(ZonedDateTime.ofInstant(localDateTime.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))) + "\"," +
            "\"gregorianCalendar\":\"" + dateTimeFormatter.format(zonedDateTime) + "\"," +
            "\"instant\":\"" + dateTimeFormatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(TimeUnit.DAYS.toMillis(localDate.toEpochDay())), ZoneId.of("UTC"))) + "\"," +
            "\"localDate\":\"" + dateTimeFormatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(TimeUnit.DAYS.toMillis(localDate.toEpochDay())), ZoneId.of("UTC"))) + "\"," +
            "\"localDateTime\":\"" + dateTimeFormatter.format(ZonedDateTime.ofInstant(localDateTime.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))) + "\"," +
            "\"offsetDateTime\":\"" + dateTimeFormatter.format(ZonedDateTime.ofInstant(OffsetDateTime.of(localDateTime, ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"))) + "\"" +
            "}";

        final Jsonb jsonb = newJsonb(dateFormat);
        
        final DateTypes types = jsonb.fromJson(new StringReader(expected), DateTypes.class);
        assertEquals(localDate, types.localDate);
        assertEquals(expected, jsonb.toJson(types));
    }
    
    private static Jsonb newJsonb() {
        return newJsonb(null);
    }
    
    private static Jsonb newJsonb(String dateFormat) {
        JsonbConfig jsonbConfig = new JsonbConfig();
        if (!StringUtils.isEmpty(dateFormat)){
            jsonbConfig.withDateFormat(dateFormat, Locale.getDefault());
        }
        return JsonbProvider.provider().create().withConfig(jsonbConfig.setProperty("johnzon.attributeOrder", new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        })).build();
    }

    public static class Types {
        private URL url;
        private URI uri;
        private Optional<String> optionalString;
        private OptionalInt optionalInt;
        private OptionalLong optionalLong;
        private OptionalDouble optionalDouble;
        private Date date;
        private Calendar calendar;
        private GregorianCalendar gregorianCalendar;
        private TimeZone timeZone;
        private ZoneId zoneId;
        private ZoneOffset zoneOffset;
        private SimpleTimeZone simpleTimeZone;
        private Instant instant;
        private Duration duration;
        private Period period;
        private LocalDateTime localDateTime;
        private LocalDate localDate;
        private OffsetDateTime offsetDateTime;
        private OffsetTime offsetTime;

        public URL getUrl() {
            return url;
        }

        public void setUrl(URL url) {
            this.url = url;
        }

        public URI getUri() {
            return uri;
        }

        public void setUri(URI uri) {
            this.uri = uri;
        }

        public Optional<String> getOptionalString() {
            return optionalString;
        }

        public void setOptionalString(Optional<String> optionalString) {
            this.optionalString = optionalString;
        }

        public OptionalInt getOptionalInt() {
            return optionalInt;
        }

        public void setOptionalInt(OptionalInt optionalInt) {
            this.optionalInt = optionalInt;
        }

        public OptionalLong getOptionalLong() {
            return optionalLong;
        }

        public void setOptionalLong(OptionalLong optionalLong) {
            this.optionalLong = optionalLong;
        }

        public OptionalDouble getOptionalDouble() {
            return optionalDouble;
        }

        public void setOptionalDouble(OptionalDouble optionalDouble) {
            this.optionalDouble = optionalDouble;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Calendar getCalendar() {
            return calendar;
        }

        public void setCalendar(Calendar calendar) {
            this.calendar = calendar;
        }

        public GregorianCalendar getGregorianCalendar() {
            return gregorianCalendar;
        }

        public void setGregorianCalendar(GregorianCalendar gregorianCalendar) {
            this.gregorianCalendar = gregorianCalendar;
        }

        public TimeZone getTimeZone() {
            return timeZone;
        }

        public void setTimeZone(TimeZone timeZone) {
            this.timeZone = timeZone;
        }

        public ZoneId getZoneId() {
            return zoneId;
        }

        public void setZoneId(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        public ZoneOffset getZoneOffset() {
            return zoneOffset;
        }

        public void setZoneOffset(ZoneOffset zoneOffset) {
            this.zoneOffset = zoneOffset;
        }

        public SimpleTimeZone getSimpleTimeZone() {
            return simpleTimeZone;
        }

        public void setSimpleTimeZone(SimpleTimeZone simpleTimeZone) {
            this.simpleTimeZone = simpleTimeZone;
        }

        public Instant getInstant() {
            return instant;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }

        public Period getPeriod() {
            return period;
        }

        public void setPeriod(Period period) {
            this.period = period;
        }

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        public void setLocalDateTime(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
        }

        public LocalDate getLocalDate() {
            return localDate;
        }

        public void setLocalDate(LocalDate localDate) {
            this.localDate = localDate;
        }

        public OffsetDateTime getOffsetDateTime() {
            return offsetDateTime;
        }

        public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
            this.offsetDateTime = offsetDateTime;
        }

        public OffsetTime getOffsetTime() {
            return offsetTime;
        }

        public void setOffsetTime(OffsetTime offsetTime) {
            this.offsetTime = offsetTime;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Types types = Types.class.cast(o);
            return Objects.equals(url, types.url) &&
                Objects.equals(uri, types.uri) &&
                Objects.equals(optionalString, types.optionalString) &&
                Objects.equals(optionalInt, types.optionalInt) &&
                Objects.equals(optionalLong, types.optionalLong) &&
                Objects.equals(optionalDouble, types.optionalDouble) &&
                Objects.equals(date, types.date) &&
                Objects.equals(calendar, types.calendar) &&
                Objects.equals(gregorianCalendar, types.gregorianCalendar) &&
                Objects.equals(timeZone, types.timeZone) &&
                Objects.equals(zoneId, types.zoneId) &&
                Objects.equals(zoneOffset, types.zoneOffset) &&
                Objects.equals(simpleTimeZone, types.simpleTimeZone) &&
                Objects.equals(instant, types.instant) &&
                Objects.equals(duration, types.duration) &&
                Objects.equals(period, types.period) &&
                Objects.equals(localDateTime, types.localDateTime) &&
                Objects.equals(localDate, types.localDate) &&
                Objects.equals(offsetDateTime, types.offsetDateTime) &&
                Objects.equals(offsetTime, types.offsetTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                url, uri, optionalString, optionalInt, optionalLong, optionalDouble, date,
                calendar, gregorianCalendar, timeZone, zoneId, zoneOffset, simpleTimeZone, instant, duration,
                period, localDateTime, localDate, offsetDateTime, offsetTime);
        }
    }

    public static class DateTypes {
        private Date date;
        private Calendar calendar;
        private GregorianCalendar gregorianCalendar;
        private Instant instant;
        private LocalDateTime localDateTime;
        private LocalDate localDate;
        private OffsetDateTime offsetDateTime;


        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Calendar getCalendar() {
            return calendar;
        }

        public void setCalendar(Calendar calendar) {
            this.calendar = calendar;
        }

        public GregorianCalendar getGregorianCalendar() {
            return gregorianCalendar;
        }

        public void setGregorianCalendar(GregorianCalendar gregorianCalendar) {
            this.gregorianCalendar = gregorianCalendar;
        }

        public Instant getInstant() {
            return instant;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        public void setLocalDateTime(LocalDateTime localDateTime) {
            this.localDateTime = localDateTime;
        }

        public LocalDate getLocalDate() {
            return localDate;
        }

        public void setLocalDate(LocalDate localDate) {
            this.localDate = localDate;
        }

        public OffsetDateTime getOffsetDateTime() {
            return offsetDateTime;
        }

        public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
            this.offsetDateTime = offsetDateTime;
        }


        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Types types = Types.class.cast(o);
            return 
                Objects.equals(date, types.date) &&
                Objects.equals(calendar, types.calendar) &&
                Objects.equals(gregorianCalendar, types.gregorianCalendar) &&
                Objects.equals(instant, types.instant) &&
                Objects.equals(localDateTime, types.localDateTime) &&
                Objects.equals(localDate, types.localDate) &&
                Objects.equals(offsetDateTime, types.offsetDateTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                date, calendar, gregorianCalendar, instant, localDateTime, localDate, offsetDateTime);
        }
    }
}
