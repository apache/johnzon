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

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import java.io.Serializable;

class JsonWriterImpl implements JsonWriter, Serializable {
    private final JsonGenerator generator;
    private boolean closed = false;

    JsonWriterImpl(final JsonGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void writeArray(final JsonArray array) {
        checkClosed();
        try {
            generator.write(array);
        } finally {
            close();
        }
    }

    @Override
    public void writeObject(final JsonObject object) {
        checkClosed();
        try {
            generator.write(object);
        } finally {
            close();
        }
    }

    @Override
    public void write(final JsonValue value) {
        checkClosed();
        try {
            generator.write(value);
        } finally {
            close();
        }
    }

    @Override
    public void write(final JsonStructure value) {
        checkClosed();
        try {
            generator.write(value);
        } finally {
            close();
        }
    }

    @Override
    public void close() {

        if (!closed) {
            closed = true;
            generator.close();
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("writeArray(), writeObject(), write() or close() method was already called");
        }

    }
}
