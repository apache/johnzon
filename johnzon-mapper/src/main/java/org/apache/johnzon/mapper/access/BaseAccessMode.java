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
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.johnzon.mapper.reflection.Records.isRecord;
import static org.apache.johnzon.mapper.reflection.Converters.matches;

import java.beans.ConstructorProperties;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.JohnzonAny;
import org.apache.johnzon.mapper.JohnzonConverter;
import org.apache.johnzon.mapper.JohnzonProperty;
import org.apache.johnzon.mapper.JohnzonRecord;
import org.apache.johnzon.mapper.MapperConverter;
import org.apache.johnzon.mapper.ObjectConverter;
import org.apache.johnzon.mapper.internal.ConverterAdapter;

// handle some specific types
public abstract class BaseAccessMode implements AccessMode {
    private static final Type[] NO_PARAMS = new Type[0];

    private FieldFilteringStrategy fieldFilteringStrategy = new SingleEntryFieldFilteringStrategy();

    private final boolean acceptHiddenConstructor;
    private final boolean useConstructor;

    protected BaseAccessMode(final boolean useConstructor, final boolean acceptHiddenConstructor) {
        this.useConstructor = useConstructor;
        this.acceptHiddenConstructor = acceptHiddenConstructor;
    }

    protected abstract Map<String,Reader> doFindReaders(Class<?> clazz);
    protected abstract Map<String,Writer> doFindWriters(Class<?> clazz);

    @Override
    public Comparator<String> fieldComparator(final Class<?> clazz) {
        return null;
    }

    @Override
    public Map<String, Reader> findReaders(final Class<?> clazz) {
        return sanitize(clazz, doFindReaders(clazz));
    }

    @Override
    public Map<String, Writer> findWriters(final Class<?> clazz) {
        return sanitize(clazz, doFindWriters(clazz));
    }

    public void setFieldFilteringStrategy(final FieldFilteringStrategy fieldFilteringStrategy) {
        this.fieldFilteringStrategy = fieldFilteringStrategy;
    }

    public FieldFilteringStrategy getFieldFilteringStrategy() {
        return fieldFilteringStrategy;
    }

    @Override
    public ObjectConverter.Reader<?> findReader(final Class<?> clazz) {
        return null; // TODO: converter?
    }

    @Override
    public ObjectConverter.Writer<?> findWriter(final Class<?> clazz) {
        return null; // TODO: converter?
    }

    @Override
    public Adapter<?, ?> findAdapter(Class<?> clazz) {
        return null; // TODO: converter?
    }

    @Override
    public void afterParsed(final Class<?> clazz) {
        // no-op
    }

