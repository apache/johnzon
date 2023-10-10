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
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonPatch;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonPointer;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;

public class JsonProviderImpl extends JsonProvider implements Serializable {
    private final Supplier<BufferStrategy.BufferProvider<char[]>> bufferProvider = new Cached<>(() ->
        BufferStrategyFactory.valueOf(System.getProperty(AbstractJsonFactory.BUFFER_STRATEGY, "QUEUE"))
            .newCharProvider(Integer.getInteger("org.apache.johnzon.default-char-provider.length", 1024)));

    private final JsonReaderFactory readerFactory = new JsonReaderFactoryImpl(null, this);
    private final JsonParserFactory parserFactory = new JsonParserFactoryImpl(null, this);
    private final JsonGeneratorFactory generatorFactory = new JsonGeneratorFactoryImpl(null);
    private final JsonWriterFactory writerFactory = new JsonWriterFactoryImpl(null);
    private final Supplier<JsonBuilderFactory> builderFactory = new Cached<>(() ->
            new JsonBuilderFactoryImpl(null, bufferProvider.get(), RejectDuplicateKeysMode.DEFAULT, this));
    private int maxBigDecimalScale = Integer.getInteger("johnzon.max-big-decimal-scale", 1_000);
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
        return (config == null || config.isEmpty()) ? parserFactory : new JsonParserFactoryImpl(config, this);
    }

    @Override
    public JsonReaderFactory createReaderFactory(final Map<String, ?> config) {
        return (config == null || config.isEmpty()) ? readerFactory : new JsonReaderFactoryImpl(config, this);
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
    public JsonObjectBuilder createObjectBuilder(Map<String, ?> initialValues) {
        return builderFactory.get().createObjectBuilder((Map<String, Object>) initialValues);
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
        return new JsonNumberImpl(value, this::checkBigDecimalScale);
    }

    @Override
    public JsonNumber createValue(Number number) {
        return createValue(new BigDecimal(number.toString()));
    }

    @Override
    public JsonNumber createValue(final BigInteger value) {
        return createValue((Number) value);
    }

    @Override
    public JsonBuilderFactory createBuilderFactory(final Map<String, ?> config) {
        final JsonBuilderFactory builderFactory = this.builderFactory.get();
        return (config == null || config.isEmpty()) ?
                builderFactory : new JsonBuilderFactoryImpl(config, bufferProvider.get(), RejectDuplicateKeysMode.from(config), this);
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
        return new JsonPointerImpl(this, path);
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
        return new JsonMergePatchImpl(patch, bufferProvider.get(), this);
    }

    @Override
    public JsonMergePatch createMergeDiff(JsonValue source, JsonValue target) {
        return new JsonMergePatchDiff(source, target, bufferProvider.get(), this).calculateDiff();
    }

    public int getMaxBigDecimalScale() {
        return maxBigDecimalScale;
    }

    public void setMaxBigDecimalScale(final int maxBigDecimalScale) {
        this.maxBigDecimalScale = maxBigDecimalScale;
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

    public void checkBigDecimalScale(final BigDecimal value) {
        // should be fine enough. Maybe we should externalize so users can pick something better if they need to
        // it becomes their responsibility to fix the limit and may expose them to a DoS attack
        final int limit = maxBigDecimalScale;
        final int absScale = Math.abs(value.scale());

        if (absScale > limit) {
            throw new ArithmeticException(String.format(
                "BigDecimal scale (%d) limit exceeds maximum allowed (%d)",
                value.scale(), limit));
        }
    }
}
