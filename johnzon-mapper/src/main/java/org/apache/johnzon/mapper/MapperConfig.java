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

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.converter.BigDecimalConverter;
import org.apache.johnzon.mapper.converter.BigIntegerConverter;
import org.apache.johnzon.mapper.converter.BooleanConverter;
import org.apache.johnzon.mapper.converter.ByteConverter;
import org.apache.johnzon.mapper.converter.CachedDelegateConverter;
import org.apache.johnzon.mapper.converter.CharacterConverter;
import org.apache.johnzon.mapper.converter.ClassConverter;
import org.apache.johnzon.mapper.converter.DateConverter;
import org.apache.johnzon.mapper.converter.DoubleConverter;
import org.apache.johnzon.mapper.converter.EnumConverter;
import org.apache.johnzon.mapper.converter.FloatConverter;
import org.apache.johnzon.mapper.converter.IntegerConverter;
import org.apache.johnzon.mapper.converter.LocaleConverter;
import org.apache.johnzon.mapper.converter.LongConverter;
import org.apache.johnzon.mapper.converter.ShortConverter;
import org.apache.johnzon.mapper.converter.StringConverter;
import org.apache.johnzon.mapper.converter.URIConverter;
import org.apache.johnzon.mapper.converter.URLConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;

/**
 * Contains internal configuration for all the mapper stuff
 */
class MapperConfig implements Cloneable {
    private static final Map<AdapterKey, Adapter<?, ?>> DEFAULT_CONVERTERS = new HashMap<AdapterKey, Adapter<?, ?>>(23);
    static {
        //DEFAULT_CONVERTERS.put(Date.class, new DateConverter("yyyy-MM-dd'T'HH:mm:ssZ")); // ISO8601 long RFC822 zone
        DEFAULT_CONVERTERS.put(new AdapterKey(Date.class, String.class), new ConverterAdapter<Date>(new DateConverter("yyyyMMddHHmmssZ"))); // ISO8601 short
        DEFAULT_CONVERTERS.put(new AdapterKey(URL.class, String.class), new ConverterAdapter<URL>(new URLConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(URI.class, String.class), new ConverterAdapter<URI>(new URIConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Class.class, String.class), new ConverterAdapter<Class<?>>(new ClassConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(String.class, String.class), new ConverterAdapter<String>(new StringConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(BigDecimal.class, String.class), new ConverterAdapter<BigDecimal>(new BigDecimalConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(BigInteger.class, String.class), new ConverterAdapter<BigInteger>(new BigIntegerConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Byte.class, String.class), new ConverterAdapter<Byte>(new CachedDelegateConverter<Byte>(new ByteConverter())));
        DEFAULT_CONVERTERS.put(new AdapterKey(Character.class, String.class), new ConverterAdapter<Character>(new CharacterConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Double.class, String.class), new ConverterAdapter<Double>(new DoubleConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Float.class, String.class), new ConverterAdapter<Float>(new FloatConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Integer.class, String.class), new ConverterAdapter<Integer>(new IntegerConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Long.class, String.class), new ConverterAdapter<Long>(new LongConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Short.class, String.class), new ConverterAdapter<Short>(new ShortConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Boolean.class, String.class), new ConverterAdapter<Boolean>(new CachedDelegateConverter<Boolean>(new BooleanConverter())));
        DEFAULT_CONVERTERS.put(new AdapterKey(byte.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Byte.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(char.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Character.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(double.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Double.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(float.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Float.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(int.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Integer.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(long.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Long.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(short.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Short.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(boolean.class, String.class), DEFAULT_CONVERTERS.get(new AdapterKey(Boolean.class, String.class)));
        DEFAULT_CONVERTERS.put(new AdapterKey(Locale.class, String.class), new LocaleConverter());
    }


    private int version = -1;

    private boolean close = false;
    private boolean skipNull = true;
    private boolean skipEmptyArray = false;
    private boolean supportsComments = false;
    private boolean treatByteArrayAsBase64;
    private boolean treatByteArrayAsBase64URL;
    private boolean readAttributeBeforeWrite;
    private boolean prettyPrint;
    private AccessMode accessMode;
    private Charset encoding = Charset.forName(System.getProperty("johnzon.mapper.encoding", "UTF-8"));
    private ConcurrentMap<AdapterKey, Adapter<?, ?>> adapters = new ConcurrentHashMap<AdapterKey, Adapter<?, ?>>(DEFAULT_CONVERTERS);;


    //X TODO we need a more elaborated approache at the end, but for now it's fine
    private Map<Class<?>, ObjectConverter<?>> objectConverters = new HashMap<Class<?>, ObjectConverter<?>>();


    MapperConfig() {
    }

    void setClose(boolean close) {
        this.close = close;
    }

    public boolean isClose() {
        return close;
    }

    public boolean isSkipNull() {
        return skipNull;
    }

    void setSkipNull(boolean skipNull) {
        this.skipNull = skipNull;
    }

    public boolean isSkipEmptyArray() {
        return skipEmptyArray;
    }

    void setSkipEmptyArray(boolean skipEmptyArray) {
        this.skipEmptyArray = skipEmptyArray;
    }

    public boolean isSupportsComments() {
        return supportsComments;
    }

    void setSupportsComments(boolean supportsComments) {
        this.supportsComments = supportsComments;
    }

    public boolean isTreatByteArrayAsBase64() {
        return treatByteArrayAsBase64;
    }

    void setTreatByteArrayAsBase64(boolean treatByteArrayAsBase64) {
        this.treatByteArrayAsBase64 = treatByteArrayAsBase64;
    }

    public boolean isTreatByteArrayAsBase64URL() {
        return treatByteArrayAsBase64URL;
    }

    void setTreatByteArrayAsBase64URL(boolean treatByteArrayAsBase64URL) {
        this.treatByteArrayAsBase64URL = treatByteArrayAsBase64URL;
    }

    public boolean isReadAttributeBeforeWrite() {
        return readAttributeBeforeWrite;
    }

    void setReadAttributeBeforeWrite(boolean readAttributeBeforeWrite) {
        this.readAttributeBeforeWrite = readAttributeBeforeWrite;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    void setAccessMode(AccessMode accessMode) {
        this.accessMode = accessMode;
    }

    public Charset getEncoding() {
        return encoding;
    }

    void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    <T> void addObjectConverter(Class<T> targetType, ObjectConverter<T> objectConverter) {
        objectConverters.put(targetType, objectConverter);
    }

    public Map<Class<?>, ObjectConverter<?>> getObjectConverters() {
        return objectConverters;
    }

    public ConcurrentMap<AdapterKey, Adapter<?, ?>> getAdapters() {
        return adapters;
    }

    void addAdapter(AdapterKey adapterKey, Adapter adapter) {
        adapters.put(adapterKey, adapter);
    }

    public int getVersion() {
        return version;
    }

    void setVersion(int version) {
        this.version = version;
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


    @Override
    public MapperConfig clone() {
        try {
            return (MapperConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
