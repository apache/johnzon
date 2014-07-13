/*
esc * Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.fleece.core;

import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentMap;

public class JsonGeneratorImpl<P extends JsonGeneratorImpl<?>> implements JsonGenerator, JsonChars, Serializable {
    protected static final String START_ARRAY = "[";
    protected static final String END_ARRAY = "]";
    protected static final String END_OBJECT = "}";
    protected static final String START_OBJECT = "{";
    protected static final String NULL = "null";
    protected static final String NULL_KEY = "null:";

    protected final Writer writer;
    protected final P parent;
    protected final boolean array;
    protected final ConcurrentMap<String, String> cache;

    protected boolean needComma = false;

    public JsonGeneratorImpl(final Writer writer, final ConcurrentMap<String, String> cache) {
        this(writer, null, false, cache);
    }

    public JsonGeneratorImpl(final Writer writer, final P parent, final boolean array,
                             final ConcurrentMap<String, String> cache) {
        this.writer = writer;
        this.parent = parent;
        this.array = array;
        this.cache = cache;
    }

    private void addCommaIfNeeded() {
        if (needComma) {
            try {
                writer.write(',');
            } catch (final IOException e) {
                throw new JsonGenerationException(e.getMessage(), e);
            }
            needComma = false;
        }
    }

    protected JsonGenerator newJsonGenerator(final Writer writer, final P parent, final boolean array) {
        return new JsonGeneratorImpl<P>(writer, parent, array, cache);
    }

    // we cache key only since they are generally fixed
    private String key(final String name) {
        if (name == null) {
            return NULL_KEY;
        }
        String k = cache.get(name);
        if (k == null) {
            k = '"' + Strings.escape(name) + "\":";
            cache.putIfAbsent(name, k);
        }
        return k;
    }

    @Override
    public JsonGenerator writeStartObject() {
        noCheckWriteAndForceComma(START_OBJECT);
        return newJsonGenerator(writer, (P) this, false);
    }

    @Override
    public JsonGenerator writeStartObject(final String name) {
        noCheckWriteAndForceComma(key(name) + START_OBJECT);
        return newJsonGenerator(writer, (P) this, false);
    }

    @Override
    public JsonGenerator writeStartArray() {
        noCheckWriteAndForceComma(START_ARRAY);
        return newJsonGenerator(writer, (P) this, true);
    }

    @Override
    public JsonGenerator writeStartArray(final String name) {
        noCheckWriteAndForceComma(key(name) + START_ARRAY);
        return newJsonGenerator(writer, (P) this, true);
    }

    @Override
    public JsonGenerator write(final String name, final JsonValue value) {
        if (JsonString.class.isInstance(value)) {
            return write(name, value == null ? null : value.toString());
        }
        checkObject();
        noCheckWriteAndForceComma(key(name));
        justWrite(value == null ? NULL : value.toString());
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final String value) {
        checkObject();
        noCheckWriteAndForceComma(key(name));
        justWrite(value == null ? NULL : "\"" + Strings.escape(value) + "\"");
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigInteger value) {
        checkObject();
        noCheckWriteAndForceComma(key(name));
        justWrite(value == null ? NULL : value.toString());
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigDecimal value) {
        checkObject();
        noCheckWriteAndForceComma(key(name));
        justWrite(value == null ? NULL : value.toString());
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final int value) {
        checkObject();
        noCheckWriteAndForceComma(key(name));
        justWrite(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final long value) {
        checkObject();
        noCheckWriteAndForceComma(key(name));
        justWrite(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final double value) {
        checkObject();
        checkDoubleRange(value);
        noCheckWriteAndForceComma(key(name));
        justWrite(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final boolean value) {
        checkObject();
        noCheckWriteAndForceComma(key(name));
        justWrite(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator writeNull(final String name) {
        checkObject();
        noCheckWriteAndForceComma(key(name));
        justWrite(NULL);
        return this;
    }

    @Override
    public JsonGenerator writeEnd() {
        needComma = false;
        noCheckWriteAndForceComma(array ? END_ARRAY : END_OBJECT);
        return parent != null ? parent : this;
    }

    @Override
    public JsonGenerator write(final JsonValue value) {
        noCheckWriteAndForceComma(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(final String value) {
        checkArray();
        noCheckWriteAndForceComma(QUOTE+Strings.escape(value)+QUOTE);
        return this;
    }

    @Override
    public JsonGenerator write(final BigDecimal value) {
        checkArray();
        noCheckWrite(String.valueOf(value));
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final BigInteger value) {
        checkArray();
        noCheckWrite(String.valueOf(value));
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final int value) {
        checkArray();
        noCheckWrite(Integer.toString(value));
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final long value) {
        checkArray();
        noCheckWrite(Long.toString(value));
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final double value) {
        checkArray();
        checkDoubleRange(value);
        noCheckWrite(Double.toString(value));
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator write(final boolean value) {
        checkArray();
        noCheckWrite(Boolean.toString(value));
        needComma = true;
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        checkArray();
        noCheckWriteAndForceComma(NULL);
        needComma = true;
        return this;
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (final IOException e) {
            throw new JsonGenerationException(e.getMessage(), e);
        }
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (final IOException e) {
            throw new JsonGenerationException(e.getMessage(), e);
        }
    }

    protected JsonGenerator noCheckWriteAndForceComma(final String value) {
        noCheckWrite(value);
        needComma = true;
        return this;
    }

    private void noCheckWrite(String value) {
        addCommaIfNeeded();
        justWrite(value);
    }

    private void justWrite(final String value) {
        try {
            writer.write(value);
        } catch (final IOException e) {
            throw new JsonGenerationException(e.getMessage(), e);
        }
    }

    private void checkObject() {
        if (array) {
            throw new JsonGenerationException("write(name, param) is only valid in objects");
        }
    }

    private void checkArray() {
        if (!array) {
            throw new JsonGenerationException("write(param) is only valid in arrays");
        }
    }

    private static void checkDoubleRange(final double value) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new NumberFormatException("double can't be infinite and NaN");
        }
    }
}
