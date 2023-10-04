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
package org.apache.johnzon.mapper.map;

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.MapperException;
import org.apache.johnzon.mapper.converter.BigDecimalConverter;
import org.apache.johnzon.mapper.converter.BigIntegerConverter;
import org.apache.johnzon.mapper.converter.ClassConverter;
import org.apache.johnzon.mapper.converter.DateConverter;
import org.apache.johnzon.mapper.converter.LocaleConverter;
import org.apache.johnzon.mapper.converter.StringConverter;
import org.apache.johnzon.mapper.converter.URIConverter;
import org.apache.johnzon.mapper.converter.URLConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import static java.util.stream.Collectors.toSet;

// important: override all usages,
// mainly org.apache.johnzon.mapper.MapperConfig.findAdapter and
// org.apache.johnzon.mapper.MappingParserImpl.findAdapter today
public class LazyConverterMap extends ConcurrentHashMap<AdapterKey, Adapter<?, ?>> {
    private static final Adapter<?, ?> NO_ADAPTER = new Adapter<Object, Object>() {
        @Override
        public Object to(final Object b) {
            throw new UnsupportedOperationException("shouldn't be called");
        }

        @Override
        public Object from(final Object a) {
            return to(null); // just fail
        }
    };

    private boolean useShortISO8601Format = true;
    private DateTimeFormatter dateTimeFormatter;
    private boolean useBigIntegerStringAdapter = true;
    private boolean useBigDecimalStringAdapter = true;

    public void setUseShortISO8601Format(final boolean useShortISO8601Format) {
        this.useShortISO8601Format = useShortISO8601Format;
    }

