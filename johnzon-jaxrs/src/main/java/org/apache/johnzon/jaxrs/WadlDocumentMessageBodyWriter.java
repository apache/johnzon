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

import org.apache.johnzon.jaxrs.xml.WadlDocumentToJson;
import org.w3c.dom.Document;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Produces({
    "application/json", "*/json",
    "*/*+json", "*/x-json",
    "*/javascript", "*/x-javascript"
})
public class WadlDocumentMessageBodyWriter implements MessageBodyWriter<Document> {
    private final WadlDocumentToJson converter = new WadlDocumentToJson();

    @Override
    public boolean isWriteable(final Class<?> aClass, final Type type,
                               final Annotation[] annotations, final MediaType mediaType) {
        return Document.class.isAssignableFrom(aClass);
    }

    @Override
    public long getSize(final Document document, final Class<?> aClass,
                        final Type type, final Annotation[] annotations,
                        final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final Document document, final Class<?> aClass,
                        final Type type, final Annotation[] annotations,
                        final MediaType mediaType, final MultivaluedMap<String, Object> stringObjectMultivaluedMap,
                        final OutputStream outputStream) throws IOException, WebApplicationException {
        try {
            outputStream.write(converter.convert(document).getBytes());
        } catch (final XMLStreamException e) {
            throw new IllegalStateException(e);
        }
    }
}
