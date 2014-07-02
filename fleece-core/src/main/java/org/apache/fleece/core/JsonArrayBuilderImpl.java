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

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonArrayBuilderImpl implements JsonArrayBuilder {
    private final JsonArrayImpl array = new JsonArrayImpl();

    @Override
    public JsonArrayBuilder add(final JsonValue value) {
        if (value == null) {
            npe();
        }
        array.addInternal(value);
        return this;
    }

    @Override
    public JsonArrayBuilder add(final String value) {
        if (value == null) {
            npe();
        }
        array.addInternal(new JsonStringImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final BigDecimal value) {
        if (value == null) {
            npe();
        }
        array.addInternal(new JsonNumberImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final BigInteger value) {
        if (value == null) {
            npe();
        }
        array.addInternal(new JsonNumberImpl(new BigDecimal(value)));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int value) {
        array.addInternal(new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final long value) {
        array.addInternal(new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final double value) {
        final Double valueObject = Double.valueOf(value);
        if (valueObject.isInfinite()) {
            throw new NumberFormatException("value must not be infinite");
        }
        if (valueObject.isNaN()) {
            throw new NumberFormatException("value must not be NaN");
        }
        array.addInternal(new JsonDoubleImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final boolean value) {
        array.addInternal(value ? JsonValue.TRUE : JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonArrayBuilder addNull() {
        array.addInternal(JsonValue.NULL);
        return this;
    }

    @Override
    public JsonArrayBuilder add(final JsonObjectBuilder builder) {
        if (builder == null) {
            throw new NullPointerException("builder must not be null");
        }
        array.addInternal(builder.build());
        return this;
    }

    @Override
    public JsonArrayBuilder add(final JsonArrayBuilder builder) {
        if (builder == null) {
            throw new NullPointerException("builder must not be null");
        }
        array.addInternal(builder.build());
        return this;
    }

    @Override
    public JsonArray build() {
        return array;
    }

    private static NullPointerException npe() {
        throw new NullPointerException("value must not be null");
    }
}
