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
package org.apache.johnzon.jaxrs.jsonb.jaxrs;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Providers;

import org.apache.johnzon.jsonb.api.experimental.PolymorphicConfig;

// here while we dont compile in java 8 jaxrs module, when migrated we'll merge it with IgnorableTypes hierarchy at least
@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Priority(value = 4900)
public class JsonbJaxrsProvider<T> implements MessageBodyWriter<T>, MessageBodyReader<T>, AutoCloseable {

    protected final Collection<String> ignores;
    protected final JsonbConfig config = new JsonbConfig();
    protected volatile Function<Class<?>, Jsonb> delegate = null;
    protected volatile ReadImpl readImpl = null;
    private boolean customized;
    private Boolean throwNoContentExceptionOnEmptyStreams;

    @Context
    private Providers providers;

    public JsonbJaxrsProvider() {
        this(null);
    }

    protected JsonbJaxrsProvider(final Collection<String> ignores) {
        this.ignores = ignores;
    }

    private boolean isIgnored(final Class<?> type) {
        return ignores != null && ignores.contains(type.getName());
    }

    public void setThrowNoContentExceptionOnEmptyStreams(final boolean throwNoContentExceptionOnEmptyStreams) {
        this.throwNoContentExceptionOnEmptyStreams = throwNoContentExceptionOnEmptyStreams;
        // customized = false since it is not a jsonb customization but a MBR one
    }

    // config - main containers support the configuration of providers this way
    public void setFailOnUnknownProperties(final boolean active) {
        config.setProperty("johnzon.fail-on-unknown-properties", active);
        customized = true;
    }

    public void setUseJsRange(final boolean value) {
        config.setProperty("johnzon.use-js-range", value);
    }

    public void setOtherProperties(final String others) {
        final Properties properties = new Properties() {{
            try {
                load(new StringReader(others));
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }};
        properties.stringPropertyNames().forEach(k -> config.setProperty(k, properties.getProperty(k)));
        customized = true;
    }

    public void setIJson(final boolean active) {
        config.withStrictIJSON(active);
        customized = true;
    }

    public void setEncoding(final String encoding) {
        config.withEncoding(encoding);
        customized = true;
    }

    public void setBinaryDataStrategy(final String binaryDataStrategy) {
        config.withBinaryDataStrategy(binaryDataStrategy);
        customized = true;
    }

    public void setPropertyNamingStrategy(final String propertyNamingStrategy) {
        config.withPropertyNamingStrategy(propertyNamingStrategy);
        customized = true;
    }

    public void setPropertyOrderStrategy(final String propertyOrderStrategy) {
        config.withPropertyOrderStrategy(propertyOrderStrategy);
        customized = true;
    }

    public void setNullValues(final boolean nulls) {
        config.withNullValues(nulls);
        customized = true;
    }

    public void setPretty(final boolean pretty) {
        config.withFormatting(pretty);
        customized = true;
    }

    public void setFailOnMissingCreatorValues(final boolean failOnMissingCreatorValues) {
        config.setProperty("failOnMissingCreatorValues", failOnMissingCreatorValues);
        customized = true;
    }

    public void setInterfaceImplementationMapping(final Map<String, String> interfaceImplementationMapping) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Function<String, Class<?>> load = name -> {
            try {
                return loader.loadClass(name.trim());
            } catch (final ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        };
        config.setProperty("johnzon.interfaceImplementationMapping", interfaceImplementationMapping.entrySet().stream()
             .collect(toMap(it -> load.apply(it.getKey()), it -> load.apply(it.getValue()))));
        customized = true;
    }


    public void setPolymorphicSerializationPredicate(final String classes) {
        final Set<Class<?>> set = asSet(classes);
        getOrCreatePolymorphicConfig().withSerializationPredicate(set::contains);
        customized = true;
    }

    public void setPolymorphicDeserializationPredicate(final String classes) {
        final Set<Class<?>> set = asSet(classes);
        getOrCreatePolymorphicConfig().withDeserializationPredicate(set::contains);
        customized = true;
    }

    public void setPolymorphicDiscriminatorMapper(final Map<String, String> discriminatorMapper) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Map<Class<?>, String> map = discriminatorMapper.entrySet().stream()
                .collect(toMap(e -> {
                    try {
                        return loader.loadClass(e.getKey().trim());
                    } catch (final ClassNotFoundException ex) {
                        throw new IllegalArgumentException(ex);
                    }
                }, Map.Entry::getValue));
        getOrCreatePolymorphicConfig().withDiscriminatorMapper(map::get);
        customized = true;
    }

