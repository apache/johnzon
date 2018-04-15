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

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static java.util.Arrays.asList;

public class JsonParserFactoryImpl extends AbstractJsonFactory implements JsonParserFactory {
    public static final String MAX_STRING_LENGTH = "org.apache.johnzon.max-string-length";
    public static final int DEFAULT_MAX_STRING_LENGTH = Integer.getInteger(MAX_STRING_LENGTH, 256 * 1024); //256kB
    
    public static final String AUTO_ADJUST_STRING_BUFFER = "org.apache.johnzon.auto-adjust-buffer";
    public static final String BUFFER_LENGTH = "org.apache.johnzon.default-char-buffer";
    public static final int DEFAULT_BUFFER_LENGTH = Integer.getInteger(BUFFER_LENGTH, 64 * 1024); //64k
    
    public static final String SUPPORTS_COMMENTS = "org.apache.johnzon.supports-comments";
    public static final boolean DEFAULT_SUPPORTS_COMMENT = Boolean.getBoolean(SUPPORTS_COMMENTS); //default is false;

    static final Collection<String> SUPPORTED_CONFIG_KEYS = asList(
        BUFFER_STRATEGY, MAX_STRING_LENGTH, BUFFER_LENGTH, SUPPORTS_COMMENTS, AUTO_ADJUST_STRING_BUFFER
    );
      
    private final int maxSize;
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;
    private final BufferStrategy.BufferProvider<char[]> valueBufferProvider;
    private final boolean supportsComments;
    private final boolean autoAdjustBuffers;

    JsonParserFactoryImpl(final Map<String, ?> config) {
        super(config, SUPPORTED_CONFIG_KEYS, null);

        final int bufferSize = getInt(BUFFER_LENGTH, DEFAULT_BUFFER_LENGTH);
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("buffer length must be greater than zero");
        }

        this.maxSize = getInt(MAX_STRING_LENGTH, DEFAULT_MAX_STRING_LENGTH);
        this.bufferProvider = getBufferProvider().newCharProvider(bufferSize);
        this.valueBufferProvider = getBufferProvider().newCharProvider(maxSize);
        this.supportsComments = getBool(SUPPORTS_COMMENTS, DEFAULT_SUPPORTS_COMMENT);
        this.autoAdjustBuffers = getBool(AUTO_ADJUST_STRING_BUFFER, true);
    }

    private JsonStreamParserImpl getDefaultJsonParserImpl(final InputStream in) {
        if (supportsComments) {
            return new CommentsJsonStreamParserImpl(in, maxSize, bufferProvider, valueBufferProvider, autoAdjustBuffers);
        }
        //UTF Auto detection RFC 4627
        return new JsonStreamParserImpl(in, maxSize, bufferProvider, valueBufferProvider, autoAdjustBuffers);
    }

    private JsonStreamParserImpl getDefaultJsonParserImpl(final InputStream in, final Charset charset) {
        if (supportsComments) {
            return new CommentsJsonStreamParserImpl(in, charset, maxSize, bufferProvider, valueBufferProvider, autoAdjustBuffers);
        }
        //use provided charset
        return new JsonStreamParserImpl(in, charset, maxSize, bufferProvider, valueBufferProvider, autoAdjustBuffers);
    }

    private JsonStreamParserImpl getDefaultJsonParserImpl(final Reader in) {
        if (supportsComments) {
            return new CommentsJsonStreamParserImpl(in, maxSize, bufferProvider, valueBufferProvider, autoAdjustBuffers);
        }
        //no charset necessary
        return new JsonStreamParserImpl(in, maxSize, bufferProvider, valueBufferProvider, autoAdjustBuffers);
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
        // no need of a comment version since JsonObject has no comment event
        return new JsonInMemoryParser(obj);
    }

    @Override
    public JsonParser createParser(final JsonArray array) {
        // no need of a comment version since JsonObject has no comment event
        return new JsonInMemoryParser(array);
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(internalConfig);
    }

    public JsonStreamParserImpl createInternalParser(final InputStream in) {
        return getDefaultJsonParserImpl(in);
    }
    
    public JsonStreamParserImpl createInternalParser(final InputStream in, final Charset charset) {
        return getDefaultJsonParserImpl(in, charset);
    }

    public JsonStreamParserImpl createInternalParser(final Reader reader) {
        return getDefaultJsonParserImpl(reader);
    }
}
