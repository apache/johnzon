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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Map;

public class Snippet implements Flushable, Closeable {

    private final JsonGenerator generator;
    private final SnippetOutputStream snippet;

    private Snippet(final int max) {
        this.snippet = new SnippetOutputStream(max);
        this.generator = Json.createGenerator(snippet);
    }

    private void write(final JsonValue value) {
        if (snippet.isComplete()) {
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
        if (snippet.isComplete()) {
            return;
        }

        if (array.isEmpty()) {
            generator.write(array);
            return;
        }

        generator.writeStartArray();
        for (final JsonValue jsonValue : array) {
            if (snippet.isComplete()) {
                break;
            }
            write(jsonValue);
        }
        generator.writeEnd();
    }

    private void write(final JsonObject object) {
        if (snippet.isComplete()) {
            return;
        }

        if (object.isEmpty()) {
            generator.write(object);
            return;
        }

        generator.writeStartObject();
        for (final Map.Entry<String, JsonValue> entry : object.entrySet()) {
            if (snippet.isComplete()) {
                break;
            }
            write(entry.getKey(), entry.getValue());
        }
        generator.writeEnd();
    }

    private void write(final String name, final JsonValue value) {
        if (snippet.isComplete()) {
            return;
        }

        switch (value.getValueType()) {
            case ARRAY:
                generator.writeStartArray(name);
                final JsonArray array = value.asJsonArray();
                for (final JsonValue jsonValue : array) {
                    write(jsonValue);
                }
                generator.writeEnd();

                break;
            case OBJECT:
                generator.writeStartObject(name);
                final JsonObject object = value.asJsonObject();
                for (final Map.Entry<String, JsonValue> keyval : object.entrySet()) {
                    write(keyval.getKey(), keyval.getValue());
                }
                generator.writeEnd();

                break;
            default: generator.write(name, value);
        }
    }

    private String get() {
        generator.close();
        return snippet.get();
    }

    @Override
    public void close() {
        generator.close();
    }

    @Override
    public void flush() {
        generator.flush();
    }

    public static String of(final JsonValue object) {
        return of(object, 50);
    }

    public static String of(final JsonValue value, final int max) {
        try (final Snippet snippet = new Snippet(max)){
            switch (value.getValueType()) {
                case TRUE: return "true";
                case FALSE: return "false";
                case NULL: return "null";
                default: {
                    snippet.write(value);
                    return snippet.get();
                }
            }
        }
    }

    private static class SnippetOutputStream extends OutputStream {

        private final ByteArrayOutputStream buffer;
        private OutputStream mode;

        public SnippetOutputStream(final int max) {
            this.buffer = new ByteArrayOutputStream();
            this.mode = new Writing(max, buffer);
        }

        public String get() {
            if (isComplete()) {
                return buffer.toString() + "...";
            } else {
                return buffer.toString();
            }
        }

        public boolean isComplete() {
            return mode instanceof Ignoring;
        }

        @Override
        public void write(final int b) throws IOException {
            mode.write(b);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            mode.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            mode.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            mode.flush();
        }

        @Override
        public void close() throws IOException {
            mode.close();
        }

        public void print(final String string) {
            try {
                mode.write(string.getBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        class Writing extends OutputStream {
            private final int max;
            private int count;
            private final OutputStream out;

            public Writing(final int max, final OutputStream out) {
                this.max = max;
                this.out = out;
            }

            @Override
            public void write(final int b) throws IOException {
                if (++count < max) {
                    out.write(b);
                } else {
                    endReached();
                }
            }

            @Override
            public void write(final byte[] b) throws IOException {
                write(b, 0, b.length);
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                final int remaining = max - count;

                if (remaining <= 0) {

                    endReached();

                } else if (len > remaining) {

                    out.write(b, off, remaining);
                    endReached();

                } else {
                    out.write(b, off, len);
                }
            }

            private void endReached() throws IOException {
                mode = new Ignoring();
                flush();
                close();
            }
        }

        static class Ignoring extends OutputStream {
            @Override
            public void write(final int b) throws IOException {
            }
        }

    }
}
