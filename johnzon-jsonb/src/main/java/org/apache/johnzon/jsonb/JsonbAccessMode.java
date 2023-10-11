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

import org.apache.johnzon.jsonb.converter.JohnzonJsonbAdapter;
import org.apache.johnzon.jsonb.converter.JsonbDateConverter;
import org.apache.johnzon.jsonb.converter.JsonbLocalDateConverter;
import org.apache.johnzon.jsonb.converter.JsonbLocalDateTimeConverter;
import org.apache.johnzon.jsonb.converter.JsonbNumberConverter;
import org.apache.johnzon.jsonb.converter.JsonbOffsetDateTimeConverter;
import org.apache.johnzon.jsonb.converter.JsonbValueConverter;
import org.apache.johnzon.jsonb.converter.JsonbZonedDateTimeConverter;
import org.apache.johnzon.jsonb.order.PerHierarchyAndLexicographicalOrderFieldComparator;
import org.apache.johnzon.jsonb.reflect.GenericArrayTypeImpl;
import org.apache.johnzon.jsonb.reflect.Types;
import org.apache.johnzon.jsonb.serializer.JohnzonDeserializationContext;
import org.apache.johnzon.jsonb.serializer.JohnzonSerializationContext;
import org.apache.johnzon.jsonb.spi.JohnzonAdapterFactory;
import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.Cleanable;
import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.JohnzonAny;
import org.apache.johnzon.mapper.JohnzonConverter;
import org.apache.johnzon.mapper.JohnzonRecord;
import org.apache.johnzon.mapper.MapperConverter;
import org.apache.johnzon.mapper.MappingGenerator;
import org.apache.johnzon.mapper.MappingParser;
import org.apache.johnzon.mapper.ObjectConverter;
import org.apache.johnzon.mapper.TypeAwareAdapter;
import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.access.BaseAccessMode;
import org.apache.johnzon.mapper.access.FieldAccessMode;
import org.apache.johnzon.mapper.access.FieldAndMethodAccessMode;
import org.apache.johnzon.mapper.access.Meta;
import org.apache.johnzon.mapper.access.MethodAccessMode;
import org.apache.johnzon.mapper.converter.ReversedAdapter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonValue;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbNumberFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.config.PropertyNamingStrategy;
import jakarta.json.bind.config.PropertyOrderStrategy;
import jakarta.json.bind.config.PropertyVisibilityStrategy;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParserFactory;
import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.johnzon.mapper.reflection.Converters.matches;
import static org.apache.johnzon.mapper.reflection.Records.isRecord;

public class JsonbAccessMode implements AccessMode, Closeable, Cleanable<Class<?>> {
    private final PropertyNamingStrategy naming;
    private final String order;
    private final PropertyVisibilityStrategy visibility;
    private final AccessMode delegate;
    private final boolean caseSensitive;
    private final Map<AdapterKey, Adapter<?, ?>> defaultConverters;
    private final JohnzonAdapterFactory factory;
    private final Collection<JohnzonAdapterFactory.Instance<?>> toRelease = new ArrayList<>();
    private final JsonProvider jsonProvider;
    private final Supplier<JsonParserFactory> parserFactory;
    private final Supplier<JsonBuilderFactory> builderFactory;
    private final ConcurrentMap<Class<?>, ParsingCacheEntry> parsingCache = new ConcurrentHashMap<>();
    private final BaseAccessMode partialDelegate = new BaseAccessMode(false, false) {
        @Override
        protected Map<String, Reader> doFindReaders(Class<?> clazz) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Map<String, Writer> doFindWriters(Class<?> clazz) {
            throw new UnsupportedOperationException();
        }
    };
    private boolean failOnMissingCreatorValues;
    private final Types types = new Types();
    private final boolean globalIsNillable;
    private final boolean supportsPrivateAccess;

    // CHECKSTYLE:OFF
    public JsonbAccessMode(final PropertyNamingStrategy propertyNamingStrategy, final String orderValue,
                           final PropertyVisibilityStrategy visibilityStrategy, final boolean caseSensitive,
                           final Map<AdapterKey, Adapter<?, ?>> defaultConverters, final JohnzonAdapterFactory factory,
                           final JsonProvider jsonProvider, final Supplier<JsonBuilderFactory> builderFactory,
                           final Supplier<JsonParserFactory> parserFactory,
                           final AccessMode delegate,
                           final boolean failOnMissingCreatorValues,
                           final boolean globalIsNillable,
                           final boolean supportsPrivateAccess) {
        // CHECKSTYLE:ON
        this.globalIsNillable = globalIsNillable;
        this.naming = propertyNamingStrategy;
        this.order = orderValue;
        this.visibility = visibilityStrategy;
        this.caseSensitive = caseSensitive;
        this.delegate = delegate;
        this.defaultConverters = defaultConverters;
        this.factory = factory;
        this.builderFactory = builderFactory;
        this.jsonProvider = jsonProvider;
        this.parserFactory = parserFactory;
        this.failOnMissingCreatorValues = failOnMissingCreatorValues;
        this.supportsPrivateAccess = supportsPrivateAccess;
    }

    @Override
    public Comparator<String> fieldComparator(final Class<?> clazz) {
        final Comparator<String> orderComparator = orderComparator(clazz);
        return caseSensitive ? orderComparator : ((o1, o2) -> o1.equalsIgnoreCase(o2) ? 0 : orderComparator.compare(o1, o2));
    }

