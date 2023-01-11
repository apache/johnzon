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

import org.apache.johnzon.core.io.BoundedOutputStreamWriter;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

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
        final Buffer buffer = new Buffer();
        try (final Buffer b = buffer) {
            b.write(value);
        }
        return buffer.get();
    }

    // IMPORTANT: should NOT be used inside johnzon project itself which should *always*
    //            use a contextual JsonGeneratorFactory - keep in mind we are not johnzon-core dependent
    //            not JSON dependent (some JsonGeneratorFactory can issue yaml or binary for ex)
    /**
     * This factory should be used only in static or other scenarios were
     * there is no JsonGeneratorFactory instance in scope - ie external code.
     *
     * @param max the maximum length of the serialized json produced via of()
     */
    public static Snippet of(final int max) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("org.apache.johnzon.default-char-buffer-generator", max);
        properties.put("org.apache.johnzon.boundedoutputstreamwriter", max);
        return new Snippet(max, Json.createGeneratorFactory(properties));
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
     * @param max the maximum length of the serialized json text
     * @param value the JsonValue to be serialized as json text
     * @return a potentially truncated json text
     */
    public static String of(final int max, final JsonValue value) {
        return of(max).of(value);
    }

    // skips some methods using a buffer
    private static abstract class PassthroughWriter extends Writer {
        @Override
        public void write(final char[] cbuf) throws IOException {
            write(cbuf, 0, cbuf.length);
        }

        @Override
        public void write(final String str) throws IOException {
            write(str.toCharArray(), 0, str.length());
        }

        @Override
        public void write(final String str, final int off, final int len) throws IOException {
            write(str.toCharArray(), 0, len);
        }

        @Override
        public Writer append(final CharSequence csq) throws IOException {
            write(csq.toString().toCharArray(), 0, csq.length());
            return this;
        }

        @Override
        public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
            write(csq.toString().toCharArray(), start, end);
            return this;
        }

        @Override
        public Writer append(final char c) throws IOException {
            write(new char[]{c}, 0, 1);
            return this;
        }

        @Override
        public void flush() throws IOException {
            // no-op
        }

        @Override
        public void close() throws IOException {
            // no-op
        }
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
    private class Buffer implements Closeable {
        private final JsonGenerator generator;
        private final SnippetWriter snippet;

        private Buffer() {
            this.snippet = new SnippetWriter(max);
            this.generator = generatorFactory.createGenerator(snippet);
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
            return snippet.terminate();
        }

        private String get() {
            return snippet.get() + (snippet.isTruncated() ? "..." : "");
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
        class SnippetWriter extends PassthroughWriter implements Buffered {
            private final ByteArrayOutputStream buffer;
            private final int max;
            private PassthroughWriter mode;

            public SnippetWriter(final int max) {
                this.max = max;
                this.buffer = new ByteArrayOutputStream(max);
                this.mode = new Writing(max, new BoundedOutputStreamWriter(
                        buffer,
                        JsonGeneratorFactoryImpl.class.isInstance(generatorFactory) ?
                                JsonGeneratorFactoryImpl.class.cast(generatorFactory).getDefaultEncoding() :
                                UTF_8,
                        max));
            }

            public String get() {
                return buffer.toString();
            }

            @Override
            public int bufferSize() {
                return max;
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

            class Writing extends PassthroughWriter {
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

                private void maxReached(final PassthroughWriter mode) throws IOException {
                    SnippetWriter.this.mode = mode;
                    writer.flush();
                    writer.close();
                }
            }

            /**
             * Signifies the last write was fully written, but there is
             * no more space for future writes.
             */
            class Completed extends PassthroughWriter {
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
            class Truncated extends PassthroughWriter {
                @Override
                public void write(final char[] cbuf, final int off, final int len) throws IOException {
                    // no-op
                }
            }
        }
    }
}
