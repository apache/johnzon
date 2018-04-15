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
import org.apache.johnzon.core.JsonParserFactoryImpl;
import org.apache.johnzon.jsonb.cdi.CDIs;
import org.apache.johnzon.jsonb.converter.JohnzonJsonbAdapter;
import org.apache.johnzon.jsonb.factory.SimpleJohnzonAdapterFactory;
import org.apache.johnzon.jsonb.serializer.JohnzonDeserializationContext;
import org.apache.johnzon.jsonb.serializer.JohnzonSerializationContext;
import org.apache.johnzon.jsonb.spi.JohnzonAdapterFactory;
import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperBuilder;
import org.apache.johnzon.mapper.ObjectConverter;
import org.apache.johnzon.mapper.SerializeValueFilter;
import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.access.FieldAndMethodAccessMode;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbVisibility;
import javax.json.bind.config.BinaryDataStrategy;
import javax.json.bind.config.PropertyNamingStrategy;
import javax.json.bind.config.PropertyVisibilityStrategy;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParserFactory;
import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import static java.util.Collections.emptyMap;
import java.util.Objects;
import static java.util.Optional.ofNullable;
import java.util.concurrent.TimeUnit;
import static javax.json.bind.config.PropertyNamingStrategy.IDENTITY;
import static javax.json.bind.config.PropertyOrderStrategy.LEXICOGRAPHICAL;

public class JohnzonBuilder implements JsonbBuilder {
    private static final Object NO_BM = new Object();

