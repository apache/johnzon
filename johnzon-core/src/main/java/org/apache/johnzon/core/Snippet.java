/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.core;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.apache.johnzon.core.JsonGeneratorFactoryImpl.GENERATOR_BUFFER_LENGTH;

/**
 * Constructs short snippets of serialized JSON text representations of
 * JsonValue instances in a way that is ideal for error messages.
 *
 * Instances of Snippet are thread-safe, reusable and memory-safe.  Snippet
 * serializes only enough of the json to fill the desired snippet size and
 * is therefore safe to use regardless of the size of the JsonValue.
 */
public class Snippet {

    private final int max;
    private final JsonGeneratorFactory generatorFactory;

    // IMPORTANT: should NOT be used inside johnzon project itself which should *always*
    //            use a contextual JsonGeneratorFactory - keep in mind we are not johnzon-core dependent
    //            not JSON dependent (some JsonGeneratorFactory can issue yaml or binary for ex)
    /**
     * This constructor should be used only in static or other scenarios were
     * there is no JsonGeneratorFactory instance in scope.
     *
     * @param max the maximum length of the serialized json produced via of()
     */
    public Snippet(final int max) {
        this(max, new JsonGeneratorFactoryImpl(new HashMap<String, Object>() {
            {
                this.put(GENERATOR_BUFFER_LENGTH, max);
            }
        }));
    }

    /**
     * This is the preferred approach to using Snippet in any context where
     * there is an existing JsonGeneratorFactory in scope.
     *
     * @param max the maximum length of the serialized json produced via of()
     * @param generatorFactory the JsonGeneratorFactory created by the user
     */
    public Snippet(final int max, final JsonGeneratorFactory generatorFactory) {
        this.max = max;
        this.generatorFactory = generatorFactory;
    }

    /**
     * Create a serialized json representation of the supplied
     * JsonValue, truncating the value to the specified max length.
     * Truncated text appears with a suffix of "..."
     *
     * This method is thread safe.
     *
     * @param value the JsonValue to be serialized as json text
     * @return a potentially truncated json text
     */
    public String of(final JsonValue value) {
        try (final Buffer buffer = new Buffer()) {
            buffer.write(value);
            return buffer.get();
        }
    }

    /**
     * Create a serialized json representation of the supplied
     * JsonValue, truncating the value to the specified max length.
     * Truncated text appears with a suffix of "..."
     *
     * This method is thread safe.
     *
     * Avoid using this method in any context where there already
     * is a JsonGeneratorFactory instance in scope. For those scenarios
     * use the constructor that accepts a JsonGeneratorFactory instead.
     *
     * @param value the JsonValue to be serialized as json text
     * @param max the maximum length of the serialized json text
     * @return a potentially truncated json text
     */
    public static String of(final JsonValue value, final int max) {
        return new Snippet(max).of(value);
    }

    /**
     * There are several buffers involved in the creation of a json string.
     * This class carefully manages them all.
     *
     * JsonGeneratorImpl with a 64k buffer (by default)
     * ObjectStreamWriter with an 8k buffer
     * SnippetOutputStream with a buffer of maxSnippetLength
     *
     * As we create json via calling the JsonGenerator it is critical we
     * flush the work in progress all the way through these buffers and into
     * the final SnippetOutputStream buffer.
     *
     * If we do not, we risk creating up to 64k of json when we may only
     * need 50 bytes.  We could potentially optimize this code so the
     * buffer held by JsonGeneratorImpl is also the maxSnippetLength.
     */
    class Buffer implements Closeable {
        private final JsonGenerator generator;
        private final SnippetWriter snippet;
        private Runnable flush;

        private Buffer() {
            this.snippet = new SnippetWriter(max);
            this.generator = generatorFactory.createGenerator(snippet);
            this.flush = generator::flush;
        }

        private void write(final JsonValue value) {
            if (terminate()) {
                return;
            }

            switch (value.getValueType()) {
                case ARRAY: {
                    write(value.asJsonArray());
                    break;
                }
                case OBJECT: {
                    write(value.asJsonObject());
                    break;
                }
                default: {
                    generator.write(value);
                }
            }
        }

        private void write(final JsonArray array) {
            if (array.isEmpty()) {
                generator.write(array);
                return;
            }

            generator.writeStartArray();
            for (final JsonValue jsonValue : array) {
                if (terminate()) {
                    break;
                }
                write(jsonValue);
            }
            generator.writeEnd();
        }

        private void write(final JsonObject object) {
            if (object.isEmpty()) {
                generator.write(object);
                return;
            }

            generator.writeStartObject();
            for (final Map.Entry<String, JsonValue> entry : object.entrySet()) {
                if (terminate()) {
                    break;
                }
                write(entry.getKey(), entry.getValue());
            }
            generator.writeEnd();
        }

