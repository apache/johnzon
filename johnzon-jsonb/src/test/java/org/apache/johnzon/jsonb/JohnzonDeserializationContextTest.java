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

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.json.Json;
import javax.json.bind.serializer.DeserializationContext;
import org.apache.johnzon.jsonb.serializer.JohnzonDeserializationContext;
import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.MapperConfig;
import org.apache.johnzon.mapper.MappingParser;
import org.apache.johnzon.mapper.MappingParserImpl;
import org.apache.johnzon.mapper.Mappings;
import org.apache.johnzon.mapper.ObjectConverter;
import org.apache.johnzon.mapper.SerializeValueFilter;
import org.apache.johnzon.mapper.access.FieldAndMethodAccessMode;
import org.apache.johnzon.mapper.converter.BigDecimalConverter;
import org.apache.johnzon.mapper.converter.BigIntegerConverter;
import org.apache.johnzon.mapper.converter.ClassConverter;
import org.apache.johnzon.mapper.converter.LocaleConverter;
import org.apache.johnzon.mapper.converter.StringConverter;
import org.apache.johnzon.mapper.converter.URIConverter;
import org.apache.johnzon.mapper.converter.URLConverter;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.apache.johnzon.mapper.internal.ConverterAdapter;
import org.junit.Assert;
import org.junit.Test;


public class JohnzonDeserializationContextTest {
    
    private static final Map<AdapterKey, Adapter<?, ?>> DEFAULT_CONVERTERS = new HashMap<AdapterKey, Adapter<?, ?>>(24);
    private final int version = -1;
    private boolean close;
    private final boolean skipNull = true;
    private boolean skipEmptyArray;
    private boolean treatByteArrayAsBase64;
    private boolean treatByteArrayAsBase64URL;
    private boolean readAttributeBeforeWrite;
    private boolean enforceQuoteString;
    private final boolean supportHiddenAccess = true;
    private final Comparator<String> attributeOrder = null;
    private boolean supportConstructors;
    private boolean useGetterForCollections;
    private final FieldAndMethodAccessMode accessMode = new FieldAndMethodAccessMode(supportConstructors, supportHiddenAccess, useGetterForCollections);
    private final Charset encoding = Charset.forName(System.getProperty("johnzon.mapper.encoding", "UTF-8"));
    private boolean failOnUnknownProperties;
    private SerializeValueFilter serializeValueFilter;
    private boolean useBigDecimalForFloats;
    private final Boolean deduplicateObjects = null;
    private final DeserializationContext deserializationContext;
   
    static {
        DEFAULT_CONVERTERS.put(new AdapterKey(URL.class, String.class), new ConverterAdapter<>(new URLConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(URI.class, String.class), new ConverterAdapter<>(new URIConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Class.class, String.class), new ConverterAdapter<>(new ClassConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(String.class, String.class), new ConverterAdapter<>(new StringConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(BigDecimal.class, String.class), new ConverterAdapter<>(new BigDecimalConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(BigInteger.class, String.class), new ConverterAdapter<>(new BigIntegerConverter()));
        DEFAULT_CONVERTERS.put(new AdapterKey(Locale.class, String.class), new LocaleConverter());
    }
    
    public JohnzonDeserializationContextTest() {
        
        ConcurrentMap<AdapterKey, Adapter<?, ?>> adapters = new ConcurrentHashMap<>(DEFAULT_CONVERTERS);
        Map<Class<?>, ObjectConverter.Reader<?>> objectConverterReaders = new HashMap<>();
        Map<Class<?>, ObjectConverter.Writer<?>> objectConverterWriters = new HashMap<>();
        
        MapperConfig mapperConfig = new MapperConfig(
                adapters, objectConverterWriters, objectConverterReaders,
                version, close,
                skipNull, skipEmptyArray,
                treatByteArrayAsBase64, treatByteArrayAsBase64URL, readAttributeBeforeWrite,
                accessMode, encoding, attributeOrder, enforceQuoteString, failOnUnknownProperties,
                serializeValueFilter, useBigDecimalForFloats, deduplicateObjects);
        
        MappingParser mappingParserImpl = new MappingParserImpl(mapperConfig, new Mappings(mapperConfig), Json.createReader(new StringReader("")), false);
        deserializationContext = new JohnzonDeserializationContext(mappingParserImpl);
        
    }      

    @Test
    public void deserialize() {
        
        Assert.assertEquals(TestEnum.ONE, deserializationContext.deserialize(TestEnum.class, Json.createParser(new StringReader("\"ONE\""))));
        
    }
    
    private enum TestEnum {
    
        ONE, TWO, THREE;
        
    }
    
}
