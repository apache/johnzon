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
import org.apache.johnzon.jsonb.converter.JsonbConverter;
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static javax.json.bind.config.PropertyNamingStrategy.IDENTITY;
import static javax.json.bind.config.PropertyOrderStrategy.ANY;

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
        final String orderValue = config.getProperty(JsonbConfig.PROPERTY_ORDER_STRATEGY).map(String::valueOf).orElse(ANY);
        final PropertyVisibilityStrategy visibilityStrategy = config.getProperty(JsonbConfig.PROPERTY_VISIBILITY_STRATEGY)
            .map(PropertyVisibilityStrategy.class::cast).orElse(new PropertyVisibilityStrategy() {
                @Override
                public boolean isVisible(final Field field) {
                    return true;
                }

                @Override
                public boolean isVisible(final Method method) {
                    return true;
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

        builder.setAccessMode(
            new JsonbAccessMode(propertyNamingStrategy, orderValue, visibilityStrategy, !namingStrategyValue.orElse("").equals(PropertyNamingStrategy.CASE_INSENSITIVE)));

        config.getProperty(JsonbConfig.ADAPTERS).ifPresent(adapters -> Stream.of(JsonbAdapter[].class.cast(adapters)).forEach(adapter ->
            builder.addConverter(ofNullable(adapter.getRuntimeBoundType()).orElse(adapter.getBoundType()), new JsonbConverter(adapter))
        ));

        // TODO:
        // - Converters for setter/getter
        // - check adapters when type is not String (breaks our Converter API it seems)

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
