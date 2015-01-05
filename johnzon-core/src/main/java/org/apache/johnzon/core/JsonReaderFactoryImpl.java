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

import static java.util.Arrays.asList;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

class JsonReaderFactoryImpl extends AbstractJsonFactory implements JsonReaderFactory {
    static final Collection<String> SUPPORTED_CONFIG_KEYS = asList(

    );
    private final JsonParserFactoryImpl parserFactory;

    JsonReaderFactoryImpl(final Map<String, ?> config) {
        super(config, SUPPORTED_CONFIG_KEYS, JsonParserFactoryImpl.SUPPORTED_CONFIG_KEYS);
        this.parserFactory = new JsonParserFactoryImpl(internalConfig);
    }

    @Override
    public JsonReader createReader(final Reader reader) {
        return new JsonReaderImpl(parserFactory.createInternalParser(reader));
    }

    @Override
    public JsonReader createReader(final InputStream in) {
        return new JsonReaderImpl(parserFactory.createInternalParser(in));
    }

    @Override
    public JsonReader createReader(final InputStream in, final Charset charset) {
        return new JsonReaderImpl(parserFactory.createInternalParser(in, charset));
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(internalConfig);
    }
}
