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
package org.apache.johnzon.mapper.jsonp;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;


/**
 * This JsonGenerator will not automatically write a startObject '{' character, but only if needed.
 *
 * The {@link #writeEnd()} method will only write a closing '}' if a start has been written before.
 * 
 * This class must only be used in cases where you would call {@code jsonGenerator.startObject(key)} !
 */
public class DeferredStartJsonGenerator implements JsonGenerator {

    private final JsonGenerator delegate;
    private final String key;
    private final boolean array;

    private boolean started = false;
    private boolean empty = true;

    // this is needed to make sure we don't close more layers than we did open.
    private int depth = 0;

    /**
     * Deferred start for Objects
     *
     * @see #DeferredStartJsonGenerator(JsonGenerator, String, boolean)
     */
    public DeferredStartJsonGenerator(JsonGenerator delegate, String key) {
        this(delegate, key, false);
    }

    /**
     * JsonGenerator which only writes a start character if an embedded json structure is later written.
     *
     * @param delegate JsonGenerator which really writes
     * @param key for the startObject, or {@code null} if no key should be used
     * @param array if {@code true} we will use a start with a '[', otherwise with an object start '{'
     */
    public DeferredStartJsonGenerator(JsonGenerator delegate, String key, boolean array) {
        this.delegate = delegate;
        this.key = key;
        this.array = array;
    }

    private void ensureStart() {
        if (!started) {
            if (array) {
                if (key != null) {
                    delegate.writeStartArray(key);
                } else {
                    delegate.writeStartArray();
                }
            } else {
                if (key != null) {
                    delegate.writeStartObject(key);
                } else {
                    delegate.writeStartObject();
                }
            }
            started = true;
            depth++;
        }
    }

    @Override
    public void close() {
        writeEnd();
        delegate.close();
    }

    @Override
    public void flush() {
        delegate.flush();
    }

    @Override
    public JsonGenerator write(String name, BigDecimal value) {
        ensureStart();
        empty = false;
        delegate.write(name, value);
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(String name, BigInteger value) {
        ensureStart();
        delegate.write(name, value);
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(String name, boolean value) {
        ensureStart();
        delegate.write(name, value);
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(String name, double value) {
        ensureStart();
        delegate.write(name, value);
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(String name, int value) {
        ensureStart();
        delegate.write(name, value);
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(String name, JsonValue value) {
        ensureStart();
        delegate.write(name, value);
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(String name, long value) {
        ensureStart();
        delegate.write(name, value);
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(String name, String value) {
        ensureStart();
        delegate.write(name, value);
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(BigDecimal value) {
        if (!started && key != null) {
            // means we write a value instead of an object
            delegate.write(key, value);
        } else {
            delegate.write(value);
        }
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(BigInteger value) {
        if (!started && key != null) {
            // means we write a value instead of an object
            delegate.write(key, value);
        } else {
            delegate.write(value);
        }
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(boolean value) {
        if (!started && key != null) {
            // means we write a value instead of an object
            delegate.write(key, value);
        } else {
            delegate.write(value);
        }
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(double value) {
        if (!started && key != null) {
            // means we write a value instead of an object
            delegate.write(key, value);
        } else {
            delegate.write(value);
        }
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(int value) {
        if (!started && key != null) {
            // means we write a value instead of an object
            delegate.write(key, value);
        } else {
            delegate.write(value);
        }
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(JsonValue value) {
        if (!started && key != null) {
            // means we write a value instead of an object
            delegate.write(key, value);
        } else {
            delegate.write(value);
        }
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(long value) {
        if (!started && key != null) {
            // means we write a value instead of an object
            delegate.write(key, value);
        } else {
            delegate.write(value);
        }
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator write(String value) {
        if (!started && key != null) {
            // means we write a value instead of an object
            delegate.write(key, value);
        } else {
            delegate.write(value);
        }
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator writeEnd() {
        if (empty) {
            if (key != null) {
                delegate.writeStartObject(key);
            } else {
                delegate.writeStartObject();
            }
            started = true;
            depth++;
        }
        if (started && depth > 0) {
            delegate.writeEnd();
            depth--;
        }


        return this;
    }

    @Override
    public JsonGenerator writeKey(String name) {
        delegate.writeKey(name);
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        if (!started && key != null) {
            // means we write a value instead of an object
            delegate.writeNull(key);
        } else {
            delegate.writeNull();
        }
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator writeNull(String name) {
        ensureStart();
        delegate.writeNull(name);
        empty = false;
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        // safeguard if Converters, Serializers ets do a manual startArray()
        if (key != null && !started) {
            delegate.writeStartArray(key);
        } else {
            delegate.writeStartArray();
        }
        started = true;
        empty = false;
        depth++;
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(String name) {
        ensureStart();
        delegate.writeStartArray(name);
        started = true;
        empty = false;
        depth++;
        return this;
    }

    @Override
    public JsonGenerator writeStartObject() {
        // safeguard if Converters, Serializers ets do a manual startObject()
        if (key != null && !started) {
            delegate.writeStartObject(key);
        } else {
            delegate.writeStartObject();
        }

        started = true;
        empty = false;
        depth++;
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(String name) {
        ensureStart();
        started = true;
        delegate.writeStartObject(name);
        empty = false;
        depth++;
        return this;
    }
}
