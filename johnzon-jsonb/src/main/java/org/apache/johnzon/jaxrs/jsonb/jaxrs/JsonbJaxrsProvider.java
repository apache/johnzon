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

import javax.json.JsonStructure;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;

// here while we dont compile in java 8 jaxrs module, when migrated we'll merge it with IgnorableTypes hierarchy at least
@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Priority(value = 4900)
public class JsonbJaxrsProvider<T> implements MessageBodyWriter<T>, MessageBodyReader<T>, AutoCloseable {

    protected final Collection<String> ignores;
    protected final JsonbConfig config = new JsonbConfig();
    protected volatile Function<Class<?>, Jsonb> delegate = null;
    private boolean customized;

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

    // config - main containers support the configuration of providers this way
    public void setFailOnUnknownProperties(final boolean active) {
        config.setProperty("johnzon.fail-on-unknown-properties", active);
        customized = true;
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

    // actual impl

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return !isIgnored(type)
                && !InputStream.class.isAssignableFrom(type)
                && !Reader.class.isAssignableFrom(type)
                && !Response.class.isAssignableFrom(type)
                && !CharSequence.class.isAssignableFrom(type)
                && !JsonStructure.class.isAssignableFrom(type);
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return !isIgnored(type)
                && !InputStream.class.isAssignableFrom(type)
                && !OutputStream.class.isAssignableFrom(type)
                && !Writer.class.isAssignableFrom(type)
                && !StreamingOutput.class.isAssignableFrom(type)
                && !CharSequence.class.isAssignableFrom(type)
                && !Response.class.isAssignableFrom(type)
                && !JsonStructure.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(final T t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public T readFrom(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType,
            final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream) throws IOException, WebApplicationException {
        return getJsonb(type).fromJson(entityStream, genericType);
    }

    @Override
    public void writeTo(final T t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType,
            final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream) throws IOException, WebApplicationException {
        getJsonb(type).toJson(t, entityStream);
    }

    protected Jsonb createJsonb() {
        return JsonbBuilder.create(config);
    }

    protected Jsonb getJsonb(final Class<?> type) {
        if (delegate == null){
            synchronized (this) {
                if (delegate == null) {
                    final ContextResolver<Jsonb> contextResolver = providers.getContextResolver(Jsonb.class, MediaType.APPLICATION_JSON_TYPE);
                    if (contextResolver != null) {
                        if (customized) {
                            Logger.getLogger(JsonbJaxrsProvider.class.getName())
                                  .warning("Customizations done on the Jsonb instance will be ignored because a ContextResolver<Jsonb> was found");
                        }
                        delegate = new DynamicInstance(contextResolver); // faster than contextResolver::getContext
                    } else {
                        delegate = new ProvidedInstance(createJsonb()); // don't recreate it
                    }
                }
            }
        }
        return delegate.apply(type);
    }

    @Override
    public synchronized void close() throws Exception {
        if (AutoCloseable.class.isInstance(delegate)) {
            AutoCloseable.class.cast(delegate).close();
        }
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
