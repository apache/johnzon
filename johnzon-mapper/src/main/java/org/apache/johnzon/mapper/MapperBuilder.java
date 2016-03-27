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
package org.apache.johnzon.mapper;

import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.access.BaseAccessMode;
import org.apache.johnzon.mapper.access.FieldAccessMode;
import org.apache.johnzon.mapper.access.FieldAndMethodAccessMode;
import org.apache.johnzon.mapper.access.MethodAccessMode;
import org.apache.johnzon.mapper.converter.BigDecimalConverter;
import org.apache.johnzon.mapper.converter.BigIntegerConverter;
import org.apache.johnzon.mapper.converter.BooleanConverter;
import org.apache.johnzon.mapper.converter.ByteConverter;
import org.apache.johnzon.mapper.converter.CachedDelegateConverter;
import org.apache.johnzon.mapper.converter.CharacterConverter;
import org.apache.johnzon.mapper.converter.ClassConverter;
import org.apache.johnzon.mapper.converter.DateConverter;
import org.apache.johnzon.mapper.converter.DoubleConverter;
import org.apache.johnzon.mapper.converter.FloatConverter;
import org.apache.johnzon.mapper.converter.IntegerConverter;
import org.apache.johnzon.mapper.converter.LocaleConverter;
import org.apache.johnzon.mapper.converter.LongConverter;
import org.apache.johnzon.mapper.converter.ShortConverter;
import org.apache.johnzon.mapper.converter.StringConverter;
import org.apache.johnzon.mapper.converter.URIConverter;
import org.apache.johnzon.mapper.converter.URLConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;

import org.apache.johnzon.core.JsonParserFactoryImpl;

