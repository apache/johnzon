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

import org.apache.johnzon.core.spi.JsonPointerFactory;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceConsumer;
import aQute.bnd.annotation.spi.ServiceProvider;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonMergePatch;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;
import javax.json.JsonPointer;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;

import static java.util.Comparator.comparing;

@ServiceProvider(value = JsonProvider.class, attribute = { "service.vendor:String='org.apache.johnzon'" })
@ServiceConsumer(value = JsonPointerFactory.class, resolution = Resolution.OPTIONAL)
public class JsonProviderImpl extends JsonProvider implements Serializable {
    private final Supplier<BufferStrategy.BufferProvider<char[]>> bufferProvider = new Cached<>(() ->
        BufferStrategyFactory.valueOf(System.getProperty(AbstractJsonFactory.BUFFER_STRATEGY, "QUEUE"))
            .newCharProvider(Integer.getInteger("org.apache.johnzon.default-char-provider.length", 1024)));

    private final JsonReaderFactory readerFactory = new JsonReaderFactoryImpl(null);
    private final JsonParserFactory parserFactory = new JsonParserFactoryImpl(null);
    private final JsonGeneratorFactory generatorFactory = new JsonGeneratorFactoryImpl(null);
    private final JsonWriterFactory writerFactory = new JsonWriterFactoryImpl(null);
    private final Supplier<JsonBuilderFactory> builderFactory = new Cached<>(() ->
            new JsonBuilderFactoryImpl(null, bufferProvider.get(), RejectDuplicateKeysMode.DEFAULT));
    private final JsonPointerFactory jsonPointerFactory;

    public JsonProviderImpl() {
        jsonPointerFactory = StreamSupport.stream(ServiceLoader.load(JsonPointerFactory.class).spliterator(), false)
                .min(comparing(JsonPointerFactory::ordinal))
                .orElseGet(DefaultJsonPointerFactory::new);
    }

    @Override
    public JsonParser createParser(final InputStream in) {
        return parserFactory.createParser(in);
    }

    @Override
    public JsonParser createParser(final Reader reader) {
        return parserFactory.createParser(reader);
    }

    @Override
    public JsonReader createReader(final InputStream in) {
        return readerFactory.createReader(in);
    }

    @Override
    public JsonReader createReader(final Reader reader) {
        return readerFactory.createReader(reader);
    }

    @Override
    public JsonParserFactory createParserFactory(final Map<String, ?> config) {
        return (config == null || config.isEmpty()) ? parserFactory : new JsonParserFactoryImpl(config);
    }

    @Override
    public JsonReaderFactory createReaderFactory(final Map<String, ?> config) {
        return (config == null || config.isEmpty()) ? readerFactory : new JsonReaderFactoryImpl(config);
    }

    @Override
    public JsonGenerator createGenerator(final Writer writer) {
        return generatorFactory.createGenerator(writer);
    }

    @Override
    public JsonGenerator createGenerator(final OutputStream out) {
        return generatorFactory.createGenerator(out);
    }

    @Override
    public JsonGeneratorFactory createGeneratorFactory(final Map<String, ?> config) {
        return (config == null || config.isEmpty()) ? generatorFactory : new JsonGeneratorFactoryImpl(config);
    }

    @Override
    public JsonWriter createWriter(final Writer writer) {
        return writerFactory.createWriter(writer);
    }

    @Override
    public JsonWriter createWriter(final OutputStream out) {
        return writerFactory.createWriter(out);
    }

    @Override
    public JsonWriterFactory createWriterFactory(final Map<String, ?> config) {
        return (config == null || config.isEmpty()) ? writerFactory : new JsonWriterFactoryImpl(config);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder() {
        return builderFactory.get().createObjectBuilder();
    }

    @Override
    public JsonObjectBuilder createObjectBuilder(JsonObject jsonObject) {
        return builderFactory.get().createObjectBuilder(jsonObject);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder(Map<String, Object> initialValues) {
        return builderFactory.get().createObjectBuilder(initialValues);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder() {
        return builderFactory.get().createArrayBuilder();
    }

    @Override
    public JsonArrayBuilder createArrayBuilder(JsonArray initialData) {
        return builderFactory.get().createArrayBuilder(initialData);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder(Collection<?> initialData) {
        return builderFactory.get().createArrayBuilder(initialData);
    }

    @Override
    public JsonString createValue(final String value) {
        return new JsonStringImpl(value);
    }

    @Override
    public JsonNumber createValue(final int value) {
        return new JsonLongImpl(value);
    }

    @Override
    public JsonNumber createValue(final long value) {
        return new JsonLongImpl(value);
    }

    @Override
    public JsonNumber createValue(final double value) {
        return new JsonDoubleImpl(value);
    }

    @Override
    public JsonNumber createValue(final BigDecimal value) {
        return new JsonNumberImpl(value);
    }

    @Override
    public JsonNumber createValue(final BigInteger value) {
        return new JsonNumberImpl(new BigDecimal(value.toString()));
    }

    @Override
    public JsonBuilderFactory createBuilderFactory(final Map<String, ?> config) {
        final JsonBuilderFactory builderFactory = this.builderFactory.get();
        return (config == null || config.isEmpty()) ?
                builderFactory : new JsonBuilderFactoryImpl(config, bufferProvider.get(), RejectDuplicateKeysMode.from(config));
    }

    @Override
    public JsonPatchBuilder createPatchBuilder() {
        return new JsonPatchBuilderImpl(this);
    }

    @Override
    public JsonPatchBuilder createPatchBuilder(JsonArray initialData) {
        return new JsonPatchBuilderImpl(this, initialData);
    }

    @Override
    public JsonPointer createPointer(String path) {
        return jsonPointerFactory.createPointer(this, path);
    }

    @Override
    public JsonPatch createPatch(JsonArray array) {
        return createPatchBuilder(array).build();
    }

    @Override
    public JsonPatch createDiff(JsonStructure source, JsonStructure target) {
        return new JsonPatchDiff(this, source, target).calculateDiff();
    }

    @Override
    public JsonMergePatch createMergePatch(JsonValue patch) {
        return new JsonMergePatchImpl(patch, bufferProvider.get());
    }

    @Override
    public JsonMergePatch createMergeDiff(JsonValue source, JsonValue target) {
        return new JsonMergePatchDiff(source, target, bufferProvider.get()).calculateDiff();
    }

    /**
     * Enables to not allocate potentially big instances or delay the initialization but ensure it happens only once.
     * @param <T> the type of the cached instance.
     */
    private static class Cached<T> implements Supplier<T> {
        private final Supplier<T> delegate;
        private volatile T computed;

        private Cached(final Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            if (computed == null) {
                synchronized (this) {
                    if (computed == null) {
                        computed = delegate.get();
                    }
                }
            }
            return computed;
        }
    }
}
