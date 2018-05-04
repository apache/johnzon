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
import org.apache.johnzon.mapper.converter.EnumConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;

import javax.json.JsonObject;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

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
        public Object fromJson(JsonObject jsonObject, Type targetType, MappingParser parser) {
            return null;
        }
    };

    private final int version;
    private final boolean close;
    private final boolean skipNull;
    private final boolean skipEmptyArray;
    private final boolean treatByteArrayAsBase64;
    private final boolean treatByteArrayAsBase64URL;
    private final boolean readAttributeBeforeWrite;
    private final AccessMode accessMode;
    private final Charset encoding;
    private final ConcurrentMap<AdapterKey, Adapter<?, ?>> adapters;
    private final Map<Class<?>, ObjectConverter.Writer<?>> objectConverterWriters;
    private final Map<Class<?>, ObjectConverter.Reader<?>> objectConverterReaders;
    private final Comparator<String> attributeOrder;
    private final boolean enforceQuoteString;
    private final boolean failOnUnknown;
    private final SerializeValueFilter serializeValueFilter;
    private final boolean useBigDecimalForFloats;
    private final Boolean deduplicateObjects;

    private final Map<Class<?>, ObjectConverter.Writer<?>> objectConverterWriterCache;
    private final Map<Class<?>, ObjectConverter.Reader<?>> objectConverterReaderCache;

    //disable checkstyle for 10+ parameters
    //CHECKSTYLE:OFF
    public MapperConfig(final ConcurrentMap<AdapterKey, Adapter<?, ?>> adapters,
                        final Map<Class<?>, ObjectConverter.Writer<?>> objectConverterWriters,
                        final Map<Class<?>, ObjectConverter.Reader<?>> objectConverterReaders,
                        final int version, final boolean close,
                        final boolean skipNull, final boolean skipEmptyArray,
                        final boolean treatByteArrayAsBase64, final boolean treatByteArrayAsBase64URL,
                        final boolean readAttributeBeforeWrite,
                        final AccessMode accessMode, final Charset encoding,
                        final Comparator<String> attributeOrder,
                        final boolean enforceQuoteString, final boolean failOnUnknown,
                        final SerializeValueFilter serializeValueFilter,
                        final boolean useBigDecimalForFloats,
                        final Boolean deduplicateObjects) {
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
        this.adapters = adapters;
        this.attributeOrder = attributeOrder;
        this.enforceQuoteString = enforceQuoteString;
        this.failOnUnknown = failOnUnknown;

        this.serializeValueFilter = serializeValueFilter != null ? serializeValueFilter : new SerializeValueFilter() {
            @Override
            public boolean shouldIgnore(String name, Object value) {
                return false;
            }
        };

        this.objectConverterWriterCache = new HashMap<Class<?>, ObjectConverter.Writer<?>>(objectConverterWriters.size());
        this.objectConverterReaderCache = new HashMap<Class<?>, ObjectConverter.Reader<?>>(objectConverterReaders.size());
        this.useBigDecimalForFloats = useBigDecimalForFloats;
        this.deduplicateObjects = deduplicateObjects;
    }


    public SerializeValueFilter getSerializeValueFilter() {
        return serializeValueFilter;
    }

    public Adapter findAdapter(final Type aClass) {
        final Adapter<?, ?> converter = adapters.get(new AdapterKey(aClass, String.class));
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
            if (clazz.isEnum()) {
                final Adapter<?, ?> enumConverter = new ConverterAdapter(new EnumConverter(clazz));
                adapters.putIfAbsent(new AdapterKey(String.class, aClass), enumConverter);
                return enumConverter;
            }
        }
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

    public ConcurrentMap<AdapterKey, Adapter<?, ?>> getAdapters() {
        return adapters;
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

    public boolean isEnforceQuoteString() {
        return enforceQuoteString;
    }

    public boolean isUseBigDecimalForFloats() {
        return useBigDecimalForFloats;
    }

    public Boolean isDeduplicateObjects() {
        return deduplicateObjects;
    }
}
