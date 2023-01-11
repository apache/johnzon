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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.ext.ContextResolver;

import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.johnzon.core.JsonReaderImpl;
import org.junit.Test;

public class JsonbJaxrsProviderTest {
    @Test(expected = NoContentException.class)
    public void noContentExceptionAuto() throws IOException { // we run on jaxrs 2 in the build
        readFoo(null, new ByteArrayInputStream(new byte[0]));
    }

    @Test(expected = NoContentException.class)
    public void noContentException() throws IOException {
        readFoo(true, new ByteArrayInputStream(new byte[0]));
    }

    @Test(expected = JsonReaderImpl.NothingToRead.class)
    public void noContentExceptionDisabled() throws IOException {
        readFoo(false, new ByteArrayInputStream(new byte[0]));
    }

    @Test // just to ensure we didnt break soemthing on read impl
    public void validTest() throws IOException {
        final Foo foo = readFoo(null, new ByteArrayInputStream("{\"name\":\"ok\"}".getBytes(StandardCharsets.UTF_8)));
        assertEquals("ok", foo.name);
    }

    @Test // just to ensure we didnt break soemthing on read impl
    public void jsonpTest() throws Exception {
        JsonObject jsonObject = Json.createObjectBuilder().add("name", "ok").build();
        try (JsonbJaxrsProvider<JsonObject> provider = new JsonbJaxrsProvider<JsonObject>() {{
            setProviders(this);
        }}) {
            assertTrue(provider.isWriteable(jsonObject.getClass(), jsonObject.getClass(), null, MediaType.APPLICATION_JSON_TYPE));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            provider.writeTo(jsonObject, jsonObject.getClass(), jsonObject.getClass(), null, MediaType.APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(), outputStream);
            assertEquals("{\"name\":\"ok\"}", outputStream.toString(StandardCharsets.UTF_8.name()));
        }
    }

    private Foo readFoo(final Boolean set, final InputStream stream) throws IOException {
        return new JsonbJaxrsProvider<Foo>() {{
            if (set != null) {
                setThrowNoContentExceptionOnEmptyStreams(set);
            }
            setProviders(this);
        }}.readFrom(Foo.class, Foo.class, new Annotation[0],
                MediaType.APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(),
                stream);
    }

    private <T> void setProviders(final JsonbJaxrsProvider<T> provider) {
        try {
            final Field providers = JsonbJaxrsProvider.class.getDeclaredField("providers");
            providers.setAccessible(true);
            providers.set(provider, new ProvidersImpl(null) {
                @Override
                public <T> ContextResolver<T> getContextResolver(final Class<T> contextType, final MediaType mediaType) {
                    return null;
                }
            });
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    public static class Foo {
        public String name;
    }
}
