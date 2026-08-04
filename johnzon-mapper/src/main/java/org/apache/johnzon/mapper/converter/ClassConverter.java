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
package org.apache.johnzon.mapper.converter;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.johnzon.mapper.Converter;

/**
 * Maps a {@link Class} to/from its name.
 * <p>
 * Deserializing a class name means loading the class the incoming document asks for, so this converter is
 * <strong>not</strong> registered by default and must be registered explicitly - see
 * {@code MapperBuilder#addConverter} - with the set of classes it is allowed to load.
 * Both allow-lists default to empty, ie deserialization always fails until they are configured.
 */
public class ClassConverter implements Converter<Class<?>> {
    private final Collection<String> allowedClasses;
    private final Collection<String> allowedPrefixes;

    /**
     * Serialization only, any incoming class name is rejected.
     */
    public ClassConverter() {
        this(emptyList(), emptyList());
    }

    /**
     * @param allowedClasses the fully qualified names which can be loaded from the incoming document.
     * @param allowedPrefixes the prefixes - generally packages - a class name must start with to be loaded.
     */
    public ClassConverter(final Collection<String> allowedClasses, final Collection<String> allowedPrefixes) {
        this.allowedClasses = copy(allowedClasses);
        this.allowedPrefixes = copy(allowedPrefixes);
    }

    @Override
    public String toString(final Class<?> instance) {
        return instance.getName();
    }

    @Override
    public Class<?> fromString(final String text) {
        if (!isAllowed(text)) {
            throw new IllegalArgumentException("'" + text + "' is not an allowed class");
        }
        try {
            // never initialize, the class must not run its static blocks just because it was named in a document
            return Class.forName(text, false, Thread.currentThread().getContextClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean isAllowed(final String name) {
        return allowedClasses.contains(name) || allowedPrefixes.stream().anyMatch(name::startsWith);
    }

    private static Collection<String> copy(final Collection<String> values) {
        return values == null || values.isEmpty() ? emptyList() : unmodifiableCollection(new ArrayList<>(values));
    }
}
