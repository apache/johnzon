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
package org.apache.johnzon.core;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.stream.JsonParser;

class JsonReaderFactoryImpl extends AbstractJsonFactory implements JsonReaderFactory {
    static final Collection<String> SUPPORTED_CONFIG_KEYS = RejectDuplicateKeysMode.CONFIG_KEYS;

    private final JsonParserFactoryImpl parserFactory;
    private final RejectDuplicateKeysMode rejectDuplicateKeys;
    private JsonProviderImpl provider;

    JsonReaderFactoryImpl(final Map<String, ?> config, final JsonProviderImpl provider) {
        super(config, SUPPORTED_CONFIG_KEYS, JsonParserFactoryImpl.SUPPORTED_CONFIG_KEYS);
        this.provider = provider;
        if (!internalConfig.isEmpty()) {
            RejectDuplicateKeysMode.CONFIG_KEYS.forEach(internalConfig::remove);
        }
        this.parserFactory = new JsonParserFactoryImpl(internalConfig, provider);
        this.rejectDuplicateKeys = RejectDuplicateKeysMode.from(config);
    }

    @Override
    public JsonReader createReader(final Reader reader) {
        return new JsonReaderImpl(parserFactory.createInternalParser(reader), parserFactory.getValueBufferProvider(), rejectDuplicateKeys, provider);
    }

    @Override
    public JsonReader createReader(final InputStream in) {
        return new JsonReaderImpl(parserFactory.createInternalParser(in), parserFactory.getValueBufferProvider(), rejectDuplicateKeys, provider);
    }

    @Override
    public JsonReader createReader(final InputStream in, final Charset charset) {
        return new JsonReaderImpl(parserFactory.createInternalParser(in, charset), parserFactory.getValueBufferProvider(), rejectDuplicateKeys, provider);
    }

    public JsonReader createReader(final JsonParser parser) {
        return new JsonReaderImpl(parser, parserFactory.getValueBufferProvider(), rejectDuplicateKeys, provider);
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(internalConfig);
    }
}