import javax.json.JsonReaderFactory;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import java.io.Closeable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MapperBuilder {
    private static final Map<AdapterKey, Adapter<?, ?>> DEFAULT_CONVERTERS = new HashMap<AdapterKey, Adapter<?, ?>>(23);

    static {
        //DEFAULT_CONVERTERS.put(Date.class, new DateConverter("yyyy-MM-dd'T'HH:mm:ssZ")); // ISO8601 long RFC822 zone
        DEFAULT_CONVERTERS.put(new AdapterKey(Date.class, String.class), new ConverterAdapter<Date>(new DateConverter("yyyyMMddHHmmssZ"))); // ISO8601 short
        DEFAULT_CONVERTERS.put(new AdapterKey(URL.class, String.class), new ConverterAdapter<URL>(new URLConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(URI.class, String.class), new ConverterAdapter<URI>(new URIConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Class.class, String.class), new ConverterAdapter<Class<?>>(new ClassConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(String.class, String.class), new ConverterAdapter<String>(new StringConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(BigDecimal.class, String.class), new ConverterAdapter<BigDecimal>(new BigDecimalConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(BigInteger.class, String.class), new ConverterAdapter<BigInteger>(new BigIntegerConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Byte.class, String.class), new ConverterAdapter<Byte>(new CachedDelegateConverter<Byte>(new ByteConverter())));
        DEFAULT_CONVERTERS.put(new AdapterKey(Character.class, String.class), new ConverterAdapter<Character>(new CharacterConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Double.class, String.class), new ConverterAdapter<Double>(new DoubleConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Float.class, String.class), new ConverterAdapter<Float>(new FloatConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Integer.class, String.class), new ConverterAdapter<Integer>(new IntegerConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Long.class, String.class), new ConverterAdapter<Long>(new LongConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Short.class, String.class), new ConverterAdapter<Short>(new ShortConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Boolean.class, String.class), new ConverterAdapter<Boolean>(new CachedDelegateConverter<Boolean>(new BooleanConverter())));
        DEFAULT_CONVERTERS.put(new AdapterKey(byte.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Byte.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(char.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Character.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(double.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Double.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(float.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Float.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(int.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Integer.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(long.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Long.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(short.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Short.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(boolean.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Boolean.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(Locale.class, String.class), new LocaleConverter());
    }

    private MapperConfig builderConfig = new MapperConfig();

    private JsonReaderFactory readerFactory;
    private JsonGeneratorFactory generatorFactory;
    private boolean supportHiddenAccess = true;
    private int version = -1;
    private int maxSize = -1;
    private int bufferSize = -1;
    private String bufferStrategy;
    private Comparator<String> attributeOrder = null;
    private final Map<AdapterKey, Adapter<?, ?>> adapters = new HashMap<AdapterKey, Adapter<?, ?>>(DEFAULT_CONVERTERS);
    private boolean supportConstructors;
    private boolean useGetterForCollections;
    private String accessModeName;
    private final Collection<Closeable> closeables = new ArrayList<Closeable>();

    public Mapper build() {
        if (readerFactory == null || generatorFactory == null) {
            final JsonProvider provider = JsonProvider.provider();
            final Map<String, Object> config = new HashMap<String, Object>();
            if (bufferStrategy != null) {
                config.put(JsonParserFactoryImpl.BUFFER_STRATEGY, bufferStrategy);
            }
            if (builderConfig.isPrettyPrint()) {
                config.put(JsonGenerator.PRETTY_PRINTING, true);
            }

            if (generatorFactory == null) {
                generatorFactory = provider.createGeneratorFactory(config);
            }

            config.remove(JsonGenerator.PRETTY_PRINTING); // doesnt mean anything anymore for reader
            if (builderConfig.isSupportsComments()) {
                config.put(JsonParserFactoryImpl.SUPPORTS_COMMENTS, "true");
            }
            if (maxSize > 0) {
                config.put(JsonParserFactoryImpl.MAX_STRING_LENGTH, maxSize);
            }
            if (bufferSize > 0) {
                config.put(JsonParserFactoryImpl.BUFFER_LENGTH, bufferSize);
            }
            if (readerFactory == null) {
                readerFactory = provider.createReaderFactory(config);
            }
        }

        if (builderConfig.getAccessMode() == null) {
            if ("field".equalsIgnoreCase(accessModeName)) {
                builderConfig.setAccessMode(new FieldAccessMode(supportConstructors, supportHiddenAccess));
            } else if ("method".equalsIgnoreCase(accessModeName)) {
                builderConfig.setAccessMode(new MethodAccessMode(supportConstructors, supportHiddenAccess, true));
            } else if ("strict-method".equalsIgnoreCase(accessModeName)) {
                builderConfig.setAccessMode(new MethodAccessMode(supportConstructors, supportHiddenAccess, false));
            } else if ("both".equalsIgnoreCase(accessModeName)) {
                builderConfig.setAccessMode(new FieldAndMethodAccessMode(supportConstructors, supportHiddenAccess));
            } else {
                builderConfig.setAccessMode(new MethodAccessMode(supportConstructors, supportHiddenAccess, useGetterForCollections));
            }
        }

        // new config so builderConfig can get tweaked again.
        MapperConfig config = builderConfig.clone();

        return new Mapper(
            readerFactory, generatorFactory,
            config,
            adapters,
            version,
            attributeOrder,
            closeables);
    }

    public MapperBuilder addCloseable(final Closeable closeable) {
        closeables.add(closeable);
        return this;
    }

    public MapperBuilder setIgnoreFieldsForType(final Class<?> type, final String... fields) {
        if (BaseAccessMode.class.isInstance(builderConfig.getAccessMode())) {
            if (fields == null || fields.length == 0) {
                BaseAccessMode.class.cast(builderConfig.getAccessMode()).getFieldsToRemove().remove(type);
            } else {
                BaseAccessMode.class.cast(builderConfig.getAccessMode()).getFieldsToRemove().put(type, fields);
            }
        } else {
            throw new IllegalStateException("AccessMode is not an BaseAccessMode");
        }
        return this;
    }

    public MapperBuilder setSupportGetterForCollections(final boolean useGetterForCollections) {
        this.useGetterForCollections = useGetterForCollections;
        return this;
    }

    public MapperBuilder setSupportsComments(final boolean supportsComments) {
        builderConfig.setSupportsComments(supportsComments);
        return this;
    }

    public MapperBuilder setPretty(final boolean pretty) {
        builderConfig.setPrettyPrint(pretty);
        return this;
    }

    public MapperBuilder setBufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public MapperBuilder setBufferStrategy(final String bufferStrategy) {
        this.bufferStrategy = bufferStrategy;
        return this;
    }

    public MapperBuilder setMaxSize(final int size) {
        this.maxSize = size;
        return this;
    }

    public MapperBuilder setAccessMode(final AccessMode mode) {
        builderConfig.setAccessMode(mode);
        return this;
    }

    public MapperBuilder setAccessModeName(final String mode) {
        if (!"field".equalsIgnoreCase(mode) && !"method".equalsIgnoreCase(mode) &&
            !"strict-method".equalsIgnoreCase(mode) && !"both".equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException("Mode " + mode + " unsupported");
        }
        this.accessModeName = mode;
        return this;
    }

    public MapperBuilder setSupportHiddenAccess(final boolean supportHiddenAccess) {
        this.supportHiddenAccess = supportHiddenAccess;
        return this;
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
        builderConfig.setClose(doCloseOnStreams);
        return this;
    }

    @Deprecated // use addAdapter
    public MapperBuilder addPropertyEditor(final Class<?> clazz, final Converter<?> converter) {
        this.adapters.put(new AdapterKey(clazz, String.class), new ConverterAdapter(converter));
        return this;
    }

    @Deprecated // use addAdapter
    public MapperBuilder addConverter(final Type clazz, final Converter<?> converter) {
        this.adapters.put(new AdapterKey(clazz, String.class), new ConverterAdapter(converter));
        return this;
    }

    public MapperBuilder addAdapter(final Type from, final Type to, final Adapter<?, ?> adapter) {
        this.adapters.put(new AdapterKey(from, to), adapter);
        return this;
    }

    public MapperBuilder addAdapter(final Adapter<?, ?> converter) {
        for (final Type gi : converter.getClass().getGenericInterfaces()) {
            if (ParameterizedType.class.isInstance(gi) && Adapter.class == ParameterizedType.class.cast(gi).getRawType()) {
                final Type[] args = ParameterizedType.class.cast(gi).getActualTypeArguments();
                this.adapters.put(new AdapterKey(args[0], args[1]), converter);
                return this;
            }
        }
        throw new IllegalArgumentException("Can't find Adapter generics from " + converter + ", please use addAdapter(t1, t2, adapter) instead");
    }

    public MapperBuilder setVersion(final int version) {
        this.version = version;
        return this;
    }

    public MapperBuilder setSkipNull(final boolean skipNull) {
        builderConfig.setSkipNull(skipNull);
        return this;
    }

    public MapperBuilder setSkipEmptyArray(final boolean skipEmptyArray) {
        builderConfig.setSkipEmptyArray(skipEmptyArray);
        return this;
    }

    public MapperBuilder setTreatByteArrayAsBase64(final boolean treatByteArrayAsBase64) {
        builderConfig.setTreatByteArrayAsBase64(treatByteArrayAsBase64);
        return this;
    }

    public MapperBuilder setTreatByteArrayAsBase64URL(final boolean treatByteArrayAsBase64URL) {
        builderConfig.setTreatByteArrayAsBase64URL(treatByteArrayAsBase64URL);
        return this;
    }

    public MapperBuilder setSupportConstructors(final boolean supportConstructors) {
        this.supportConstructors = supportConstructors;
        return this;
    }

    public MapperBuilder setEncoding(final String encoding) {
        builderConfig.setEncoding(Charset.forName(encoding));
        return this;
    }

    public MapperBuilder setReadAttributeBeforeWrite(final boolean readAttributeBeforeWrite) {
        builderConfig.setReadAttributeBeforeWrite(readAttributeBeforeWrite);
        return this;
    }
}
