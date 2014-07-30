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

import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

public class JsonGeneratorFactoryImpl implements JsonGeneratorFactory, Serializable {    
    public static final String BUFFER_LENGTH = "org.apache.fleece.default-char-buffer-generator";
    public static final int DEFAULT_BUFFER_LENGTH = Integer.getInteger(BUFFER_LENGTH, 1024); //TODO check default string length/buffer size
    private final Map<String, Object> internalConfig = new HashMap<String, Object>();
    private static final String[] SUPPORTED_CONFIG_KEYS = new String[] {
        
        JsonGenerator.PRETTY_PRINTING, BUFFER_LENGTH, JsonParserFactoryImpl.BUFFER_STRATEGY 
        
    };
    //key caching currently disabled
    private final ConcurrentMap<String, String> cache = null;//new ConcurrentHashMap<String, String>();
    private final boolean pretty;
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;

    public JsonGeneratorFactoryImpl(final Map<String, ?> config) {
        
          if(config != null) {
          
              for (String configKey : SUPPORTED_CONFIG_KEYS) {
                  if(config.containsKey(configKey)) {
                      internalConfig.put(configKey, config.get(configKey));
                  }
              }
          } 

          if(internalConfig.containsKey(JsonGenerator.PRETTY_PRINTING)) {
              this.pretty = Boolean.TRUE.equals(internalConfig.get(JsonGenerator.PRETTY_PRINTING)) || "true".equals(internalConfig.get(JsonGenerator.PRETTY_PRINTING));
          } else {
              this.pretty = false;
          }
          
          final int bufferSize = getInt(BUFFER_LENGTH);
          if (bufferSize <= 0) {
              throw new IllegalArgumentException("buffer length must be greater than zero");
          }

          this.bufferProvider = getBufferProvider().newCharProvider(bufferSize);
    }
    
    private BufferStrategy getBufferProvider() {
        final Object name = internalConfig.get(JsonParserFactoryImpl.BUFFER_STRATEGY);
        if (name != null) {
            return BufferStrategy.valueOf(name.toString().toUpperCase(Locale.ENGLISH));
        }
        return BufferStrategy.QUEUE;
    }

    private int getInt(final String key) {
        final Object maxStringSize = internalConfig.get(key);
        if (maxStringSize == null) {
            return DEFAULT_BUFFER_LENGTH;
        } else if (Number.class.isInstance(maxStringSize)) {
            return Number.class.cast(maxStringSize).intValue();
        }
        return Integer.parseInt(maxStringSize.toString());
    }

    @Override
    public JsonGenerator createGenerator(final Writer writer) {
        if (pretty) {
            return new JsonPrettyGeneratorImpl(writer, bufferProvider, cache);
        }
        return new JsonGeneratorImpl(writer, bufferProvider, cache);
    }

   

    @Override
    public JsonGenerator createGenerator(final OutputStream out) {
        if (pretty) {
            return new JsonPrettyGeneratorImpl(out, bufferProvider, cache);
        }
        return new JsonGeneratorImpl(out, bufferProvider, cache);
    }

    @Override
    public JsonGenerator createGenerator(final OutputStream out, final Charset charset) {
        if (pretty) {
            return new JsonPrettyGeneratorImpl(out,charset, bufferProvider, cache);
        }
        return new JsonGeneratorImpl(out,charset, bufferProvider, cache);
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(internalConfig);
    }
}
