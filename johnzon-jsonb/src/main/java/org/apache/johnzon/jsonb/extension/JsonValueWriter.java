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
package org.apache.johnzon.jsonb.extension;

import java.io.StringReader;
import java.io.Writer;
import java.util.function.Consumer;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

public class JsonValueWriter extends Writer implements Consumer<JsonValue> {
    private JsonValue result;
    private StringBuilder fallbackOutput;

    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        if (fallbackOutput == null) {
            fallbackOutput = new StringBuilder();
        }
        fallbackOutput.append(cbuf, off, len);
    }

    @Override
    public void flush() {
        // no-op
    }

    @Override
    public void close() {
        flush();
    }

    @Deprecated
    public void setResult(final JsonValue result) {
        this.result = result;
    }

    public JsonValue getResult() {
        if (result == null && fallbackOutput != null) {
            try (final JsonReader reader = Json.createReader(new StringReader(fallbackOutput.toString()))) {
                result = reader.readValue();
            }
        }
        return result;
    }

    public JsonObject getObject() {
        return getResult().asJsonObject();
    }

    public JsonArray getArray() {
        return getResult().asJsonArray();
    }

    @Override
    public void accept(final JsonValue jsonValue) {
        this.result = jsonValue;
    }
}