        private void write(final String name, final JsonValue value) {
            switch (value.getValueType()) {
                case ARRAY:
                    generator.writeStartArray(name);
                    final JsonArray array = value.asJsonArray();
                    for (final JsonValue jsonValue : array) {
                        if (terminate()) {
                            break;
                        }
                        write(jsonValue);
                    }
                    generator.writeEnd();

                    break;
                case OBJECT:
                    generator.writeStartObject(name);
                    final JsonObject object = value.asJsonObject();
                    for (final Map.Entry<String, JsonValue> keyval : object.entrySet()) {
                        if (terminate()) {
                            break;
                        }
                        write(keyval.getKey(), keyval.getValue());
                    }
                    generator.writeEnd();

                    break;
                default: {
                    generator.write(name, value);
                }
            }
        }

        private boolean terminate() {
            flush.run();
            return snippet.terminate();
        }

        private String get() {
            generator.flush();
            return snippet.isTruncated() ? snippet.get() + "..." : snippet.get();
        }

        @Override
        public void close() {
            generator.close();
        }

        /**
         * Specialized Writer with three internal states:
         * Writing, Completed, Truncated.
         *
         * When there is still space left for more json, the
         * state will be Writing
         *
         * If the last write brought is exactly to the end of
         * the max length, the state will be Completed.
         *
         * If the last write brought us over the max length, the
         * state will be Truncated.
         */
        class SnippetWriter extends Writer implements IODescriptor {

            private final ByteArrayOutputStream buffer;
            private Mode mode;
            private Supplier<Integer> bufferSize;

            public SnippetWriter(final int max) {
                final int size = Math.min(max, 8192);

                this.buffer = new ByteArrayOutputStream(size);
                this.mode = new Writing(max, new OutputStreamWriter(buffer));

                /*
                 * The first time the buffer size is requested, disable flushing
                 * as we know our requested buffer size will be respected
                 */
                this.bufferSize = () -> {
                    // disable flushing
                    flush = () -> {
                    };
                    // future calls can just return the size
                    bufferSize = () -> size;
                    return size;
                };
            }

            public String get() {
                return buffer.toString();
            }

            @Override
            public int bufferSize() {
                return bufferSize.get();
            }

            /**
             * Calling this method implies the need to continue
             * writing and a question on if that is ok.
             *
             * It impacts internal state in the same way as
             * calling a write method.
             *
             * @return true if no more writes are possible
             */
            public boolean terminate() {
                if (mode instanceof Truncated) {
                    return true;
                }

                if (mode instanceof Completed) {
                    mode = new Truncated();
                    return true;
                }

                return false;
            }

            public boolean isTruncated() {
                return mode instanceof Truncated;
            }

            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                mode.write(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                mode.flush();
            }

            @Override
            public void close() throws IOException {
                mode.close();
            }

            abstract class Mode extends Writer {
                @Override
                public void flush() throws IOException {
                }

                @Override
                public void close() throws IOException {
                }
            }

            class Writing extends Mode {
                private final int max;
                private int count;
                private final Writer writer;

                public Writing(final int max, final Writer writer) {
                    this.max = max;
                    this.writer = writer;
                }

                @Override
                public void write(final char[] cbuf, final int off, final int len) throws IOException {
                    final int remaining = max - count;

                    if (remaining <= 0) {

                        maxReached(new Truncated());

                    } else if (len == remaining) {

                        count += len;
                        writer.write(cbuf, off, remaining);
                        maxReached(new Completed());

                    } else if (len > remaining) {

                        count += len;
                        writer.write(cbuf, off, remaining);
                        maxReached(new Truncated());

                    } else {
                        count += len;
                        writer.write(cbuf, off, len);
                    }
                }

                @Override
                public void flush() throws IOException {
                    writer.flush();
                }

                @Override
                public void close() throws IOException {
                    writer.close();
                }

                private void maxReached(final Mode mode) throws IOException {
                    SnippetWriter.this.mode = mode;
                    writer.flush();
                    writer.close();
                }
            }

            /**
             * Signifies the last write was fully written, but there is
             * no more space for future writes.
             */
            class Completed extends Mode {

                @Override
                public void write(final char[] cbuf, final int off, final int len) throws IOException {
                    if (len > 0) {
                        SnippetWriter.this.mode = new Truncated();
                    }
                }
            }

            /**
             * Signifies the last write was not completely written and there was
             * no more space for this or future writes.
             */
            class Truncated extends Mode {
                @Override
                public void write(final char[] cbuf, final int off, final int len) throws IOException {
                }
            }
        }
    }
}
