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

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

public class JsonGeneratorFactoryImpl extends AbstractJsonFactory implements JsonGeneratorFactory {    
    public static final String GENERATOR_BUFFER_LENGTH = "org.apache.johnzon.default-char-buffer-generator";
    public static final int DEFAULT_GENERATOR_BUFFER_LENGTH =  Integer.getInteger(GENERATOR_BUFFER_LENGTH, 64 * 1024); //64k
   
    static final Collection<String> SUPPORTED_CONFIG_KEYS = asList(
        JsonGenerator.PRETTY_PRINTING, GENERATOR_BUFFER_LENGTH, BUFFER_STRATEGY
    );
    //key caching currently disabled
    private final ConcurrentMap<String, String> cache = null;//new ConcurrentHashMap<String, String>();
    private final boolean pretty;
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;

    public JsonGeneratorFactoryImpl(final Map<String, ?> config) {
        
          super(config, SUPPORTED_CONFIG_KEYS, null); 
          
          this.pretty = getBool(JsonGenerator.PRETTY_PRINTING, false);
          
          final int bufferSize = getInt(GENERATOR_BUFFER_LENGTH, DEFAULT_GENERATOR_BUFFER_LENGTH);
          if (bufferSize <= 0) {
              throw new IllegalArgumentException("buffer length must be greater than zero");
          }

          this.bufferProvider = getBufferProvider().newCharProvider(bufferSize);
    }

    @Override
    public JsonGenerator createGenerator(final Writer writer) {
        return new JsonGeneratorImpl(writer, bufferProvider, cache, pretty);
    }

    @Override
    public JsonGenerator createGenerator(final OutputStream out) {
        return new JsonGeneratorImpl(out, bufferProvider, cache, pretty);
    }

    @Override
    public JsonGenerator createGenerator(final OutputStream out, final Charset charset) {
        return new JsonGeneratorImpl(out,charset, bufferProvider, cache, pretty);
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(internalConfig);
    }
}
