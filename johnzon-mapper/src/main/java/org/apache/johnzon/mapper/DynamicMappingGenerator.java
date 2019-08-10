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

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

public class DynamicMappingGenerator implements MappingGenerator {
    private final MappingGenerator delegate;
    private final Runnable writeStart;
    private final Runnable writeEnd;
    private final String keyName;

    private InObjectOrPrimitiveJsonGenerator generator;

    public DynamicMappingGenerator(final MappingGenerator delegate,
                                   final Runnable writeStart,
                                   final Runnable writeEnd,
                                   final String keyName) {
        this.delegate = delegate;
        this.writeStart = writeStart;
        this.writeEnd = writeEnd;
        this.keyName = keyName;
    }

    @Override
    public JsonGenerator getJsonGenerator() {
        return generator == null ? generator = new InObjectOrPrimitiveJsonGenerator(
                delegate.getJsonGenerator(), writeStart, keyName) : generator;
    }

    @Override
    public MappingGenerator writeObject(final String key, final Object o, final JsonGenerator generator) {
        return delegate.writeObject(key, o, ensureGenerator(generator));
    }

    @Override
    public MappingGenerator writeObject(final Object o, final JsonGenerator generator) {
        return delegate.writeObject(o, ensureGenerator(generator));
    }

    private JsonGenerator ensureGenerator(final JsonGenerator generator) {
        if (this.generator != null && this.generator != generator && this.generator.delegate != generator) {
            this.generator = null;
        }
        return getJsonGenerator(); // ensure we wrap it
    }

    public void flushIfNeeded() {
        if (this.generator.state == WritingState.WROTE_START_OBJECT) {
            writeEnd.run();
            this.generator.state = WritingState.NONE;
        }
    }

    private enum WritingState {
        NONE, WROTE_START_OBJECT,
        DONT_WRITE_END
    }

    private static class InObjectOrPrimitiveJsonGenerator implements JsonGenerator {
        private final JsonGenerator delegate;
        private final Runnable writeStart;
        private final String keyIfNoObject;
        private WritingState state = WritingState.NONE;

        private InObjectOrPrimitiveJsonGenerator(final JsonGenerator generator, final Runnable writeStart,
                                                 final String keyName) {
            this.delegate = generator;
            this.writeStart = writeStart;
            this.keyIfNoObject = keyName;
        }

        private void ensureStart() {
            if (state == WritingState.WROTE_START_OBJECT) {
                return;
            }
            writeStart.run();
            state = WritingState.WROTE_START_OBJECT;
        }

        @Override
        public JsonGenerator writeStartObject() {
            // return delegate.writeStartObject();
            return this;
        }

        @Override
        public JsonGenerator writeStartObject(final String name) {
            ensureStart();
            return delegate.writeStartObject(name);
        }

        @Override
        public JsonGenerator writeStartArray() {
            if (keyIfNoObject != null && state == WritingState.NONE) {
                state = WritingState.DONT_WRITE_END; // skip writeEnd since the impl will do it
                return delegate.writeStartArray(keyIfNoObject);
            }
            return delegate.writeStartArray();
        }

        @Override
        public JsonGenerator writeStartArray(final String name) {
            ensureStart();
            return delegate.writeStartArray(name);
        }

        @Override
        public JsonGenerator writeKey(final String name) {
            ensureStart();
            return delegate.writeKey(name);
        }

        @Override
        public JsonGenerator write(final String name, final JsonValue value) {
            ensureStart();
            return delegate.write(name, value);
        }

        @Override
        public JsonGenerator write(final String name, final String value) {
            ensureStart();
            return delegate.write(name, value);
        }

        @Override
        public JsonGenerator write(final String name, final BigInteger value) {
            ensureStart();
            return delegate.write(name, value);
        }

        @Override
        public JsonGenerator write(final String name, final BigDecimal value) {
            ensureStart();
            return delegate.write(name, value);
        }

        @Override
        public JsonGenerator write(final String name, final int value) {
            ensureStart();
            return delegate.write(name, value);
        }

        @Override
        public JsonGenerator write(final String name, final long value) {
            ensureStart();
            return delegate.write(name, value);
        }

        @Override
        public JsonGenerator write(final String name, final double value) {
            ensureStart();
            return delegate.write(name, value);
        }

        @Override
        public JsonGenerator write(final String name, final boolean value) {
            ensureStart();
            return delegate.write(name, value);
        }

        @Override
        public JsonGenerator writeNull(final String name) {
            ensureStart();
            return delegate.writeNull(name);
        }

        @Override
        public JsonGenerator writeEnd() {
            return delegate.writeEnd();
        }

        @Override
        public JsonGenerator write(final JsonValue value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                return delegate.write(keyIfNoObject, value);
            }
            return delegate.write(value);
        }

        @Override
        public JsonGenerator write(final String value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                return delegate.write(keyIfNoObject, value);
            }
            return delegate.write(value);
        }

        @Override
        public JsonGenerator write(final BigDecimal value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                return delegate.write(keyIfNoObject, value);
            }
            return delegate.write(value);
        }

        @Override
        public JsonGenerator write(final BigInteger value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                return delegate.write(keyIfNoObject, value);
            }
            return delegate.write(value);
        }

        @Override
        public JsonGenerator write(final int value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                return delegate.write(keyIfNoObject, value);
            }
            return delegate.write(value);
        }

        @Override
        public JsonGenerator write(final long value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                return delegate.write(keyIfNoObject, value);
            }
            return delegate.write(value);
        }

        @Override
        public JsonGenerator write(final double value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                return delegate.write(keyIfNoObject, value);
            }
            return delegate.write(value);
        }

        @Override
        public JsonGenerator write(boolean value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                return delegate.write(keyIfNoObject, value);
            }
            return delegate.write(value);
        }

        @Override
        public JsonGenerator writeNull() {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                return delegate.writeNull(keyIfNoObject);
            }
            return delegate.writeNull();
        }

        private boolean isWritingPrimitive() {
            return state == WritingState.NONE && keyIfNoObject != null;
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public void flush() {
            delegate.flush();
        }
    }
}
