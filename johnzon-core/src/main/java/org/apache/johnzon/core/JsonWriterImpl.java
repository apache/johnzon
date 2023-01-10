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
package org.apache.johnzon.core;

import java.io.Serializable;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.stream.JsonGenerator;

class JsonWriterImpl implements JsonWriter, Serializable {
    private final JsonGenerator generator;
    private boolean called = false;
    private boolean closed = false;

    JsonWriterImpl(final JsonGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void writeArray(final JsonArray array) {
        checkState();
        generator.write(array);
        markCalled();
    }

    @Override
    public void writeObject(final JsonObject object) {
        checkState();
        generator.write(object);
        markCalled();
    }

    @Override
    public void write(final JsonValue value) {
        checkState();
        generator.write(value);
        markCalled();
    }

    @Override
    public void write(final JsonStructure value) {
        checkState();
        generator.write(value);
        markCalled();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            generator.close();
        }
    }

    private void markCalled() {
        generator.flush();
        called = true;
    }

    private void checkState() {
        if (closed || called) {
            throw new IllegalStateException("writeArray(), writeObject(), write() or close() method was already called");
        }

    }
}
