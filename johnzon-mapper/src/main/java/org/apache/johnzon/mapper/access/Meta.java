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
package org.apache.johnzon.mapper.access;

import static java.util.Arrays.asList;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class Meta {
    private Meta() {
        // no-op
    }

    public static <T extends Annotation> T getAnnotation(final AnnotatedElement holder, final Class<T> api) {
        return getDirectAnnotation(holder, api);
    }

    public static <T extends Annotation> T getClassOrPackageAnnotation(final Method holder, final Class<T> api) {
        return getIndirectAnnotation(api, holder::getDeclaringClass, () -> holder.getDeclaringClass().getPackage());
    }

    public static <T extends Annotation> T getClassOrPackageAnnotation(final Field holder, final Class<T> api) {
        return getIndirectAnnotation(api, holder::getDeclaringClass, () -> holder.getDeclaringClass().getPackage());
    }

    private static <T extends Annotation> T getDirectAnnotation(final AnnotatedElement holder, final Class<T> api) {
        final T annotation = holder.getAnnotation(api);
        if (annotation != null) {
            return annotation;
        }
        final T meta = findMeta(holder.getAnnotations(), api);
        if (meta != null) {
            return meta;
        }
        return null;
    }

    private static <T extends Annotation> T getIndirectAnnotation(final Class<T> api,
                                                                  final Supplier<Class<?>> ownerSupplier,
                                                                  final Supplier<Package> packageSupplier) {
        final T ownerAnnotation = ownerSupplier.get().getAnnotation(api);
        if (ownerAnnotation != null) {
            return ownerAnnotation;
        } // todo: meta?
        final Package pck = packageSupplier.get();
        if (pck != null) {
            final T pckAnnotation = pck.getAnnotation(api);
            if (pckAnnotation != null) {
                return pckAnnotation;
            }
        } // todo: meta?
        return null;
    }

    public static <T extends Annotation> T getAnnotation(final Class<?> clazz, final Class<T> api) {
        Class<?> current = clazz;
        final Set<Class<?>> visited = new HashSet<>();
        while (current != null && current != Object.class) {
            if (!visited.add(current)) {
                return null;
            }
            final T annotation = current.getAnnotation(api);
            if (annotation != null) {
                return annotation;
            }
            final T meta = findMeta(clazz.getAnnotations(), api);
            if (meta != null) {
                return meta;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    public static <T extends Annotation> T getAnnotation(final Package pck, final Class<T> api) {
        final T annotation = pck.getAnnotation(api);
        if (annotation != null) {
            return annotation;
        }
        return findMeta(pck.getAnnotations(), api);
    }

    public static <T extends Annotation> T findMeta(final Annotation[] annotations, final Class<T> api) {
        for (final Annotation a : annotations) {
            final Class<? extends Annotation> userType = a.annotationType();
            final T aa = userType.getAnnotation(api);
            if (aa != null) {
                boolean overriden = false;
                final Map<String, Method> mapping = new HashMap<String, Method>();
                for (final Class<?> cm : asList(api, userType)) {
                    for (final Method m : cm.getMethods()) {
                        overriden = mapping.put(m.getName(), m) != null || overriden;
                    }
                }
                if (!overriden) {
                    return aa;
                }
                return api.cast(newAnnotation(mapping, a, aa));
            }
        }
        return null;
    }

    private static <T extends Annotation> T newAnnotation(final Map<String, Method> methodMapping, final Annotation user, final T johnzon) {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{johnzon.annotationType()},
                (proxy, method, args) -> {
                    final Method m = methodMapping.get(method.getName());
                    try {
                        if (m.getDeclaringClass() == user.annotationType()) {
                            return m.invoke(user, args);
                        }
                        return m.invoke(johnzon, args);
                    } catch (final InvocationTargetException ite) {
                        throw ite.getTargetException();
                    }
                });
    }
}