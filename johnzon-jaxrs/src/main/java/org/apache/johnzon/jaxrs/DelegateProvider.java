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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public abstract class DelegateProvider<T> implements MessageBodyWriter<T>, MessageBodyReader<T>  {
    private final MessageBodyReader<T> reader;
    private final MessageBodyWriter<T> writer;

    protected DelegateProvider(final MessageBodyReader<T> reader, final MessageBodyWriter<T> writer) {
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public boolean isReadable(final Class<?> rawType, final Type genericType,
                              final Annotation[] annotations, final MediaType mediaType) {
        return reader.isReadable(rawType, genericType, annotations, mediaType);
    }

    @Override
    public T readFrom(final Class<T> rawType, final Type genericType,
                      final Annotation[] annotations, final MediaType mediaType,
                      final MultivaluedMap<String, String> httpHeaders,
                      final InputStream entityStream) throws IOException {
        return reader.readFrom(rawType, genericType, annotations, mediaType, httpHeaders, entityStream);
    }

    @Override
    public long getSize(final T t, final Class<?> rawType, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType) {
        return writer.getSize(t, rawType, genericType, annotations, mediaType);
    }

    @Override
    public boolean isWriteable(final Class<?> rawType, final Type genericType,
                               final Annotation[] annotations, final MediaType mediaType) {
        return writer.isWriteable(rawType, genericType, annotations, mediaType);
    }

    @Override
    public void writeTo(final T t, final Class<?> rawType, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException {
        writer.writeTo(t, rawType, genericType, annotations, mediaType, httpHeaders, entityStream);
    }
}
