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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Locale.ROOT;

// import org.apache.johnzon.core.JsonParserFactoryImpl; // don't depend on core in mapper
import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.access.BaseAccessMode;
import org.apache.johnzon.mapper.access.FieldAccessMode;
import org.apache.johnzon.mapper.access.FieldAndMethodAccessMode;
import org.apache.johnzon.mapper.access.MethodAccessMode;
import org.apache.johnzon.mapper.access.KnownNotOpenedJavaTypesAccessMode;
import org.apache.johnzon.mapper.converter.BooleanConverter;
import org.apache.johnzon.mapper.converter.ByteConverter;
import org.apache.johnzon.mapper.converter.CachedDelegateConverter;
import org.apache.johnzon.mapper.converter.CharacterConverter;
import org.apache.johnzon.mapper.converter.DoubleConverter;
import org.apache.johnzon.mapper.converter.EnumConverter;
import org.apache.johnzon.mapper.converter.FloatConverter;
import org.apache.johnzon.mapper.converter.IntegerConverter;
import org.apache.johnzon.mapper.converter.LongConverter;
import org.apache.johnzon.mapper.converter.ShortConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;
import org.apache.johnzon.mapper.map.LazyConverterMap;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonReaderFactory;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import java.io.Closeable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

// this class is responsible to hold any needed config
// to build the runtime
public class MapperBuilder {
    private JsonReaderFactory readerFactory;
    private JsonGeneratorFactory generatorFactory;
    private JsonProvider provider;
    private JsonBuilderFactory builderFactory;
    private boolean supportHiddenAccess = true;
    private int maxSize = -1;
    private int bufferSize = -1;
    private String bufferStrategy;
    private boolean autoAdjustStringBuffers;
    private Comparator<String> attributeOrder = null;
    private boolean supportConstructors;
    private boolean useGetterForCollections;
    private String accessModeName;
    private boolean pretty;
    private final Collection<Closeable> closeables = new ArrayList<Closeable>();
    private int version = -1;
    private int snippetMaxLength = 50;
    private boolean close;
    private boolean skipNull = true;
    private boolean skipEmptyArray;
    private boolean supportsComments;
    private boolean treatByteArrayAsBase64;
    private boolean treatByteArrayAsBase64URL;
    private boolean readAttributeBeforeWrite;
    private AccessMode accessMode;
    private Charset encoding = Charset.forName(System.getProperty("johnzon.mapper.encoding", "UTF-8"));
    private LazyConverterMap adapters = new LazyConverterMap();
    private Map<Class<?>, ObjectConverter.Reader<?>> objectConverterReaders = new HashMap<Class<?>, ObjectConverter.Reader<?>>();
    private Map<Class<?>, ObjectConverter.Writer<?>> objectConverterWriters = new HashMap<Class<?>, ObjectConverter.Writer<?>>();
    private Map<Class<?>, String[]> ignoredForFields = new HashMap<Class<?>, String[]>();
    private Map<Class<?>, Class<?>> interfaceImplementationMapping = new HashMap<>();
    private BaseAccessMode.FieldFilteringStrategy fieldFilteringStrategy = null;
    private boolean primitiveConverters;
    private boolean failOnUnknownProperties;
    private SerializeValueFilter serializeValueFilter;
    private boolean useBigDecimalForFloats;
    private Boolean deduplicateObjects = null;
    private boolean useJsRange;
    private boolean useBigDecimalForObjectNumbers;
    private boolean supportEnumContainerDeserialization = true;
    private Function<Class<?>, MapperConfig.CustomEnumConverter<?>> enumConverterFactory = type -> new EnumConverter(type);
    private boolean skipAccessModeWrapper;
    private Function<MapperConfig, Mappings> mappingsFactory;

    // @experimental polymorphic api
    private Function<String, Class<?>> typeLoader;
    private Function<Class<?>, String> discriminatorMapper;
    private Predicate<Class<?>> deserializationPredicate;
    private Predicate<Class<?>> serializationPredicate;
    private String discriminator;

