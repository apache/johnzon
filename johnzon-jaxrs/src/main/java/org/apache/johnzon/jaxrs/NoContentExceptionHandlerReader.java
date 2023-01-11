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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.ext.MessageBodyReader;

public class NoContentExceptionHandlerReader<T> implements MessageBodyReader<T> {
    private final MessageBodyReader<T> delegate;

    public NoContentExceptionHandlerReader(final MessageBodyReader<T> delegate) {
        this.delegate = delegate;
    }

    public MessageBodyReader<T> getDelegate() {
        return delegate;
    }

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return delegate.isReadable(type, genericType, annotations, mediaType);
    }

    @Override
    public T readFrom(final Class<T> type, final Type genericType, final Annotation[] annotations,
                      final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
                      final InputStream entityStream) throws IOException, WebApplicationException {
        try {
            return delegate.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
        } catch (final IllegalStateException ise) {
            if (ise.getClass().getName()
                    .equals("org.apache.johnzon.core.JsonReaderImpl$NothingToRead")) {
                // spec enables to return an empty java object but it does not mean anything in JSON context so just fail
                throw new NoContentException(ise);
            }
            throw ise;
        }
    }
}