    @Override
    public Factory findFactory(final Class<?> clazz, final Function<AnnotatedElement, String>... parameterNameExtractors) {
        Constructor<?> constructor = null;
        Method factory = null;
        boolean invalidConstructorForDeserialization = false;
        for (final Constructor<?> c : supportsPrivateAccess ? clazz.getDeclaredConstructors() : clazz.getConstructors()) {
            if (c.isAnnotationPresent(JsonbCreator.class)) {
                if (constructor != null) {
                    throw new JsonbException("Only one constructor or method can have @JsonbCreator");
                }
                if (!c.isAccessible()) {
                    c.setAccessible(true);
                }
                constructor = c;
            }
        }
        for (final Method m : findPotentialFactoryMethods(clazz).collect(toList())) {
            final int modifiers = m.getModifiers();
            if ((!supportsPrivateAccess && !Modifier.isPublic(modifiers)) || !m.isAnnotationPresent(JsonbCreator.class)) {
                continue;
            }
            if (constructor != null || factory != null) {
                throw new JsonbException("Only one constructor or method can have @JsonbCreator");
            }
            if (!m.isAccessible()) {
                m.setAccessible(true);
            }
            factory = m;
        }
        final boolean record = isRecord(clazz) || Meta.getAnnotation(clazz, JohnzonRecord.class) != null;
        if (constructor == null && record) {
            constructor = findRecordConstructor(clazz).orElse(null);
        }
        if (constructor == null && factory == null) {
            invalidConstructorForDeserialization = Stream.of(clazz.getDeclaredConstructors())
                    .anyMatch(it -> it.getParameterCount() == 0 &&
                            !(Modifier.isPublic(it.getModifiers()) || Modifier.isProtected(it.getModifiers())));
        }
        final Constructor<?> finalConstructor = constructor;
        final Method finalFactory = factory;
        final Consumer<Object[]> factoryValidator = failOnMissingCreatorValues ?
                args -> {
                    if (args == null || Stream.of(args).anyMatch(Objects::isNull)) {
                        throw new JsonbException("Missing @JsonbCreator argument");
                    }
                } :
                args -> {
                };
        final Type[] types;
        final String[] params;
        final Adapter<?, ?>[] converters;
        final Adapter<?, ?>[] itemConverters;
        final ObjectConverter.Codec<?>[] objectConverters;
        if (finalConstructor != null || finalFactory != null) {
            types = finalConstructor != null ? finalConstructor.getGenericParameterTypes() : finalFactory.getGenericParameterTypes();
            params = new String[types.length];
            converters = new Adapter<?, ?>[types.length];
            itemConverters = new Adapter<?, ?>[types.length];
            objectConverters = new ObjectConverter.Codec<?>[types.length];

            int i = 0;
            for (final Parameter parameter : (finalConstructor == null ? finalFactory : finalConstructor).getParameters()) {
                final JsonbProperty property = getAnnotation(parameter, JsonbProperty.class);
                params[i] = property != null && !property.value().isEmpty() ?
                        property.value() :
                        (record ?
                                ofNullable(parameter.getAnnotation(JohnzonRecord.Name.class))
                                        .map(JohnzonRecord.Name::value)
                                        .orElseGet(() -> naming.translateName(parameter.getName())) :
                                naming.translateName(parameter.getName()));

                final JsonbTypeAdapter adapter = getAnnotation(parameter, JsonbTypeAdapter.class);
                final JsonbDateFormat dateFormat = getAnnotation(parameter, JsonbDateFormat.class);
                final JsonbNumberFormat numberFormat = getAnnotation(parameter, JsonbNumberFormat.class);
                final JohnzonConverter johnzonConverter = getAnnotation(parameter, JohnzonConverter.class);
                final JsonbTypeDeserializer deserializer = getAnnotation(parameter, JsonbTypeDeserializer.class);
                if (adapter == null && dateFormat == null && numberFormat == null && johnzonConverter == null && deserializer == null) {
                    converters[i] = defaultConverters.get(parameter.getType());
                    itemConverters[i] = null;
                } else {
                    validateAnnotations(parameter, adapter, dateFormat, numberFormat, johnzonConverter);

                    try {
                        if (adapter != null || dateFormat != null || numberFormat != null) {
                            final Adapter converter = toConverter(
                                    this.types, parameter.getType(), adapter, dateFormat, numberFormat);
                            if (matches(parameter.getParameterizedType(), converter)) {
                                converters[i] = converter;
                                itemConverters[i] = null;
                            } else {
                                converters[i] = null;
                                itemConverters[i] = converter;
                            }
                        } else if (johnzonConverter != null) {
                            objectConverters[i] = (ObjectConverter.Codec<?>) johnzonConverter.value().newInstance();

                        } else if (deserializer != null) {
                            final Class<? extends JsonbDeserializer> value = deserializer.value();
                            final JohnzonAdapterFactory.Instance<? extends JsonbDeserializer> instance = newInstance(value);
                            final ParameterizedType pt = this.types.findParameterizedType(value, JsonbDeserializer.class);
                            final Class<?> mappedType = this.types.findParamType(pt, JsonbDeserializer.class);
                            toRelease.add(instance);
                            final JsonBuilderFactory builderFactoryInstance = builderFactory.get();
                            final Type[] arguments = this.types.findParameterizedType(value, JsonbDeserializer.class).getActualTypeArguments();
                            final boolean global = arguments.length == 1 && arguments[0] != null && arguments[0].equals(parameter.getType());
                            objectConverters[i] = new ObjectConverter.Codec() {
                                private final ConcurrentMap<Type, BiFunction<JsonValue, MappingParser, Object>> impl =
                                    new ConcurrentHashMap<>();
                                @Override
                                public Object fromJson(final JsonValue value, final Type targetType, final MappingParser parser) {
                                    final JsonbDeserializer jsonbDeserializer = instance.getValue();
                                    if (global || targetType == mappedType) { // fast test and matches most cases
                                        return mapItem(value, targetType, parser, jsonbDeserializer);
                                    }

                                    BiFunction<JsonValue, MappingParser, Object> fn = impl.get(targetType);
                                    if (fn == null) {
                                        if (value.getValueType() == JsonValue.ValueType.ARRAY) {
                                            if (ParameterizedType.class.isInstance(targetType)) {
                                                final ParameterizedType parameterizedType = ParameterizedType.class.cast(targetType);
                                                final Class<?> paramType = JsonbAccessMode.this.types.findParamType(parameterizedType, Collection.class);
                                                if (paramType != null && (mappedType == null /*Object*/ || mappedType.isAssignableFrom(paramType))) {
                                                    final Collector<Object, ?, ? extends Collection<Object>> collector =
                                                        Set.class.isAssignableFrom(
                                                            JsonbAccessMode.this.types.asClass(parameterizedType.getRawType())) ? toSet() : toList();
                                                    fn = (json, mp) -> json.asJsonArray().stream()
                                                                           .map(i -> mapItem(i, paramType, mp, jsonbDeserializer))
                                                                           .collect(collector);
                                                }
                                            }
                                        }
                                        if (fn == null) {
                                            fn = (json, mp) -> mapItem(json, targetType, mp, jsonbDeserializer);
                                        }
                                        impl.putIfAbsent(targetType, fn);
                                    }
                                    return fn.apply(value, parser);
                                }

                                private Object mapItem(final JsonValue jsonValue, final Type targetType,
                                                       final MappingParser parser, final JsonbDeserializer jsonbDeserializer) {
                                    return jsonbDeserializer.deserialize(
                                        JsonValueParserAdapter.createFor(jsonValue, parserFactory),
                                        new JohnzonDeserializationContext(parser, builderFactoryInstance, jsonProvider),
                                        targetType);
                                }

                                @Override
                                public void writeJson(final Object instance, final MappingGenerator jsonbGenerator) {
                                    // no-op, it's for factories only
                                }
                            };
                        }
                    } catch (final InstantiationException | IllegalAccessException e) {
                        throw new IllegalArgumentException(e);
                    }

                }
                i++;
            }
        } else {
            types = null;
            params = null;
            converters = null;
            itemConverters = null;
            objectConverters = null;
        }

        if (constructor == null && factory == null && !invalidConstructorForDeserialization) {
            final Stream<Function<AnnotatedElement, String>> jsonbFn = Stream.of(this::getJsonbProperty);
            final Factory delegateFactory = delegate.findFactory(
                    clazz,
                    (parameterNameExtractors == null ?
                            jsonbFn : Stream.concat(jsonbFn, Stream.of(parameterNameExtractors))).toArray(Function[]::new));
            return delegateFactory;
        }
        if (constructor != null || invalidConstructorForDeserialization) {
            return constructorFactory(finalConstructor, invalidConstructorForDeserialization ? (Consumer<Object[]>) objects -> {
                throw new JsonbException("No available constructor");
            } : factoryValidator, types, params, converters, itemConverters, objectConverters);
        }
        return methodFactory(clazz, finalFactory, factoryValidator, types, params, converters, itemConverters, objectConverters);
    }

