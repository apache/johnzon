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
package org.apache.johnzon.mapper;

import org.apache.johnzon.mapper.internal.JsonPointerTracker;
import org.apache.johnzon.mapper.reflection.JohnzonCollectionType;
import org.apache.johnzon.mapper.util.ArrayUtil;

import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import jakarta.json.stream.JsonParser;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.apache.johnzon.mapper.internal.Streams.noClose;

public class Mapper implements Closeable {

    protected final MapperConfig config;
    protected final Mappings mappings;
    protected final JsonReaderFactory readerFactory;
    protected final JsonGeneratorFactory generatorFactory;
    protected final JsonBuilderFactory builderFactory;
    protected final JsonProvider provider;
    protected final Collection<Closeable> closeables;
    protected final Charset charset;

    Mapper(final JsonReaderFactory readerFactory, final JsonGeneratorFactory generatorFactory,
           final JsonBuilderFactory builderFactory, final JsonProvider provider,
           final MapperConfig config, final Collection<Closeable> closeables) {
        this.readerFactory = readerFactory;
        this.generatorFactory = generatorFactory;
        this.builderFactory = builderFactory;
        this.provider = provider;
        this.config = config;
        this.mappings = this.config.getMappingsFactory() != null ? this.config.getMappingsFactory().apply(config) : new Mappings(config);
        this.closeables = closeables;
        this.charset = config.getEncoding();
    }

    public <T> void writeArray(final Object object, final OutputStream stream) {
        if (object instanceof short[]) {
            writeObject(ArrayUtil.asList((short[]) object), stream);
        } else if (object instanceof int[]) {
            writeObject(ArrayUtil.asList((int[]) object), stream);
        } else if (object instanceof long[]) {
            writeObject(ArrayUtil.asList((long[]) object), stream);
        } else if (object instanceof byte[]) {
            writeObject(ArrayUtil.asList((byte[]) object), stream);
        } else if (object instanceof char[]) {
            writeObject(ArrayUtil.asList((char[]) object), stream);
        } else if (object instanceof float[]) {
            writeObject(ArrayUtil.asList((float[]) object), stream);
        } else if (object instanceof double[]) {
            writeObject(ArrayUtil.asList((double[]) object), stream);
        } else {
            writeObject(Arrays.asList((T[]) object), stream);
        }
    }


    public <T> void writeArray(final T[] object, final OutputStream stream) {
        writeObject(Arrays.asList(object), stream);
    }

    public <T> void writeArray(final T[] object, final Writer stream) {
        writeObject(Arrays.asList(object), stream);
    }

    public <T> void writeArray(final Collection<T> object, final OutputStream stream) {
        writeArray(object, new OutputStreamWriter(stream, config.getEncoding()));
    }

    public <T> void writeArray(final Collection<T> object, final Writer stream) {
        try (final JsonGenerator generator = generatorFactory.createGenerator(stream(stream))) {
            writeObject(object, generator, null, config.isDeduplicateObjects() ? JsonPointerTracker.ROOT : null);
        }
    }

    public <T> void writeIterable(final Iterable<T> object, final OutputStream stream) {
        writeIterable(object, new OutputStreamWriter(stream, config.getEncoding()));
    }

    public <T> void writeIterable(final Iterable<T> object, final Writer stream) {
        try (final JsonGenerator generator = generatorFactory.createGenerator(stream(stream))) {
            writeObject(object, generator, null, config.isDeduplicateObjects() ? JsonPointerTracker.ROOT : null);
        }
    }

    public JsonValue toStructure(final Object object) {
        if (object == null) {
            return JsonValue.NULL;
        }
        if (JsonStructure.class.isInstance(object)) {
            return JsonStructure.class.cast(object);
        }
        if (Boolean.class.isInstance(object)) {
            return Boolean.class.cast(object) ? JsonValue.TRUE : JsonValue.FALSE;
        }
        if (String.class.isInstance(object)) {
            return provider.createValue(String.class.cast(object));
        }
        if (Double.class.isInstance(object)) {
            return provider.createValue(Double.class.cast(object));
        }
        if (Float.class.isInstance(object)) {
            return provider.createValue(Float.class.cast(object));
        }
        if (Long.class.isInstance(object)) {
            return provider.createValue(Long.class.cast(object));
        }
        if (Integer.class.isInstance(object)) {
            return provider.createValue(Integer.class.cast(object));
        }
        if (BigDecimal.class.isInstance(object)) {
            return provider.createValue(BigDecimal.class.cast(object));
        }
        if (BigInteger.class.isInstance(object)) {
            return provider.createValue(BigInteger.class.cast(object));
        }
        final JsonObjectGenerator objectGenerator = new JsonObjectGenerator(builderFactory);
        writeObject(object, objectGenerator, null, null);
        return objectGenerator.getResult();
    }

