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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGeneratorFactory;

class JsonWriterFactoryImpl extends AbstractJsonFactory implements JsonWriterFactory{
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    static final Collection<String> SUPPORTED_CONFIG_KEYS = asList(

    );
    private final JsonGeneratorFactory factory;

    JsonWriterFactoryImpl(final Map<String, ?> config) {
        super(config, SUPPORTED_CONFIG_KEYS, JsonGeneratorFactoryImpl.SUPPORTED_CONFIG_KEYS);
        this.factory = new JsonGeneratorFactoryImpl(internalConfig);
    }

    @Override
    public JsonWriter createWriter(final Writer writer) {
        return new JsonWriterImpl(factory.createGenerator(writer));
    }

    @Override
    public JsonWriter createWriter(final OutputStream out) {
        return createWriter(new OutputStreamWriter(out, UTF8_CHARSET));
    }

    @Override
    public JsonWriter createWriter(final OutputStream out, final Charset charset) {
        return createWriter(new OutputStreamWriter(out, charset));
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(internalConfig);
    }
}
