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

import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonProviderImpl extends JsonProvider {
    @Override
    public JsonParser createParser(final InputStream in) {
        return createParserFactory(Collections.<String, Object>emptyMap()).createParser(in);
    }

    @Override
    public JsonParser createParser(final Reader reader) {
        return createParserFactory(Collections.<String, Object>emptyMap()).createParser(reader);
    }

    @Override
    public JsonReader createReader(final InputStream in) {
        return new JsonReaderImpl(in);
    }

    @Override
    public JsonReader createReader(final Reader reader) {
        return new JsonReaderImpl(reader);
    }

    @Override
    public JsonParserFactory createParserFactory(final Map<String, ?> config) {
        return new JsonParserFactoryImpl(config);
    }

    @Override
    public JsonReaderFactory createReaderFactory(final Map<String, ?> config) {
        return new JsonReaderFactoryImpl(config);
    }

    @Override
    public JsonGenerator createGenerator(final Writer writer) {
        return new JsonGeneratorFacade(new JsonGeneratorImpl(writer, new ConcurrentHashMap<String, String>()));
    }

    @Override
    public JsonGenerator createGenerator(final OutputStream out) {
        return createGenerator(new OutputStreamWriter(out));
    }

    @Override
    public JsonGeneratorFactory createGeneratorFactory(final Map<String, ?> config) {
        return new JsonGeneratorFactoryImpl(config);
    }

    @Override
    public JsonWriter createWriter(final Writer writer) {
        return new JsonWriterImpl(createGenerator(writer));
    }

    @Override
    public JsonWriter createWriter(final OutputStream out) {
        return createWriter(new OutputStreamWriter(out));
    }

    @Override
    public JsonWriterFactory createWriterFactory(final Map<String, ?> config) {
        return new JsonWriterFactoryImpl(config);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder() {
        return new JsonObjectBuilderImpl();
    }

    @Override
    public JsonArrayBuilder createArrayBuilder() {
        return new JsonArrayBuilderImpl();
    }

    @Override
    public JsonBuilderFactory createBuilderFactory(final Map<String, ?> config) {
        return new JsonBuilderFactoryImpl(config);
    }
}