    public void writeObject(final Object object, final Writer stream) {
        if (object == null) {
            try (final Writer w = stream) {
                w.write("null");
            } catch (final IOException e) {
                throw new MapperException(e);
            }
            return;
        }
        final Adapter adapter = config.findAdapter(object.getClass());
        if (adapter != null && TypeAwareAdapter.class.isInstance(adapter) && TypeAwareAdapter.class.cast(adapter).getTo() == JsonString.class) {
            writeObject(adapter.from(object), stream);
            return;
        }
        try (final JsonGenerator generator = generatorFactory.createGenerator(stream(stream))) {
            final Object converted = adapter == null ? object : adapter.from(object);
            if (CharSequence.class.isInstance(converted)) {
                writeObjectWithGenerator(provider.createValue(converted.toString()), generator);
            } else {
                writeObjectWithGenerator(converted, generator);
            }
        }
    }

    public void writeObjectWithGenerator(final Object object, final JsonGenerator generator) {
        writeObject(object, generator, null, null);
    }

    public void writeObject(final Object object, final OutputStream stream) {
        Charset charset = config.getEncoding();
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }

        writeObject(object, new OutputStreamWriter(stream, charset));
    }

    private void writeObject(final Object object, final JsonGenerator generator, final Collection<String> ignored,
                             final JsonPointerTracker tracker) {
        final MappingGeneratorImpl mappingGenerator = new MappingGeneratorImpl(config, generator, mappings);
        mappingGenerator.doWriteObject(object, generator, true, ignored, tracker);
    }

    public String writeArrayAsString(final Collection<?> instance) {
        final StringWriter writer = new StringWriter(2048);
        writeArray(instance, writer);
        return writer.toString();
    }

    public <T> String writeArrayAsString(final T[] instance) {
        final StringWriter writer = new StringWriter(2048);
        writeArray(instance, writer);
        return writer.toString();
    }

    public String writeObjectAsString(final Object instance) {
        final StringWriter writer = new StringWriter(2048);
        writeObject(instance, writer);
        return writer.toString();
    }

    public <T> T readObject(final JsonValue value, final Type clazz) {
        return new MappingParserImpl(config, mappings, new JsonReader() {
            @Override
            public JsonStructure read() {
                switch (value.getValueType()) {
                    case STRING:
                    case FALSE:
                    case TRUE:
                    case NULL:
                    case NUMBER:
                        throw new UnsupportedOperationException("use readValue()");
                    default:
                        return JsonStructure.class.cast(readValue());
                }
            }

            @Override
            public JsonValue readValue() {
                return value;
            }

            @Override
            public JsonObject readObject() {
                return value.asJsonObject();
            }

            @Override
            public JsonArray readArray() {
                return value.asJsonArray();
            }

            @Override
            public void close() {
                // no-op
            }
        }, null).readObject(clazz);
    }

    public <T> T readObject(final String string, final Type clazz) {
        return readObject(new StringReader(string), clazz);
    }

    public <T> T readObject(final Reader stream, final Type clazz) {
        try (final JsonReader reader = readerFactory.createReader(stream(stream))) {
            return mapObject(clazz, reader);
        }
    }

    public <T> T readObject(final JsonParser stream, final Type clazz) {
        try (final JsonReader reader = JohnzonCores.map(stream, readerFactory)) {
            return mapObject(clazz, reader);
        }
    }

    public <T> T readObject(final InputStream stream, final Type clazz) {
        try (final JsonReader reader = charset == null ? readerFactory.createReader(stream(stream)) : readerFactory.createReader(
                stream(stream), charset)) {
            return mapObject(clazz, reader);
        }
    }

    public <T> Collection<T> readCollection(final InputStream stream, final ParameterizedType genericType) {
        try (final JsonReader reader = charset == null ? readerFactory.createReader(stream(stream)): readerFactory.createReader(stream(stream), charset)) {
            return mapObject(genericType, reader);
        }
    }

    public <T> T readJohnzonCollection(final InputStream stream, final JohnzonCollectionType<T> genericType) {
        return (T) readCollection(stream, genericType);
    }

    public <T> T readJohnzonCollection(final Reader stream, final JohnzonCollectionType<T> genericType) {
        return (T) readCollection(stream, genericType);
    }

    public <T> Collection<T> readCollection(final Reader stream, final ParameterizedType genericType) {
        try (final JsonReader reader = readerFactory.createReader(stream(stream))) {
            return mapObject(genericType, reader);
        }
    }

    public <T> Collection<T> readCollection(final JsonParser stream, final ParameterizedType genericType) {
        try (final JsonReader reader = JohnzonCores.map(stream, readerFactory)) {
            return mapObject(genericType, reader);
        }
    }

    public <T> T[] readArray(final Reader stream, final Class<T> clazz) {
        try (final JsonReader reader = readerFactory.createReader(stream(stream))) {
            return (T[]) mapArray(clazz, reader);
        }
    }

    public <T> T readTypedArray(final InputStream stream, final Class<?> elementType, final Class<T> arrayType) {
        try (final JsonReader reader = charset == null ? readerFactory.createReader(stream(stream)): readerFactory.createReader(stream(stream), charset)) {
            return arrayType.cast(mapArray(elementType, reader));
        }
    }

    public <T> T readTypedArray(final Reader stream, final Class<?> elementType, final Class<T> arrayType) {
        try (final JsonReader reader = readerFactory.createReader(stream(stream))) {
            return arrayType.cast(mapArray(elementType, reader));
        }
    }

    public <T> T readTypedArray(final JsonParser parser, final Class<?> elementType, final Class<T> arrayType) {
        try (final JsonReader reader = JohnzonCores.map(parser, readerFactory)) {
            return arrayType.cast(mapArray(elementType, reader));
        }
    }

    public JsonArray readJsonArray(final Reader stream) {
        try (final JsonReader reader = readerFactory.createReader(stream(stream))) {
            return reader.readArray();
        }
    }

    public JsonArray readJsonArray(final JsonParser stream) {
        try (final JsonReader reader = JohnzonCores.map(stream, readerFactory)) {
            return reader.readArray();
        }
    }

    public <T> T[] readArray(final InputStream stream, final Class<T> clazz) {
        try (final JsonReader reader = charset == null ? readerFactory.createReader(stream(stream)): readerFactory.createReader(stream(stream), charset)) {
            return (T[]) mapArray(clazz, reader);
        }
    }

    public JsonArray readJsonArray(final InputStream stream) {
        try (final JsonReader reader = charset == null ? readerFactory.createReader(stream(stream)): readerFactory.createReader(stream(stream), charset)) {
            return reader.readArray();
        }
    }

    private Object mapArray(final Class<?> clazz, final JsonReader reader) {

        return mapObject(ArrayUtil.getArrayTypeFor(clazz), reader);
    }


    private <T> T mapObject(final Type clazz, final JsonReader reader) {
        return new MappingParserImpl(config, mappings, reader, null).readObject(clazz);
    }


    private Reader stream(final Reader stream) {
        return !config.isClose() ? noClose(stream) : stream;
    }

    private Writer stream(final Writer stream) {
        return !config.isClose() ? noClose(stream) : stream;
    }

    private InputStream stream(final InputStream stream) {
        return !config.isClose() ? noClose(stream) : stream;
    }

    @Override
    public synchronized void close() {
        Collection<Exception> errors = null;
        for (final Closeable c : closeables) {
            try {
                c.close();
            } catch (final IOException e) {
                if (errors == null) {
                    errors = new ArrayList<>();
                }
                errors.add(e);
            }
        }
        closeables.clear();
        if (errors != null) {
            throw new IllegalStateException(errors.toString());
        }
    }

    public JsonBuilderFactory getBuilderFactory() {
        return builderFactory;
    }

    public JsonProvider getProvider() {
        return provider;
    }
}
