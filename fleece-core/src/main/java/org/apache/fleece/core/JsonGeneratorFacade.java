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
package org.apache.fleece.core;

import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import java.math.BigDecimal;
import java.math.BigInteger;

// basically a proxy but java.lang.reflect.Proxy would be too slow for JSon
//
// just a facade allowing to reuse the same pointer (instance)
// without using a fluent API
public class JsonGeneratorFacade implements JsonGenerator {
    private JsonGenerator delegate;

    public JsonGeneratorFacade(final JsonGenerator delegate) {
        this.delegate = delegate;
    }

    @Override
    public JsonGenerator writeStartObject() {
        delegate = delegate.writeStartObject();
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(final String name) {
        delegate = delegate.writeStartObject(name);
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        delegate = delegate.writeStartArray();
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(final String name) {
        delegate = delegate.writeStartArray(name);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final JsonValue value) {
        delegate = delegate.write(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final String value) {
        delegate = delegate.write(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigInteger value) {
        delegate = delegate.write(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigDecimal value) {
        delegate = delegate.write(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final int value) {
        delegate = delegate.write(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final long value) {
        delegate = delegate.write(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final double value) {
        delegate = delegate.write(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final boolean value) {
        delegate = delegate.write(name, value);
        return this;
    }

    @Override
    public JsonGenerator writeNull(final String name) {
        delegate = delegate.writeNull(name);
        return this;
    }

    @Override
    public JsonGenerator writeEnd() {
        delegate = delegate.writeEnd();
        return this;
    }

    @Override
    public JsonGenerator write(final JsonValue value) {
        delegate = delegate.write(value);
        return this;
    }

    @Override
    public JsonGenerator write(final String value) {
        delegate = delegate.write(value);
        return this;
    }

    @Override
    public JsonGenerator write(final BigDecimal value) {
        delegate = delegate.write(value);
        return this;
    }

    @Override
    public JsonGenerator write(final BigInteger value) {
        delegate = delegate.write(value);
        return this;
    }

    @Override
    public JsonGenerator write(final int value) {
        delegate = delegate.write(value);
        return this;
    }

    @Override
    public JsonGenerator write(final long value) {
        delegate = delegate.write(value);
        return this;
    }

    @Override
    public JsonGenerator write(final double value) {
        delegate = delegate.write(value);
        return this;
    }

    @Override
    public JsonGenerator write(final boolean value) {
        delegate = delegate.write(value);
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        delegate = delegate.writeNull();
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
