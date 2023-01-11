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
import java.util.LinkedList;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerator;

// assume usage is right, since it is an internal based on the fact we correctly use jsongenerator api it is fine
// todo: drop reflection, it is not needed here but it was simpler for a first impl
public class JsonObjectGenerator implements JsonGenerator {
    private final JsonBuilderFactory factory;
    private final LinkedList<Object> builders = new LinkedList<>();

    private JsonObjectBuilder objectBuilder;
    private JsonArrayBuilder arrayBuilder;

    public JsonObjectGenerator(final JsonBuilderFactory factory) {
        this.factory = factory;
    }

    @Override
    public JsonGenerator writeStartObject() {
        objectBuilder = factory.createObjectBuilder();
        builders.add(objectBuilder);
        arrayBuilder = null;
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(final String name) {
        objectBuilder = factory.createObjectBuilder();
        builders.add(new NamedBuilder<>(objectBuilder, name));
        arrayBuilder = null;
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        arrayBuilder = factory.createArrayBuilder();
        builders.add(arrayBuilder);
        objectBuilder = null;
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(final String name) {
        arrayBuilder = factory.createArrayBuilder();
        builders.add(new NamedBuilder<>(arrayBuilder, name));
        objectBuilder = null;
        return this;
    }

    @Override
    public JsonGenerator writeKey(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonGenerator write(final String name, final JsonValue value) {
        objectBuilder.add(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final String value) {
        objectBuilder.add(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigInteger value) {
        objectBuilder.add(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final BigDecimal value) {
        objectBuilder.add(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final int value) {
        objectBuilder.add(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final long value) {
        objectBuilder.add(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final double value) {
        objectBuilder.add(name, value);
        return this;
    }

    @Override
    public JsonGenerator write(final String name, final boolean value) {
        objectBuilder.add(name, value);
        return this;
    }

    @Override
    public JsonGenerator writeNull(final String name) {
        objectBuilder.addNull(name);
        return this;
    }

    @Override
    public JsonGenerator write(final JsonValue value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final String value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final BigDecimal value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final BigInteger value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final int value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final long value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final double value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator write(final boolean value) {
        arrayBuilder.add(value);
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        arrayBuilder.addNull();
        return this;
    }

    @Override
    public JsonGenerator writeEnd() {
        if (builders.size() == 1) {
            return this;
        }

        final Object last = builders.removeLast();

        /*
         * Previous potential cases:
         * 1. json array -> we add the builder directly
         * 2. NamedBuilder{array|object} -> we add the builder in the previous object
         */

        final String name;
        Object previous = builders.getLast();
        if (NamedBuilder.class.isInstance(previous)) {
            final NamedBuilder namedBuilder = NamedBuilder.class.cast(previous);
            name = namedBuilder.name;
            previous = namedBuilder.builder;
        } else {
            name = null;
        }

        if (JsonArrayBuilder.class.isInstance(last)) {
            final JsonArrayBuilder array = JsonArrayBuilder.class.cast(last);
            if (JsonArrayBuilder.class.isInstance(previous)) {
                arrayBuilder = JsonArrayBuilder.class.cast(previous);
                objectBuilder = null;
                arrayBuilder.add(array);
            } else if (JsonObjectBuilder.class.isInstance(previous)) {
                objectBuilder = JsonObjectBuilder.class.cast(previous);
                arrayBuilder = null;
                objectBuilder.add(name, array);
            } else {
                throw new IllegalArgumentException("Unsupported previous builder: " + previous);
            }
        } else if (JsonObjectBuilder.class.isInstance(last)) {
            final JsonObjectBuilder object = JsonObjectBuilder.class.cast(last);
            if (JsonArrayBuilder.class.isInstance(previous)) {
                arrayBuilder = JsonArrayBuilder.class.cast(previous);
                objectBuilder = null;
                arrayBuilder.add(object);
            } else if (JsonObjectBuilder.class.isInstance(previous)) {
                objectBuilder = JsonObjectBuilder.class.cast(previous);
                arrayBuilder = null;
                objectBuilder.add(name, object);
            } else {
                throw new IllegalArgumentException("Unsupported previous builder: " + previous);
            }
        } else if (NamedBuilder.class.isInstance(last)) {
            final NamedBuilder<?> namedBuilder = NamedBuilder.class.cast(last);
            if (JsonObjectBuilder.class.isInstance(previous)) {
                objectBuilder = JsonObjectBuilder.class.cast(previous);
                if (JsonArrayBuilder.class.isInstance(namedBuilder.builder)) {
                    objectBuilder.add(namedBuilder.name, JsonArrayBuilder.class.cast(namedBuilder.builder));
                    arrayBuilder = null;
                } else if (JsonObjectBuilder.class.isInstance(namedBuilder.builder)) {
                    objectBuilder.add(namedBuilder.name, JsonObjectBuilder.class.cast(namedBuilder.builder));
                    arrayBuilder = null;
                } else {
                    throw new IllegalArgumentException("Unsupported previous builder: " + previous);
                }
            } else {
                throw new IllegalArgumentException("Unsupported previous builder, expected object builder: " + previous);
            }
        } else {
            throw new IllegalArgumentException("Unsupported previous builder: " + previous);
        }
        return this;
    }

    @Override
    public void flush() {
        // no-op
    }

    @Override
    public void close() {
        flush();
    }

    public JsonValue getResult() {
        final Object last = builders.getLast();
        if (JsonArrayBuilder.class.isInstance(last)) {
            return JsonArrayBuilder.class.cast(last).build();
        }
        if (JsonObjectBuilder.class.isInstance(last)) {
            return JsonObjectBuilder.class.cast(last).build();
        }
        throw new IllegalArgumentException("Nothing prepared or wrongly prepared");
    }

    private static class NamedBuilder<T> {
        private final T builder;
        private final String name;

        private NamedBuilder(final T builder, final String name) {
            this.builder = builder;
            this.name = name;
        }
    }
}
