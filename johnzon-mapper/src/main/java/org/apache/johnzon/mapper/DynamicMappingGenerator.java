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

import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerator;
import java.math.BigDecimal;
import java.math.BigInteger;

public class DynamicMappingGenerator implements MappingGenerator {
    private final MappingGenerator delegate;
    private final Runnable writeStart;
    private final Runnable writeEnd;
    private final String keyName;

    protected InObjectOrPrimitiveJsonGenerator generator;

    public DynamicMappingGenerator(final MappingGenerator delegate,
                                   final Runnable writeStart,
                                   final Runnable writeEnd,
                                   final String keyName) {
        this.delegate = delegate;
        this.writeStart = writeStart;
        this.writeEnd = writeEnd;
        this.keyName = keyName;
    }

    protected JsonGenerator getRawJsonGenerator() {
        return delegate.getJsonGenerator();
    }

    @Override
    public JsonGenerator getJsonGenerator() {
        return generator == null ? generator = new InObjectOrPrimitiveJsonGenerator(
                getRawJsonGenerator(), writeStart, keyName, writeEnd) : generator;
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
            reset();
        }
        return getJsonGenerator(); // ensure we wrap it
    }

    protected void reset() {
        // no-op
    }

    public void flushIfNeeded() {
        if (generator != null) {
            generator.endIfNeeded();
        }
    }

    private enum WritingState {
        NONE,
        WROTE_START,
        DONT_WRITE_END
    }

    public static class InObjectOrPrimitiveJsonGenerator implements JsonGenerator {
        private final JsonGenerator delegate;
        private final Runnable writeStart;
        private final Runnable writeEnd;
        private final String keyIfNoObject;
        private WritingState state = WritingState.NONE; // todo: we need a stack (linkedlist) here to be accurate
        private int nested = 0;
        private boolean implicitStart;

        private InObjectOrPrimitiveJsonGenerator(final JsonGenerator generator, final Runnable writeStart,
                                                 final String keyName, final Runnable writeEnd) {
            this.delegate = generator;
            this.writeStart = writeStart;
            this.writeEnd = writeEnd;
            this.keyIfNoObject = keyName;
        }

        private void ensureStart() {
            if (state != WritingState.NONE) {
                return;
            }
            writeStart.run();
            state = WritingState.WROTE_START;
        }

        @Override
        public JsonGenerator writeStartObject() {
            if (state == WritingState.NONE) {
                ensureStart();
            } else {
                nested++;
                delegate.writeStartObject();
            }
            return this;
        }

        @Override
        public JsonGenerator writeStartObject(final String name) {
            if (state == WritingState.NONE) {
                ensureStart();
            }
            nested++;
            delegate.writeStartObject(name);
            return this;
        }

        @Override
        public JsonGenerator writeStartArray() {
            if (state != WritingState.NONE) {
                nested++;
            }
            if (keyIfNoObject != null && state == WritingState.NONE) {
                state = WritingState.DONT_WRITE_END; // skip writeEnd since the impl will do it
                return delegate.writeStartArray(keyIfNoObject);
            } else if (state == WritingState.NONE) {
                ensureStart();
                return this;
            }
            delegate.writeStartArray();
            return this;
        }

        @Override
        public JsonGenerator writeStartArray(final String name) {
            if (state == WritingState.NONE && !implicitStart) { // force an enclosing object since we write in an object (we have a key)
                writeStartObject();
                implicitStart = true;
            }
            if (state != WritingState.NONE) {
                nested++;
            }
            ensureStart();
            delegate.writeStartArray(name);
            return this;
        }

        @Override
        public JsonGenerator writeKey(final String name) {
            ensureStart();
            delegate.writeKey(name);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final JsonValue value) {
            ensureStart();
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final String value) {
            ensureStart();
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final BigInteger value) {
            ensureStart();
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final BigDecimal value) {
            ensureStart();
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final int value) {
            ensureStart();
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final long value) {
            ensureStart();
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final double value) {
            ensureStart();
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final boolean value) {
            ensureStart();
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator writeNull(final String name) {
            ensureStart();
            delegate.writeNull(name);
            return this;
        }

        @Override
        public JsonGenerator write(final JsonValue value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                delegate.write(keyIfNoObject, value);
                return this;
            }
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final String value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                delegate.write(keyIfNoObject, value);
                return this;
            }
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final BigDecimal value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                delegate.write(keyIfNoObject, value);
                return this;
            }
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final BigInteger value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                delegate.write(keyIfNoObject, value);
                return this;
            }
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final int value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                delegate.write(keyIfNoObject, value);
                return this;
            }
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final long value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                delegate.write(keyIfNoObject, value);
                return this;
            }
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final double value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                delegate.write(keyIfNoObject, value);
                return this;
            }
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(boolean value) {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                delegate.write(keyIfNoObject, value);
                return this;
            }
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator writeNull() {
            if (isWritingPrimitive()) {
                state = WritingState.DONT_WRITE_END;
                delegate.writeNull(keyIfNoObject);
                return this;
            }
            delegate.writeNull();
            return this;
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public JsonGenerator writeEnd() {
            final JsonGenerator generator = doWriteEnd(false);
            if (nested == 0 && implicitStart) {
                doWriteEnd(false);
            }
            return generator;
        }

        private JsonGenerator doWriteEnd(final boolean useDelegate) {
            if (nested == 0 && state == WritingState.WROTE_START) {
                state = WritingState.NONE;
            }
            if (nested > 0) {
                nested--;
            }
            if (!useDelegate && nested == 0 && SkipEnclosingWriteEnd.NOOP != writeEnd) {
                writeEnd.run();
            } else {
                if (nested == 0) {
                    final JsonGenerator unwrap = unwrap(delegate);
                    unwrap.writeEnd();
                    implicitStart = false;
                } else {
                    delegate.writeEnd();
                }
            }
            return this;
        }

        private JsonGenerator unwrap(final JsonGenerator delegate) {
            JsonGenerator current = delegate;
            while (SkipLastWriteEndGenerator.class.isInstance(current)) {
                current = SkipLastWriteEndGenerator.class.cast(current).delegate;
            }
            return current;
        }

        public void endIfNeeded() {
            endIfNeeded(this);
        }

        private boolean isWritingPrimitive() {
            return state == WritingState.NONE && keyIfNoObject != null;
        }

        public static void endIfNeeded(final JsonGenerator generator) {
            if (!InObjectOrPrimitiveJsonGenerator.class.isInstance(generator)) {
                return;
            }
            final InObjectOrPrimitiveJsonGenerator jsonGenerator = InObjectOrPrimitiveJsonGenerator.class.cast(generator);
            if (jsonGenerator.state == WritingState.WROTE_START) {
                jsonGenerator.doWriteEnd(true);
                jsonGenerator.state = WritingState.DONT_WRITE_END;
            }
        }
    }

    private static abstract class DelegatingGenerator implements JsonGenerator {
        protected final JsonGenerator delegate;

        protected DelegatingGenerator(final JsonGenerator generator) {
            this.delegate = generator;
        }

        @Override
        public JsonGenerator writeKey(final String name) {
            delegate.writeKey(name);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final JsonValue value) {
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final String value) {
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final BigInteger value) {
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final BigDecimal value) {
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final int value) {
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final long value) {
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final double value) {
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator write(final String name, final boolean value) {
            delegate.write(name, value);
            return this;
        }

        @Override
        public JsonGenerator writeNull(final String name) {
            delegate.writeNull(name);
            return this;
        }

        @Override
        public JsonGenerator write(final JsonValue value) {
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final String value) {
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final BigDecimal value) {
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final BigInteger value) {
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final int value) {
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final long value) {
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(final double value) {
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator write(boolean value) {
            delegate.write(value);
            return this;
        }

        @Override
        public JsonGenerator writeNull() {
            delegate.writeNull();
            return this;
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

    private static class SkipLastWriteEndGenerator extends DelegatingGenerator {
        private int level = -1;

        private SkipLastWriteEndGenerator(final JsonGenerator generator) {
            super(generator);
        }

        @Override
        public JsonGenerator writeStartObject() {
            level++;
            delegate.writeStartObject();
            return this;
        }

        @Override
        public JsonGenerator writeStartObject(final String name) {
            level++;
            if (level == 0) {
                level++; // force a writeEnd since it will be a nested object and not the object we are writing
            }
            delegate.writeStartObject(name);
            return this;
        }

        @Override
        public JsonGenerator writeStartArray() {
            level++;
            delegate.writeStartArray();
            return this;
        }

        @Override
        public JsonGenerator writeStartArray(final String name) {
            delegate.writeStartArray(name);
            level++;
            return this;
        }

        @Override
        public JsonGenerator writeEnd() {
            if (level > 0) {
                delegate.writeEnd();
            } else if (level == 0 &&
                    InObjectOrPrimitiveJsonGenerator.class.isInstance(delegate) && // normally always true
                    InObjectOrPrimitiveJsonGenerator.class.cast(delegate).implicitStart) {
                delegate.writeEnd();
            }
            level--;
            return this;
        }
    }

    public static class SkipEnclosingWriteEnd extends DynamicMappingGenerator {
        private static final Runnable NOOP = () -> {
        };
        private final JsonGenerator rawGenerator;

        private SkipLastWriteEndGenerator skippingGenerator;

        public SkipEnclosingWriteEnd(final MappingGenerator delegate, final String keyName, final JsonGenerator generator) {
            super(delegate, NOOP, NOOP, keyName);
            this.rawGenerator = generator;
        }

        @Override
        protected JsonGenerator getRawJsonGenerator() {
            return rawGenerator;
        }

        @Override
        public JsonGenerator getJsonGenerator() {
            if (skippingGenerator == null) {
                skippingGenerator = new SkipLastWriteEndGenerator(super.getJsonGenerator());
            }
            return skippingGenerator;
        }

        @Override
        protected void reset() {
            super.reset();
            skippingGenerator = null;
        }
    }
}
