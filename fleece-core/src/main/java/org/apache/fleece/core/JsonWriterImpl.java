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
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import java.io.Flushable;
import java.io.IOException;
import java.io.Serializable;

public class JsonWriterImpl implements JsonWriter, Serializable, Flushable {
    private final JsonGenerator generator;

    public JsonWriterImpl(final JsonGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void writeArray(final JsonArray array) {
        generator.write(array);
    }

    @Override
    public void writeObject(final JsonObject object) {
        generator.write(object);
    }

    @Override
    public void write(final JsonStructure value) {
        generator.write(value);
    }

    @Override
    public void close() {
        generator.close();
    }

    @Override
    public void flush() throws IOException {
        generator.flush();
    }
}
