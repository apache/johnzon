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

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Contains internal configuration for all the mapper stuff.
 * It needs to be immutable and 100% runtime oriented.
 */
class MapperConfig implements Cloneable {
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
    private final Map<Class<?>, ObjectConverter<?>> objectConverters;
    private final Comparator<String> attributeOrder;

    //disable checkstyle for 10+ parameters
    //CHECKSTYLE:OFF
    public MapperConfig(final ConcurrentMap<AdapterKey, Adapter<?, ?>> adapters,
                        final Map<Class<?>, ObjectConverter<?>> objectConverters,
                        final int version, final boolean close,
                        final boolean skipNull, final boolean skipEmptyArray,
                        final boolean treatByteArrayAsBase64, final boolean treatByteArrayAsBase64URL,
                        final boolean readAttributeBeforeWrite,
                        final AccessMode accessMode, final Charset encoding,
                        final Comparator<String> attributeOrder) {
    //CHECKSTYLE:ON
        this.objectConverters = objectConverters;
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
    }

    public Adapter findAdapter(final Type aClass) {
        final Adapter<?, ?> converter = adapters.get(new AdapterKey(aClass, String.class));
        if (converter != null) {
            return converter;
        }
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

    public Map<Class<?>, ObjectConverter<?>> getObjectConverters() {
        return objectConverters;
    }

    public Comparator<String> getAttributeOrder() {
        return attributeOrder;
    }
}
