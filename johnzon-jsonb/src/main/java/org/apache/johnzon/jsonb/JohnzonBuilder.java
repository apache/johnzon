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

import org.apache.johnzon.jsonb.adapter.JsonbEnumAdapter;
import org.apache.johnzon.jsonb.api.experimental.PolymorphicConfig;
import org.apache.johnzon.jsonb.cdi.CDIs;
import org.apache.johnzon.jsonb.converter.JohnzonJsonbAdapter;
import org.apache.johnzon.jsonb.factory.SimpleJohnzonAdapterFactory;
import org.apache.johnzon.jsonb.reflect.Types;
import org.apache.johnzon.jsonb.serializer.JohnzonDeserializationContext;
import org.apache.johnzon.jsonb.serializer.JohnzonSerializationContext;
import org.apache.johnzon.jsonb.spi.JohnzonAdapterFactory;
import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperBuilder;
import org.apache.johnzon.mapper.MapperConfig;
import org.apache.johnzon.mapper.Mappings;
import org.apache.johnzon.mapper.ObjectConverter;
import org.apache.johnzon.mapper.SerializeValueFilter;
import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.access.FieldAndMethodAccessMode;
import org.apache.johnzon.mapper.converter.LocaleConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.config.BinaryDataStrategy;
import jakarta.json.bind.config.PropertyNamingStrategy;
import jakarta.json.bind.config.PropertyVisibilityStrategy;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParserFactory;
import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static jakarta.json.bind.config.PropertyNamingStrategy.IDENTITY;
import static jakarta.json.bind.config.PropertyOrderStrategy.ANY;

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
        builder.setEnumConverterFactory(type -> newEnumConverter(Class.class.cast(type)));
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

        final boolean skipCdi = shouldSkipCdi();

        // todo: global spec toggle to disable all these ones at once?
        builder.setUseBigDecimalForObjectNumbers(
                config.getProperty("johnzon.use-big-decimal-for-object").map(this::toBool).orElse(true));
        builder.setSupportEnumContainerDeserialization( // https://github.com/eclipse-ee4j/jakartaee-tck/issues/103
                toBool(System.getProperty("johnzon.support-enum-container-deserialization", config.getProperty("johnzon.support-enum-container-deserialization")
                        .map(String::valueOf).orElse("true"))));

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

        config.getProperty(PolymorphicConfig.class.getName())
                .map(PolymorphicConfig.class::cast)
                .ifPresent(pc -> {
                    builder.setPolymorphicDiscriminator(pc.getDiscriminator());
                    builder.setPolymorphicDeserializationPredicate(pc.getDeserializationPredicate());
                    builder.setPolymorphicSerializationPredicate(pc.getSerializationPredicate());
                    builder.setPolymorphicDiscriminatorMapper(pc.getDiscriminatorMapper());
                    builder.setPolymorphicTypeLoader(pc.getTypeLoader());
                });
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
        config.getProperty("johnzon.primitiveConverters")
                .map(this::toBool)
                .ifPresent(builder::setPrimitiveConverters);
        config.getProperty("johnzon.useBigDecimalForFloats")
                .map(this::toBool)
                .ifPresent(builder::setUseBigDecimalForFloats);
        config.getProperty("johnzon.deduplicateObjects")
                .map(this::toBool)
                .ifPresent(builder::setDeduplicateObjects);
        config.getProperty("johnzon.interfaceImplementationMapping")
                .map(Map.class::cast)
                .ifPresent(builder::setInterfaceImplementationMapping);
        builder.setUseJsRange(toBool( // https://github.com/eclipse-ee4j/jsonb-api/issues/180
                System.getProperty("johnzon.use-js-range", config.getProperty("johnzon.use-js-range")
                .map(String::valueOf).orElse("false"))));
        builder.setUseShortISO8601Format(false);
        config.getProperty(JsonbConfig.DATE_FORMAT)
                .map(String.class::cast)
                .ifPresent(dateFormat -> builder.setAdaptersDateTimeFormatter(config.getProperty(JsonbConfig.LOCALE)
                        .map(it -> String.class.isInstance(it) ? new LocaleConverter().to(it.toString()) : Locale.class.cast(it))
                        .map(value -> ofPattern(dateFormat, value))
                        .orElseGet(() -> ofPattern(dateFormat))));

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
        }).orElseGet(() -> findFactory(skipCdi));

        ofNullable(config.getProperty("johnzon.skip-exception-serialization"))
                .map(v -> Boolean.parseBoolean(String.valueOf(v)))
                .ifPresent(builder::setSkipAccessModeWrapper);

        final AccessMode accessMode = config.getProperty("johnzon.accessMode")
                .map(this::toAccessMode)
                .orElseGet(() -> new JsonbAccessMode(
                        propertyNamingStrategy, orderValue, visibilityStrategy,
                        !namingStrategyValue.orElse("").equals(PropertyNamingStrategy.CASE_INSENSITIVE),
                        builder.getAdapters(),
                        factory, jsonp, builderFactorySupplier, parserFactoryProvider,
                        config.getProperty("johnzon.accessModeDelegate")
                                .map(this::toAccessMode)
                                .orElseGet(() -> new FieldAndMethodAccessMode(true, true, false, false, true)),
                        // this changes in v3 of the spec so let's use this behavior which makes everyone happy by default
                        config.getProperty("johnzon.failOnMissingCreatorValues")
                                .map(this::toBool)
                                .orElseGet(() -> config.getProperty("jsonb.creator-parameters-required")
                                        .map(this::toBool)
                                        .orElse(false)),
                        isNillable,
                        config.getProperty("johnzon.supportsPrivateAccess")
                                .map(this::toBool)
                                .orElse(false)));
        builder.setAccessMode(accessMode);

        config.getProperty("johnzon.snippetMaxLength")
                .map(it -> Number.class.isInstance(it)?
                        Number.class.cast(it).intValue() :
                        Integer.parseInt(it.toString()))
                .ifPresent(builder::setSnippetMaxLength);

        // user adapters
        final Types types = new Types();

        config.getProperty(JsonbConfig.ADAPTERS).ifPresent(adapters -> Stream.of(JsonbAdapter[].class.cast(adapters)).forEach(adapter -> {
            final ParameterizedType pt = types.findParameterizedType(adapter.getClass(), JsonbAdapter.class);
            if (pt == null) {
                throw new IllegalArgumentException(adapter + " doesn't implement JsonbAdapter");
            }
            final Type[] args = pt.getActualTypeArguments();
            final JohnzonJsonbAdapter johnzonJsonbAdapter = new JohnzonJsonbAdapter(adapter, args[0], args[1]);
            builder.addAdapter(args[0], args[1], johnzonJsonbAdapter);
            builder.getAdapters().put(new AdapterKey(args[0], args[1]), johnzonJsonbAdapter);
        }));

        ofNullable(config.getProperty("johnzon.fail-on-unknown-properties")
                .orElseGet(() -> config.getProperty("jsonb.fail-on-unknown-properties").orElse(null)))
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

        if (!skipCdi) {
            getBeanManager(); // force detection
        }

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

        if (Closeable.class.isInstance(accessMode)) {
            builder.addCloseable(Closeable.class.cast(accessMode));
        }

        builder.setMappingsFactory(config.getProperty("johnzon.mappings-factory")
                .map(it -> (Function<MapperConfig, Mappings>) it)
                .orElse(JsonbMappings::new));

        return doCreateJsonb(skipCdi, ijson, builder.build());
    }

    private <T extends Enum<T>> MapperConfig.CustomEnumConverter<T> newEnumConverter(final Class<T> enumType) {
        return new JsonbEnumAdapter<>(enumType);
    }

    // note: this method must stay as small as possible to enable graalvm to replace it by "false" when needed
    private Jsonb doCreateJsonb(final boolean skipCdi, final boolean ijson, final Mapper mapper) {
        if (!skipCdi && cdiIntegration != null && cdiIntegration.isCanWrite()) {
            final JohnzonJsonb jsonb = new JohnzonJsonb(mapper, ijson, i -> {
                if (cdiIntegration.isCanWrite()) {
                    cdiIntegration.untrack(i);
                }
            });
            cdiIntegration.track(jsonb);
            return jsonb;
        }
        return new JohnzonJsonb(mapper, ijson, null);
    }

    private Boolean toBool(final Object v) {
        return !Boolean.class.isInstance(v) ? Boolean.parseBoolean(v.toString()) : Boolean.class.cast(v);
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
                final Class<?> cdi = tccl().loadClass("jakarta.enterprise.inject.spi.CDI");
                final Object cdiInstance = cdi.getMethod("current").invoke(null);
                beanManager = cdi.getMethod("getBeanManager").invoke(cdiInstance);
                cdiIntegration = new CDIs(beanManager);
            } catch (final NoClassDefFoundError | Exception e) {
                beanManager = NO_BM;
            }
        }
        return beanManager;
    }

    private JohnzonAdapterFactory findFactory(final boolean skipCdi) {
        if (skipCdi || getBeanManager() == NO_BM) {
            return new SimpleJohnzonAdapterFactory();
        }
        try { // don't trigger CDI is not there
            return new org.apache.johnzon.jsonb.factory.CdiJohnzonAdapterFactory(beanManager);
        } catch (final NoClassDefFoundError | Exception e) {
            return new SimpleJohnzonAdapterFactory();
        }
    }

    private Boolean shouldSkipCdi() {
        return config.getProperty("johnzon.skip-cdi")
                .map(s -> "true".equalsIgnoreCase(String.valueOf(s)))
                .orElseGet(() -> !config.getProperty("johnzon.cdi.activated").map(Boolean.class::cast).orElse(Boolean.TRUE));
    }

    private ClassLoader tccl() {
        return ofNullable(Thread.currentThread().getContextClassLoader()).orElseGet(ClassLoader::getSystemClassLoader);
    }

    private Map<String, ?> generatorConfig() {
        final Map<String, Object> map = new HashMap<>();
        if (config == null) {
            return map;
        }
        config.getProperty("org.apache.johnzon.default-char-buffer-generator").ifPresent(b -> map.put("org.apache.johnzon.default-char-buffer-generator", b));
        config.getProperty("org.apache.johnzon.boundedoutputstreamwriter").ifPresent(b -> map.put("org.apache.johnzon.boundedoutputstreamwriter", b));
        config.getProperty("org.apache.johnzon.buffer-strategy").ifPresent(b -> map.put("org.apache.johnzon.buffer-strategy", b));
        config.getProperty(JsonbConfig.FORMATTING).ifPresent(b -> map.put(JsonGenerator.PRETTY_PRINTING, b));
        return map;
    }

    private Map<String, ?> readerConfig() {
        final Map<String, Object> map = new HashMap<>();
        if (config == null) {
            return map;
        }
        config.getProperty("org.apache.johnzon.default-char-buffer").ifPresent(b -> map.put("org.apache.johnzon.default-char-buffer", b));
        config.getProperty("org.apache.johnzon.max-string-length").ifPresent(b -> map.put("org.apache.johnzon.max-string-length", b));
        config.getProperty("org.apache.johnzon.supports-comments").ifPresent(b -> map.put("org.apache.johnzon.supports-comments", b));
        config.getProperty("org.apache.johnzon.buffer-strategy").ifPresent(b -> map.put("org.apache.johnzon.buffer-strategy", b));
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