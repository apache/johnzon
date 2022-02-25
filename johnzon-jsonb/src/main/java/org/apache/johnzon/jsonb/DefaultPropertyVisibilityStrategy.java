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
package org.apache.johnzon.jsonb;

import static java.util.Optional.ofNullable;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbVisibility;
import javax.json.bind.config.PropertyVisibilityStrategy;

class DefaultPropertyVisibilityStrategy implements javax.json.bind.config.PropertyVisibilityStrategy {
    private final ConcurrentMap<Class<?>, PropertyVisibilityStrategy> strategies = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, List<String>> getters = new ConcurrentHashMap<>();

    private volatile boolean skipGetpackage;

    @Override
    public boolean isVisible(final Field field) {
        if (field.getAnnotation(JsonbProperty.class) != null) {
            return true;
        }
        final PropertyVisibilityStrategy strategy = strategies.computeIfAbsent(
                field.getDeclaringClass(), this::visibilityStrategy);
        return strategy == this ? isFieldVisible(field) : strategy.isVisible(field);
    }

    private boolean isFieldVisible(Field field) {
        if (!Modifier.isPublic(field.getModifiers())) {
            return false;
        }
        // also check if there is any setter, in which case the field should be treated as non-visible as well.
        return !getters.computeIfAbsent(field.getDeclaringClass(), this::calculateGetters).contains(field.getName());
    }

    /**
     * Calculate all the getters of the given class.
     */
    private List<String> calculateGetters(Class<?> clazz) {
        List<String> getters = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getParameterCount() == 0) {
                if (m.getName().startsWith("get")) {
                    getters.add(Introspector.decapitalize(m.getName().substring(3)));
                } else if (m.getName().startsWith("is")) {
                    getters.add(Introspector.decapitalize(m.getName().substring(2)));
                }
            }
        }
        if (clazz.getSuperclass() != Object.class) {
            getters.addAll(calculateGetters(clazz.getSuperclass()));
        }
        return getters.isEmpty() ? Collections.emptyList() : getters;
    }

    @Override
    public boolean isVisible(final Method method) {
        final PropertyVisibilityStrategy strategy = strategies.computeIfAbsent(
                method.getDeclaringClass(), this::visibilityStrategy);
        return strategy == this ? Modifier.isPublic(method.getModifiers()) : strategy.isVisible(method);
    }

    private PropertyVisibilityStrategy visibilityStrategy(final Class<?> type) { // can be cached
        JsonbVisibility visibility = type.getAnnotation(JsonbVisibility.class);
        if (visibility != null) {
            try {
                return visibility.value().newInstance();
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }
        Package p = type.getPackage();
        while (p != null) {
            visibility = p.getAnnotation(JsonbVisibility.class);
            if (visibility != null) {
                try {
                    return visibility.value().newInstance();
                } catch (final InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            final String name = p.getName();
            final int end = name.lastIndexOf('.');
            if (end < 0) {
                break;
            }
            p = null;
            final String parentPack = name.substring(0, end);
            if (!skipGetpackage) {
                try {
                    p = Package.getPackage(parentPack);
                } catch (final Error unsupported) {
                    skipGetpackage = true; // graalvm likely
                }
            }
            if (p == null) {
                try {
                    p = ofNullable(type.getClassLoader()).orElseGet(ClassLoader::getSystemClassLoader)
                                                         .loadClass(parentPack + ".package-info").getPackage();
                } catch (final ClassNotFoundException e) {
                    // no-op
                }
            }
        }
        return this;
    }
}
