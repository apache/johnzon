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

import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;
import org.apache.johnzon.mapper.map.LazyConverterMap;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

/**
 * Contains internal configuration for all the mapper stuff.
 * It needs to be immutable and 100% runtime oriented.
 */
public /* DON'T MAKE IT HIDDEN */ class MapperConfig implements Cloneable {

    private static final ObjectConverter.Codec NO_CONVERTER = new ObjectConverter.Codec() {
        @Override
        public void writeJson(Object instance, MappingGenerator jsonbGenerator) {
            // just a dummy
        }

        @Override
        public Object fromJson(JsonValue jsonObject, Type targetType, MappingParser parser) {
            return null;
        }
    };

    private final int version;
    private final boolean useJsRange;
    private final boolean close;
    private final boolean skipNull;
    private final boolean skipEmptyArray;
    private final boolean treatByteArrayAsBase64;
    private final boolean treatByteArrayAsBase64URL;
    private final boolean readAttributeBeforeWrite;
    private final boolean supportEnumMapDeserialization; // for tck
    private final AccessMode accessMode;
    private final Charset encoding;
    private final LazyConverterMap adapters;
    private final ConcurrentMap<Adapter<?, ?>, AdapterKey> reverseAdapters;

    private final Map<Class<?>, ObjectConverter.Writer<?>> objectConverterWriters;
    private final Map<Class<?>, ObjectConverter.Reader<?>> objectConverterReaders;
    private final Comparator<String> attributeOrder;
    private final boolean failOnUnknown;
    private final SerializeValueFilter serializeValueFilter;
    private final boolean useBigDecimalForFloats;
    private final Boolean deduplicateObjects;
    private final Map<Class<?>, Class<?>> interfaceImplementationMapping;
    private final boolean useBigDecimalForObjectNumbers;

    private final Function<String, Class<?>> typeLoader;
    private final Function<Class<?>, String> discriminatorMapper;
    private final Predicate<Class<?>> serializationPredicate;
    private final Predicate<Class<?>> deserializationPredicate;
    private final String discriminator;

    private final Map<Class<?>, ObjectConverter.Writer<?>> objectConverterWriterCache;
    private final Map<Class<?>, ObjectConverter.Reader<?>> objectConverterReaderCache;

    private final Collection<Type> noParserAdapterTypes = new ConcurrentHashMap<Type, Boolean>().keySet(true);
    private final Collection<Type> noGeneratorAdapterTypes = new ConcurrentHashMap<Type, Boolean>().keySet(true);

    private final Function<Class<?>, CustomEnumConverter<?>> enumConverterFactory;

    private final SnippetFactory snippet;

    private final Function<MapperConfig, Mappings> mappingsFactory;

    //CHECKSTYLE:OFF
    @Deprecated
    public MapperConfig(final LazyConverterMap adapters,
                        final Map<Class<?>, ObjectConverter.Writer<?>> objectConverterWriters,
                        final Map<Class<?>, ObjectConverter.Reader<?>> objectConverterReaders,
                        final int version, final boolean close,
                        final boolean skipNull, final boolean skipEmptyArray,
                        final boolean treatByteArrayAsBase64, final boolean treatByteArrayAsBase64URL,
                        final boolean readAttributeBeforeWrite,
                        final AccessMode accessMode, final Charset encoding,
                        final Comparator<String> attributeOrder,
                        final boolean failOnUnknown,
                        final SerializeValueFilter serializeValueFilter,
                        final boolean useBigDecimalForFloats,
                        final Boolean deduplicateObjects,
                        final Map<Class<?>, Class<?>> interfaceImplementationMapping,
                        final boolean useJsRange,
                        final boolean useBigDecimalForObjectNumbers,
                        final boolean supportEnumMapDeserialization,
                        final Function<String, Class<?>> typeLoader,
                        final Function<Class<?>, String> discriminatorMapper,
                        final String discriminator,
                        final Predicate<Class<?>> deserializationPredicate,
                        final Predicate<Class<?>> serializationPredicate,
                        final Function<Class<?>, CustomEnumConverter<?>> enumConverterFactory) {
        //CHECKSTYLE:ON
        this(adapters, objectConverterWriters, objectConverterReaders, version, close, skipNull, skipEmptyArray,
                treatByteArrayAsBase64, treatByteArrayAsBase64URL, readAttributeBeforeWrite, accessMode, encoding,
                attributeOrder, failOnUnknown, serializeValueFilter, useBigDecimalForFloats, deduplicateObjects, interfaceImplementationMapping,
                useJsRange, useBigDecimalForObjectNumbers, supportEnumMapDeserialization, typeLoader,
                discriminatorMapper, discriminator, deserializationPredicate, serializationPredicate, enumConverterFactory,
                JohnzonCores.snippetFactory(50, Json.createGeneratorFactory(emptyMap())), null);
    }

    //disable checkstyle for 10+ parameters
    //CHECKSTYLE:OFF
    public MapperConfig(final LazyConverterMap adapters,
                        final Map<Class<?>, ObjectConverter.Writer<?>> objectConverterWriters,
                        final Map<Class<?>, ObjectConverter.Reader<?>> objectConverterReaders,
                        final int version, final boolean close,
                        final boolean skipNull, final boolean skipEmptyArray,
                        final boolean treatByteArrayAsBase64, final boolean treatByteArrayAsBase64URL,
                        final boolean readAttributeBeforeWrite,
                        final AccessMode accessMode, final Charset encoding,
                        final Comparator<String> attributeOrder,
                        final boolean failOnUnknown,
                        final SerializeValueFilter serializeValueFilter,
                        final boolean useBigDecimalForFloats,
                        final Boolean deduplicateObjects,
                        final Map<Class<?>, Class<?>> interfaceImplementationMapping,
                        final boolean useJsRange,
                        final boolean useBigDecimalForObjectNumbers,
                        final boolean supportEnumMapDeserialization,
                        final Function<String, Class<?>> typeLoader,
                        final Function<Class<?>, String> discriminatorMapper,
                        final String discriminator,
                        final Predicate<Class<?>> deserializationPredicate,
                        final Predicate<Class<?>> serializationPredicate,
                        final Function<Class<?>, CustomEnumConverter<?>> enumConverterFactory,
                        final SnippetFactory snippet,
                        final Function<MapperConfig, Mappings> mappingsFactory) {
        //CHECKSTYLE:ON
        this.objectConverterWriters = objectConverterWriters;
        this.objectConverterReaders = objectConverterReaders;
        this.version = version;
        this.close = close;
        this.skipNull = skipNull;
        this.skipEmptyArray = skipEmptyArray;
        this.treatByteArrayAsBase64 = treatByteArrayAsBase64;
        this.treatByteArrayAsBase64URL = treatByteArrayAsBase64URL;
        this.readAttributeBeforeWrite = readAttributeBeforeWrite;
        this.accessMode = accessMode;
        this.encoding = encoding;
        this.useJsRange = useJsRange;
        this.useBigDecimalForObjectNumbers = useBigDecimalForObjectNumbers;
        this.supportEnumMapDeserialization = supportEnumMapDeserialization;
        this.typeLoader = typeLoader;
        this.discriminatorMapper = discriminatorMapper;
        this.serializationPredicate = serializationPredicate;
        this.deserializationPredicate = deserializationPredicate;
        this.discriminator = discriminator;
        this.enumConverterFactory = enumConverterFactory;

        // handle Adapters
        this.adapters = adapters;
        this.reverseAdapters = new ConcurrentHashMap<>(adapters.size());
        adapters.forEach((k, v) -> this.reverseAdapters.put(v, k));


        this.attributeOrder = attributeOrder;
        this.failOnUnknown = failOnUnknown;
        this.serializeValueFilter = serializeValueFilter == null ? (name, value) -> false : serializeValueFilter;
        this.interfaceImplementationMapping = interfaceImplementationMapping;

        this.objectConverterWriterCache = new HashMap<>(objectConverterWriters.size());
        this.objectConverterReaderCache = new HashMap<>(objectConverterReaders.size());
        this.useBigDecimalForFloats = useBigDecimalForFloats;
        this.deduplicateObjects = deduplicateObjects;
        this.snippet = snippet;

        this.mappingsFactory = mappingsFactory;
    }

    public SnippetFactory getSnippet() {
        return snippet;
    }

    public Function<Class<?>, CustomEnumConverter<?>> getEnumConverterFactory() {
        return enumConverterFactory;
    }

    public Collection<Type> getNoParserAdapterTypes() {
        return noParserAdapterTypes;
    }

    public Collection<Type> getNoGeneratorAdapterTypes() {
        return noGeneratorAdapterTypes;
    }

    public Function<String, Class<?>> getTypeLoader() {
        return typeLoader;
    }

    public Function<Class<?>, String> getDiscriminatorMapper() {
        return discriminatorMapper;
    }

    public Predicate<Class<?>> getDeserializationPredicate() {
        return deserializationPredicate;
    }

    public Predicate<Class<?>> getSerializationPredicate() {
        return serializationPredicate;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public boolean isUseBigDecimalForObjectNumbers() {
        return useBigDecimalForObjectNumbers;
    }

    public boolean isUseJsRange() {
        return useJsRange;
    }

    public Map<Class<?>, Class<?>> getInterfaceImplementationMapping() {
        return interfaceImplementationMapping;
    }

    public SerializeValueFilter getSerializeValueFilter() {
        return serializeValueFilter;
    }

    public Adapter findAdapter(final Type aClass) {
        if (getNoGeneratorAdapterTypes().contains(aClass)) { // avoid to create a key for nothing
            return null;
        }

        final Adapter<?, ?> converter = adapters.get(new AdapterKey(aClass, String.class, true));
        if (converter != null) {
            return converter;
        }
        /* could be an option but doesnt fit well our old converters
        final Adapter<?, ?> reverseConverter = adapters.get(new AdapterKey(String.class, aClass));
        if (reverseConverter != null) {
            return new ReversedAdapter<>(reverseConverter);
        }
        */
        if (Class.class.isInstance(aClass)) {
            final Class<?> clazz = Class.class.cast(aClass);
            if (Enum.class.isAssignableFrom(clazz)) {
                final Adapter<?, ?> enumConverter = new ConverterAdapter(enumConverterFactory.apply(clazz), clazz);
                adapters.putIfAbsent(new AdapterKey(String.class, aClass), enumConverter);
                return enumConverter;
            }
        }
        final List<AdapterKey> matched = adapters.adapterKeys().stream()
                .filter(k -> k.isAssignableFrom(aClass))
                .collect(toList());
        if (matched.size() == 1) {
            final Adapter<?, ?> adapter = adapters.get(matched.iterator().next());
            if (TypeAwareAdapter.class.isInstance(adapter)) {
                adapters.put(new AdapterKey(aClass, TypeAwareAdapter.class.cast(adapter).getTo()), adapter);
            }
            return adapter;
        }
        getNoGeneratorAdapterTypes().add(aClass);
        return null;
    }

    /**
     * Search for an {@link ObjectConverter} for the given class.
     *
     * If no {@link ObjectConverter} was found for the specific class,
     * the whole type hierarchy will be scanned for a matching {@link ObjectConverter}.
     *
     * In case the given class implements more than on interfaces and for at least two
     * we have configured an {@link ObjectConverter} the {@link ObjectConverter} for the
     * first interface we get will be taken.
     *
     * @param clazz the {@link Class}
     *
     * @return the found {@link ObjectConverter} or {@code null} if no {@link ObjectConverter} has been found
     *
     * @throws IllegalArgumentException if {@code clazz} is {@code null}
     */
    public ObjectConverter.Reader findObjectConverterReader(Class clazz) {
        return findObjectConverter(clazz, objectConverterReaders, objectConverterReaderCache);
    }
    public ObjectConverter.Writer findObjectConverterWriter(Class clazz) {
        return findObjectConverter(clazz, objectConverterWriters, objectConverterWriterCache);
    }

    private <T> T findObjectConverter(final Class clazz,
                                      final Map<Class<?>, T> from,
                                      final Map<Class<?>, T> cache) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }

        // first lets look in our cache
        T converter = cache.get(clazz);
        if (converter != null && converter != NO_CONVERTER) {
            return converter;
        }

        // if we have found a dummy, we return null
        if (converter == NO_CONVERTER) {
            return null;
        }

        // we get called the first time for this class
        // lets search...

        Map<Class<?>, T> matchingConverters = new HashMap<Class<?>, T>();

        for (Map.Entry<Class<?>, T> entry : from.entrySet()) {

            if (clazz == entry.getKey()) {
                converter = entry.getValue();
                break;
            }

            if (entry.getKey().isAssignableFrom(clazz)) {
                matchingConverters.put(entry.getKey(), entry.getValue());
            }
        }

        if (converter != null) {
            cache.put(clazz, converter);
            return converter;
        }

        if (matchingConverters.isEmpty()) {
            cache.put(clazz, (T) NO_CONVERTER);
            return null;
        }

        // search the most significant
        Class toProcess = clazz;
        while (toProcess != null && converter == null) {

            converter = matchingConverters.get(toProcess);
            if (converter != null) {
                break;
            }

            Class[] interfaces = toProcess.getInterfaces();
            if (interfaces.length > 0) {
                for (Class interfaceToSearch : interfaces) {

                    converter = matchingConverters.get(interfaceToSearch);
                    if (converter != null) {
                        break;
                    }
                }
            }

            if (converter == null && toProcess.isInterface()) {
                converter = matchingConverters.get(Object.class);
                break;
            }

            toProcess = toProcess.getSuperclass();
        }

        if (converter == null) {
            cache.put(clazz, (T) NO_CONVERTER);
        } else {
            cache.put(clazz, converter);
        }

        return converter;
    }

    public boolean isFailOnUnknown() {
        return failOnUnknown;
    }

    public int getVersion() {
        return version;
    }

    public boolean isClose() {
        return close;
    }

    public boolean isSkipNull() {
        return skipNull;
    }

    public boolean isSkipEmptyArray() {
        return skipEmptyArray;
    }

    public boolean isTreatByteArrayAsBase64() {
        return treatByteArrayAsBase64;
    }

    public boolean isTreatByteArrayAsBase64URL() {
        return treatByteArrayAsBase64URL;
    }

    public boolean isReadAttributeBeforeWrite() {
        return readAttributeBeforeWrite;
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public LazyConverterMap getAdapters() {
        return adapters;
    }

    public ConcurrentMap<Adapter<?, ?>, AdapterKey> getReverseAdapters() {
        return reverseAdapters;
    }

    public Map<Class<?>, ObjectConverter.Writer<?>> getObjectConverterWriters() {
        return objectConverterWriters;
    }

    public Map<Class<?>, ObjectConverter.Reader<?>> getObjectConverterReaders() {
        return objectConverterReaders;
    }

    public Comparator<String> getAttributeOrder() {
        return attributeOrder;
    }

    public boolean isUseBigDecimalForFloats() {
        return useBigDecimalForFloats;
    }

    public boolean isDeduplicateObjects() {
        return Boolean.TRUE.equals(deduplicateObjects);
    }

    public boolean isSupportEnumContainerDeserialization() {
        return supportEnumMapDeserialization;
    }

    public Function<MapperConfig, Mappings> getMappingsFactory() {
        return mappingsFactory;
    }

    public interface CustomEnumConverter<A> extends Converter<A>, Converter.TypeAccess {
    }
}