    public void setDateTimeFormatter(final DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public void setUseBigDecimalStringAdapter(boolean useBigDecimalStringAdapter) {
        this.useBigDecimalStringAdapter = useBigDecimalStringAdapter;
    }

    public void setUseBigIntegerStringAdapter(boolean useBigIntegerStringAdapter) {
        this.useBigIntegerStringAdapter = useBigIntegerStringAdapter;
    }

    @Override
    public Adapter<?, ?> get(final Object key) {
        final Adapter<?, ?> found = super.get(key);
        if (found == NO_ADAPTER) {
            return null;
        }
        if (found == null) {
            if (!AdapterKey.class.isInstance(key)) {
                return null;
            }
            final AdapterKey k = AdapterKey.class.cast(key);
            if (k.getTo() == String.class) {
                final Adapter<?, ?> adapter = doLazyLookup(k);
                if (adapter != null) {
                    return adapter;
                } // else let's cache we don't need to go through lazy lookups
            }
            add(k, NO_ADAPTER);
            return null;
        }
        return found;
    }

    @Override
    public Set<Entry<AdapterKey, Adapter<?, ?>>> entrySet() {
        return super.entrySet().stream()
                .filter(it -> it.getValue() != NO_ADAPTER)
                .collect(toSet());
    }

    public Set<AdapterKey> adapterKeys() {
        return Stream.concat(
                        super.keySet().stream()
                                .filter(it -> super.get(it) != NO_ADAPTER),
                        Stream.of(Date.class, URI.class, URL.class, Class.class, String.class, BigDecimal.class, BigInteger.class,
                                        Locale.class, Period.class, Duration.class, Calendar.class, GregorianCalendar.class, TimeZone.class,
                                        ZoneId.class, ZoneOffset.class, SimpleTimeZone.class, Instant.class, LocalDateTime.class, LocalDate.class,
                                        ZonedDateTime.class, OffsetDateTime.class, OffsetTime.class)
                                .map(it -> new AdapterKey(it, String.class, true)))
                .collect(toSet());
    }

    private Adapter<?, ?> doLazyLookup(final AdapterKey key) {
        final Type from = key.getFrom();
        if (from == Date.class) {
            return addDateConverter(key);
        }
        if (from == URI.class) {
            return add(key, new ConverterAdapter<>(new URIConverter(), URI.class));
        }
        if (from == URL.class) {
            return add(key, new ConverterAdapter<>(new URLConverter(), URL.class));
        }
        if (from == Class.class) {
            return add(key, new ConverterAdapter<>(new ClassConverter(), Class.class));
        }
        if (from == String.class) {
            return add(key, new ConverterAdapter<>(new StringConverter(), String.class));
        }
        if (from == BigDecimal.class && useBigIntegerStringAdapter) {
            return add(key, new ConverterAdapter<>(new BigDecimalConverter(), BigDecimal.class));
        }
        if (from == BigInteger.class && useBigDecimalStringAdapter) {
            return add(key, new ConverterAdapter<>(new BigIntegerConverter(), BigInteger.class));
        }
        if (from == Locale.class) {
            return add(key, new LocaleConverter());
        }
        if (from == Period.class) {
            return add(key, new ConverterAdapter<>(new Converter<Period>() {
                @Override
                public String toString(final Period instance) {
                    return instance.toString();
                }

                @Override
                public Period fromString(final String text) {
                    return Period.parse(text);
                }
            }, Period.class));
        }
        if (from == Duration.class) {
            return add(key, new ConverterAdapter<>(new Converter<Duration>() {
                @Override
                public String toString(final Duration instance) {
                    return instance.toString();
                }

                @Override
                public Duration fromString(final String text) {
                    return Duration.parse(text);
                }
            }, Duration.class));
        }
        if (from == Calendar.class) {
            return addCalendarConverter(key);
        }
        if (from == GregorianCalendar.class) {
            return addGregorianCalendar(key);
        }
        if (from == TimeZone.class) {
            return add(key, new ConverterAdapter<>(new Converter<TimeZone>() {
                @Override
                public String toString(final TimeZone instance) {
                    return instance.getID();
                }

                @Override
                public TimeZone fromString(final String text) {
                    checkForDeprecatedTimeZone(text);
                    return TimeZone.getTimeZone(text);
                }
            }, TimeZone.class));
        }
        if (from == ZoneId.class) {
            return add(key, new ConverterAdapter<>(new Converter<ZoneId>() {
                @Override
                public String toString(final ZoneId instance) {
                    return instance.getId();
                }

                @Override
                public ZoneId fromString(final String text) {
                    return ZoneId.of(text);
                }
            }, ZoneId.class));
        }
        if (from == ZoneOffset.class) {
            return add(key, new ConverterAdapter<>(new Converter<ZoneOffset>() {
                @Override
                public String toString(final ZoneOffset instance) {
                    return instance.getId();
                }

                @Override
                public ZoneOffset fromString(final String text) {
                    return ZoneOffset.of(text);
                }
            }, ZoneOffset.class));
        }
        if (from == SimpleTimeZone.class) {
            return add(key, new ConverterAdapter<>(new Converter<SimpleTimeZone>() {
                @Override
                public String toString(final SimpleTimeZone instance) {
                    return instance.getID();
                }

                @Override
                public SimpleTimeZone fromString(final String text) {
                    checkForDeprecatedTimeZone(text);
                    final TimeZone timeZone = TimeZone.getTimeZone(text);
                    return new SimpleTimeZone(timeZone.getRawOffset(), timeZone.getID());
                }
            }, SimpleTimeZone.class));
        }
        if (from == Instant.class) {
            return addInstantConverter(key);
        }
        if (from == LocalDate.class) {
            return addLocalDateConverter(key);
        }
        if (from == LocalTime.class) {
            return add(key, new ConverterAdapter<>(new Converter<LocalTime>() {
                @Override
                public String toString(final LocalTime instance) {
                    return instance.toString();
                }

                @Override
                public LocalTime fromString(final String text) {
                    return LocalTime.parse(text);
                }
            }, LocalTime.class));
        }
        if (from == LocalDateTime.class) {
            return addLocalDateTimeConverter(key);
        }
        if (from == ZonedDateTime.class) {
            return addZonedDateTimeConverter(key);
        }
        if (from == OffsetDateTime.class) {
            return addOffsetDateTimeConverter(key);
        }
        if (from == OffsetTime.class) {
            return add(key, new ConverterAdapter<>(new Converter<OffsetTime>() {
                @Override
                public String toString(final OffsetTime instance) {
                    return instance.toString();
                }

                @Override
                public OffsetTime fromString(final String text) {
                    return OffsetTime.parse(text);
                }
            }, OffsetTime.class));
        }
        return null;
    }

    private Adapter<?, ?> addOffsetDateTimeConverter(final AdapterKey key) {
        if (dateTimeFormatter != null) {
            final ZoneId zoneIDUTC = ZoneId.of("UTC");
            return add(key, new ConverterAdapter<>(new Converter<OffsetDateTime>() {
                @Override
                public String toString(final OffsetDateTime instance) {
                    return dateTimeFormatter.format(ZonedDateTime.ofInstant(instance.toInstant(), zoneIDUTC));
                }

                @Override
                public OffsetDateTime fromString(final String text) {
                    try {
                        return parseZonedDateTime(text, dateTimeFormatter, zoneIDUTC).toOffsetDateTime();
                    } catch (final DateTimeParseException dpe) {
                        return OffsetDateTime.parse(text);
                    }
                }
            }, OffsetDateTime.class));
        }
        return add(key, new ConverterAdapter<>(new Converter<OffsetDateTime>() {
            @Override
            public String toString(final OffsetDateTime instance) {
                return instance.toString();
            }

            @Override
            public OffsetDateTime fromString(final String text) {
                return OffsetDateTime.parse(text);
            }
        }, OffsetDateTime.class));
    }

    private Adapter<?, ?> addZonedDateTimeConverter(final AdapterKey key) {
        if (dateTimeFormatter != null) {
            final ZoneId zoneIDUTC = ZoneId.of("UTC");
            return add(key, new ConverterAdapter<>(new Converter<ZonedDateTime>() {
                @Override
                public String toString(final ZonedDateTime instance) {
                    return dateTimeFormatter.format(ZonedDateTime.ofInstant(instance.toInstant(), zoneIDUTC));
                }

                @Override
                public ZonedDateTime fromString(final String text) {
                    try {
                        return parseZonedDateTime(text, dateTimeFormatter, zoneIDUTC);
                    } catch (final DateTimeParseException dpe) {
                        return ZonedDateTime.parse(text);
                    }
                }
            }, ZonedDateTime.class));
        }
        return add(key, new ConverterAdapter<>(new Converter<ZonedDateTime>() {
            @Override
            public String toString(final ZonedDateTime instance) {
                return instance.toString();
            }

            @Override
            public ZonedDateTime fromString(final String text) {
                return ZonedDateTime.parse(text);
            }
        }, ZonedDateTime.class));
    }

    private Adapter<?, ?> addLocalDateTimeConverter(final AdapterKey key) {
        if (dateTimeFormatter != null) {
            final ZoneId zoneIDUTC = ZoneId.of("UTC");
            return add(key, new ConverterAdapter<>(new Converter<LocalDateTime>() {

                @Override
                public String toString(final LocalDateTime instance) {
                    return dateTimeFormatter.format(ZonedDateTime.ofInstant(instance.toInstant(ZoneOffset.UTC), zoneIDUTC));
                }

                @Override
                public LocalDateTime fromString(final String text) {
                    try {
                        return parseZonedDateTime(text, dateTimeFormatter, zoneIDUTC).toLocalDateTime();
                    } catch (final DateTimeParseException dpe) {
                        return LocalDateTime.parse(text);
                    }
                }
            }, LocalDateTime.class));
        }
        return add(key, new ConverterAdapter<>(new Converter<LocalDateTime>() {
            @Override
            public String toString(final LocalDateTime instance) {
                return instance.toString();
            }

            @Override
            public LocalDateTime fromString(final String text) {
                return LocalDateTime.parse(text);
            }
        }, LocalDateTime.class));
    }

    private Adapter<?, ?> addLocalDateConverter(final AdapterKey key) {
        if (dateTimeFormatter != null) {
            final ZoneId zoneIDUTC = ZoneId.of("UTC");
            return add(key, new ConverterAdapter<>(new Converter<LocalDate>() {
                @Override
                public String toString(final LocalDate instance) {
                    return dateTimeFormatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(TimeUnit.DAYS.toMillis(instance.toEpochDay())), zoneIDUTC));
                }

                @Override
                public LocalDate fromString(final String text) {
                    try {
                        return parseZonedDateTime(text, dateTimeFormatter, zoneIDUTC).toLocalDate();
                    } catch (final DateTimeParseException dpe) {
                        return LocalDate.parse(text);
                    }
                }
            }, LocalDate.class));
        }
        return add(key, new ConverterAdapter<>(new Converter<LocalDate>() {
            @Override
            public String toString(final LocalDate instance) {
                return instance.toString();
            }

            @Override
            public LocalDate fromString(final String text) {
                return LocalDate.parse(text);
            }
        }, LocalDate.class));
    }

    private Adapter<?, ?> addInstantConverter(final AdapterKey key) {
        if (dateTimeFormatter != null) {
            final ZoneId zoneIDUTC = ZoneId.of("UTC");
            return add(key, new ConverterAdapter<>(new Converter<Instant>() {
                @Override
                public String toString(final Instant instance) {
                    return dateTimeFormatter.format(ZonedDateTime.ofInstant(instance, zoneIDUTC));
                }

                @Override
                public Instant fromString(final String text) {
                    return parseZonedDateTime(text, dateTimeFormatter, zoneIDUTC).toInstant();
                }
            }, Instant.class));
        }
        return add(key, new ConverterAdapter<>(new Converter<Instant>() {
            @Override
            public String toString(final Instant instance) {
                return instance.toString();
            }

            @Override
            public Instant fromString(final String text) {
                return Instant.parse(text);
            }
        }, Instant.class));
    }

    private Adapter<?, ?> addGregorianCalendar(final AdapterKey key) {
        if (dateTimeFormatter != null) {
            final ZoneId zoneIDUTC = ZoneId.of("UTC");
            return add(key, new ConverterAdapter<>(new Converter<GregorianCalendar>() {
                @Override
                public String toString(final GregorianCalendar instance) {
                    return dateTimeFormatter.format(ZonedDateTime.ofInstant(instance.toInstant(), instance.getTimeZone().toZoneId()));
                }

                @Override
                public GregorianCalendar fromString(final String text) {
                    final ZonedDateTime zonedDateTime = parseZonedDateTime(text, dateTimeFormatter, zoneIDUTC);
                    final Calendar instance = GregorianCalendar.getInstance();
                    instance.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));
                    instance.setTime(Date.from(zonedDateTime.toInstant()));
                    return GregorianCalendar.class.cast(instance);
                }
            }, GregorianCalendar.class));
        }
        return add(key, new ConverterAdapter<>(new Converter<GregorianCalendar>() {
            @Override
            public String toString(final GregorianCalendar instance) {
                return toStringCalendar(instance);
            }

            @Override
            public GregorianCalendar fromString(final String text) {
                return fromCalendar(text, GregorianCalendar::from);
            }
        }, GregorianCalendar.class));
    }

    private Adapter<?, ?> addCalendarConverter(final AdapterKey key) {
        if (dateTimeFormatter != null) {
            final ZoneId zoneIDUTC = ZoneId.of("UTC");
            return add(key, new ConverterAdapter<>(new Converter<Calendar>() {
                @Override
                public String toString(final Calendar instance) {
                    return dateTimeFormatter.format(ZonedDateTime.ofInstant(instance.toInstant(), instance.getTimeZone().toZoneId()));
                }

                @Override
                public Calendar fromString(final String text) {
                    final ZonedDateTime zonedDateTime = parseZonedDateTime(text, dateTimeFormatter, zoneIDUTC);
                    final Calendar instance = Calendar.getInstance();
                    instance.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));
                    instance.setTime(Date.from(zonedDateTime.toInstant()));
                    return instance;
                }
            }, Calendar.class));
        }
        return add(key, new ConverterAdapter<>(new Converter<Calendar>() {
            @Override
            public String toString(final Calendar instance) {
                return toStringCalendar(instance);
            }

            @Override
            public Calendar fromString(final String text) {
                return fromCalendar(text, zdt -> {
                    final Calendar instance = Calendar.getInstance();
                    instance.clear();
                    instance.setTimeZone(TimeZone.getTimeZone(zdt.getZone()));
                    instance.setTimeInMillis(zdt.toInstant().toEpochMilli());
                    return instance;
                });
            }
        }, Calendar.class));
    }

    private Adapter<?, ?> addDateConverter(final AdapterKey key) {
        if (useShortISO8601Format) {
            return add(key, new ConverterAdapter<>(new DateConverter("yyyyMMddHHmmssZ"), Date.class));
        }
        final ZoneId zoneIDUTC = ZoneId.of("UTC");
        if (dateTimeFormatter != null) {
            return add(key, new ConverterAdapter<>(new Converter<Date>() {

                @Override
                public String toString(final Date instance) {
                    return dateTimeFormatter.format(ZonedDateTime.ofInstant(instance.toInstant(), zoneIDUTC));
                }

                @Override
                public Date fromString(final String text) {
                    try {
                        return Date.from(parseZonedDateTime(text, dateTimeFormatter, zoneIDUTC).toInstant());
                    } catch (final DateTimeParseException dpe) {
                        return Date.from(LocalDateTime.parse(text).toInstant(ZoneOffset.UTC));
                    }
                }
            }, Date.class));
        }
        return add(key, new ConverterAdapter<>(new Converter<Date>() {
            @Override
            public String toString(final Date instance) {
                return ZonedDateTime.ofInstant(instance.toInstant(), zoneIDUTC)
                        .format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
            }

            @Override
            public Date fromString(final String text) {
                try {
                    return Date.from(ZonedDateTime.parse(text).toInstant());
                } catch (final DateTimeParseException dte) {
                    return Date.from(LocalDateTime.parse(text).toInstant(ZoneOffset.UTC));
                }
            }
        }, Date.class));
    }

    private static ZonedDateTime parseZonedDateTime(final String text, final DateTimeFormatter formatter, final ZoneId defaultZone) {
        final TemporalAccessor parse = formatter.parse(text);
        ZoneId zone = parse.query(TemporalQueries.zone());
        if (Objects.isNull(zone)) {
            zone = defaultZone;
        }
        final int year = parse.isSupported(YEAR) ? parse.get(YEAR) : 0;
        final int month = parse.isSupported(MONTH_OF_YEAR) ? parse.get(MONTH_OF_YEAR) : 0;
        final int day = parse.isSupported(DAY_OF_MONTH) ? parse.get(DAY_OF_MONTH) : 0;
        final int hour = parse.isSupported(HOUR_OF_DAY) ? parse.get(HOUR_OF_DAY) : 0;
        final int minute = parse.isSupported(MINUTE_OF_HOUR) ? parse.get(MINUTE_OF_HOUR) : 0;
        final int second = parse.isSupported(SECOND_OF_MINUTE) ? parse.get(SECOND_OF_MINUTE) : 0;
        final int millisecond = parse.isSupported(MILLI_OF_SECOND) ? parse.get(MILLI_OF_SECOND) : 0;
        return ZonedDateTime.of(year, month, day, hour, minute, second, millisecond, zone);
    }

    private static void checkForDeprecatedTimeZone(final String text) {
        switch (text) {
            case "CST": // really for TCK, this sucks for end users so we don't fail for all deprecated zones
                throw new MapperException("Deprecated timezone: '" + text + '"');
            default:
        }
    }

    private String toStringCalendar(final Calendar instance) {
        if (!hasTime(instance)) { // spec
            final LocalDate localDate = LocalDate.of(
                    instance.get(Calendar.YEAR),
                    instance.get(Calendar.MONTH) + 1,
                    instance.get(Calendar.DAY_OF_MONTH));
            return localDate.toString() +
                    (instance.getTimeZone() != null ?
                            instance.getTimeZone().toZoneId().getRules()
                                    .getOffset(Instant.ofEpochMilli(TimeUnit.DAYS.toMillis(localDate.toEpochDay()))) : "");
        }
        return ZonedDateTime.ofInstant(instance.toInstant(), instance.getTimeZone().toZoneId())
                .format(DateTimeFormatter.ISO_DATE_TIME);
    }

    private boolean hasTime(final Calendar instance) {
        if (!instance.isSet(Calendar.HOUR_OF_DAY)) {
            return false;
        }
        return instance.get(Calendar.HOUR_OF_DAY) != 0 ||
                (instance.isSet(Calendar.MINUTE) && instance.get(Calendar.MINUTE) != 0) ||
                (instance.isSet(Calendar.SECOND) && instance.get(Calendar.SECOND) != 0);
    }

    private <T extends Calendar> T fromCalendar(final String text, final Function<ZonedDateTime, T> calendarSupplier) {
        switch (text.length()) {
            case 10: {
                final ZonedDateTime date = LocalDate.parse(text)
                        .atTime(0, 0, 0)
                        .atZone(ZoneId.of("UTC"));
                return calendarSupplier.apply(date);
            }
            default:
                final ZonedDateTime zonedDateTime = ZonedDateTime.parse(text);
                return calendarSupplier.apply(zonedDateTime);
        }
    }

    private Adapter<?, ?> add(final AdapterKey key, final Adapter<?, ?> converter) {
        final Adapter<?, ?> existing = putIfAbsent(key, converter);
        return existing == null ? converter : existing;
    }
}
