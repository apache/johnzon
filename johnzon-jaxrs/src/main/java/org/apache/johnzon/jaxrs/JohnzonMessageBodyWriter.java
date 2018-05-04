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

import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperBuilder;

import javax.json.JsonStructure;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

@Provider
@Produces({
    "application/json", "*/json",
    "*/*+json", "*/x-json",
    "*/javascript", "*/x-javascript"
})
public class JohnzonMessageBodyWriter<T> extends IgnorableTypes implements MessageBodyWriter<T> {
    private final Mapper mapper;

    public JohnzonMessageBodyWriter() {
        this(new MapperBuilder().setDoCloseOnStreams(false).build(), null);
    }

    public JohnzonMessageBodyWriter(final Mapper mapper, final Collection<String> ignoredTypes) {
        super(ignoredTypes);
        this.mapper = mapper;
    }

    @Override
    public long getSize(final T t, final Class<?> rawType, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(final Class<?> rawType, final Type genericType,
                               final Annotation[] annotations, final MediaType mediaType) {
        return !isIgnored(rawType)
                && !InputStream.class.isAssignableFrom(rawType)
                && !OutputStream.class.isAssignableFrom(rawType)
                && !Writer.class.isAssignableFrom(rawType)
                && !StreamingOutput.class.isAssignableFrom(rawType)
                && !CharSequence.class.isAssignableFrom(rawType)
                && !Response.class.isAssignableFrom(rawType)
                && !JsonStructure.class.isAssignableFrom(rawType);
    }

    @Override
    public void writeTo(final T t, final Class<?> rawType, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException {
        if (rawType.isArray()) {
            mapper.writeArray(t, entityStream);
        } else if (Collection.class.isInstance(t)) {
            mapper.writeArray(Collection.class.cast(t), entityStream);
        } else {
            mapper.writeObject(t, entityStream);
        }
    }
}
