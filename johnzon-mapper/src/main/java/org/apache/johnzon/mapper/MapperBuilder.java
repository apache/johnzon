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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class MapperBuilder {
    private static final Map<AdapterKey, Adapter<?, ?>> DEFAULT_CONVERTERS = new HashMap<AdapterKey, Adapter<?, ?>>(23);


    private MapperConfig builderConfig = new MapperConfig();

    private JsonReaderFactory readerFactory;
    private JsonGeneratorFactory generatorFactory;
    private boolean supportHiddenAccess = true;
    private int maxSize = -1;
    private int bufferSize = -1;
    private String bufferStrategy;
    private Comparator<String> attributeOrder = null;
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
        MapperConfig mapperConfig = builderConfig.clone();

        return new Mapper(
            readerFactory, generatorFactory,
            mapperConfig,
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
        builderConfig.addAdapter(new AdapterKey(clazz, String.class), new ConverterAdapter(converter));
        return this;
    }

    @Deprecated // use addAdapter
    public MapperBuilder addConverter(final Type clazz, final Converter<?> converter) {
        builderConfig.addAdapter(new AdapterKey(clazz, String.class), new ConverterAdapter(converter));
        return this;
    }

    public MapperBuilder addAdapter(final Type from, final Type to, final Adapter<?, ?> adapter) {
        builderConfig.addAdapter(new AdapterKey(from, to), adapter);
        return this;
    }

    public MapperBuilder addAdapter(final Adapter<?, ?> converter) {
        for (final Type gi : converter.getClass().getGenericInterfaces()) {
            if (ParameterizedType.class.isInstance(gi) && Adapter.class == ParameterizedType.class.cast(gi).getRawType()) {
                final Type[] args = ParameterizedType.class.cast(gi).getActualTypeArguments();
                builderConfig.addAdapter(new AdapterKey(args[0], args[1]), converter);
                return this;
            }
        }
        throw new IllegalArgumentException("Can't find Adapter generics from " + converter + ", please use addAdapter(t1, t2, adapter) instead");
    }

    public MapperBuilder setVersion(final int version) {
        builderConfig.setVersion(version);
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

    public <T> MapperBuilder addObjectConverter(Class<T> targetType, ObjectConverter<T> objectConverter) {
        builderConfig.addObjectConverter(targetType, objectConverter);
        return this;
    }

}
