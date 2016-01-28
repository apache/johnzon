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

import org.apache.johnzon.core.AbstractJsonFactory;
import org.apache.johnzon.core.JsonGeneratorFactoryImpl;
import org.apache.johnzon.jsonb.converter.JsonbConverterFromString;
import org.apache.johnzon.jsonb.converter.JsonbConverterToString;
import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.MapperBuilder;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;
import javax.json.bind.annotation.JsonbVisibility;
import javax.json.bind.config.BinaryDataStrategy;
import javax.json.bind.config.PropertyNamingStrategy;
import javax.json.bind.config.PropertyVisibilityStrategy;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
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
import java.util.Base64;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static javax.json.bind.config.PropertyNamingStrategy.IDENTITY;
import static javax.json.bind.config.PropertyOrderStrategy.LEXICOGRAPHICAL;

public class JohnzonBuilder implements JsonbBuilder {
    private final MapperBuilder builder = new MapperBuilder();
    private JsonProvider jsonp;
    private JsonbConfig config;

    @Override
    public JsonbBuilder withConfig(final JsonbConfig config) {
        this.config = config;
        return this;
    }

    @Override
    public JsonbBuilder withProvider(final JsonProvider jsonpProvider) {
        this.jsonp = jsonpProvider;
        return this;
    }

    @Override
    public Jsonb build() {
        if (jsonp != null) {
            builder.setGeneratorFactory(jsonp.createGeneratorFactory(generatorConfig()));
            builder.setReaderFactory(jsonp.createReaderFactory(emptyMap()));
        }

        if (config == null) {
            config = new JsonbConfig();
        }

        if (config.getProperty(JsonbConfig.FORMATTING).map(Boolean.class::cast).orElse(false)) {
            builder.setPretty(true);
        }

        config.getProperty(JsonbConfig.ENCODING).ifPresent(encoding -> builder.setEncoding(String.valueOf(encoding)));
        config.getProperty(JsonbConfig.NULL_VALUES).ifPresent(serNulls -> builder.setSkipNull(!Boolean.class.cast(serNulls)));

        final Optional<Object> namingStrategyValue = config.getProperty(JsonbConfig.PROPERTY_NAMING_STRATEGY);

        final PropertyNamingStrategy propertyNamingStrategy = new PropertyNamingStrategyFactory(namingStrategyValue.orElse(IDENTITY)).create();
        final String orderValue = config.getProperty(JsonbConfig.PROPERTY_ORDER_STRATEGY).map(String::valueOf).orElse(LEXICOGRAPHICAL);
        final PropertyVisibilityStrategy visibilityStrategy = config.getProperty(JsonbConfig.PROPERTY_VISIBILITY_STRATEGY)
            .map(PropertyVisibilityStrategy.class::cast).orElse(new PropertyVisibilityStrategy() {
                private final ConcurrentMap<Class<?>, PropertyVisibilityStrategy> strategies = new ConcurrentHashMap<>();

                @Override
                public boolean isVisible(final Field field) {
                    final PropertyVisibilityStrategy strategy = strategies.computeIfAbsent(field.getDeclaringClass(), this::visibilityStrategy);
                    return strategy == this ? Modifier.isPublic(field.getModifiers()) : strategy.isVisible(field);
                }

                @Override
                public boolean isVisible(final Method method) {
                    final PropertyVisibilityStrategy strategy = strategies.computeIfAbsent(method.getDeclaringClass(), this::visibilityStrategy);
                    return strategy == this ? Modifier.isPublic(method.getModifiers()) : strategy.isVisible(method);
                }

                private PropertyVisibilityStrategy visibilityStrategy(final Class<?> type) { // can be cached
                    Package p = type.getPackage();
                    while (p != null) {
                        final JsonbVisibility visibility = p.getAnnotation(JsonbVisibility.class);
                        if (visibility != null) {
                            try {
                                return visibility.value().newInstance();
                            } catch (final InstantiationException | IllegalAccessException e) {
                                throw new IllegalArgumentException(e);
                            }
                        }
                        final String name = p.getName();
                        final int end = name.lastIndexOf('.');
                        if (end < 0) {
                            break;
                        }
                        p = Package.getPackage(name.substring(0, end));
                    }
                    return this;
                }
            });

        config.getProperty("johnzon.attributeOrder").ifPresent(comp -> builder.setAttributeOrder(Comparator.class.cast(comp)));

        final Map<Class<?>, Converter<?>> defaultConverters = createJava8Converters();
        defaultConverters.forEach(builder::addConverter);

        builder.setAccessMode(
            new JsonbAccessMode(
                propertyNamingStrategy, orderValue, visibilityStrategy,
                !namingStrategyValue.orElse("").equals(PropertyNamingStrategy.CASE_INSENSITIVE),
                defaultConverters));


        // user adapters
        config.getProperty(JsonbConfig.ADAPTERS).ifPresent(adapters -> Stream.of(JsonbAdapter[].class.cast(adapters)).forEach(adapter -> {
            final ParameterizedType pt = ParameterizedType.class.cast(
                Stream.of(adapter.getClass().getGenericInterfaces())
                    .filter(i -> ParameterizedType.class.isInstance(i) && ParameterizedType.class.cast(i).getRawType() == JsonbAdapter.class).findFirst().orElse(null));
            if (pt == null) {
                throw new IllegalArgumentException(adapter + " doesn't implement JsonbAdapter");
            }
            final Type[] args = pt.getActualTypeArguments();
            final boolean fromString = args[0] == String.class;
            builder.addConverter(fromString ? args[1] : args[0], fromString ? new JsonbConverterFromString<>(adapter) : new JsonbConverterToString<>(adapter));
        }));

        config.getProperty(JsonbConfig.STRICT_IJSON).map(Boolean.class::cast).ifPresent(ijson -> {
            // no-op: https://tools.ietf.org/html/rfc7493 the only MUST of the spec sould be fine by default
        });

        config.getProperty(JsonbConfig.BINARY_DATA_STRATEGY).map(String.class::cast).ifPresent(bin -> {
            switch (bin) {
                case BinaryDataStrategy.BYTE:
                    // no-op: our default
                    break;
                case BinaryDataStrategy.BASE_64:
                    builder.setTreatByteArrayAsBase64(true);
                    break;
                case BinaryDataStrategy.BASE_64_URL: // needs j8
                    builder.addConverter(byte[].class, new Converter<byte[]>() {
                        @Override
                        public String toString(final byte[] instance) {
                            return Base64.getUrlEncoder().encodeToString(instance);
                        }

                        @Override
                        public byte[] fromString(final String text) {
                            return Base64.getUrlDecoder().decode(text.getBytes(StandardCharsets.UTF_8));
                        }
                    });
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported binary configuration: " + bin);
            }
        });

        return new JohnsonJsonb(builder.build());
    }

