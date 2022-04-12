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

import org.apache.johnzon.core.util.ClassUtil;
import org.apache.johnzon.mapper.Cleanable;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbVisibility;
import javax.json.bind.config.PropertyVisibilityStrategy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Optional.ofNullable;

class DefaultPropertyVisibilityStrategy implements javax.json.bind.config.PropertyVisibilityStrategy, Cleanable<Class<?>> {
    private final ConcurrentMap<Class<?>, PropertyVisibilityStrategy> strategies = new ConcurrentHashMap<>();

    private volatile boolean skipGetpackage;

    @Override
    public boolean isVisible(final Field field) {
        return isVisible(field, field.getDeclaringClass(), true);
    }

    public boolean isVisible(final Field field, final Class<?> root, final boolean useGetter) {
        if (field.getAnnotation(JsonbProperty.class) != null) {
            return true;
        }
        final PropertyVisibilityStrategy strategy = strategies.computeIfAbsent(root, this::visibilityStrategy);
        return strategy == this ? isFieldVisible(field, root, useGetter) : strategy.isVisible(field);
    }

    private boolean isFieldVisible(final Field field, final Class<?> root, final boolean useGetter) {
        if (!Modifier.isPublic(field.getModifiers())) {
            return false;
        }
        // 3.7.1. Scope and Field access strategy
        // note: we should bind the class since a field of a parent class can have a getter in a child
        if (!useGetter) {
            return !hasMethod(root, ClassUtil.setterName(field.getName()));
        }
        final String capitalizedName = ClassUtil.capitalizeName(field.getName());
        return !hasMethod(root, "get" + capitalizedName) ||  hasMethod(root, "is" + capitalizedName);
    }

    private boolean hasMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            clazz.getDeclaredMethod(methodName, paramTypes);
            return true;
        } catch (NoSuchMethodException e) {
            final Class<?> superclass = clazz.getSuperclass();
            if (superclass == Object.class) {
                return false;
            }
            return hasMethod(superclass, methodName, paramTypes);
        }
    }

    /**
     * Calculate all the getters of the given class.
     */
    private Map<String, Boolean> calculateGetters(final Class<?> clazz) {
        final Map<String, Boolean> getters = new HashMap<>();
        for (final Method m : clazz.getDeclaredMethods()) {
            if (m.getParameterCount() > 0) {
                continue;
            }
            if (m.getName().startsWith("get") && m.getName().length() > 3) {
                getters.put(
                        Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4),
                        Modifier.isPublic(m.getModifiers()));
            } else if (m.getName().startsWith("is") && m.getName().length() > 2) {
                getters.put(
                        Character.toLowerCase(m.getName().charAt(2)) + m.getName().substring(3),
                        Modifier.isPublic(m.getModifiers()));
            }
        }
        final Class<?> superclass = clazz.getSuperclass();
        if (superclass != Object.class && superclass != null && !"java.lang.Record".equals(superclass.getName())) {
            calculateGetters(superclass).forEach(getters::putIfAbsent); // don't override child getter if exists
        }
        return getters.isEmpty() ? Collections.emptyMap() : getters;
    }

    private Map<String, Boolean> calculateSetters(final Class<?> clazz) {
        final Map<String, Boolean> result = new HashMap<>();
        for (final Method m : clazz.getDeclaredMethods()) {
            if (m.getParameterCount() != 1) {
                continue;
            }
            if (m.getName().startsWith("set") && m.getName().length() > 3) {
                result.put(
                        Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4),
                        Modifier.isPublic(m.getModifiers()));
            }
        }
        if (clazz.getSuperclass() != Object.class) {
            calculateSetters(clazz.getSuperclass()).forEach(result::putIfAbsent);
        }
        return result.isEmpty() ? Collections.emptyMap() : result;
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
                    p = ofNullable(type.getClassLoader())
                            .orElseGet(ClassLoader::getSystemClassLoader)
                             .loadClass(parentPack + ".package-info").getPackage();
                } catch (final ClassNotFoundException e) {
                    // no-op
                }
            }
        }
        return this;
    }

    @Override
    public void clean(final Class<?> clazz) {
        strategies.remove(clazz);
    }
}