    public Mapper build() {
        if (readerFactory == null || generatorFactory == null) {
            final JsonProvider provider;
            if (this.provider != null) {
                provider = this.provider;
            } else {
                provider = JsonProvider.provider();
                this.provider = provider;
            }
            final Map<String, Object> config = new HashMap<String, Object>();
            if (bufferStrategy != null) {
                config.put("org.apache.johnzon.buffer-strategy", bufferStrategy);
            }
            if (encoding != null) {
                config.put("org.apache.johnzon.encoding", encoding);
            }

            if (generatorFactory == null) {
                if (pretty) {
                    config.put(JsonGenerator.PRETTY_PRINTING, true);
                }
                generatorFactory = provider.createGeneratorFactory(config);
            }

            if (readerFactory == null) {
                config.remove(JsonGenerator.PRETTY_PRINTING); // doesnt mean anything anymore for reader
                if (supportsComments) {
                    config.put("org.apache.johnzon.supports-comments", "true");
                }
                if (maxSize > 0) {
                    config.put("org.apache.johnzon.max-string-length", maxSize);
                }
                if (bufferSize > 0) {
                    config.put("org.apache.johnzon.default-char-buffer", bufferSize);
                }
                if (autoAdjustStringBuffers) {
                    config.put("org.apache.johnzon.auto-adjust-buffer", true);
                }
                readerFactory = provider.createReaderFactory(config);
            }
        } else if (this.provider == null) {
            this.provider = JsonProvider.provider();
        }
        if (builderFactory == null) {
            builderFactory = provider.createBuilderFactory(emptyMap());
        }

        if (accessMode == null) {
            if ("field".equalsIgnoreCase(accessModeName)) {
                accessMode = new FieldAccessMode(supportConstructors, supportHiddenAccess);
            } else if ("method".equalsIgnoreCase(accessModeName)) {
                accessMode = new MethodAccessMode(supportConstructors, supportHiddenAccess, true);
            } else if ("strict-method".equalsIgnoreCase(accessModeName)) {
                accessMode = new MethodAccessMode(supportConstructors, supportHiddenAccess, false);
            } else if ("both".equalsIgnoreCase(accessModeName) || accessModeName == null) {
                accessMode = new FieldAndMethodAccessMode(supportConstructors, supportHiddenAccess, useGetterForCollections, true, false);
            } else {
                throw new IllegalArgumentException("Unsupported access mode: " + accessModeName);
            }
        }
        if (fieldFilteringStrategy != null) {
            if (!BaseAccessMode.class.isInstance(accessMode)) {
                throw new IllegalArgumentException("fieldFilteringStrategy can't be set with this access mode: " + accessMode);
            }
            BaseAccessMode.class.cast(accessMode).setFieldFilteringStrategy(fieldFilteringStrategy);
        }
        if (!ignoredForFields.isEmpty()) {
            if (BaseAccessMode.class.isInstance(accessMode)) {
                final BaseAccessMode baseAccessMode = BaseAccessMode.class.cast(accessMode);
                final BaseAccessMode.FieldFilteringStrategy strategy = baseAccessMode.getFieldFilteringStrategy();
                if (BaseAccessMode.ConfiguredFieldFilteringStrategy.class.isInstance(strategy)) {
                    final BaseAccessMode.ConfiguredFieldFilteringStrategy filteringStrategy =
                            BaseAccessMode.ConfiguredFieldFilteringStrategy.class.cast(strategy);
                    for (final Map.Entry<Class<?>, String[]> ignored : ignoredForFields.entrySet()) {
                        final String[] fields = ignored.getValue();
                        if (fields == null || fields.length == 0) {
                            filteringStrategy.getFieldsToRemove().remove(ignored.getKey());
                        } else {
                            filteringStrategy.getFieldsToRemove().put(ignored.getKey(), asList(fields));
                        }
                    }
                }
            } else {
                throw new IllegalStateException("AccessMode is not an BaseAccessMode");
            }
        }
        if (!skipAccessModeWrapper && !KnownNotOpenedJavaTypesAccessMode.class.isInstance(accessMode)) {
            accessMode = new KnownNotOpenedJavaTypesAccessMode(accessMode);
        }

        if (primitiveConverters) {
            adapters.put(new AdapterKey(Byte.class, String.class), new ConverterAdapter<>(new CachedDelegateConverter<>(new ByteConverter()), Byte.class));
            adapters.put(new AdapterKey(Character.class, String.class), new ConverterAdapter<>(new CharacterConverter(), Character.class));
            adapters.put(new AdapterKey(Double.class, String.class), new ConverterAdapter<>(new DoubleConverter(), Double.class));
            adapters.put(new AdapterKey(Float.class, String.class), new ConverterAdapter<>(new FloatConverter(), Float.class));
            adapters.put(new AdapterKey(Integer.class, String.class), new ConverterAdapter<>(new IntegerConverter(), Integer.class));
            adapters.put(new AdapterKey(Long.class, String.class), new ConverterAdapter<>(new LongConverter(), Long.class));
            adapters.put(new AdapterKey(Short.class, String.class), new ConverterAdapter<>(new ShortConverter(), Short.class));
            adapters.put(new AdapterKey(Boolean.class, String.class), new ConverterAdapter<>(new CachedDelegateConverter<>(new BooleanConverter()), Boolean.class));
            adapters.put(new AdapterKey(byte.class, String.class), adapters.get(new AdapterKey(Byte.class, String.class)));
            adapters.put(new AdapterKey(char.class, String.class), adapters.get(new AdapterKey(Character.class, String.class)));
            adapters.put(new AdapterKey(double.class, String.class), adapters.get(new AdapterKey(Double.class, String.class)));
            adapters.put(new AdapterKey(float.class, String.class), adapters.get(new AdapterKey(Float.class, String.class)));
            adapters.put(new AdapterKey(int.class, String.class), adapters.get(new AdapterKey(Integer.class, String.class)));
            adapters.put(new AdapterKey(long.class, String.class), adapters.get(new AdapterKey(Long.class, String.class)));
            adapters.put(new AdapterKey(short.class, String.class), adapters.get(new AdapterKey(Short.class, String.class)));
            adapters.put(new AdapterKey(boolean.class, String.class), adapters.get(new AdapterKey(Boolean.class, String.class)));
        }

        return new Mapper(
                readerFactory, generatorFactory, builderFactory, provider,
                new MapperConfig(
                        adapters, objectConverterWriters, objectConverterReaders,
                        version, close,
                        skipNull, skipEmptyArray,
                        treatByteArrayAsBase64, treatByteArrayAsBase64URL, readAttributeBeforeWrite,
                        accessMode, encoding, attributeOrder, failOnUnknownProperties,
                        serializeValueFilter, useBigDecimalForFloats, deduplicateObjects,
                        interfaceImplementationMapping, useJsRange, useBigDecimalForObjectNumbers,
                        supportEnumContainerDeserialization,
                        typeLoader, discriminatorMapper, discriminator,
                        deserializationPredicate, serializationPredicate,
                        enumConverterFactory,
                        JohnzonCores.snippetFactory(snippetMaxLength, generatorFactory), mappingsFactory),
                closeables);
    }

