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
package org.apache.johnzon.jaxrs;

import org.apache.johnzon.mapper.MapperBuilder;
import org.apache.johnzon.mapper.SerializeValueFilter;
import org.apache.johnzon.mapper.access.AccessMode;

import javax.json.JsonReaderFactory;
import javax.json.stream.JsonGeneratorFactory;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;

@Provider
@Produces("application/json")
@Consumes("application/json")
public class ConfigurableJohnzonProvider<T> implements MessageBodyWriter<T>, MessageBodyReader<T> {
    // build/configuration
    private MapperBuilder builder = new MapperBuilder();
    private List<String> ignores;

    // runtime
    private AtomicReference<JohnzonProvider<T>> delegate = new AtomicReference<JohnzonProvider<T>>();

    private JohnzonProvider<T> instance() {
        JohnzonProvider<T> instance;
        do {
            instance = delegate.get();
            if (builder != null && delegate.compareAndSet(null, new JohnzonProvider<T>(builder.build(), ignores))) {
                // reset build instances
                builder = null;
                ignores = null;
            }
        } while (instance == null);
        return instance;
    }

    @Override
    public boolean isReadable(final Class<?> rawType, final Type genericType,
                              final Annotation[] annotations, final MediaType mediaType) {
        return instance().isReadable(rawType, genericType, annotations, mediaType);
    }

    @Override
    public T readFrom(final Class<T> rawType, final Type genericType,
                      final Annotation[] annotations, final MediaType mediaType,
                      final MultivaluedMap<String, String> httpHeaders,
                      final InputStream entityStream) throws IOException {
        return instance().readFrom(rawType, genericType, annotations, mediaType, httpHeaders, entityStream);
    }

    @Override
    public long getSize(final T t, final Class<?> rawType, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType) {
        return instance().getSize(t, rawType, genericType, annotations, mediaType);
    }

    @Override
    public boolean isWriteable(final Class<?> rawType, final Type genericType,
                               final Annotation[] annotations, final MediaType mediaType) {
        return instance().isWriteable(rawType, genericType, annotations, mediaType);
    }

    @Override
    public void writeTo(final T t, final Class<?> rawType, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException {
        instance().writeTo(t, rawType, genericType, annotations, mediaType, httpHeaders, entityStream);
    }

    // type=a,b,c|type2=d,e
    public void setIgnoreFieldsForType(final String mapping) {
        for (final String config : mapping.split(" *| *")) {
            final String[] parts = config.split(" *= *");
            try {
                final Class<?> type = Thread.currentThread().getContextClassLoader().loadClass(parts[0]);
                if (parts.length == 1) {
                    builder.setIgnoreFieldsForType(type);
                } else {
                    builder.setIgnoreFieldsForType(type, parts[1].split(" *, *"));
                }
            } catch (final ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public void setFailOnUnknownProperties(final boolean active) {
        builder.setFailOnUnknownProperties(active);
    }

    public void setSupportConstructors(final boolean supportConstructors) {
        builder.setSupportConstructors(supportConstructors);
    }

    public void setPretty(final boolean pretty) {
        builder.setPretty(pretty);
    }

    public void setSupportGetterForCollections(final boolean supportGetterForCollections) {
        builder.setSupportGetterForCollections(supportGetterForCollections);
    }

    public void setSupportsComments(final boolean supportsComments) {
        builder.setSupportsComments(supportsComments);
    }

    public void setIgnores(final String ignores) {
        this.ignores = ignores == null ? null : asList(ignores.split(" *, *"));
    }

    public void setAccessMode(final AccessMode mode) {
        builder.setAccessMode(mode);
    }

    public void setAccessModeName(final String mode) {
        builder.setAccessModeName(mode);
    }

    public void setSupportHiddenAccess(final boolean supportHiddenAccess) {
        builder.setSupportHiddenAccess(supportHiddenAccess);
    }

    public void setAttributeOrder(final Comparator<String> attributeOrder) {
        builder.setAttributeOrder(attributeOrder);
    }

    public void setReaderFactory(final JsonReaderFactory readerFactory) {
        builder.setReaderFactory(readerFactory);
    }

    public void setGeneratorFactory(final JsonGeneratorFactory generatorFactory) {
        builder.setGeneratorFactory(generatorFactory);
    }

    public void setDoCloseOnStreams(final boolean doCloseOnStreams) {
        builder.setDoCloseOnStreams(doCloseOnStreams);
    }

    public void setVersion(final int version) {
        builder.setVersion(version);
    }

    public void setSkipNull(final boolean skipNull) {
        builder.setSkipNull(skipNull);
    }

    public void setSkipEmptyArray(final boolean skipEmptyArray) {
        builder.setSkipEmptyArray(skipEmptyArray);
    }

    public void setBufferSize(final int bufferSize) {
        builder.setBufferSize(bufferSize);
    }

    public void setBufferStrategy(final String bufferStrategy) {
        builder.setBufferStrategy(bufferStrategy);
    }

    public void setMaxSize(final int size) {
        builder.setMaxSize(size);
    }

    public void setTreatByteArrayAsBase64(final boolean treatByteArrayAsBase64) {
        builder.setTreatByteArrayAsBase64(treatByteArrayAsBase64);
    }

    public void setEncoding(final String encoding) {
        builder.setEncoding(encoding);
    }

    public void setReadAttributeBeforeWrite(final boolean rabw) {
        builder.setReadAttributeBeforeWrite(rabw);
    }

    public void setEnforceQuoteString(final boolean val) {
        builder.setEnforceQuoteString(val);
    }

    public void setPrimitiveConverters(final boolean val) {
        builder.setPrimitiveConverters(val);
    }

    public MapperBuilder setDeduplicateObjects(boolean deduplicateObjects) {
        return builder.setDeduplicateObjects(deduplicateObjects);
    }

    public void setSerializeValueFilter(final String val) {
        try {
            builder.setSerializeValueFilter(SerializeValueFilter.class.cast(
                    Thread.currentThread().getContextClassLoader().loadClass(val).getConstructor().newInstance()));
        } catch (final InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e.getCause());
        }
    }

    public void setUseBigDecimalForFloats(final boolean useBigDecimalForFloats) {
        builder.setUseBigDecimalForFloats(useBigDecimalForFloats);
    }

    public void setAutoAdjustStringBuffers(final boolean autoAdjustStringBuffers) {
        builder.setAutoAdjustStringBuffers(autoAdjustStringBuffers);
    }
}
