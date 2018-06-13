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

public class JsonProviderImpl extends JsonProvider implements Serializable {
    private static final JsonProvider DELEGATE = new JsonProviderDelegate();

    @Override
    public JsonParser createParser(final Reader reader) {
        return DELEGATE.createParser(reader);
    }

    @Override
    public JsonParser createParser(final InputStream inputStream) {
        return DELEGATE.createParser(inputStream);
    }

    @Override
    public JsonParserFactory createParserFactory(final Map<String, ?> stringMap) {
        return DELEGATE.createParserFactory(stringMap);
    }

    @Override
    public JsonGenerator createGenerator(final Writer writer) {
        return DELEGATE.createGenerator(writer);
    }

    @Override
    public JsonGenerator createGenerator(final OutputStream outputStream) {
        return DELEGATE.createGenerator(outputStream);
    }

    @Override
    public JsonGeneratorFactory createGeneratorFactory(final Map<String, ?> stringMap) {
        return DELEGATE.createGeneratorFactory(stringMap);
    }

    @Override
    public JsonReader createReader(final Reader reader) {
        return DELEGATE.createReader(reader);
    }

    @Override
    public JsonReader createReader(final InputStream inputStream) {
        return DELEGATE.createReader(inputStream);
    }

    @Override
    public JsonWriter createWriter(final Writer writer) {
        return DELEGATE.createWriter(writer);
    }

    @Override
    public JsonWriter createWriter(final OutputStream outputStream) {
        return DELEGATE.createWriter(outputStream);
    }

    @Override
    public JsonWriterFactory createWriterFactory(final Map<String, ?> stringMap) {
        return DELEGATE.createWriterFactory(stringMap);
    }

    @Override
    public JsonReaderFactory createReaderFactory(final Map<String, ?> stringMap) {
        return DELEGATE.createReaderFactory(stringMap);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder() {
        return DELEGATE.createObjectBuilder();
    }

    @Override
    public JsonArrayBuilder createArrayBuilder() {
        return DELEGATE.createArrayBuilder();
    }

    @Override
    public JsonBuilderFactory createBuilderFactory(final Map<String, ?> stringMap) {
        return DELEGATE.createBuilderFactory(stringMap);
    }
    
    @Override
    public JsonPatchBuilder createPatchBuilder() {
        return DELEGATE.createPatchBuilder();
    }

    @Override
    public JsonPatchBuilder createPatchBuilder(JsonArray initialData) {
        return DELEGATE.createPatchBuilder(initialData);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder(JsonObject jsonObject) {
        return DELEGATE.createObjectBuilder(jsonObject);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder(Map<String, Object> map) {
        return DELEGATE.createObjectBuilder(map);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder(JsonArray initialData) {
        return DELEGATE.createArrayBuilder(initialData);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder(Collection<?> initialData) {
        return DELEGATE.createArrayBuilder(initialData);
    }

    @Override
    public JsonPointer createPointer(String path) {
        return DELEGATE.createPointer(path);
    }

    @Override
    public JsonString createValue(String value) {
        return DELEGATE.createValue(value);
    }

    @Override
    public JsonNumber createValue(int value) {
        return DELEGATE.createValue(value);
    }

    @Override
    public JsonNumber createValue(long value) {
        return DELEGATE.createValue(value);
    }

    @Override
    public JsonNumber createValue(double value) {
        return DELEGATE.createValue(value);
    }

    @Override
    public JsonNumber createValue(BigDecimal value) {
        return DELEGATE.createValue(value);
    }

    @Override
    public JsonNumber createValue(BigInteger value) {
        return DELEGATE.createValue(value);
    }

    @Override
    public JsonPatch createPatch(JsonArray array) {
        return DELEGATE.createPatch(array);
    }

    @Override
    public JsonPatch createDiff(JsonStructure source, JsonStructure target) {
        return DELEGATE.createDiff(source, target);
    }

    @Override
    public JsonMergePatch createMergePatch(JsonValue patch) {
        return DELEGATE.createMergePatch(patch);
    }

    @Override
    public JsonMergePatch createMergeDiff(JsonValue source, JsonValue target) {
        return DELEGATE.createMergeDiff(source, target);
    }

    static class JsonProviderDelegate extends JsonProvider {
        private final JsonReaderFactory readerFactory = new JsonReaderFactoryImpl(null);
        private final JsonParserFactory parserFactory = new JsonParserFactoryImpl(null);
        private final JsonGeneratorFactory generatorFactory = new JsonGeneratorFactoryImpl(null);
        private final JsonWriterFactory writerFactory = new JsonWriterFactoryImpl(null);
        private final JsonBuilderFactoryImpl builderFactory = new JsonBuilderFactoryImpl(null);

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
            return builderFactory.createObjectBuilder();
        }

        @Override
        public JsonObjectBuilder createObjectBuilder(JsonObject jsonObject) {
            return builderFactory.createObjectBuilder(jsonObject);
        }

        @Override
        public JsonObjectBuilder createObjectBuilder(Map<String, Object> initialValues) {
            return builderFactory.createObjectBuilder(initialValues);
        }

        @Override
        public JsonArrayBuilder createArrayBuilder() {
            return builderFactory.createArrayBuilder();
        }

        @Override
        public JsonArrayBuilder createArrayBuilder(JsonArray initialData) {
            return builderFactory.createArrayBuilder(initialData);
        }

        public JsonArrayBuilder createArrayBuilder(Collection<?> initialData) {
            return builderFactory.createArrayBuilder(initialData);
        }

        @Override
        public JsonString createValue(String value) {
            return new JsonStringImpl(value);
        }

        @Override
        public JsonNumber createValue(int value) {
            return new JsonLongImpl(value);
        }

        @Override
        public JsonNumber createValue(long value) {
            return new JsonLongImpl(value);
        }

        @Override
        public JsonNumber createValue(double value) {
            return new JsonDoubleImpl(value);
        }

        @Override
        public JsonNumber createValue(BigDecimal value) {
            return new JsonNumberImpl(value);
        }

        @Override
        public JsonNumber createValue(BigInteger value) {
            return new JsonLongImpl(value.longValue());
        }

        @Override
        public JsonBuilderFactory createBuilderFactory(final Map<String, ?> config) {
            return (config == null || config.isEmpty()) ? builderFactory : new JsonBuilderFactoryImpl(config);
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

        public JsonPatch createPatch(JsonArray array) {
            return createPatchBuilder(array).build();
        }

        @Override
        public JsonPatch createDiff(JsonStructure source, JsonStructure target) {
            return new JsonPatchDiff(this, source, target).calculateDiff();
        }

        public JsonMergePatch createMergePatch(JsonValue patch) {
            return new JsonMergePatchImpl(patch);
        }

        @Override
        public JsonMergePatch createMergeDiff(JsonValue source, JsonValue target) {
            return new JsonMergePatchDiff(source, target).calculateDiff();
        }
    }
}