    private final MapperBuilder builder = new MapperBuilder();
    private JsonProvider jsonp;
    private JsonbConfig config;
    private Object beanManager;
    private CDIs cdiIntegration;

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
            builder.setReaderFactory(jsonp.createReaderFactory(readerConfig()));
        }
        final Supplier<JsonParserFactory> parserFactoryProvider = createJsonParserFactory();

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
                        if (field.getAnnotation(JsonbProperty.class) != null) {
                            return true;
                        }
                        final PropertyVisibilityStrategy strategy = strategies.computeIfAbsent(field.getDeclaringClass(), this::visibilityStrategy);
                        return strategy == this ? Modifier.isPublic(field.getModifiers()) : strategy.isVisible(field);
                    }

                    @Override
                    public boolean isVisible(final Method method) {
                        final PropertyVisibilityStrategy strategy = strategies.computeIfAbsent(method.getDeclaringClass(), this::visibilityStrategy);
                        return strategy == this ? Modifier.isPublic(method.getModifiers()) : strategy.isVisible(method);
                    }

                    private PropertyVisibilityStrategy visibilityStrategy(final Class<?> type) { // can be cached
                        JsonbVisibility visibility = type.getAnnotation(JsonbVisibility.class);
                        if (visibility != null) {
                            try {
                                return visibility.value().newInstance();
                            } catch (final InstantiationException | IllegalAccessException e) {
                                throw new IllegalArgumentException(e);
                            }
                        }
                        Package p = type.getPackage();
                        while (p != null) {
                            visibility = p.getAnnotation(JsonbVisibility.class);
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
        config.getProperty("johnzon.enforceQuoteString")
                .map(v -> !Boolean.class.isInstance(v) ? Boolean.parseBoolean(v.toString()) : Boolean.class.cast(v))
                .ifPresent(builder::setEnforceQuoteString);
        config.getProperty("johnzon.primitiveConverters")
                .map(v -> !Boolean.class.isInstance(v) ? Boolean.parseBoolean(v.toString()) : Boolean.class.cast(v))
                .ifPresent(builder::setPrimitiveConverters);
        config.getProperty("johnzon.useBigDecimalForFloats")
                .map(v -> !Boolean.class.isInstance(v) ? Boolean.parseBoolean(v.toString()) : Boolean.class.cast(v))
                .ifPresent(builder::setUseBigDecimalForFloats);
        config.getProperty("johnzon.deduplicateObjects")
                .map(v -> !Boolean.class.isInstance(v) ? Boolean.parseBoolean(v.toString()) : Boolean.class.cast(v))
                .ifPresent(builder::setDeduplicateObjects);

        final Map<AdapterKey, Adapter<?, ?>> defaultConverters = createJava8Converters(builder);

        final JohnzonAdapterFactory factory = config.getProperty("johnzon.factory").map(val -> {
            if (JohnzonAdapterFactory.class.isInstance(val)) {
                return JohnzonAdapterFactory.class.cast(val);
            }
            if (String.class.isInstance(val)) {
                try {
                    return JohnzonAdapterFactory.class.cast(tccl().loadClass(val.toString()).newInstance());
                } catch (final InstantiationException | ClassNotFoundException | IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            if (Class.class.isInstance(val)) {
                try {
                    return JohnzonAdapterFactory.class.cast(Class.class.cast(val).newInstance());
                } catch (final InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            throw new IllegalArgumentException("Unsupported factory: " + val);
        }).orElseGet(this::findFactory);
        final AccessMode accessMode = config.getProperty("johnzon.accessMode")
                .map(this::toAccessMode)
                .orElseGet(() -> new JsonbAccessMode(
                        propertyNamingStrategy, orderValue, visibilityStrategy,
                        !namingStrategyValue.orElse("").equals(PropertyNamingStrategy.CASE_INSENSITIVE),
                        defaultConverters,
                        factory, parserFactoryProvider,
                        config.getProperty("johnzon.accessModeDelegate")
                                .map(this::toAccessMode)
                                .orElseGet(() -> new FieldAndMethodAccessMode(true, true, false))));
        builder.setAccessMode(accessMode);


        // user adapters
        config.getProperty(JsonbConfig.ADAPTERS).ifPresent(adapters -> Stream.of(JsonbAdapter[].class.cast(adapters)).forEach(adapter -> {
            final ParameterizedType pt = ParameterizedType.class.cast(
                    Stream.of(adapter.getClass().getGenericInterfaces())
                            .filter(i -> ParameterizedType.class.isInstance(i) && ParameterizedType.class.cast(i).getRawType() == JsonbAdapter.class).findFirst().orElse(null));
            if (pt == null) {
                throw new IllegalArgumentException(adapter + " doesn't implement JsonbAdapter");
            }
            final Type[] args = pt.getActualTypeArguments();
            builder.addAdapter(args[0], args[1], new JohnzonJsonbAdapter(adapter, args[0], args[1]));
        }));

        config.getProperty(JsonbConfig.STRICT_IJSON).map(Boolean.class::cast).ifPresent(ijson -> {
            // no-op: https://tools.ietf.org/html/rfc7493 the only MUST of the spec should be fine by default
        });
        config.getProperty("johnzon.fail-on-unknown-properties")
                .map(v -> Boolean.class.isInstance(v) ? Boolean.class.cast(v) : Boolean.parseBoolean(String.valueOf(v)))
                .ifPresent(builder::setFailOnUnknownProperties);

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

        getBeanManager(); // force detection

        builder.setReadAttributeBeforeWrite(
                config.getProperty("johnzon.readAttributeBeforeWrite").map(Boolean.class::cast).orElse(false));
        builder.setAutoAdjustStringBuffers(
                config.getProperty("johnzon.autoAdjustBuffer").map(Boolean.class::cast).orElse(true));
        config.getProperty("johnzon.serialize-value-filter")
                .map(s -> {
                    if (String.class.isInstance(s)) {
                        try {
                            return SerializeValueFilter.class.cast(
                                    Thread.currentThread().getContextClassLoader().loadClass(s.toString()).getConstructor().newInstance());
                        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
                            throw new IllegalArgumentException(e);
                        } catch (InvocationTargetException e) {
                            throw new IllegalArgumentException(e.getCause());
                        }
                    }
                    return s;
                })
                .ifPresent(s -> builder.setSerializeValueFilter(SerializeValueFilter.class.cast(s)));

        config.getProperty(JsonbConfig.SERIALIZERS).map(JsonbSerializer[].class::cast).ifPresent(serializers -> {
            Stream.of(serializers).forEach(s -> {
                final ParameterizedType pt = findPT(s, JsonbSerializer.class);
                if (pt == null) {
                    throw new IllegalArgumentException(s + " doesn't implement JsonbSerializer");
                }
                final Type[] args = pt.getActualTypeArguments();
                // TODO: support PT in ObjectConverter (list)
                if (args.length != 1 || !Class.class.isInstance(args[0])) {
                    throw new IllegalArgumentException("We only support serializer on Class for now");
                }
                builder.addObjectConverter(
                        Class.class.cast(args[0]), (ObjectConverter.Writer)
                                (instance, jsonbGenerator) -> s.serialize(instance, jsonbGenerator.getJsonGenerator(), new JohnzonSerializationContext(jsonbGenerator)));
            });
        });
        config.getProperty(JsonbConfig.DESERIALIZERS).map(JsonbDeserializer[].class::cast).ifPresent(deserializers -> {
            Stream.of(deserializers).forEach(d -> {
                final ParameterizedType pt = findPT(d, JsonbDeserializer.class);
                if (pt == null) {
                    throw new IllegalArgumentException(d + " doesn't implement JsonbDeserializer");
                }
                final Type[] args = pt.getActualTypeArguments();
                if (args.length != 1 || !Class.class.isInstance(args[0])) {
                    throw new IllegalArgumentException("We only support deserializer on Class for now");
                }
                // TODO: support PT in ObjectConverter (list)
                builder.addObjectConverter(
                        Class.class.cast(args[0]), (ObjectConverter.Reader)
                                (jsonObject, targetType, parser) -> d.deserialize(
                                        parserFactoryProvider.get().createParser(jsonObject), new JohnzonDeserializationContext(parser), targetType));
            });
        });

        final boolean useCdi = cdiIntegration != null && cdiIntegration.isCanWrite() && config.getProperty("johnzon.cdi.activated").map(Boolean.class::cast).orElse(Boolean.TRUE);
        if (Closeable.class.isInstance(accessMode)) {
            builder.addCloseable(Closeable.class.cast(accessMode));
        }
        final Mapper mapper = builder.build();

        return useCdi ? new JohnzonJsonb(mapper) {
            {
                cdiIntegration.track(this);
            }

            @Override
            public void close() {
                try {
                    super.close();
                } finally {
                    if (cdiIntegration.isCanWrite()) {
                        cdiIntegration.untrack(this);
                    }
                }
            }
        } : new JohnzonJsonb(mapper);
    }

    private AccessMode toAccessMode(final Object s) {
        if (String.class.isInstance(s)) {
            try {
                return AccessMode.class.cast(
                        Thread.currentThread().getContextClassLoader().loadClass(s.toString()).getConstructor().newInstance());
            } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException(e.getCause());
            }
        }
        return AccessMode.class.cast(s);
    }

    private Supplier<JsonParserFactory> createJsonParserFactory() {
        return new Supplier<JsonParserFactory>() { // thread safety is not mandatory
            private final AtomicReference<JsonParserFactory> ref = new AtomicReference<>();

            @Override
            public JsonParserFactory get() {
                JsonParserFactory factory = ref.get();
                if (factory == null) {
                    factory = doCreate();
                    if (!ref.compareAndSet(null, factory)) {
                        factory = ref.get();
                    }
                }
                return factory;
            }

            private JsonParserFactory doCreate() {
                return (jsonp == null ? JsonProvider.provider() : jsonp).createParserFactory(emptyMap());
            }
        };
    }

    private ParameterizedType findPT(final Object s, final Class<?> type) {
        return ParameterizedType.class.cast(
                Stream.of(s.getClass().getGenericInterfaces())
                        .filter(i -> ParameterizedType.class.isInstance(i) && ParameterizedType.class.cast(i).getRawType() == type)
                        .findFirst().orElse(null));
    }

    private Object getBeanManager() {
        if (beanManager == null) {
            try { // don't trigger CDI is not there
                final Class<?> cdi = tccl().loadClass("javax.enterprise.inject.spi.CDI");
                final Object cdiInstance = cdi.getMethod("current").invoke(null);
                beanManager = cdi.getMethod("getBeanManager").invoke(cdiInstance);
                cdiIntegration = new CDIs(beanManager);
            } catch (final NoClassDefFoundError | Exception e) {
                beanManager = NO_BM;
            }
        }
        return beanManager;
    }

    private JohnzonAdapterFactory findFactory() {
        if (getBeanManager() == NO_BM || config.getProperty("johnzon.skip-cdi")
                .map(s -> "true".equalsIgnoreCase(String.valueOf(s))).orElse(false)) {
            return new SimpleJohnzonAdapterFactory();
        }
        try { // don't trigger CDI is not there
            return new org.apache.johnzon.jsonb.factory.CdiJohnzonAdapterFactory(beanManager);
        } catch (final NoClassDefFoundError | Exception e) {
            return new SimpleJohnzonAdapterFactory();
        }
    }

    private ClassLoader tccl() {
        return ofNullable(Thread.currentThread().getContextClassLoader()).orElseGet(ClassLoader::getSystemClassLoader);
    }

    // TODO: move these converters in converter package
    private Map<AdapterKey, Adapter<?, ?>> createJava8Converters(final MapperBuilder builder) {
        final Map<AdapterKey, Adapter<?, ?>> converters = new HashMap<>();

        final TimeZone timeZoneUTC = TimeZone.getTimeZone("UTC");
        final ZoneId zoneIDUTC = ZoneId.of("UTC");

        // built-in converters not in mapper
        converters.put(new AdapterKey(Period.class, String.class), new ConverterAdapter<>(new Converter<Period>() {
            @Override
            public String toString(final Period instance) {
                return instance.toString();
            }

            @Override
            public Period fromString(final String text) {
                return Period.parse(text);
            }
        }));
        converters.put(new AdapterKey(Duration.class, String.class), new ConverterAdapter<>(new Converter<Duration>() {
            @Override
            public String toString(final Duration instance) {
                return instance.toString();
            }

            @Override
            public Duration fromString(final String text) {
                return Duration.parse(text);
            }
        }));
        converters.put(new AdapterKey(Date.class, String.class), new ConverterAdapter<>(new Converter<Date>() {
            @Override
            public String toString(final Date instance) {
                return LocalDateTime.ofInstant(instance.toInstant(), zoneIDUTC).toString();
            }

            @Override
            public Date fromString(final String text) {
                return Date.from(LocalDateTime.parse(text).toInstant(ZoneOffset.UTC));
            }
        }));
        converters.put(new AdapterKey(Calendar.class, String.class), new ConverterAdapter<>(new Converter<Calendar>() {
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
        }));
        converters.put(new AdapterKey(GregorianCalendar.class, String.class), new ConverterAdapter<>(new Converter<GregorianCalendar>() {
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
        }));
        converters.put(new AdapterKey(TimeZone.class, String.class), new ConverterAdapter<>(new Converter<TimeZone>() {
            @Override
            public String toString(final TimeZone instance) {
                return instance.getID();
            }

            @Override
            public TimeZone fromString(final String text) {
                logIfDeprecatedTimeZone(text);
                return TimeZone.getTimeZone(text);
            }
        }));
        converters.put(new AdapterKey(ZoneId.class, String.class), new ConverterAdapter<>(new Converter<ZoneId>() {
            @Override
            public String toString(final ZoneId instance) {
                return instance.getId();
            }

            @Override
            public ZoneId fromString(final String text) {
                return ZoneId.of(text);
            }
        }));
        converters.put(new AdapterKey(ZoneOffset.class, String.class), new ConverterAdapter<>(new Converter<ZoneOffset>() {
            @Override
            public String toString(final ZoneOffset instance) {
                return instance.getId();
            }

            @Override
            public ZoneOffset fromString(final String text) {
                return ZoneOffset.of(text);
            }
        }));
        converters.put(new AdapterKey(SimpleTimeZone.class, String.class), new ConverterAdapter<>(new Converter<SimpleTimeZone>() {
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
        }));
        converters.put(new AdapterKey(Instant.class, String.class), new ConverterAdapter<>(new Converter<Instant>() {
            @Override
            public String toString(final Instant instance) {
                return instance.toString();
            }

            @Override
            public Instant fromString(final String text) {
                return Instant.parse(text);
            }
        }));
        converters.put(new AdapterKey(LocalDate.class, String.class), new ConverterAdapter<>(new Converter<LocalDate>() {
            @Override
            public String toString(final LocalDate instance) {
                return instance.toString();
            }

            @Override
            public LocalDate fromString(final String text) {
                return LocalDate.parse(text);
            }
        }));
        converters.put(new AdapterKey(LocalDateTime.class, String.class), new ConverterAdapter<>(new Converter<LocalDateTime>() {
            @Override
            public String toString(final LocalDateTime instance) {
                return instance.toString();
            }

            @Override
            public LocalDateTime fromString(final String text) {
                return LocalDateTime.parse(text);
            }
        }));
        converters.put(new AdapterKey(ZonedDateTime.class, String.class), new ConverterAdapter<>(new Converter<ZonedDateTime>() {
            @Override
            public String toString(final ZonedDateTime instance) {
                return instance.toString();
            }

            @Override
            public ZonedDateTime fromString(final String text) {
                return ZonedDateTime.parse(text);
            }
        }));
        converters.put(new AdapterKey(OffsetDateTime.class, String.class), new ConverterAdapter<>(new Converter<OffsetDateTime>() {
            @Override
            public String toString(final OffsetDateTime instance) {
                return instance.toString();
            }

            @Override
            public OffsetDateTime fromString(final String text) {
                return OffsetDateTime.parse(text);
            }
        }));
        converters.put(new AdapterKey(OffsetTime.class, String.class), new ConverterAdapter<>(new Converter<OffsetTime>() {
            @Override
            public String toString(final OffsetTime instance) {
                return instance.toString();
            }

            @Override
            public OffsetTime fromString(final String text) {
                return OffsetTime.parse(text);
            }
        }));
        addDateFormatConfigConverters(converters, zoneIDUTC);


        converters.forEach((k, v) -> builder.addAdapter(k.getFrom(), k.getTo(), v));
        return converters;
    }

    private void addDateFormatConfigConverters(final Map<AdapterKey, Adapter<?, ?>> converters, final ZoneId zoneIDUTC) {
        // config, override defaults
        config.getProperty(JsonbConfig.DATE_FORMAT).map(String.class::cast).ifPresent(dateFormat -> {
            final Optional<Locale> locale = config.getProperty(JsonbConfig.LOCALE).map(Locale.class::cast);
            final DateTimeFormatter formatter = locale.isPresent() ? ofPattern(dateFormat, locale.get()) : ofPattern(dateFormat);

            converters.put(new AdapterKey(Date.class, String.class), new ConverterAdapter<>(new Converter<Date>() {

                @Override
                public String toString(final Date instance) {
                    return formatter.format(ZonedDateTime.ofInstant(instance.toInstant(), zoneIDUTC));
                }

                @Override
                public Date fromString(final String text) {
                    try {
                        return Date.from(parseZonedDateTime(text, formatter, zoneIDUTC).toInstant());
                    } catch (final DateTimeParseException dpe) {
                        return Date.from(LocalDateTime.parse(text).toInstant(ZoneOffset.UTC));
                    }
                }
            }));
            converters.put(new AdapterKey(LocalDateTime.class, String.class), new ConverterAdapter<>(new Converter<LocalDateTime>() {

                @Override
                public String toString(final LocalDateTime instance) {
                    return formatter.format(ZonedDateTime.ofInstant(instance.toInstant(ZoneOffset.UTC), zoneIDUTC));
                }

                @Override
                public LocalDateTime fromString(final String text) {
                    try {
                        return parseZonedDateTime(text, formatter, zoneIDUTC).toLocalDateTime();
                    } catch (final DateTimeParseException dpe) {
                        return LocalDateTime.parse(text);
                    }
                }
            }));
            converters.put(new AdapterKey(LocalDate.class, String.class), new ConverterAdapter<>(new Converter<LocalDate>() {

                @Override
                public String toString(final LocalDate instance) {
                    return formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(TimeUnit.DAYS.toMillis(instance.toEpochDay())), zoneIDUTC));
                }

                @Override
                public LocalDate fromString(final String text) {
                    try {
                        return parseZonedDateTime(text, formatter, zoneIDUTC).toLocalDate();
                    } catch (final DateTimeParseException dpe) {
                        return LocalDate.parse(text);
                    }
                }
            }));
            converters.put(new AdapterKey(OffsetDateTime.class, String.class), new ConverterAdapter<>(new Converter<OffsetDateTime>() {

                @Override
                public String toString(final OffsetDateTime instance) {
                    return formatter.format(ZonedDateTime.ofInstant(instance.toInstant(), zoneIDUTC));
                }

                @Override
                public OffsetDateTime fromString(final String text) {
                    try {
                        return parseZonedDateTime(text, formatter, zoneIDUTC).toOffsetDateTime();
                    } catch (final DateTimeParseException dpe) {
                        return OffsetDateTime.parse(text);
                    }
                }
            }));
            converters.put(new AdapterKey(ZonedDateTime.class, String.class), new ConverterAdapter<>(new Converter<ZonedDateTime>() {

                @Override
                public String toString(final ZonedDateTime instance) {
                    return formatter.format(ZonedDateTime.ofInstant(instance.toInstant(), zoneIDUTC));
                }

                @Override
                public ZonedDateTime fromString(final String text) {
                    try {
                        return parseZonedDateTime(text, formatter, zoneIDUTC);
                    } catch (final DateTimeParseException dpe) {
                        return ZonedDateTime.parse(text);
                    }
                }
            }));
            converters.put(new AdapterKey(Calendar.class, String.class), new ConverterAdapter<>(new Converter<Calendar>() {

                @Override
                public String toString(final Calendar instance) {
                    return formatter.format(ZonedDateTime.ofInstant(instance.toInstant(), zoneIDUTC));
                }

                @Override
                public Calendar fromString(final String text) {
                    Calendar instance = Calendar.getInstance();
                    instance.setTime(Date.from(parseZonedDateTime(text, formatter, zoneIDUTC).toInstant()));
                    return instance;
                }
            }));
            converters.put(new AdapterKey(GregorianCalendar.class, String.class), new ConverterAdapter<>(new Converter<GregorianCalendar>() {

                @Override
                public String toString(final GregorianCalendar instance) {
                    return formatter.format(ZonedDateTime.ofInstant(instance.toInstant(), zoneIDUTC));
                }

                @Override
                public GregorianCalendar fromString(final String text) {
                    Calendar instance = GregorianCalendar.getInstance();
                    instance.setTime(Date.from(parseZonedDateTime(text, formatter, zoneIDUTC).toInstant()));
                    return GregorianCalendar.class.cast(instance);
                }
            }));
            converters.put(new AdapterKey(Instant.class, String.class), new ConverterAdapter<>(new Converter<Instant>() {

                @Override
                public String toString(final Instant instance) {
                    return formatter.format(ZonedDateTime.ofInstant(instance, zoneIDUTC));
                }

                @Override
                public Instant fromString(final String text) {
                    return parseZonedDateTime(text, formatter, zoneIDUTC).toInstant();
                }
            }));
        });
    }
    
    private static ZonedDateTime parseZonedDateTime(final String text, final DateTimeFormatter formatter, final ZoneId defaultZone){
        TemporalAccessor parse = formatter.parse(text);
        ZoneId zone = parse.query(TemporalQueries.zone());
        if (Objects.isNull(zone)) {
            zone = defaultZone;
        }
        int year = parse.isSupported(YEAR) ? parse.get(YEAR) : 0;
        int month = parse.isSupported(MONTH_OF_YEAR) ? parse.get(MONTH_OF_YEAR) : 0;
        int day = parse.isSupported(DAY_OF_MONTH) ? parse.get(DAY_OF_MONTH) : 0;
        int hour = parse.isSupported(HOUR_OF_DAY) ? parse.get(HOUR_OF_DAY) : 0;
        int minute = parse.isSupported(MINUTE_OF_HOUR) ? parse.get(MINUTE_OF_HOUR) : 0;
        int second = parse.isSupported(SECOND_OF_MINUTE) ? parse.get(SECOND_OF_MINUTE) : 0;
        int millisecond = parse.isSupported(MILLI_OF_SECOND) ? parse.get(MILLI_OF_SECOND) : 0;
        return ZonedDateTime.of(year, month, day, hour, minute, second, millisecond, zone);
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

    private Map<String, ?> readerConfig() {
        final Map<String, Object> map = new HashMap<>();
        if (config == null) {
            return map;
        }
        config.getProperty(JsonParserFactoryImpl.BUFFER_LENGTH).ifPresent(b -> map.put(JsonParserFactoryImpl.BUFFER_LENGTH, b));
        config.getProperty(JsonParserFactoryImpl.MAX_STRING_LENGTH).ifPresent(b -> map.put(JsonParserFactoryImpl.MAX_STRING_LENGTH, b));
        config.getProperty(JsonParserFactoryImpl.SUPPORTS_COMMENTS).ifPresent(b -> map.put(JsonParserFactoryImpl.SUPPORTS_COMMENTS, b));
        config.getProperty(AbstractJsonFactory.BUFFER_STRATEGY).ifPresent(b -> map.put(AbstractJsonFactory.BUFFER_STRATEGY, b));
        return map;
    }
}
