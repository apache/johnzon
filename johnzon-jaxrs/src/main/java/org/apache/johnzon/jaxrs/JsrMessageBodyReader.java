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
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonStructure;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;

import static org.apache.johnzon.mapper.internal.Streams.noClose;

// @Provider // don't let it be scanned, it would conflict with JsrProvider
@Consumes({
    "application/json", "*/json",
    "*/*+json", "*/x-json",
    "*/javascript", "*/x-javascript"
})
public class JsrMessageBodyReader implements MessageBodyReader<JsonStructure> {
    private final JsonReaderFactory factory;
    private final boolean closeStream;

    public JsrMessageBodyReader() {
        this(Json.createReaderFactory(Collections.<String, Object>emptyMap()), false);
    }

    public JsrMessageBodyReader(final JsonReaderFactory factory, final boolean closeStream) {
        this.factory = factory;
        this.closeStream = closeStream;
    }

    @Override
    public boolean isReadable(final Class<?> aClass, final Type type,
                              final Annotation[] annotations, final MediaType mediaType) {
        return JsonStructure.class.isAssignableFrom(aClass);
    }

    @Override
    public JsonStructure readFrom(final Class<JsonStructure> jsonStructureClass, final Type type,
                                  final Annotation[] annotations, final MediaType mediaType,
                                  final MultivaluedMap<String, String> stringStringMultivaluedMap,
                                  final InputStream inputStream) throws IOException, WebApplicationException {
        JsonReader reader = null;
        try {
            reader = factory.createReader(closeStream ? inputStream : noClose(inputStream));
            return reader.read();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