    public void setEnumConverterFactory(final Function<Class<?>, MapperConfig.CustomEnumConverter<?>> enumConverterFactory) {
        this.enumConverterFactory = enumConverterFactory;
    }

    public ConcurrentHashMap<AdapterKey, Adapter<?,?>> getAdapters() {
        return adapters;
    }

    public MapperBuilder setSnippetMaxLength(final int snippetMaxLength) {
        this.snippetMaxLength = snippetMaxLength;
        return this;
    }

    public MapperBuilder setUseShortISO8601Format(final boolean useShortISO8601Format) {
        adapters.setUseShortISO8601Format(useShortISO8601Format);
        return this;
    }

    public MapperBuilder setAdaptersDateTimeFormatter(final DateTimeFormatter dateTimeFormatter) {
        adapters.setDateTimeFormatter(dateTimeFormatter);
        return this;
    }

    public MapperBuilder setAdaptersDateTimeFormatterString(final String dateTimeFormatter) {
        adapters.setDateTimeFormatter(DateTimeFormatter.ofPattern(dateTimeFormatter));
        return this;
    }

    public MapperBuilder setInterfaceImplementationMapping(final Map<Class<?>, Class<?>> interfaceImplementationMapping) {
        this.interfaceImplementationMapping = interfaceImplementationMapping;
        return this;
    }

    public MapperBuilder setFailOnUnknownProperties(final boolean failOnUnknownProperties) {
        this.failOnUnknownProperties = failOnUnknownProperties;
        return this;
    }

    public MapperBuilder addCloseable(final Closeable closeable) {
        closeables.add(closeable);
        return this;
    }

    public MapperBuilder setIgnoreFieldsForType(final Class<?> type, final String... fields) {
        ignoredForFields.put(type, fields == null ? new String[0] : fields);
        return this;
    }

    public MapperBuilder setSupportGetterForCollections(final boolean useGetterForCollections) {
        this.useGetterForCollections = useGetterForCollections;
        return this;
    }

    public MapperBuilder setSupportsComments(final boolean supportsComments) {
        this.supportsComments = supportsComments;
        return this;
    }

    public MapperBuilder setPretty(final boolean pretty) {
        this.pretty = pretty;
        return this;
    }

    public MapperBuilder setBufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public MapperBuilder setBufferStrategy(final String bufferStrategy) {
        this.bufferStrategy = bufferStrategy;
        return this;
    }

    public MapperBuilder setMaxSize(final int size) {
        this.maxSize = size;
        return this;
    }