    public void setPolymorphicTypeLoader(final Map<String, String> aliasTypeMapping) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Map<String, Class<?>> map = aliasTypeMapping.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> {
                    try {
                        return loader.loadClass(e.getValue().trim());
                    } catch (final ClassNotFoundException ex) {
                        throw new IllegalArgumentException(ex);
                    }
                }));
        getOrCreatePolymorphicConfig().withTypeLoader(map::get);
        customized = true;
    }

    public void setPolymorphicDiscriminator(final String value) {
        getOrCreatePolymorphicConfig().withDiscriminator(value);
        customized = true;
    }

    private PolymorphicConfig getOrCreatePolymorphicConfig() {
        return config.getProperty(PolymorphicConfig.class.getName())
                .map(PolymorphicConfig.class::cast)
                .orElseGet(() -> {
                    final PolymorphicConfig config = new PolymorphicConfig();
                    this.config.setProperty(PolymorphicConfig.class.getName(), config);
                    return config;
                });
    }

    private Set<Class<?>> asSet(final String classes) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return Stream.of(classes.split(" *, *"))
                .map(n -> {
                    try {
                        return loader.loadClass(n.trim());
                    } catch (final ClassNotFoundException ex) {
                        throw new IllegalArgumentException(ex);
                    }
                }).collect(toSet());
    }

    // actual impl

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return !isIgnored(type)
                && !InputStream.class.isAssignableFrom(type)
                && !Reader.class.isAssignableFrom(type)
                && !Response.class.isAssignableFrom(type)
                && !CharSequence.class.isAssignableFrom(type);
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return !isIgnored(type)
                && !InputStream.class.isAssignableFrom(type)
                && !OutputStream.class.isAssignableFrom(type)
                && !Writer.class.isAssignableFrom(type)
                && !StreamingOutput.class.isAssignableFrom(type)
                && !CharSequence.class.isAssignableFrom(type)
                && !Response.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(final T t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public T readFrom(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType,
            final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream) throws WebApplicationException, IOException {
        final Jsonb jsonb = getJsonb(type);
        return (T) readImpl.doRead(jsonb, genericType, entityStream);
    }

    @Override
    public void writeTo(final T t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream) throws WebApplicationException {
        getJsonb(type).toJson(t, entityStream);
    }

    protected Jsonb createJsonb() {
        return JsonbBuilder.create(config);
    }

    protected Jsonb getJsonb(final Class<?> type) {
        if (delegate == null){
            synchronized (this) {
                if (delegate == null) {
                    if (throwNoContentExceptionOnEmptyStreams == null) {
                        throwNoContentExceptionOnEmptyStreams = initThrowNoContentExceptionOnEmptyStreams();
                    }
                    final ContextResolver<Jsonb> contextResolver = providers.getContextResolver(Jsonb.class, MediaType.APPLICATION_JSON_TYPE);
                    if (contextResolver != null) {
                        if (customized) {
                            logger().warning("Customizations done on the Jsonb instance will be ignored because a ContextResolver<Jsonb> was found");
                        }
                        if (throwNoContentExceptionOnEmptyStreams) {
                            logger().warning("Using a ContextResolver<Jsonb>, NoContentException will not be thrown for empty streams");
                        }
                        delegate = new DynamicInstance(contextResolver); // faster than contextResolver::getContext
                    } else {
                        // don't recreate it for perfs
                        delegate = new ProvidedInstance(createJsonb());
                    }
                }
                readImpl = throwNoContentExceptionOnEmptyStreams ?
                        this::doReadWithNoContentException :
                        this::doRead;
            }
        }
        return delegate.apply(type);
    }

    private boolean initThrowNoContentExceptionOnEmptyStreams() {
        try {
            ofNullable(Thread.currentThread().getContextClassLoader())
                    .orElseGet(ClassLoader::getSystemClassLoader)
                    .loadClass("jakarta.ws.rs.core.Feature");
            return true;
        } catch (final NoClassDefFoundError | ClassNotFoundException e) {
            return false;
        }
    }

    private Object doRead(final Jsonb jsonb, final Type t, final InputStream stream) {
        return jsonb.fromJson(stream, t);
    }

    private Object doReadWithNoContentException(final Jsonb jsonb, final Type t, final InputStream stream) throws NoContentException {
        try {
            return doRead(jsonb, t, stream);
        } catch (final IllegalStateException ise) {
            if (ise.getClass().getName()
                    .equals("org.apache.johnzon.core.JsonReaderImpl$NothingToRead")) {
                // spec enables to return an empty java object but it does not mean anything in JSON context so just fail
                throw new NoContentException(ise);
            }
            throw ise;
        }
    }

    private Logger logger() {
        return Logger.getLogger(JsonbJaxrsProvider.class.getName());
    }

    @Override
    public synchronized void close() throws Exception {
        if (AutoCloseable.class.isInstance(delegate)) {
            AutoCloseable.class.cast(delegate).close();
        }
    }

    private interface ReadImpl {
        Object doRead(Jsonb jsonb, Type type, InputStream entityStream) throws IOException;
    }

    private static final class DynamicInstance implements Function<Class<?>, Jsonb> {
        private final ContextResolver<Jsonb> contextResolver;

        private DynamicInstance(final ContextResolver<Jsonb> resolver) {
            this.contextResolver = resolver;
        }

        @Override
        public Jsonb apply(final Class<?> type) {
            return contextResolver.getContext(type);
        }
    }

    private static final class ProvidedInstance implements Function<Class<?>, Jsonb>, AutoCloseable {
        private final Jsonb instance;

        private ProvidedInstance(final Jsonb instance) {
            this.instance = instance;
        }

        @Override
        public Jsonb apply(final Class<?> aClass) {
            return instance;
        }

        @Override
        public void close() throws Exception {
            instance.close();
        }
    }
}