    private Optional<Constructor<?>> findRecordConstructor(final Class<?> clazz) {
        return Stream.of(clazz.getDeclaredConstructors())
                .max(comparing(Constructor::getParameterCount))
                .map(c -> {
                    if (!c.isAccessible()) {
                        c.setAccessible(true);
                    }
                    return c;
                });
    }

    private String getJsonbProperty(final AnnotatedElement a) {
        final JsonbProperty p = Meta.getAnnotation(a, JsonbProperty.class);
        return p != null ? p.value() : null;
    }

    private Stream<Method> findPotentialFactoryMethods(final Class<?> clazz) {
        return (!supportsPrivateAccess ?
                Stream.of(clazz.getMethods()) :
                Stream.concat(
                        Stream.of(clazz.getDeclaredMethods()),
                        clazz.getSuperclass() == null || clazz.getSuperclass() == Object.class || clazz.getSuperclass() == clazz ?
                                Stream.empty() :
                                findPotentialFactoryMethods(clazz.getSuperclass())));
    }

    private Factory methodFactory(final Class<?> clazz, final Method finalFactory,
                                  final Consumer<Object[]> factoryValidator, final Type[] types,
                                  final String[] params, final Adapter<?, ?>[] converters,
                                  final Adapter<?, ?>[] itemConverters, final ObjectConverter.Codec<?>[] objectConverters) {
        final Object instance = Modifier.isStatic(finalFactory.getModifiers()) ?
                null : tryToCreateInstance(finalFactory.getDeclaringClass());
        return new Factory() {
            @Override
            public Object create(final Object[] params) {
                factoryValidator.accept(params);
                try {
                    final Object invoke = finalFactory.invoke(instance, params);
                    if (!clazz.isInstance(invoke)) {
                        throw new IllegalArgumentException(invoke + " is not a " + clazz.getName());
                    }
                    return invoke;
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException(e.getCause());
                }
            }

            @Override
            public Type[] getParameterTypes() {
                return types;
            }

            @Override
            public String[] getParameterNames() {
                return params;
            }

            @Override
            public Adapter<?, ?>[] getParameterConverter() {
                return converters;
            }

            @Override
            public Adapter<?, ?>[] getParameterItemConverter() {
                return itemConverters;
            }

            @Override
            public ObjectConverter.Codec<?>[] getObjectConverter() {
                return objectConverters;
            }
        };
    }

    private Object tryToCreateInstance(final Class<?> declaringClass) {
        try {
            final Constructor<?> declaredConstructor = declaringClass.getDeclaredConstructor();
            if (!declaredConstructor.isAccessible()) {
                declaredConstructor.setAccessible(true);
            }
            return declaredConstructor.newInstance();
        } catch (final Exception e) {
            return null;
        }
    }

    private Factory constructorFactory(final Constructor<?> finalConstructor, final Consumer<Object[]> factoryValidator,
                                       final Type[] types, final String[] params, final Adapter<?, ?>[] converters,
                                       final Adapter<?, ?>[] itemConverters, final ObjectConverter.Codec<?>[] objectConverters) {
        return new Factory() {
            @Override
            public Object create(final Object[] params) {
                factoryValidator.accept(params);
                try {
                    return finalConstructor.newInstance(params);
                } catch (final InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException(e.getCause());
                }
            }

            @Override
            public Type[] getParameterTypes() {
                return types;
            }

            @Override
            public String[] getParameterNames() {
                return params;
            }

            @Override
            public Adapter<?, ?>[] getParameterConverter() {
                return converters;
            }

            @Override
            public Adapter<?, ?>[] getParameterItemConverter() {
                return itemConverters;
            }

            @Override
            public ObjectConverter.Codec<?>[] getObjectConverter() {
                return objectConverters;
            }
        };
    }

    private void validateAnnotations(final Object parameter,
                                     final JsonbTypeAdapter adapter, final JsonbDateFormat dateFormat,
                                     final JsonbNumberFormat numberFormat,
                                     final JohnzonConverter johnzonConverter) {
        int notNull = adapter != null ? 1 : 0;
        notNull += dateFormat != null ? 1 : 0;
        notNull += numberFormat != null ? 1 : 0;
        notNull += johnzonConverter != null ? 1 : 0;
        if (notNull > 1) {
            throw new IllegalArgumentException("Conflicting @JsonbXXX/@JohnzonConverter on " + parameter);
        }
    }

