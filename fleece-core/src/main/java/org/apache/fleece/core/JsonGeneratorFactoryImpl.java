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

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JsonGeneratorFactoryImpl implements JsonGeneratorFactory {
    private final Map<String, ?> config;
    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<String, String>();
    private final boolean pretty;

    public JsonGeneratorFactoryImpl(final Map<String, ?> config) {
        this.config = config;
        this.pretty = Boolean.TRUE.equals(config.get(JsonGenerator.PRETTY_PRINTING)) || "true".equals(config.get(JsonGenerator.PRETTY_PRINTING));
    }

    @Override
    public JsonGenerator createGenerator(final Writer writer) {
        return new JsonGeneratorFacade(newJsonGeneratorImpl(writer));
    }

    private JsonGenerator newJsonGeneratorImpl(final Writer writer) {
        if (pretty) {
            return new JsonPrettyGeneratorImpl(writer, cache);
        }
        return new JsonGeneratorImpl<JsonGeneratorImpl<?>>(writer, cache);
    }

    @Override
    public JsonGenerator createGenerator(final OutputStream out) {
        return createGenerator(new OutputStreamWriter(out));
    }

    @Override
    public JsonGenerator createGenerator(final OutputStream out, final Charset charset) {
        return createGenerator(new OutputStreamWriter(out, charset));
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(config);
    }
}