    private static Map<Class<?>, Converter<?>> createJava8Converters() { // TODO: move these converters in converter package
        final Map<Class<?>, Converter<?>> converters = new HashMap<>();

        final TimeZone timeZoneUTC = TimeZone.getTimeZone("UTC");
        final ZoneId zoneIDUTC = ZoneId.of("UTC");

        // built-in converters not in mapper
        converters.put(Period.class, new Converter<Period>() {
            @Override
            public String toString(final Period instance) {
                return instance.toString();
            }

            @Override
            public Period fromString(final String text) {
                return Period.parse(text);
            }
        });
        converters.put(Duration.class, new Converter<Duration>() {
            @Override
            public String toString(final Duration instance) {
                return instance.toString();
            }

            @Override
            public Duration fromString(final String text) {
                return Duration.parse(text);
            }
        });
        converters.put(Date.class, new Converter<Date>() {
            @Override
            public String toString(final Date instance) {
                return LocalDateTime.ofInstant(instance.toInstant(), zoneIDUTC).toString();
            }

            @Override
            public Date fromString(final String text) {
                return Date.from(LocalDateTime.parse(text).toInstant(ZoneOffset.UTC));
            }
        });
        converters.put(Calendar.class, new Converter<Calendar>() {
            @Override
            public String toString(final Calendar instance) {
                return ZonedDateTime.ofInstant(instance.toInstant(), zoneIDUTC).toString();
            }

            @Override
            public Calendar fromString(final String text) {
                final Calendar calendar = Calendar.getInstance();
                calendar.setTimeZone(timeZoneUTC);
                calendar.setTimeInMillis(ZonedDateTime.parse(text).toInstant().toEpochMilli());
                return calendar;
            }
        });
        converters.put(GregorianCalendar.class, new Converter<GregorianCalendar>() {
            @Override
            public String toString(final GregorianCalendar instance) {
                return instance.toZonedDateTime().toString();
            }

            @Override
            public GregorianCalendar fromString(final String text) {
                final GregorianCalendar calendar = new GregorianCalendar();
                calendar.setTimeZone(timeZoneUTC);
                calendar.setTimeInMillis(ZonedDateTime.parse(text).toInstant().toEpochMilli());
                return calendar;
            }
        });
        converters.put(TimeZone.class, new Converter<TimeZone>() {
            @Override
            public String toString(final TimeZone instance) {
                return instance.getID();
            }

            @Override
            public TimeZone fromString(final String text) {
                logIfDeprecatedTimeZone(text);
                return TimeZone.getTimeZone(text);
            }
        });
        converters.put(ZoneId.class, new Converter<ZoneId>() {
            @Override
            public String toString(final ZoneId instance) {
                return instance.getId();
            }

            @Override
            public ZoneId fromString(final String text) {
                return ZoneId.of(text);
            }
        });
        converters.put(ZoneOffset.class, new Converter<ZoneOffset>() {
            @Override
            public String toString(final ZoneOffset instance) {
                return instance.getId();
            }

            @Override
            public ZoneOffset fromString(final String text) {
                return ZoneOffset.of(text);
            }
        });
        converters.put(SimpleTimeZone.class, new Converter<SimpleTimeZone>() {
            @Override
            public String toString(final SimpleTimeZone instance) {
                return instance.getID();
            }

            @Override
            public SimpleTimeZone fromString(final String text) {
                logIfDeprecatedTimeZone(text);
                final TimeZone timeZone = TimeZone.getTimeZone(text);
                return new SimpleTimeZone(timeZone.getRawOffset(), timeZone.getID());
            }
        });
        converters.put(Instant.class, new Converter<Instant>() {
            @Override
            public String toString(final Instant instance) {
                return instance.toString();
            }

            @Override
            public Instant fromString(final String text) {
                return Instant.parse(text);
            }
        });
        converters.put(LocalDate.class, new Converter<LocalDate>() {
            @Override
            public String toString(final LocalDate instance) {
                return instance.toString();
            }

            @Override
            public LocalDate fromString(final String text) {
                return LocalDate.parse(text);
            }
        });
        converters.put(LocalDateTime.class, new Converter<LocalDateTime>() {
            @Override
            public String toString(final LocalDateTime instance) {
                return instance.toString();
            }

            @Override
            public LocalDateTime fromString(final String text) {
                return LocalDateTime.parse(text);
            }
        });
        converters.put(ZonedDateTime.class, new Converter<ZonedDateTime>() {
            @Override
            public String toString(final ZonedDateTime instance) {
                return instance.toString();
            }

            @Override
            public ZonedDateTime fromString(final String text) {
                return ZonedDateTime.parse(text);
            }
        });
        converters.put(OffsetDateTime.class, new Converter<OffsetDateTime>() {
            @Override
            public String toString(final OffsetDateTime instance) {
                return instance.toString();
            }

            @Override
            public OffsetDateTime fromString(final String text) {
                return OffsetDateTime.parse(text);
            }
        });
        converters.put(OffsetTime.class, new Converter<OffsetTime>() {
            @Override
            public String toString(final OffsetTime instance) {
                return instance.toString();
            }

            @Override
            public OffsetTime fromString(final String text) {
                return OffsetTime.parse(text);
            }
        });
        return converters;
    }

    private static void logIfDeprecatedTimeZone(final String text) {
        /* TODO: get the list, UTC is clearly not deprecated but uses 3 letters
        if (text.length() == 3) { // don't fail but log it
            Logger.getLogger(JohnzonBuilder.class.getName()).severe("Deprecated timezone: " + text);
        }
        */
    }

    private Map<String, ?> generatorConfig() {
        final Map<String, Object> map = new HashMap<>();
        if (config == null) {
            return map;
        }
        config.getProperty(JsonGeneratorFactoryImpl.GENERATOR_BUFFER_LENGTH).ifPresent(b -> map.put(JsonGeneratorFactoryImpl.GENERATOR_BUFFER_LENGTH, b));
        config.getProperty(AbstractJsonFactory.BUFFER_STRATEGY).ifPresent(b -> map.put(AbstractJsonFactory.BUFFER_STRATEGY, b));
        config.getProperty(JsonbConfig.FORMATTING).ifPresent(b -> map.put(JsonGenerator.PRETTY_PRINTING, b));
        return map;
    }
}