    private Adapter<?, ?> toConverter(final Types types, final Type type,
                                      final JsonbTypeAdapter adapter, final JsonbDateFormat dateFormat,
                                      final JsonbNumberFormat numberFormat) {
        final Adapter converter;
        if (adapter != null) {
            final Class<? extends JsonbAdapter> value = adapter.value();
            final ParameterizedType pt = types.findParameterizedType(value, JsonbAdapter.class);
            final JohnzonAdapterFactory.Instance<? extends JsonbAdapter> instance = newInstance(value);
            toRelease.add(instance);
            final Type[] actualTypeArguments = pt.getActualTypeArguments();
            converter = new JohnzonJsonbAdapter(instance.getValue(), actualTypeArguments[0], actualTypeArguments[1]);
        } else if (dateFormat != null) { // TODO: support lists, LocalDate?
            if (Date.class == type) {
                converter = new ConverterAdapter<>(new JsonbDateConverter(dateFormat), Date.class);
            } else if (LocalDateTime.class == type) {
                converter = new ConverterAdapter<>(new JsonbLocalDateTimeConverter(dateFormat), LocalDateTime.class);
            } else if (LocalDate.class == type) {
                converter = new ConverterAdapter<>(new JsonbLocalDateConverter(dateFormat), LocalDate.class);
            } else if (ZonedDateTime.class == type) {
                converter = new ConverterAdapter<>(new JsonbZonedDateTimeConverter(dateFormat), ZonedDateTime.class);
            } else if (OffsetDateTime.class == type) {
                converter = new ConverterAdapter<>(new JsonbOffsetDateTimeConverter(dateFormat), OffsetDateTime.class);
            } else { // can happen if set on the class, todo: refine the checks
                converter = null; // todo: should we fallback on numberformat?
            }
        } else if (numberFormat != null) {  // TODO: support lists?
            converter = new ConverterAdapter<>(new JsonbNumberConverter(numberFormat), Number.class);
        } else {
            converter = new ConverterAdapter<>(new JsonbValueConverter(), Object.class);
        }
        return converter;
    }

    private JohnzonAdapterFactory.Instance newInstance(final Class<?> value) {
        return factory.create(value);
    }

    @Override
    public Map<String, Reader> findReaders(final Class<?> clazz) {
        final Map<String, Reader> readers = delegate.findReaders(clazz);

        final boolean record = isRecord(clazz) || Meta.getAnnotation(clazz, JohnzonRecord.class) != null;
        final Map<String, Parameter> recordParams = record ?
                findRecordConstructor(clazz)
                        .map(c -> Stream.of(c.getParameters())
                                .collect(toMap(p -> ofNullable(p.getAnnotation(JohnzonRecord.Name.class))
                                        .map(JohnzonRecord.Name::value)
                                        .orElseGet(p::getName), identity())))
                        .orElseGet(Collections::emptyMap) :
                null;
        final Comparator<String> keyComparator = fieldComparator(clazz);
        final Map<String, Reader> result = keyComparator == null ? new HashMap<>() : new TreeMap<>(keyComparator);
        for (final Map.Entry<String, Reader> entry : readers.entrySet()) {
            final Reader initialReader = entry.getValue();
            final DecoratedType annotations = record ? new DecoratedType() {
                private final Parameter parameter = recordParams.get(entry.getKey());

                @Override
                public Type getType() {
                    return initialReader.getType();
                }

                @Override
                public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
                    final T annotation = initialReader.getAnnotation(clazz);
                    return annotation == null && parameter != null ? parameter.getAnnotation(clazz) : annotation;
                }

                @Override
                public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
                    final T annotation = parameter == null ? null : parameter.getAnnotation(clazz);
                    return annotation == null ? initialReader.getClassOrPackageAnnotation(clazz) : annotation;
                }

                @Override
                public Adapter<?, ?> findConverter() {
                    return initialReader.findConverter();
                }

                @Override
                public boolean isNillable(final boolean globalConfig) {
                    return initialReader.isNillable(globalConfig);
                }
            } : initialReader;
            if (isTransient(initialReader, visibility, clazz, true)) {
                validateAnnotationsOnTransientField(initialReader);
                continue;
            }
            if (annotations.getAnnotation(JohnzonAny.class) != null) {
                continue;
            }

            final Reader finalReader;
            if (FieldAndMethodAccessMode.CompositeDecoratedType.class.isInstance(initialReader)) { // unwrap to use the right reader
                final FieldAndMethodAccessMode.CompositeDecoratedType decoratedType = FieldAndMethodAccessMode.CompositeDecoratedType.class.cast(initialReader);
                final DecoratedType type2 = decoratedType.getType2();
                if (MethodAccessMode.MethodReader.class.isInstance(type2)) {
                    finalReader = Reader.class.cast(type2);
                } else {
                    finalReader = initialReader;
                }
            } else {
                finalReader = initialReader;
            }

            // handle optionals since mapper is still only java 7
            final Type type;
            final Function<Object, Object> reader;
            final Type readerType = finalReader.getType();
            if (isOptional(readerType)) {
                type = ParameterizedType.class.cast(readerType).getActualTypeArguments()[0];
                reader = i -> ofNullable(finalReader.read(i)).map(o -> Optional.class.cast(o).orElse(null)).orElse(null);
            } else if (OptionalInt.class == readerType) {
                type = Integer.class;
                reader = i -> {
                    final OptionalInt optionalInt = OptionalInt.class.cast(finalReader.read(i));
                    return optionalInt == null || !optionalInt.isPresent() ? null : optionalInt.getAsInt();
                };
            } else if (OptionalLong.class == readerType) {
                type = Long.class;
                reader = i -> {
                    final OptionalLong optionalLong = OptionalLong.class.cast(finalReader.read(i));
                    return optionalLong == null || !optionalLong.isPresent() ? null : optionalLong.getAsLong();
                };
            } else if (OptionalDouble.class == readerType) {
                type = Double.class;
                reader = i -> {
                    final OptionalDouble optionalDouble = OptionalDouble.class.cast(finalReader.read(i));
                    return optionalDouble == null || !optionalDouble.isPresent() ? null : optionalDouble.getAsDouble();
                };
            } else if (isOptionalArray(finalReader)) {
                final Type optionalUnwrappedType = findOptionalType(GenericArrayType.class.cast(readerType).getGenericComponentType());
                type = new GenericArrayTypeImpl(optionalUnwrappedType);
                reader = i -> {
                    final Object[] optionals = Object[].class.cast(finalReader.read(i));
                    return optionals == null ?
                            null : Stream.of(optionals)
                            .map(Optional.class::cast)
                            .map(o -> o.orElse(null))
                            .toArray();
                };
            } else {
                type = readerType;
                reader = finalReader::read;
            }

