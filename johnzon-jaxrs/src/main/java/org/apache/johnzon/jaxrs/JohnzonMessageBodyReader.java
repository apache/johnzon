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
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

@Provider
@Consumes({
    "application/json", "*/json",
    "*/*+json", "*/x-json",
    "*/javascript", "*/x-javascript"
})
public class JohnzonMessageBodyReader<T> extends IgnorableTypes implements MessageBodyReader<T> {
    private final Mapper mapper;

    public JohnzonMessageBodyReader() {
        this(new MapperBuilder().setDoCloseOnStreams(false).build(), null);
    }

    public JohnzonMessageBodyReader(final Mapper mapper, final Collection<String> ignoredTypes) {
        super(ignoredTypes);
        this.mapper = mapper;
    }

    @Override
    public boolean isReadable(final Class<?> rawType, final Type genericType,
                              final Annotation[] annotations, final MediaType mediaType) {
        return !isIgnored(rawType)
                && !InputStream.class.isAssignableFrom(rawType)
                && !Reader.class.isAssignableFrom(rawType)
                && !Response.class.isAssignableFrom(rawType)
                && !CharSequence.class.isAssignableFrom(rawType)
                && !JsonStructure.class.isAssignableFrom(rawType);
    }

    @Override
    public T readFrom(final Class<T> rawType, final Type genericType,
                      final Annotation[] annotations, final MediaType mediaType,
                      final MultivaluedMap<String, String> httpHeaders,
                      final InputStream entityStream) throws IOException {
        if (rawType.isArray()) {
            return (T) mapper.readArray(entityStream, rawType.getComponentType());
        } else if (Collection.class.isAssignableFrom(rawType) && ParameterizedType.class.isInstance(genericType)) {
            return (T) mapper.readCollection(entityStream, ParameterizedType.class.cast(genericType));
        }
        return mapper.readObject(entityStream, genericType);
    }
}
