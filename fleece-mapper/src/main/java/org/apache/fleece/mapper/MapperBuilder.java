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
package org.apache.fleece.mapper;

import org.apache.fleece.mapper.converter.BigDecimalConverter;
import org.apache.fleece.mapper.converter.BigIntegerConverter;
import org.apache.fleece.mapper.converter.BooleanConverter;
import org.apache.fleece.mapper.converter.ByteConverter;
import org.apache.fleece.mapper.converter.CachedDelegateConverter;
import org.apache.fleece.mapper.converter.ClassConverter;
import org.apache.fleece.mapper.converter.DateConverter;
import org.apache.fleece.mapper.converter.DoubleConverter;
import org.apache.fleece.mapper.converter.FloatConverter;
import org.apache.fleece.mapper.converter.IntegerConverter;
import org.apache.fleece.mapper.converter.LongConverter;
import org.apache.fleece.mapper.converter.ShortConverter;
import org.apache.fleece.mapper.converter.StringConverter;

import javax.json.JsonReaderFactory;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGeneratorFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MapperBuilder {
    private static final Map<Class<?>, Converter<?>> DEFAULT_CONVERTERS = new HashMap<Class<?>, Converter<?>>();

    static {
        //DEFAULT_CONVERTERS.put(Date.class, new DateConverter("yyyy-MM-dd'T'HH:mm:ssZ")); // ISO8601 long RFC822 zone
        DEFAULT_CONVERTERS.put(Date.class, new DateConverter("yyyyMMddHHmmssZ")); // ISO8601 short
        DEFAULT_CONVERTERS.put(Class.class, new ClassConverter());
        DEFAULT_CONVERTERS.put(String.class, new StringConverter());
        DEFAULT_CONVERTERS.put(BigDecimal.class, new BigDecimalConverter());
        DEFAULT_CONVERTERS.put(BigInteger.class, new BigIntegerConverter());
        DEFAULT_CONVERTERS.put(Byte.class, new CachedDelegateConverter<Byte>(new ByteConverter()));
        DEFAULT_CONVERTERS.put(Double.class, new DoubleConverter());
        DEFAULT_CONVERTERS.put(Float.class, new FloatConverter());
        DEFAULT_CONVERTERS.put(Integer.class, new IntegerConverter());
        DEFAULT_CONVERTERS.put(Long.class, new LongConverter());
        DEFAULT_CONVERTERS.put(Short.class, new ShortConverter());
        DEFAULT_CONVERTERS.put(Boolean.class, new CachedDelegateConverter<Boolean>(new BooleanConverter()));
        DEFAULT_CONVERTERS.put(byte.class, DEFAULT_CONVERTERS.get(Byte.class));
        DEFAULT_CONVERTERS.put(double.class, DEFAULT_CONVERTERS.get(Double.class));
        DEFAULT_CONVERTERS.put(float.class, DEFAULT_CONVERTERS.get(Float.class));
        DEFAULT_CONVERTERS.put(int.class, DEFAULT_CONVERTERS.get(Integer.class));
        DEFAULT_CONVERTERS.put(long.class, DEFAULT_CONVERTERS.get(Long.class));
        DEFAULT_CONVERTERS.put(short.class, DEFAULT_CONVERTERS.get(Short.class));
        DEFAULT_CONVERTERS.put(boolean.class, DEFAULT_CONVERTERS.get(Boolean.class));
    }

    private JsonReaderFactory readerFactory;
    private JsonGeneratorFactory generatorFactory;
    private boolean doCloseOnStreams = false;
    private int version = -1;
    private Comparator<String> attributeOrder = null;
    private final Map<Class<?>, Converter<?>> converters = new HashMap<Class<?>, Converter<?>>(DEFAULT_CONVERTERS);

    public Mapper build() {
        if (readerFactory == null || generatorFactory == null) {
            final JsonProvider provider = JsonProvider.provider();
            final Map<String, Object> config = Collections.<String, Object>emptyMap();
            if (readerFactory == null) {
                readerFactory = provider.createReaderFactory(config);
            }
            if (generatorFactory == null) {
                generatorFactory = provider.createGeneratorFactory(config);
            }
        }

        return new Mapper(readerFactory, generatorFactory, doCloseOnStreams, converters, version, attributeOrder);
    }

    public MapperBuilder setAttributeOrder(final Comparator<String> attributeOrder) {
        this.attributeOrder = attributeOrder;
        return this;
    }

    public MapperBuilder setReaderFactory(final JsonReaderFactory readerFactory) {
        this.readerFactory = readerFactory;
        return this;
    }

    public MapperBuilder setGeneratorFactory(final JsonGeneratorFactory generatorFactory) {
        this.generatorFactory = generatorFactory;
        return this;
    }

    public MapperBuilder setDoCloseOnStreams(final boolean doCloseOnStreams) {
        this.doCloseOnStreams = doCloseOnStreams;
        return this;
    }

    public MapperBuilder addPropertyEditor(final Class<?> clazz, final Converter<?> converter) {
        this.converters.put(clazz, converter);
        return this;
    }

    public MapperBuilder setVersion(final int version) {
        this.version = version;
        return this;
    }
}
