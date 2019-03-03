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

import java.io.Writer;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class JsonValueWriter extends Writer {
    private JsonValue result;

    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        // no-op
    }

    @Override
    public void close() {
        flush();
    }

    public void setResult(final JsonValue result) {
        this.result = result;
    }

    public JsonValue getResult() {
        return result;
    }

    public JsonObject getObject() {
        return result.asJsonObject();
    }

    public JsonArray getArray() {
        return result.asJsonArray();
    }
}
