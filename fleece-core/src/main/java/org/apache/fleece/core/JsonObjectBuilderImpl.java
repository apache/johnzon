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

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonObjectBuilderImpl implements JsonObjectBuilder {
    private final JsonObjectImpl delegate = new JsonObjectImpl();

    @Override
    public JsonObjectBuilder add(final String name, final JsonValue value) {
        delegate.putInternal(name, value);
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final String value) {
        delegate.putInternal(name, new JsonStringImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final BigInteger value) {
        delegate.putInternal(name, new JsonNumberImpl(new BigDecimal(value)));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final BigDecimal value) {
        delegate.putInternal(name, new JsonNumberImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final int value) {
        delegate.putInternal(name, new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final long value) {
        delegate.putInternal(name, new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final double value) {
        delegate.putInternal(name, new JsonDoubleImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final boolean value) {
        delegate.putInternal(name, value ? JsonValue.TRUE : JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonObjectBuilder addNull(final String name) {
        delegate.putInternal(name, JsonValue.NULL);
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final JsonObjectBuilder builder) {
        delegate.putInternal(name, builder.build());
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final JsonArrayBuilder builder) {
        delegate.putInternal(name, builder.build());
        return this;
    }

    @Override
    public JsonObject build() {
        return delegate;
    }
}
