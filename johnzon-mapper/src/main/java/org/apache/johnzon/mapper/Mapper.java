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

import org.apache.johnzon.mapper.reflection.JohnzonCollectionType;

import javax.json.JsonException;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.apache.johnzon.mapper.internal.Streams.noClose;

public class Mapper implements Closeable {

    protected final MapperConfig config;
    protected final Mappings mappings;
    protected final JsonReaderFactory readerFactory;
    protected final JsonGeneratorFactory generatorFactory;
    protected final ReaderHandler readerHandler;
    protected final Collection<Closeable> closeables;
    protected final Charset charset;

    Mapper(final JsonReaderFactory readerFactory, final JsonGeneratorFactory generatorFactory, MapperConfig config,
                  final Collection<Closeable> closeables) {
        this.readerFactory = readerFactory;
        this.generatorFactory = generatorFactory;
        this.config = config;
        this.mappings = new Mappings(config);
        this.readerHandler = ReaderHandler.create(readerFactory);
        this.closeables = closeables;
        this.charset = config.getEncoding() == null ? null : config.getEncoding();
    }


    public <T> void writeArray(final Object object, final OutputStream stream) {
        writeObject(asList((T[]) object), stream);
    }

    public <T> void writeArray(final T[] object, final OutputStream stream) {
        writeObject(asList(object), stream);
    }

    public <T> void writeArray(final T[] object, final Writer stream) {
        writeObject(asList(object), stream);
    }

    public <T> void writeArray(final Collection<T> object, final OutputStream stream) {
        writeArray(object, new OutputStreamWriter(stream, config.getEncoding()));
    }

    public <T> void writeArray(final Collection<T> object, final Writer stream) {
        JsonGenerator generator = generatorFactory.createGenerator(stream(stream));
        writeObject(object, generator);
    }

    public <T> void writeIterable(final Iterable<T> object, final OutputStream stream) {
        writeIterable(object, new OutputStreamWriter(stream, config.getEncoding()));
    }

    public <T> void writeIterable(final Iterable<T> object, final Writer stream) {
        JsonGenerator generator = generatorFactory.createGenerator(stream(stream));
        writeObject(object, generator);
    }

    public void writeObject(final Object object, final Writer stream) {
        if (JsonValue.class.isInstance(object)
                || Boolean.class.isInstance(object) || String.class.isInstance(object) || Number.class.isInstance(object)
                || object == null) {
            try {
                final String valueOf = String.valueOf(object);
                stream.write(config.isEnforceQuoteString() && String.class.isInstance(object) && !valueOf.startsWith("\"") ? '"' + valueOf + '"' : valueOf);
            } catch (final IOException e) {
                throw new MapperException(e);
            } finally {
                if (config.isClose()) {
                    try {
                        stream.close();
                    } catch (final IOException e) {
                        // no-op
                    }
                } else {
                    try {
                        stream.flush();
                    } catch (final IOException e) {
                        // no-op
                    }
                }
            }
            return;
        }

        final JsonGenerator generator = generatorFactory.createGenerator(stream(stream));
        writeObject(object, generator);
    }

    public void writeObject(final Object object, final OutputStream stream) {
        final JsonGenerator generator = generatorFactory.createGenerator(stream(stream), config.getEncoding());
        writeObject(object, generator);
    }

    private void writeObject(final Object object, final JsonGenerator generator) {
        MappingGeneratorImpl mappingGenerator = new MappingGeneratorImpl(config, generator, mappings);

        RuntimeException originalException = null;
        try {
            mappingGenerator.doWriteObject(object, generator, true);
        } catch (RuntimeException e) {
            originalException = e;
        } finally {

            try {
                generator.close();
            } catch (JsonException e) {

                if (originalException != null) {
                    throw originalException;
                } else {
                    throw e;
                }
            }
        }

    }

    public String writeArrayAsString(final Collection<?> instance) {
        final StringWriter writer = new StringWriter();
        writeArray(instance, writer);
        return writer.toString();
    }

    public <T> String writeArrayAsString(final T[] instance) {
        final StringWriter writer = new StringWriter();
        writeArray(instance, writer);
        return writer.toString();
    }

    public String writeObjectAsString(final Object instance) {
        final StringWriter writer = new StringWriter();
        writeObject(instance, writer);
        return writer.toString();
    }


    public <T> T readObject(final String string, final Type clazz) {
        return readObject(new StringReader(string), clazz);
    }

    public <T> T readObject(final Reader stream, final Type clazz) {
        return mapObject(clazz, readerFactory.createReader(stream(stream)));
    }

    public <T> T readObject(final InputStream stream, final Type clazz) {
        return mapObject(clazz, charset == null ? readerFactory.createReader(stream(stream)): readerFactory.createReader(stream(stream), charset));
    }

    public <T> Collection<T> readCollection(final InputStream stream, final ParameterizedType genericType) {
        return mapObject(genericType, charset == null ? readerFactory.createReader(stream(stream)): readerFactory.createReader(stream(stream), charset));
    }

    public <T> T readJohnzonCollection(final InputStream stream, final JohnzonCollectionType<T> genericType) {
        return (T) readCollection(stream, genericType);
    }

    public <T> T readJohnzonCollection(final Reader stream, final JohnzonCollectionType<T> genericType) {
        return (T) readCollection(stream, genericType);
    }

    public <T> Collection<T> readCollection(final Reader stream, final ParameterizedType genericType) {
        return mapObject(genericType, readerFactory.createReader(stream(stream)));
    }

    public <T> T[] readArray(final Reader stream, final Class<T> clazz) {
        final JsonReader reader = readerFactory.createReader(stream(stream));
        return (T[]) mapArray(clazz, reader);
    }

    public <T> T readTypedArray(final InputStream stream, final Class<?> elementType, final Class<T> arrayType) {
        final JsonReader reader = charset == null ? readerFactory.createReader(stream(stream)): readerFactory.createReader(stream(stream), charset);
        return arrayType.cast(mapArray(elementType, reader));
    }

    public <T> T readTypedArray(final Reader stream, final Class<?> elementType, final Class<T> arrayType) {
        final JsonReader reader = readerFactory.createReader(stream(stream));
        return arrayType.cast(mapArray(elementType, reader));
    }

    public <T> T[] readArray(final InputStream stream, final Class<T> clazz) {
        final JsonReader reader = charset == null ? readerFactory.createReader(stream(stream)): readerFactory.createReader(stream(stream), charset);
        return (T[]) mapArray(clazz, reader);
    }

    private Object mapArray(final Class<?> clazz, final JsonReader reader) {
        return mapObject(Array.newInstance(clazz, 0).getClass(), reader);
    }


    private <T> T mapObject(final Type clazz, final JsonReader reader) {
        return new MappingParserImpl(config, mappings, reader).readObject(clazz);
    }


    private Reader stream(final Reader stream) {
        return !config.isClose() ? noClose(stream) : stream;
    }

    private Writer stream(final Writer stream) {
        return !config.isClose() ? noClose(stream) : stream;
    }

    private OutputStream stream(final OutputStream stream) {
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
                    errors = new ArrayList<Exception>();
                }
                errors.add(e);
            }
        }
        closeables.clear();
        if (errors != null) {
            throw new IllegalStateException(errors.toString());
        }
    }

}
