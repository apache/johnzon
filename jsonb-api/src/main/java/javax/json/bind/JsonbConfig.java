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
package javax.json.bind;

import javax.json.bind.adapter.JsonbAdapter;
import javax.json.bind.config.PropertyNamingStrategy;
import javax.json.bind.config.PropertyVisibilityStrategy;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class JsonbConfig {
    private final Map<String, Object> configuration = new HashMap<>();

    public static final String FORMATTING = "jsonb.formatting";
    public static final String ENCODING = "jsonb.encoding";
    public static final String PROPERTY_NAMING_STRATEGY = "jsonb.property-naming-strategy";
    public static final String PROPERTY_ORDER_STRATEGY = "jsonb.property-order-strategy";
    public static final String NULL_VALUES = "jsonb.null-values";
    public static final String STRICT_IJSON = "jsonb.strict-ijson";
    public static final String PROPERTY_VISIBILITY_STRATEGY = "jsonb.property-visibility-strategy";
    public static final String ADAPTERS = "jsonb.adapters";
    public static final String BINARY_DATA_STRATEGY = "jsonb.binary-data-strategy";
    public static final String DATE_FORMAT = "jsonb.date-format";
    public static final String LOCALE = "jsonb.locale";
    public static final String SERIALIZERS = "jsonb.serializers";
    public static final String DESERIALIZERS = "jsonb.derializers";

    public final JsonbConfig withDateFormat(final String dateFormat, final Locale locale) {
        return setProperty(DATE_FORMAT, dateFormat).setProperty(LOCALE, locale != null ? locale : Locale.getDefault());
    }

    public final JsonbConfig withLocale(final Locale locale) {
        return setProperty(LOCALE, locale);
    }

    public final JsonbConfig setProperty(final String name, final Object value) {
        configuration.put(name, value);
        return this;
    }

    public final Optional<Object> getProperty(final String name) {
        return Optional.ofNullable(configuration.get(name));
    }

    public final Map<String, Object> getAsMap() {
        return Collections.unmodifiableMap(configuration);
    }

    public final JsonbConfig withFormatting(final Boolean formatted) {
        return setProperty(FORMATTING, formatted);
    }

    public final JsonbConfig withNullValues(final Boolean serializeNullValues) {
        return setProperty(NULL_VALUES, serializeNullValues);
    }

    public final JsonbConfig withEncoding(final String encoding) {
        return setProperty(ENCODING, encoding);
    }

    public final JsonbConfig withStrictIJSON(final Boolean enabled) {
        return setProperty(STRICT_IJSON, enabled);
    }

    public final JsonbConfig withPropertyNamingStrategy(final PropertyNamingStrategy propertyNamingStrategy) {
        return setProperty(PROPERTY_NAMING_STRATEGY, propertyNamingStrategy);
    }

    public final JsonbConfig withPropertyNamingStrategy(final String propertyNamingStrategy) {
        return setProperty(PROPERTY_NAMING_STRATEGY, propertyNamingStrategy);
    }

    public final JsonbConfig withPropertyOrderStrategy(final String propertyOrderStrategy) {
        return setProperty(PROPERTY_ORDER_STRATEGY, propertyOrderStrategy);
    }

    public final JsonbConfig withPropertyVisibilityStrategy(final PropertyVisibilityStrategy propertyVisibilityStrategy) {
        return setProperty(PROPERTY_VISIBILITY_STRATEGY, propertyVisibilityStrategy);
    }

    public final JsonbConfig withAdapters(final JsonbAdapter... adapters) {
        if (adapters == null || adapters.length == 0) {
            return this;
        }

        final Optional<Object> opt = getProperty(ADAPTERS);
        if (opt.isPresent()) {
            final JsonbAdapter[] existing = JsonbAdapter[].class.cast(opt.get());
            final JsonbAdapter[] aggregated = new JsonbAdapter[existing.length + adapters.length];
            System.arraycopy(existing, 0, aggregated, 0, existing.length);
            System.arraycopy(adapters, 0, aggregated, existing.length + 1, adapters.length);
            return setProperty(ADAPTERS, aggregated);
        }
        return setProperty(ADAPTERS, adapters);
    }

    public final JsonbConfig withBinaryDataStrategy(final String binaryDataStrategy) {
        return setProperty(BINARY_DATA_STRATEGY, binaryDataStrategy);
    }

    public final JsonbConfig withSerializers(final JsonbSerializer... serializers) {
        return setProperty(SERIALIZERS, serializers);
    }

    public final JsonbConfig withDeserializers(final JsonbDeserializer... deserializers) {
        return setProperty(DESERIALIZERS, deserializers);
    }
}