    public MapperBuilder setAccessMode(final AccessMode mode) {
        this.accessMode = mode;
        return this;
    }

    public MapperBuilder setAccessModeName(final String mode) {
        if (!"field".equalsIgnoreCase(mode) && !"method".equalsIgnoreCase(mode) &&
                !"strict-method".equalsIgnoreCase(mode) && !"both".equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException("Mode " + mode + " unsupported");
        }
        this.accessModeName = mode;
        return this;
    }

    public MapperBuilder setAccessModeFieldFilteringStrategy(final BaseAccessMode.FieldFilteringStrategy strategy) {
        this.fieldFilteringStrategy = strategy;
        return this;
    }

    public MapperBuilder setAccessModeFieldFilteringStrategyName(final String mode) {
        switch (mode.toLowerCase(ROOT)) {
            case "all":
                return setAccessModeFieldFilteringStrategy(new BaseAccessMode.AllEntriesFieldFilteringStrategy());
            case "single":
                return setAccessModeFieldFilteringStrategy(new BaseAccessMode.SingleEntryFieldFilteringStrategy());
            default:
                throw new IllegalArgumentException("Unknown field filter strategy: " + mode);
        }
    }

    public MapperBuilder setSupportHiddenAccess(final boolean supportHiddenAccess) {
        this.supportHiddenAccess = supportHiddenAccess;
        return this;
    }

    public MapperBuilder setAttributeOrder(final Comparator<String> attributeOrder) {
        this.attributeOrder = attributeOrder;
        return this;
    }

    public MapperBuilder setReaderFactory(final JsonReaderFactory readerFactory) {
        this.readerFactory = readerFactory;
        return this;
    }

    public MapperBuilder setGeneratorFactory(final JsonGeneratorFactory generatorFactory) {
        this.generatorFactory = generatorFactory;
        return this;
    }

    public MapperBuilder setProvider(final JsonProvider provider) {
        this.provider = provider;
        return this;
    }

    public MapperBuilder setBuilderFactory(final JsonBuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
        return this;
    }

    public MapperBuilder setDoCloseOnStreams(final boolean doCloseOnStreams) {
        this.close = doCloseOnStreams;
        return this;
    }

    @Deprecated // use addAdapter
    public MapperBuilder addPropertyEditor(final Class<?> clazz, final Converter<?> converter) {
        adapters.put(new AdapterKey(clazz, String.class), new ConverterAdapter(converter, clazz));
        return this;
    }

    @Deprecated // use addAdapter
    public MapperBuilder addConverter(final Type clazz, final Converter<?> converter) {
        adapters.put(new AdapterKey(clazz, String.class), new ConverterAdapter(converter, clazz));
        return this;
    }

    public MapperBuilder addAdapter(final Type from, final Type to, final Adapter<?, ?> adapter) {
        adapters.put(new AdapterKey(from, to), adapter);
        return this;
    }

    public MapperBuilder addAdapter(final Adapter<?, ?> converter) {
        for (final Type gi : converter.getClass().getGenericInterfaces()) {
            if (ParameterizedType.class.isInstance(gi) && Adapter.class == ParameterizedType.class.cast(gi).getRawType()) {
                final Type[] args = ParameterizedType.class.cast(gi).getActualTypeArguments();
                adapters.put(new AdapterKey(args[0], args[1]), converter);
                return this;
            }
        }
        throw new IllegalArgumentException("Can't find Adapter generics from " + converter + ", please use addAdapter(t1, t2, adapter) instead");
    }

    public MapperBuilder setVersion(final int version) {
        this.version = version;
        return this;
    }

    public MapperBuilder setSkipNull(final boolean skipNull) {
        this.skipNull = skipNull;
        return this;
    }

    public MapperBuilder setSkipEmptyArray(final boolean skipEmptyArray) {
        this.skipEmptyArray = skipEmptyArray;
        return this;
    }

    public MapperBuilder setTreatByteArrayAsBase64(final boolean treatByteArrayAsBase64) {
        this.treatByteArrayAsBase64 = treatByteArrayAsBase64;
        return this;
    }

    public MapperBuilder setTreatByteArrayAsBase64URL(final boolean treatByteArrayAsBase64URL) {
        this.treatByteArrayAsBase64URL = treatByteArrayAsBase64URL;
        return this;
    }

    public MapperBuilder setSupportConstructors(final boolean supportConstructors) {
        this.supportConstructors = supportConstructors;
        return this;
    }

    public MapperBuilder setEncoding(final String encoding) {
        this.encoding = encoding == null ? null : Charset.forName(encoding);
        return this;
    }

