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
package org.apache.fleece.core;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

public class JsonParserFactoryImpl implements JsonParserFactory {
    public static final String MAX_STRING_LENGTH = "org.apache.fleece.max-string-length";
    public static final int DEFAULT_MAX_SIZE = Integer.getInteger(MAX_STRING_LENGTH, 8192);

    private final Map<String, ?> config;
    private final int maxSize;

    public JsonParserFactoryImpl(final Map<String, ?> config) {
        this.config = config;
        final Object maxStringSize = config.get(MAX_STRING_LENGTH);
        if (maxStringSize == null) {
            maxSize = DEFAULT_MAX_SIZE;
        } else if (Number.class.isInstance(maxStringSize)) {
            maxSize = Number.class.cast(maxStringSize).intValue();
        } else {
            maxSize = Integer.parseInt(maxStringSize.toString());
        }
    }
    
    JsonParser getDefaultJsonParserImpl(InputStream in) {
        return new JsonCharBufferStreamParser(in, maxSize);
    }
    
    JsonParser getDefaultJsonParserImpl(InputStream in, Charset charset) {
        return new JsonCharBufferStreamParser(in, charset, maxSize);
    }
    
    JsonParser getDefaultJsonParserImpl(Reader in) {
        return new JsonCharBufferStreamParser(in, maxSize);
    }

    @Override
    public JsonParser createParser(final Reader reader) {
        return getDefaultJsonParserImpl(reader);
    }

    @Override
    public JsonParser createParser(final InputStream in) {
        return getDefaultJsonParserImpl(in);
    }

    @Override
    public JsonParser createParser(final InputStream in, final Charset charset) {
        return getDefaultJsonParserImpl(in, charset);
    }

    @Override
    public JsonParser createParser(final JsonObject obj) {
        return new JsonInMemoryParser(obj);
    }

    @Override
    public JsonParser createParser(final JsonArray array) {
        return new JsonInMemoryParser(array);
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(config);
    }
}
