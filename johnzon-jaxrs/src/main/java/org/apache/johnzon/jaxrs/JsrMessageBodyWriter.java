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

import jakarta.json.Json;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;

import static org.apache.johnzon.mapper.internal.Streams.noClose;

// @Provider // don't let it be scanned, it would conflict with JsrProvider
@Produces({
    "application/json", "*/json",
    "*/*+json", "*/x-json",
    "*/javascript", "*/x-javascript"
})
public class JsrMessageBodyWriter implements MessageBodyWriter<JsonStructure> {
    private final JsonWriterFactory factory;
    private final boolean close;

    public JsrMessageBodyWriter() {
        this(Json.createWriterFactory(Collections.<String, Object>emptyMap()), false);
    }

    public JsrMessageBodyWriter(final JsonWriterFactory factory, final boolean closeStreams) {
        this.factory = factory;
        this.close = closeStreams;
    }

    @Override
    public boolean isWriteable(final Class<?> aClass, final Type type,
                               final Annotation[] annotations, final MediaType mediaType) {
        return JsonStructure.class.isAssignableFrom(aClass);
    }

    @Override
    public long getSize(final JsonStructure jsonStructure, final Class<?> aClass,
                        final Type type, final Annotation[] annotations,
                        final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final JsonStructure jsonStructure,
                        final Class<?> aClass, final Type type,
                        final Annotation[] annotations, final MediaType mediaType,
                        final MultivaluedMap<String, Object> stringObjectMultivaluedMap,
                        final OutputStream outputStream) throws IOException, WebApplicationException {
        JsonWriter writer = null;
        try {
            writer = factory.createWriter(close ? outputStream : noClose(outputStream));
            writer.write(jsonStructure);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