            final WriterConverters writerConverters = new WriterConverters(annotations, types);
            final JsonbProperty property = annotations.getAnnotation(JsonbProperty.class);
            final JsonbNillable propertyNillable = annotations.getAnnotation(JsonbNillable.class);
            final JsonbNillable classOrPackageNillable = annotations.getClassOrPackageAnnotation(JsonbNillable.class);
            final boolean isNillable = isNillable(property, propertyNillable, classOrPackageNillable);
            final String key = property == null || property.value().isEmpty() ? naming.translateName(entry.getKey()) : property.value();
            if (result.put(key, new Reader() {
                @Override
                public Object read(final Object instance) {
                    return reader.apply(instance);
                }

                @Override
                public ObjectConverter.Writer<?> findObjectConverterWriter() {
                    return writerConverters.writer;
                }

                @Override
                public Type getType() {
                    return type;
                }

                @Override
                public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
                    return finalReader.getAnnotation(clazz);
                }

                @Override
                public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
                    return finalReader.getClassOrPackageAnnotation(clazz);
                }

                @Override
                public Adapter<?, ?> findConverter() {
                    return writerConverters.converter;
                }

                @Override
                public boolean isNillable(final boolean global) {
                    return isNillable;
                }
            }) != null) {
                throw new JsonbException("Ambiguous field " + key);
            }
        }
        return result;
    }

    private void validateAnnotationsOnTransientField(final DecoratedType type) {
        if (type.getAnnotation(JsonbProperty.class) != null
                || type.getAnnotation(JsonbDateFormat.class) != null
                || type.getAnnotation(JsonbNumberFormat.class) != null
                || type.getAnnotation(JsonbTypeAdapter.class) != null
                || type.getAnnotation(JsonbTypeSerializer.class) != null
                || type.getAnnotation(JsonbTypeDeserializer.class) != null) {
            throw new JsonbException("Invalid configuration for " + type + " property");
        }
    }

    @Override
    public Map<String, Writer> findWriters(final Class<?> clazz) {
        final Map<String, Writer> writers = delegate.findWriters(clazz);

        final Comparator<String> keyComparator = fieldComparator(clazz);
        final Map<String, Writer> result = keyComparator == null ? new HashMap<>() : new TreeMap<>(keyComparator);
        for (final Map.Entry<String, Writer> entry : writers.entrySet()) {
            Writer initialWriter = entry.getValue();
            if (isTransient(initialWriter, visibility, clazz, false)) {
                validateAnnotationsOnTransientField(initialWriter);
                continue;
            }

            final Writer finalWriter;
            if (FieldAndMethodAccessMode.CompositeDecoratedType.class.isInstance(initialWriter)) { // unwrap to use the right reader
                final FieldAndMethodAccessMode.CompositeDecoratedType decoratedType = FieldAndMethodAccessMode.CompositeDecoratedType.class.cast(initialWriter);
                final DecoratedType type2 = decoratedType.getType2();
                if (MethodAccessMode.MethodWriter.class.isInstance(type2)) {
                    finalWriter = Writer.class.cast(type2);
                } else {
                    finalWriter = initialWriter;
                }
            } else {
                finalWriter = initialWriter;
            }

            // handle optionals since mapper is still only java 7
            final Type type;
            final BiConsumer<Object, Object> writer;
            final Type writerType = initialWriter.getType();
            if (isOptional(writerType)) {
                type = findOptionalType(writerType);
                writer = (i, val) -> finalWriter.write(i, Optional.ofNullable(val));
            } else if (OptionalInt.class == writerType) {
                type = Integer.class;
                writer = (i, value) -> finalWriter.write(i, value == null ?
                        OptionalInt.empty() : OptionalInt.of(Number.class.cast(value).intValue()));
            } else if (OptionalLong.class == writerType) {
                type = Long.class;
                writer = (i, value) -> finalWriter.write(i, value == null ?
                        OptionalLong.empty() : OptionalLong.of(Number.class.cast(value).longValue()));
            } else if (OptionalDouble.class == writerType) {
                type = Double.class;
                writer = (i, value) -> finalWriter.write(i, value == null ?
                        OptionalDouble.empty() : OptionalDouble.of(Number.class.cast(value).doubleValue()));
            } else if (isOptionalArray(initialWriter)) {
                final Type optionalUnwrappedType = findOptionalType(GenericArrayType.class.cast(writerType).getGenericComponentType());
                type = new GenericArrayTypeImpl(optionalUnwrappedType);
                writer = (i, value) -> {
                    if (value != null) {
                        finalWriter.write(i, Stream.of(Object[].class.cast(value))
                                .map(Optional::ofNullable)
                                .toArray(Optional[]::new));
                    }
                };
            } else {
                type = writerType;
                writer = finalWriter::write;
            }

            final ReaderConverters converters = new ReaderConverters(initialWriter);
            final JsonbProperty property = initialWriter.getAnnotation(JsonbProperty.class);
            final JsonbNillable propertyNillable = initialWriter.getAnnotation(JsonbNillable.class);
            final JsonbNillable classOrPackageNillable = initialWriter.getClassOrPackageAnnotation(JsonbNillable.class);
            final boolean isNillable = isNillable(property, propertyNillable, classOrPackageNillable);
            final String key = property == null || property.value().isEmpty() ? naming.translateName(entry.getKey()) : property.value();
            if (result.put(key, new Writer() {
                @Override
                public void write(final Object instance, final Object val) {
                    writer.accept(instance, val);
                }

                @Override
                public ObjectConverter.Reader<?> findObjectConverterReader() {
                    return converters.reader;
                }

                @Override
                public Type getType() {
                    return type;
                }

                @Override
                public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
                    return initialWriter.getAnnotation(clazz);
                }

                @Override
                public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
                    return initialWriter.getClassOrPackageAnnotation(clazz);
                }

                @Override
                public Adapter<?, ?> findConverter() {
                    return converters.converter;
                }

                @Override
                public boolean isNillable(final boolean global) {
                    return isNillable;
                }
            }) != null) {
                throw new JsonbException("Ambiguous field " + key);
            }
        }
        return result;
    }

    @Override
    public ObjectConverter.Reader<?> findReader(final Class<?> clazz) {
        return getClassEntry(clazz).readers.reader;
    }

    @Override
    public ObjectConverter.Writer<?> findWriter(final Class<?> clazz) {
        return getClassEntry(clazz).writers.writer;
    }

    @Override
    public Adapter<?, ?> findAdapter(final Class<?> clazz) { // TODO: find a way to not parse twice
        final Adapter<?, ?> converter = getClassEntry(clazz).readers.converter;
        if (converter != null && isReversedAdapter(clazz, converter.getClass(), converter)) {
            return new ReversedAdapter<>(converter);
        }
        return converter;
    }

    @Override
    public Method findAnyGetter(final Class<?> clazz) {
        return partialDelegate.findAnyGetter(clazz);
    }

    @Override
    public Method findAnySetter(final Class<?> clazz) {
        return partialDelegate.findAnySetter(clazz);
    }

    @Override
    public Field findAnyField(final Class<?> clazz) {
        return partialDelegate.findAnyField(clazz);
    }

    @Override
    public void afterParsed(final Class<?> clazz) {
        parsingCache.remove(clazz);
        partialDelegate.afterParsed(clazz);
    }

    private boolean isReversedAdapter(final Class<?> payloadType, final Class<?> aClass, final Adapter<?, ?> instance) {
        if (TypeAwareAdapter.class.isInstance(instance)) {
            return payloadType.isAssignableFrom(Class.class.cast(TypeAwareAdapter.class.cast(instance).getTo()))
                    && !payloadType.isAssignableFrom(Class.class.cast(TypeAwareAdapter.class.cast(instance).getFrom()));
        }
        final Type[] genericInterfaces = aClass.getGenericInterfaces();
        return Stream.of(genericInterfaces).filter(ParameterizedType.class::isInstance)
                .filter(i -> Adapter.class.isAssignableFrom(Class.class.cast(ParameterizedType.class.cast(i).getRawType())))
                .findFirst()
                .map(pt -> {
                    final Type argument = ParameterizedType.class.cast(pt).getActualTypeArguments()[0];
                    return Class.class.isInstance(argument) && payloadType.isAssignableFrom(Class.class.cast(argument));
                })
                .orElseGet(() -> {
                    final Class<?> superclass = aClass.getSuperclass();
                    return superclass != Object.class && isReversedAdapter(payloadType, superclass, instance);
                });
    }

    private boolean isNillable(final JsonbProperty property, final JsonbNillable propertyNillable, final JsonbNillable classOrPackageNillable) {
        if (propertyNillable != null) {
            return propertyNillable.value();
        } else if (property != null) {
            return property.nillable();
        }
        if (classOrPackageNillable != null) {
            return classOrPackageNillable.value();
        }
        return globalIsNillable;
    }

    private ParsingCacheEntry getClassEntry(final Class<?> clazz) {
        ParsingCacheEntry cache = parsingCache.get(clazz);
        if (cache == null) {
            cache = new ParsingCacheEntry(new ClassDecoratedType(clazz), types);
            parsingCache.putIfAbsent(clazz, cache);
        }
        return cache;
    }

    private Type findOptionalType(final Type writerType) {
        return ParameterizedType.class.cast(writerType).getActualTypeArguments()[0];
    }

    private boolean isOptional(final Type type) {
        return ParameterizedType.class.isInstance(type) && Optional.class == ParameterizedType.class.cast(type).getRawType();
    }

    private boolean isOptionalArray(final DecoratedType value) {
        return GenericArrayType.class.isInstance(value.getType()) &&
                isOptional(GenericArrayType.class.cast(value.getType()).getGenericComponentType());
    }

    private boolean isTransient(final DecoratedType dt, final PropertyVisibilityStrategy visibility, final Class<?> root, final boolean read) {
        if (!FieldAndMethodAccessMode.CompositeDecoratedType.class.isInstance(dt)) {
            return isTransient(dt) || shouldSkip(visibility, dt, root, read);
        }
        final FieldAndMethodAccessMode.CompositeDecoratedType cdt = FieldAndMethodAccessMode.CompositeDecoratedType.class.cast(dt);
        return isTransient(cdt.getType1()) || isTransient(cdt.getType2()) ||
                (shouldSkip(visibility, cdt.getType1(), root, read) && shouldSkip(visibility, cdt.getType2(), root, read));
    }

    private boolean shouldSkip(final PropertyVisibilityStrategy visibility, final DecoratedType t, final Class<?> root, final boolean read) {
        return isNotVisible(visibility, t, root, read);
    }

    private boolean isTransient(final DecoratedType t) {
        if (t.getAnnotation(JsonbTransient.class) != null) {
            return true;
        }
        // TODO: spec requirement, this sounds wrong since you cant customize 2 kind of serializations on the same model
        if (FieldAccessMode.FieldDecoratedType.class.isInstance(t)) {
            final Field field = FieldAccessMode.FieldDecoratedType.class.cast(t).getField();
            return Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers());
        }
        return false;
    }

    private boolean isNotVisible(final PropertyVisibilityStrategy visibility,
                                 final DecoratedType t,
                                 final Class<?> root,
                                 final boolean read) {
        if (FieldAccessMode.FieldDecoratedType.class.isInstance(t)) {
            final Field field = FieldAccessMode.FieldDecoratedType.class.cast(t).getField();
            if (DefaultPropertyVisibilityStrategy.class.isInstance(visibility)) {
                return !DefaultPropertyVisibilityStrategy.class.cast(visibility).isVisible(field, root, read);
            }
            return !visibility.isVisible(field);
        }
        if (MethodAccessMode.MethodDecoratedType.class.isInstance(t)) {
            final Method method = MethodAccessMode.MethodDecoratedType.class.cast(t).getMethod();
            return !visibility.isVisible(method);
        }
        return false;
    }

    private Comparator<String> orderComparator(final Class<?> clazz) {
        final Comparator<String> keyComparator;
        final JsonbPropertyOrder orderAnnotation = Meta.getAnnotation(clazz, JsonbPropertyOrder.class);
        if (orderAnnotation != null) {
            final List<String> indexed = new ArrayList<>(asList(orderAnnotation.value()));
            if (naming != null) { // JsonbPropertyOrder applies on java names
                for (int i = 0; i < indexed.size(); i++) {
                    indexed.set(i, naming.translateName(indexed.get(i)));
                }
            }
            keyComparator = (o1, o2) -> {
                if (o1 != null && o1.equals(o2)) {
                    return 0;
                }
                final int i1 = indexed.indexOf(o1);
                final int i2 = indexed.indexOf(o2);
                if (i1 < 0) {
                    if (i2 < 0) {
                        if (order != null) {
                            switch (order) {
                                case PropertyOrderStrategy.ANY:
                                case PropertyOrderStrategy.LEXICOGRAPHICAL:
                                    return o1.compareTo(o2);
                                case PropertyOrderStrategy.REVERSE:
                                    return o2.compareTo(o1);
                                default:
                                    return 1;
                            }
                        }
                    }
                    return 1;
                }
                if (i2 < 0) {
                    return -1;
                }
                return i1 - i2;
            };
        } else if (order != null) {
            switch (order) {
                case PropertyOrderStrategy.ANY:
                    keyComparator = new PerHierarchyAndLexicographicalOrderFieldComparator(clazz);
                    break;
                case PropertyOrderStrategy.LEXICOGRAPHICAL:
                    keyComparator = String::compareTo;
                    break;
                case PropertyOrderStrategy.REVERSE:
                    keyComparator = Comparator.reverseOrder();
                    break;
                default: // unlikely
                    keyComparator = null;
            }
        } else { // unlikely
            keyComparator = null;
        }
        return keyComparator;
    }

    @Override
    public void close() throws IOException {
        toRelease.forEach(JohnzonAdapterFactory.Instance::release);
        if (Closeable.class.isInstance(delegate)) {
            Closeable.class.cast(delegate).close();
        }
        toRelease.clear();
    }

    // belongs to Meta but java 8
    private static <T extends Annotation> T getAnnotation(final Parameter param, final Class<T> api) {
        final T annotation = param.getAnnotation(api);
        if (annotation != null) {
            return annotation;
        }
        return Meta.findMeta(param.getAnnotations(), api);
    }

    @Override
    public void clean(final Class<?> value) {
        if (Cleanable.class.isInstance(visibility)) {
            Cleanable.class.cast(visibility).clean(value);
        }
    }

    private class ReaderConverters {
        private Adapter<?, ?> converter;
        private ObjectConverter.Reader reader;

        ReaderConverters(final DecoratedType annotationHolder) {
            final boolean numberType = isNumberType(annotationHolder.getType());
            final boolean dateType = isDateType(annotationHolder.getType());
            final boolean hasRawType = hasRawType(annotationHolder.getType());
            final JsonbTypeDeserializer deserializer = annotationHolder.getAnnotation(JsonbTypeDeserializer.class);
            final JsonbTypeAdapter adapter = annotationHolder.getAnnotation(JsonbTypeAdapter.class);
            final JsonbTypeAdapter typeAdapter = hasRawType ? getRawTargetType(annotationHolder.getType()).getDeclaredAnnotation(JsonbTypeAdapter.class) : null;
            JsonbDateFormat dateFormat = dateType ? annotationHolder.getAnnotation(JsonbDateFormat.class) : null;
            JsonbNumberFormat numberFormat = numberType ? annotationHolder.getAnnotation(JsonbNumberFormat.class) : null;
            final JohnzonConverter johnzonConverter = annotationHolder.getAnnotation(JohnzonConverter.class);
            validateAnnotations(annotationHolder, adapter, dateFormat, numberFormat, johnzonConverter);
            if (dateFormat == null && dateType) {
                dateFormat = annotationHolder.getClassOrPackageAnnotation(JsonbDateFormat.class);
            }
            if (numberFormat == null && numberType) {
                numberFormat = annotationHolder.getClassOrPackageAnnotation(JsonbNumberFormat.class);
            }

            converter = adapter == null && typeAdapter == null && dateFormat == null && numberFormat == null && johnzonConverter == null ?
                    defaultConverters.get(new AdapterKey(annotationHolder.getType(), String.class)) :
                    toConverter(types, annotationHolder.getType(), adapter != null ? adapter : typeAdapter, dateFormat, numberFormat);

            if (deserializer != null) {
                final Class<? extends JsonbDeserializer> value = deserializer.value();
                final JohnzonAdapterFactory.Instance<? extends JsonbDeserializer> instance = newInstance(value);
                final ParameterizedType pt = types.findParameterizedType(value, JsonbDeserializer.class);
                final Class<?> mappedType = types.findParamType(pt, JsonbDeserializer.class);
                toRelease.add(instance);
                final JsonBuilderFactory builderFactoryInstance = builderFactory.get();
                final Type[] arguments = types.findParameterizedType(value, JsonbDeserializer.class).getActualTypeArguments();
                final boolean global = arguments.length == 1 && arguments[0] != null && arguments[0].equals(annotationHolder.getType());
                reader = new ObjectConverter.Reader() {
                    private final ConcurrentMap<Type, BiFunction<JsonValue, MappingParser, Object>> impl =
                            new ConcurrentHashMap<>();

                    @Override
                    public boolean isGlobal() {
                        return global;
                    }

                    @Override
                    public Object fromJson(final JsonValue value,
                                           final Type targetType,
                                           final MappingParser parser) {
                        final JsonbDeserializer jsonbDeserializer = instance.getValue();
                        if (global || targetType == mappedType) { // fast test and matches most cases
                            return mapItem(value, targetType, parser, jsonbDeserializer);
                        }

                        BiFunction<JsonValue, MappingParser, Object> fn = impl.get(targetType);
                        if (fn == null) {
                            if (value.getValueType() == JsonValue.ValueType.ARRAY) {
                                if (ParameterizedType.class.isInstance(targetType)) {
                                    final ParameterizedType parameterizedType = ParameterizedType.class.cast(targetType);
                                    final Class<?> paramType = types.findParamType(parameterizedType, Collection.class);
                                    if (paramType != null && (mappedType == null /*Object*/ || mappedType.isAssignableFrom(paramType))) {
                                        final Collector<Object, ?, ? extends Collection<Object>> collector =
                                                Set.class.isAssignableFrom(
                                                        types.asClass(parameterizedType.getRawType())) ? toSet() : toList();
                                        fn = (json, mp) -> json.asJsonArray().stream()
                                                .map(i -> mapItem(i, paramType, mp, jsonbDeserializer))
                                                .collect(collector);
                                    }
                                }
                            }
                            if (fn == null) {
                                fn = (json, mp) -> mapItem(json, targetType, mp, jsonbDeserializer);
                            }
                            impl.putIfAbsent(targetType, fn);
                        }
                        return fn.apply(value, parser);
                    }

                    private Object mapItem(final JsonValue jsonValue, final Type targetType,
                                           final MappingParser parser, final JsonbDeserializer jsonbDeserializer) {
                        return jsonbDeserializer.deserialize(
                                JsonValueParserAdapter.createFor(jsonValue, parserFactory),
                                new JohnzonDeserializationContext(parser, builderFactoryInstance, jsonProvider),
                                targetType);
                    }
                };
            } else if (johnzonConverter != null) {
                try {
                    MapperConverter mapperConverter = johnzonConverter.value().newInstance();
                    if (mapperConverter instanceof Converter) {
                        converter = new ConverterAdapter<>((Converter) mapperConverter, annotationHolder.getType());
                    } else if (mapperConverter instanceof ObjectConverter.Reader) {
                        reader = (ObjectConverter.Reader) mapperConverter;
                    }
                } catch (final InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }

    private class WriterConverters {
        private Adapter<?, ?> converter;
        private ObjectConverter.Writer writer;

        WriterConverters(final DecoratedType reader, final Types types) {
            final boolean numberType = isNumberType(reader.getType());
            final boolean dateType = isDateType(reader.getType());
            final boolean hasRawType = hasRawType(reader.getType());
            final JsonbTypeSerializer serializer = reader.getAnnotation(JsonbTypeSerializer.class);
            final JsonbTypeAdapter adapter = reader.getAnnotation(JsonbTypeAdapter.class);
            final JsonbTypeAdapter typeAdapter = hasRawType ? getRawTargetType(reader.getType()).getDeclaredAnnotation(JsonbTypeAdapter.class) : null;
            JsonbDateFormat dateFormat = dateType ? reader.getAnnotation(JsonbDateFormat.class) : null;
            JsonbNumberFormat numberFormat = numberType ? reader.getAnnotation(JsonbNumberFormat.class) : null;
            final JohnzonConverter johnzonConverter = reader.getAnnotation(JohnzonConverter.class);
            validateAnnotations(reader, adapter != null ? adapter : typeAdapter, dateFormat, numberFormat, johnzonConverter);
            if (dateFormat == null && isDateType(reader.getType())) {
                dateFormat = reader.getClassOrPackageAnnotation(JsonbDateFormat.class);
            }
            if (numberFormat == null && isNumberType(reader.getType())) {
                numberFormat = reader.getClassOrPackageAnnotation(JsonbNumberFormat.class);
            }

            converter = adapter == null && typeAdapter == null && dateFormat == null && numberFormat == null && johnzonConverter == null ?
                    defaultConverters.get(new AdapterKey(reader.getType(), String.class)) :
                    toConverter(types, reader.getType(), adapter != null ? adapter : typeAdapter, dateFormat, numberFormat);

            if (serializer != null) {
                final Class<? extends JsonbSerializer> value = serializer.value();
                final JohnzonAdapterFactory.Instance<? extends JsonbSerializer> instance = newInstance(value);
                toRelease.add(instance);
                final Type[] arguments = types.findParameterizedType(value, JsonbSerializer.class).getActualTypeArguments();
                final boolean global = arguments.length == 1 && arguments[0] != null && arguments[0].equals(reader.getType());
                final JsonbSerializer jsonbSerializer = instance.getValue();
                writer = new ObjectConverter.Writer() {
                    @Override
                    public void writeJson(final Object instance, final MappingGenerator jsonbGenerator) {
                        final JsonGenerator generator = jsonbGenerator.getJsonGenerator();
                        jsonbSerializer.serialize(instance, generator, new JohnzonSerializationContext(jsonbGenerator));
                    }

                    @Override
                    public boolean isGlobal() {
                        return global;
                    }
                };
            } else if (johnzonConverter != null) {
                try {
                    MapperConverter mapperConverter = johnzonConverter.value().newInstance();
                    if (mapperConverter instanceof Converter) {
                        converter = new ConverterAdapter<>((Converter) mapperConverter, reader.getType());
                    } else if (mapperConverter instanceof ObjectConverter.Writer) {
                        writer = (ObjectConverter.Writer) mapperConverter;
                    }
                } catch (final InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }

    private boolean isDateType(final Type type) {
        if (!Class.class.isInstance(type)) {
            return false;
        }
        final Class<?> clazz = Class.class.cast(type);
        return type.getTypeName().startsWith("java.time.") || Date.class == type || Calendar.class.isAssignableFrom(clazz);
    }

    private boolean isNumberType(final Type type) {
        if (!Class.class.isInstance(type)) {
            return false;
        }
        final Class<?> clazz = Class.class.cast(type);
        return Number.class.isAssignableFrom(clazz) || clazz.isPrimitive();
    }

    private boolean hasRawType(final Type type) {
        return Class.class.isInstance(type) ||
                (ParameterizedType.class.isInstance(type) &&
                        Class.class.isInstance(ParameterizedType.class.cast(type).getRawType()));
    }

    private Class<?> getRawTargetType(final Type type) { // only intended to be used after hasRawType check
        if (Class.class.isInstance(type)) {
            return Class.class.cast(type);
        }
        ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
        Class<?> rawType = Class.class.cast(parameterizedType.getRawType());
        if (Collection.class.isAssignableFrom(rawType) || Map.class.isAssignableFrom(rawType)) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            Type itemType = actualTypeArguments[actualTypeArguments.length - 1];
            if (Class.class.isInstance(itemType)) {
                return Class.class.cast(itemType);
            }
        }
        return rawType;
    }

    private static class ClassDecoratedType implements DecoratedType {
        private final Class<?> annotations;

        ClassDecoratedType(final Class<?> clazz) {
            this.annotations = clazz;
        }

        @Override
        public Type getType() {
            return annotations;
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
            return Meta.getAnnotation(annotations, clazz);
        }

        @Override
        public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
            return Meta.getAnnotation(clazz.getPackage(), clazz);
        }

        @Override
        public Adapter<?, ?> findConverter() {
            return null;
        }

        @Override
        public boolean isNillable(final boolean global) {
            return global;
        }
    }

    private class ParsingCacheEntry {
        private final ReaderConverters readers;
        private final WriterConverters writers;

        ParsingCacheEntry(final DecoratedType type, final Types types) {
            readers = new ReaderConverters(type);
            writers = new WriterConverters(type, types);
        }
    }
}
