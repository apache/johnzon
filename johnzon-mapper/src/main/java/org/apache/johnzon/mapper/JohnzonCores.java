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
package org.apache.johnzon.mapper;

import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGeneratorFactory;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.util.Optional.ofNullable;

public final class JohnzonCores {
    private static final Method CREATE_READER;

    static {
        Method m = null;
        try {
            final Class<?> jrfi = ofNullable(JohnzonCores.class.getClassLoader())
                    .orElseGet(ClassLoader::getSystemClassLoader)
                    .loadClass("org.apache.johnzon.core.JsonReaderFactoryImpl");
            m = jrfi.getDeclaredMethod("createReader", JsonParser.class);
            if (!m.isAccessible()) {
                m.setAccessible(true);
            }
        } catch (final Exception e) {
            // no-op
        }
        CREATE_READER = m;
    }

    private JohnzonCores() {
        // no-op
    }

    public static SnippetFactory snippetFactory(final int max, final JsonGeneratorFactory factory) {
        if (CREATE_READER == null) {
            return JsonValue::toString;
        }
        return Snippets.of(max, factory);
    }

    public static JsonReader map(final JsonParser parser, final JsonReaderFactory readerFactory) {
        if (CREATE_READER == null) {
            throw new IllegalStateException("Ensure to use johnzon-core as JSON-P provider for johnzon-mapper");
        }
        try {
            return JsonReader.class.cast(CREATE_READER.invoke(readerFactory, parser));
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    // indirection (for classloading)
    private static class Snippets {
        private Snippets() {
            // no-op
        }

        private static SnippetFactory of(final int max, final JsonGeneratorFactory factory) {
            return new org.apache.johnzon.core.Snippet(max, factory)::of;
        }
    }
}
