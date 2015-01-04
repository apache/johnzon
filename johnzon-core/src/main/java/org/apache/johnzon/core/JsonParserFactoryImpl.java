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
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;

class JsonParserFactoryImpl implements JsonParserFactory, Serializable {
    private static final Logger LOGGER = Logger.getLogger(JsonParserFactoryImpl.class.getName());

    public static final String BUFFER_STRATEGY = "org.apache.johnzon.buffer-strategy";
    public static final String MAX_STRING_LENGTH = "org.apache.johnzon.max-string-length";
    public static final String BUFFER_LENGTH = "org.apache.johnzon.default-char-buffer";
    public static final String SUPPORTS_COMMENTS = "org.apache.johnzon.supports-comments";
    public static final int DEFAULT_MAX_SIZE = Integer.getInteger(MAX_STRING_LENGTH, 8192*32); //TODO check default string length/buffer size
    public static final boolean DEFAULT_SUPPORTS_COMMENT = false;

    private final Map<String, Object> internalConfig = new HashMap<String, Object>();
    private static final String[] SUPPORTED_CONFIG_KEYS = new String[] {
        BUFFER_STRATEGY, MAX_STRING_LENGTH, BUFFER_LENGTH, SUPPORTS_COMMENTS
    };
      
    private final int maxSize;
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;
    private final BufferStrategy.BufferProvider<char[]> valueBufferProvider;
    private final boolean supportsComments;

    JsonParserFactoryImpl(final Map<String, ?> config) {
        if(config != null) {
            
            for (String configKey : SUPPORTED_CONFIG_KEYS) {
                if(config.containsKey(configKey)) {
                    internalConfig.put(configKey, config.get(configKey));
                } else {
                    LOGGER.warning(configKey + " is not supported by " + getClass().getName());
                }
            }
        } 
        

        final int bufferSize = getInt(BUFFER_LENGTH);
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("buffer length must be greater than zero");
        }

        this.maxSize = getInt(MAX_STRING_LENGTH);
        this.bufferProvider = getBufferProvider().newCharProvider(bufferSize);
        this.valueBufferProvider = getBufferProvider().newCharProvider(maxSize);
        this.supportsComments = getBool(SUPPORTS_COMMENTS);
    }

    private BufferStrategy getBufferProvider() {
        final Object name = internalConfig.get(BUFFER_STRATEGY);
        if (name != null) {
            return BufferStrategy.valueOf(name.toString().toUpperCase(Locale.ENGLISH));
        }
        return BufferStrategy.QUEUE;
    }

    private int getInt(final String key) {
        final Object maxStringSize = internalConfig.get(key);
        if (maxStringSize == null) {
            return DEFAULT_MAX_SIZE;
        } else if (Number.class.isInstance(maxStringSize)) {
            return Number.class.cast(maxStringSize).intValue();
        }
        return Integer.parseInt(maxStringSize.toString());
    }

    private boolean getBool(final String key) {
        final Object maxStringSize = internalConfig.get(key);
        if (maxStringSize == null) {
            return DEFAULT_SUPPORTS_COMMENT;
        } else if (Boolean.class.isInstance(maxStringSize)) {
            return Boolean.class.cast(maxStringSize);
        }
        return Boolean.parseBoolean(maxStringSize.toString());
    }

    private JsonParser getDefaultJsonParserImpl(final InputStream in) {
        if (supportsComments) {
            return new CommentsJsonStreamParserImpl(in, maxSize, bufferProvider, valueBufferProvider);
        }
        //UTF Auto detection RFC 4627
        return new JsonStreamParserImpl(in, maxSize, bufferProvider, valueBufferProvider);
    }

    private JsonParser getDefaultJsonParserImpl(final InputStream in, final Charset charset) {
        if (supportsComments) {
            return new CommentsJsonStreamParserImpl(in, charset, maxSize, bufferProvider, valueBufferProvider);
        }
        //use provided charset
        return new JsonStreamParserImpl(in, charset, maxSize, bufferProvider, valueBufferProvider);
    }

    private JsonParser getDefaultJsonParserImpl(final Reader in) {
        if (supportsComments) {
            return new CommentsJsonStreamParserImpl(in, maxSize, bufferProvider, valueBufferProvider);
        }
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

    public JsonParser createInternalParser(final InputStream in) {
        return getDefaultJsonParserImpl(in);
    }
    
    public JsonParser createInternalParser(final InputStream in, final Charset charset) {
        return getDefaultJsonParserImpl(in, charset);
    }

    public JsonParser createInternalParser(final Reader reader) {
        return getDefaultJsonParserImpl(reader);
    }
}