    public MapperBuilder setReadAttributeBeforeWrite(final boolean readAttributeBeforeWrite) {
        this.readAttributeBeforeWrite = readAttributeBeforeWrite;
        return this;
    }

    public <T> MapperBuilder addObjectConverter(final Class<T> targetType, final MapperConverter objectConverter) {
        if (ObjectConverter.Reader.class.isInstance(objectConverter)) {
            this.objectConverterReaders.put(targetType, ObjectConverter.Reader.class.cast(objectConverter));
        }
        if (ObjectConverter.Writer.class.isInstance(objectConverter)) {
            this.objectConverterWriters.put(targetType, ObjectConverter.Writer.class.cast(objectConverter));
        }
        return this;
    }

    public MapperBuilder setPrimitiveConverters(final boolean val) {
        this.primitiveConverters = val;
        return this;
    }

    public MapperBuilder setSerializeValueFilter(final SerializeValueFilter serializeValueFilter) {
        this.serializeValueFilter = serializeValueFilter;
        return this;
    }

    public MapperBuilder setUseBigDecimalForFloats(final boolean useBigDecimalForFloats) {
        this.useBigDecimalForFloats = useBigDecimalForFloats;
        return this;
    }

    public MapperBuilder setAutoAdjustStringBuffers(final boolean autoAdjustStringBuffers) {
        this.autoAdjustStringBuffers = autoAdjustStringBuffers;
        return this;
    }

    /**
     * If any non-primitive Java Object gets serialised more than just one time,
     * then we write a JsonPointer to the first occurrence instead.
     *
     * This will effectively also avoid endless loops in data with cycles!
     *
     * An example: Assume you have a Person with a name 'Sarah' and her daughter,
     * a Person with the name 'Clemens' both stored in a JSON array.
     * Given the Java Code:
     * <pre>
     * Person sarah = new Person("Sarah");
     * Person clemens = new Person("Clemens");
     * clemens.setMother(sarah);
     * Person[] family = new Person[]{sarah, clemens};
     * </pre>
     * Transformed to JSON this will now look like the following:
     * <pre>
     * [{"name":"Sarah"},{"name":"Clemens","mother":"/0"}]
     * </pre>
     * That means instead of serialising 'mother' as full object we will
     * now only store a JsonPointer to the Person 'Sarah'.
     *
     * When deserialised back Johnzon will automatically de-reference the JsonPointer
     * back to the correct instance.
     *
     * Possible values:
     * <ul>
     *     <li>{@code true}: deduplicate objects</li>
     *     <li>{@code false}: do <b>not</b> deduplicate objects</li>
     *     <li>{@code null}: dedupliate based on the {@link JohnzonDeduplicateObjects} annotation. This is the default</li>
     * </ul>
     */
    public MapperBuilder setDeduplicateObjects(Boolean deduplicateObjects) {
        this.deduplicateObjects = deduplicateObjects;
        return this;
    }

    public MapperBuilder setUseJsRange(boolean value) {
        this.useJsRange = value;
        return this;
    }

    public MapperBuilder setUseBigDecimalForObjectNumbers(final boolean value) {
        this.useBigDecimalForObjectNumbers = value;
        return this;
    }

    public MapperBuilder setSupportEnumContainerDeserialization(final boolean supportEnumContainerDeserialization) {
        this.supportEnumContainerDeserialization = supportEnumContainerDeserialization;
        return this;
    }

    public MapperBuilder setPolymorphicSerializationPredicate(final Predicate<Class<?>> serializationPredicate) {
        this.serializationPredicate = serializationPredicate;
        return this;
    }

    public MapperBuilder setPolymorphicDeserializationPredicate(final Predicate<Class<?>> deserializationPredicate) {
        this.deserializationPredicate = deserializationPredicate;
        return this;
    }

    public MapperBuilder setPolymorphicDiscriminatorMapper(final Function<Class<?>, String> discriminatorMapper) {
        this.discriminatorMapper = discriminatorMapper;
        return this;
    }

    public MapperBuilder setPolymorphicTypeLoader(final Function<String, Class<?>> typeLoader) {
        this.typeLoader = typeLoader;
        return this;
    }

    public MapperBuilder setPolymorphicDiscriminator(final String value) {
        this.discriminator = value;
        return this;
    }

    public MapperBuilder setSkipAccessModeWrapper(final boolean skipAccessModeWrapper) {
        this.skipAccessModeWrapper = skipAccessModeWrapper;
        return this;
    }

    public MapperBuilder setMappingsFactory(Function<MapperConfig, Mappings> mappingsFactory) {
        this.mappingsFactory = mappingsFactory;
        return this;
    }
}