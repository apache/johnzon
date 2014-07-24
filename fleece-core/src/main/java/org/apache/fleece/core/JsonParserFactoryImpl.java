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
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class JsonParserFactoryImpl implements JsonParserFactory, Serializable {
    public static final String BUFFER_STRATEGY = "org.apache.fleece.buffer-strategy";
    public static final String MAX_STRING_LENGTH = "org.apache.fleece.max-string-length";
    public static final String BUFFER_LENGTH = "org.apache.fleece.default-char-buffer";
    public static final int DEFAULT_MAX_SIZE = Integer.getInteger(MAX_STRING_LENGTH, 8192*32);

    private final Map<String, ?> config;
    private final int maxSize;
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;
    private final BufferStrategy.BufferProvider<char[]> valueBufferProvider;

    public JsonParserFactoryImpl(final Map<String, ?> config) {
        this.config = config;

        final int bufferSize = getInt(BUFFER_LENGTH);
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("buffer length must be greater than zero");
        }

        this.maxSize = getInt(MAX_STRING_LENGTH);
        this.bufferProvider = getBufferProvider().newCharProvider(bufferSize);
        this.valueBufferProvider = getBufferProvider().newCharProvider(maxSize);
    }

    private BufferStrategy getBufferProvider() {
        if(config==null) {
            return BufferStrategy.QUEUE;
        }
        final Object name = config.get(BUFFER_STRATEGY);
        if (name != null) {
            return BufferStrategy.valueOf(name.toString().toUpperCase(Locale.ENGLISH));
        }
        return BufferStrategy.QUEUE;
    }

    private int getInt(final String key) {
        if(config==null) {
            return DEFAULT_MAX_SIZE;
        }
        final Object maxStringSize = config.get(key);
        if (maxStringSize == null) {
            return DEFAULT_MAX_SIZE;
        } else if (Number.class.isInstance(maxStringSize)) {
            return Number.class.cast(maxStringSize).intValue();
        }
        return Integer.parseInt(maxStringSize.toString());
    }

    private EscapedStringAwareJsonParser getDefaultJsonParserImpl(final InputStream in) {
        //UTF Auto detection RFC 4627
        return new JsonStreamParserImpl(in, maxSize, bufferProvider, valueBufferProvider);
    }

    private EscapedStringAwareJsonParser getDefaultJsonParserImpl(final InputStream in, final Charset charset) {
        //use provided charset
        return new JsonStreamParserImpl(in, charset, maxSize, bufferProvider, valueBufferProvider);
    }

    private EscapedStringAwareJsonParser getDefaultJsonParserImpl(final Reader in) {
        //no charset necessary
        return new JsonStreamParserImpl(in, maxSize, bufferProvider, valueBufferProvider);
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

    public EscapedStringAwareJsonParser createInternalParser(final InputStream in) {
        return getDefaultJsonParserImpl(in);
    }
    
    public EscapedStringAwareJsonParser createInternalParser(final InputStream in, final Charset charset) {
        return getDefaultJsonParserImpl(in, charset);
    }

    public EscapedStringAwareJsonParser createInternalParser(final Reader reader) {
        return getDefaultJsonParserImpl(reader);
    }
}