    @Override
    public Factory findFactory(final Class<?> clazz, final Function<AnnotatedElement, String>... parameterNameExtractors) {
        Constructor<?> constructor = null;
        final boolean record = isRecord(clazz);
        if (record || Meta.getAnnotation(clazz, JohnzonRecord.class) != null) {
            constructor = findRecordConstructor(clazz);
        } else {
            for (final Constructor<?> c : clazz.getDeclaredConstructors()) {
                if (c.getParameterTypes().length == 0) {
                    if (!Modifier.isPublic(c.getModifiers()) && acceptHiddenConstructor) {
                        c.setAccessible(true);
                    }
                    constructor = c;
                    if (!useConstructor) {
                        break;
                    }
                } else if (c.getAnnotation(ConstructorProperties.class) != null) {
                    constructor = c;
                    break;
                }
            }
            if (constructor == null) {
                try {
                    constructor = clazz.getConstructor();
                } catch (final NoSuchMethodException e) {
                    return null; // readOnly class
                }
            }
        }

        final boolean constructorHasArguments = constructor != null && constructor.getGenericParameterTypes().length > 0;
        final Type[] factoryParameterTypes;
        final String[] constructorParameters;
        final Adapter<?, ?>[] constructorParameterConverters;
        final Adapter<?, ?>[] constructorItemParameterConverters;
        final ObjectConverter.Codec<?>[] objectConverters;
        if (constructorHasArguments) {
            factoryParameterTypes = constructor.getGenericParameterTypes();

            constructorParameters = new String[constructor.getGenericParameterTypes().length];

            final Constructor<?> fc = constructor;
            final String[] constructorProperties = ofNullable(constructor.getAnnotation(ConstructorProperties.class))
                    .map(ConstructorProperties::value)
                    .orElseGet(() -> {
                        if (record) {
                            return Stream.of(fc.getParameters())
                                    .map(p -> {
                                        try {
                                            if (parameterNameExtractors != null) {
                                                return Stream.of(parameterNameExtractors)
                                                        .map(fn -> fn.apply(p))
                                                        .filter(Objects::nonNull)
                                                        .findFirst()
                                                        .orElseGet(p::getName);
                                            }
                                            final JohnzonProperty property = Meta.getAnnotation(
                                                    clazz.getMethod(p.getName()), JohnzonProperty.class);
                                            return property != null ? property.value() : p.getName();
                                        } catch (final NoSuchMethodException e) {
                                            return p.getName();
                                        }
                                    })
                                    .toArray(String[]::new);
                        }
                        return Stream.of(fc.getParameters())
                                .map(p -> ofNullable(p.getAnnotation(JohnzonRecord.Name.class))
                                        .map(JohnzonRecord.Name::value)
                                        .orElseGet(p::getName))
                                .toArray(String[]::new);
                    });
            System.arraycopy(constructorProperties, 0, constructorParameters, 0, constructorParameters.length);

            constructorParameterConverters = new Adapter<?, ?>[constructor.getGenericParameterTypes().length];
            constructorItemParameterConverters = new Adapter<?, ?>[constructorParameterConverters.length];
            objectConverters = new ObjectConverter.Codec[constructorParameterConverters.length];
            for (int i = 0; i < constructorParameters.length; i++) {
                for (final Annotation a : constructor.getParameterAnnotations()[i]) {
                    if (a.annotationType() == JohnzonConverter.class) {
                        try {
                            MapperConverter mapperConverter = JohnzonConverter.class.cast(a).value().newInstance();
                            if (mapperConverter instanceof Converter) {
                                final Adapter<?, ?> converter = new ConverterAdapter((Converter) mapperConverter, constructor.getGenericParameterTypes()[i]);
                                if (matches(constructor.getParameterTypes()[i], converter)) {
                                    constructorParameterConverters[i] = converter;
                                    constructorItemParameterConverters[i] = null;
                                } else {
                                    constructorParameterConverters[i] = null;
                                    constructorItemParameterConverters[i] = converter;
                                }
                            } else {
                                objectConverters[i] = (ObjectConverter.Codec<?>) mapperConverter;
                            }
                        } catch (final Exception e) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                }
            }
        } else {
            factoryParameterTypes = NO_PARAMS;
            constructorParameters = null;
            constructorParameterConverters = null;
            constructorItemParameterConverters = null;
            objectConverters = null;
        }

        final Constructor<?> cons = constructor;
        if (cons != null && !cons.isAccessible()) {
            cons.setAccessible(true);
        }
        return new Factory() {
            @Override
            public Object create(final Object[] params) {
                if (cons == null) {
                    throw new IllegalArgumentException(clazz.getName() + " can't be instantiated by Johnzon, this is a write only class");
                }
                try {
                    return params == null ? cons.newInstance() : cons.newInstance(params);
                } catch (final InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException(e.getCause());
                }
            }

            @Override
            public Type[] getParameterTypes() {
                return factoryParameterTypes;
            }

            @Override
            public String[] getParameterNames() {
                return constructorParameters;
            }

            @Override
            public Adapter<?, ?>[] getParameterConverter() {
                return constructorParameterConverters;
            }

            @Override
            public Adapter<?, ?>[] getParameterItemConverter() {
                return constructorItemParameterConverters;
            }

            @Override
            public ObjectConverter.Codec<?>[] getObjectConverter() {
                return objectConverters;
            }
        };
    }

    private Constructor<?> findRecordConstructor(final Class<?> clazz) {
        return Stream.of(clazz.getDeclaredConstructors())
                .max(comparing(Constructor::getParameterCount))
                .map(c -> {
                    if (!c.isAccessible()) {
                        c.setAccessible(true);
                    }
                    return c;
                })
                .orElse(null);
    }

    @Override
    public Method findAnyGetter(final Class<?> clazz) {
        Method m = null;
        for (final Method current : clazz.getMethods()) {
            if (current.getAnnotation(JohnzonAny.class) != null) {
                if (current.getParameterTypes().length == 0) {
                    if (!Map.class.isAssignableFrom(current.getReturnType())) {
                        throw new IllegalArgumentException("@JohnzonAny getters can only return a Map<String, Object>");
                    }
                    if (m != null) {
                        throw new IllegalArgumentException("Ambiguous @JohnzonAny on " + m + " and " + current);
                    }
                    m = current;
                }
            }
        }
        return m;
    }

    @Override
    public Method findAnySetter(final Class<?> clazz) {
        Method m = null;
        for (final Method current : clazz.getMethods()) {
            if (current.getAnnotation(JohnzonAny.class) != null) {
                final Class<?>[] parameterTypes = current.getParameterTypes();
                if (parameterTypes.length == 2 && parameterTypes[0] == String.class) {
                    if (m != null) {
                        throw new IllegalArgumentException("Ambiguous @JohnzonAny on " + m + " and " + current);
                    }
                    m = current;
                }
            }
        }
        return m;
    }

    @Override
    public Field findAnyField(final Class<?> clazz) {
        if (clazz.isInterface() || clazz.isEnum()) {
            return null;
        }
        Class<?> current = clazz;
        final Set<Class<?>> visited = new HashSet<>();
        while (current != null && current != Object.class && visited.add(current)) {
            for (final Field f : current.getDeclaredFields()) {
                if (f.isAnnotationPresent(JohnzonAny.class)) { // todo: validation? waiting for jsonb standard behavior
                    return f;
                }
            }
            current = clazz.getSuperclass();
        }
        return null;
    }

    private <T> Map<String, T> sanitize(final Class<?> type, final Map<String, T> delegate) {
        for (final String field : fieldFilteringStrategy.select(type)) {
            delegate.remove(field);
        }
        return delegate;
    }

    public interface FieldFilteringStrategy {
        Collection<String> select(final Class<?> type);
    }

    public static abstract class ConfiguredFieldFilteringStrategy implements FieldFilteringStrategy {
        private final Map<Class<?>, Collection<String>> fieldsToRemove = new LinkedHashMap<>();

        public ConfiguredFieldFilteringStrategy() {
            // mainly built it in the JVM types == user cant handle them
            fieldsToRemove.put(Throwable.class, asList("suppressedExceptions", "cause"));
        }

        public Map<Class<?>, Collection<String>> getFieldsToRemove() {
            return fieldsToRemove;
        }
    }

    public static class SingleEntryFieldFilteringStrategy extends ConfiguredFieldFilteringStrategy {
        @Override
        public Collection<String> select(final Class<?> type) {
            for (final Map.Entry<Class<?>, Collection<String>> entry : getFieldsToRemove().entrySet()) {
                if (entry.getKey().isAssignableFrom(type)) {
                    return entry.getValue();
                }
            }
            return emptySet();
        }
    }

    public static class AllEntriesFieldFilteringStrategy extends ConfiguredFieldFilteringStrategy {
        @Override
        public Collection<String> select(final Class<?> type) {
            return getFieldsToRemove().entrySet().stream()
                    .filter(entry -> entry.getKey().isAssignableFrom(type))
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(toSet());
        }
    }
}
