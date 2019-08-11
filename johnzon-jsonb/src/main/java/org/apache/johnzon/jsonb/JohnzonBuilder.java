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

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static javax.json.bind.config.PropertyNamingStrategy.IDENTITY;
import static javax.json.bind.config.PropertyOrderStrategy.ANY;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
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
import java.util.Base64;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.json.JsonBuilderFactory;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;
import javax.json.bind.config.BinaryDataStrategy;
import javax.json.bind.config.PropertyNamingStrategy;
import javax.json.bind.config.PropertyVisibilityStrategy;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParserFactory;

import org.apache.johnzon.core.AbstractJsonFactory;
import org.apache.johnzon.core.JsonGeneratorFactoryImpl;
import org.apache.johnzon.core.JsonParserFactoryImpl;
import org.apache.johnzon.core.Types;
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
        } else {
            jsonp = JsonProvider.provider();
        }
        final Supplier<JsonBuilderFactory> builderFactorySupplier = createJsonBuilderFactory();
        final Supplier<JsonParserFactory> parserFactoryProvider = createJsonParserFactory();

        if (config == null) {
            config = new JsonbConfig();
        }

        final boolean ijson = config.getProperty(JsonbConfig.STRICT_IJSON)
                .map(Boolean.class::cast)
                .filter(it -> it)
                .map(it -> {
                    if (!config.getProperty(JsonbConfig.BINARY_DATA_STRATEGY).isPresent()) {
                        config.withBinaryDataStrategy(BinaryDataStrategy.BASE_64);
                    }
                    if (!config.getProperty(JsonbConfig.DATE_FORMAT).isPresent()) {
                        config.withDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'xxx", Locale.ROOT);
                    }
                    return it;
                }).orElse(false);

        if (config.getProperty(JsonbConfig.FORMATTING).map(Boolean.class::cast).orElse(false)) {
            builder.setPretty(true);
        }

        config.getProperty(JsonbConfig.ENCODING).ifPresent(encoding -> builder.setEncoding(String.valueOf(encoding)));
        final boolean isNillable = config.getProperty(JsonbConfig.NULL_VALUES)
                .map(it -> String.class.isInstance(it) ? Boolean.parseBoolean(it.toString()) : Boolean.class.cast(it))
                .map(serNulls -> {
                    builder.setSkipNull(!serNulls);
                    return serNulls;
                })
                .orElse(false);

        final Optional<Object> namingStrategyValue = config.getProperty(JsonbConfig.PROPERTY_NAMING_STRATEGY);

        final PropertyNamingStrategy propertyNamingStrategy = new PropertyNamingStrategyFactory(namingStrategyValue.orElse(IDENTITY)).create();
        final String orderValue = config.getProperty(JsonbConfig.PROPERTY_ORDER_STRATEGY).map(String::valueOf).orElse(ANY);
        final PropertyVisibilityStrategy visibilityStrategy = config.getProperty(JsonbConfig.PROPERTY_VISIBILITY_STRATEGY)
                .map(PropertyVisibilityStrategy.class::cast).orElse(new DefaultPropertyVisibilityStrategy());

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
        config.getProperty("johnzon.interfaceImplementationMapping")
                .map(Map.class::cast)
                .ifPresent(builder::setInterfaceImplementationMapping);
        builder.setUseJsRange(config.getProperty("johnzon.use-js-range")
                .map(v -> !Boolean.class.isInstance(v) ? Boolean.parseBoolean(v.toString()) : Boolean.class.cast(v))
                .orElse(true));

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
                        factory, jsonp, builderFactorySupplier, parserFactoryProvider,
                        config.getProperty("johnzon.accessModeDelegate")
                                .map(this::toAccessMode)
                                .orElseGet(() -> new FieldAndMethodAccessMode(true, true, false)),
                        config.getProperty("johnzon.failOnMissingCreatorValues")
                              .map(it -> String.class.isInstance(it) ? Boolean.parseBoolean(it.toString()) : Boolean.class.cast(it))
                              .orElse(true) /*spec 1.0 requirement*/,
                        isNillable));
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
            final JohnzonJsonbAdapter johnzonJsonbAdapter = new JohnzonJsonbAdapter(adapter, args[0], args[1]);
            builder.addAdapter(args[0], args[1], johnzonJsonbAdapter);
            defaultConverters.put(new AdapterKey(args[0], args[1]), johnzonJsonbAdapter);
        }));

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

        final Types types = new Types();
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
                final ParameterizedType pt = types.findParameterizedType(s.getClass(), JsonbSerializer.class);
                final Type[] args = pt.getActualTypeArguments();
                // TODO: support PT in ObjectConverter (list)
                if (args.length != 1 || !Class.class.isInstance(args[0])) {
                    throw new IllegalArgumentException("We only support serializer on Class for now");
                }
                builder.addObjectConverter(
                    Class.class.cast(args[0]), (ObjectConverter.Writer) (instance, jsonbGenerator) ->
                        s.serialize(
                                instance, jsonbGenerator.getJsonGenerator(),
                                new JohnzonSerializationContext(jsonbGenerator)));
            });
        });
        config.getProperty(JsonbConfig.DESERIALIZERS).map(JsonbDeserializer[].class::cast).ifPresent(deserializers -> {
            Stream.of(deserializers).forEach(d -> {
                final ParameterizedType pt = types.findParameterizedType(d.getClass(), JsonbDeserializer.class);
                final Type[] args = pt.getActualTypeArguments();
                if (args.length != 1 || !Class.class.isInstance(args[0])) {
                    throw new IllegalArgumentException("We only support deserializer on Class for now");
                }
                // TODO: support PT in ObjectConverter (list)
                final JsonBuilderFactory builderFactory = builderFactorySupplier.get();
                builder.addObjectConverter(
                        Class.class.cast(args[0]), (ObjectConverter.Reader)
                                (jsonObject, targetType, parser) -> d.deserialize(
                                        JsonValueParserAdapter.createFor(jsonObject, parserFactoryProvider),
                                        new JohnzonDeserializationContext(parser, builderFactory, jsonp), targetType));
            });
        });

        final boolean useCdi = cdiIntegration != null && cdiIntegration.isCanWrite() && config.getProperty("johnzon.cdi.activated").map(Boolean.class::cast).orElse(Boolean.TRUE);
        if (Closeable.class.isInstance(accessMode)) {
            builder.addCloseable(Closeable.class.cast(accessMode));
        }
        final Mapper mapper = builder.build();

        return useCdi ? new JohnzonJsonb(mapper, ijson) {
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
        } : new JohnzonJsonb(mapper, ijson);
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
        return new Lazy<JsonParserFactory>() { // thread safety is not mandatory
            @Override
            protected JsonParserFactory doCreate() {
                return jsonp.createParserFactory(emptyMap());
            }
        };
    }

    private Supplier<JsonBuilderFactory> createJsonBuilderFactory() {
        return new Lazy<JsonBuilderFactory>() { // thread safety is not mandatory
            @Override
            protected JsonBuilderFactory doCreate() {
                return jsonp.createBuilderFactory(emptyMap());
            }
        };
    }

    private Object getBeanManager() {
        if (beanManager == null) {
            try { // don't trigger CDI if not there
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
                return ZonedDateTime.ofInstant(instance.toInstant(), instance.getTimeZone().toZoneId()).toString();
            }

            @Override
            public Calendar fromString(final String text) {
                final ZonedDateTime zonedDateTime = ZonedDateTime.parse(text);
                final Calendar calendar = Calendar.getInstance();
                calendar.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));
                calendar.setTimeInMillis(zonedDateTime.toInstant().toEpochMilli());
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
                final ZonedDateTime zonedDateTime = ZonedDateTime.parse(text);
                final GregorianCalendar calendar = new GregorianCalendar();
                calendar.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));
                calendar.setTimeInMillis(zonedDateTime.toInstant().toEpochMilli());
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
        converters.put(new AdapterKey(LocalTime.class, String.class), new ConverterAdapter<>(new Converter<LocalTime>() {
            @Override
            public String toString(final LocalTime instance) {
                return instance.toString();
            }

            @Override
            public LocalTime fromString(final String text) {
                return LocalTime.parse(text);
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
                    return formatter.format(ZonedDateTime.ofInstant(instance.toInstant(), instance.getTimeZone().toZoneId()));
                }

                @Override
                public Calendar fromString(final String text) {
                    final ZonedDateTime zonedDateTime = parseZonedDateTime(text, formatter, zoneIDUTC);
                    final Calendar instance = Calendar.getInstance();
                    instance.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));
                    instance.setTime(Date.from(zonedDateTime.toInstant()));
                    return instance;
                }
            }));
            converters.put(new AdapterKey(GregorianCalendar.class, String.class), new ConverterAdapter<>(new Converter<GregorianCalendar>() {

                @Override
                public String toString(final GregorianCalendar instance) {
                    return formatter.format(ZonedDateTime.ofInstant(instance.toInstant(), instance.getTimeZone().toZoneId()));
                }

                @Override
                public GregorianCalendar fromString(final String text) {
                    final ZonedDateTime zonedDateTime = parseZonedDateTime(text, formatter, zoneIDUTC);
                    final Calendar instance = GregorianCalendar.getInstance();
                    instance.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));
                    instance.setTime(Date.from(zonedDateTime.toInstant()));
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

    private static abstract class Lazy<T> implements Supplier<T> {
        private final AtomicReference<T> ref = new AtomicReference<>();

        @Override
        public T get() {
            T factory = ref.get();
            if (factory == null) {
                factory = doCreate();
                if (!ref.compareAndSet(null, factory)) {
                    factory = ref.get();
                }
            }
            return factory;
        }

        protected abstract T doCreate();
    }
}
