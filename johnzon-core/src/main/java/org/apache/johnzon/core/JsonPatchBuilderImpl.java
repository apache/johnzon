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
package org.apache.johnzon.core;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;

public class JsonPatchBuilderImpl implements JsonPatchBuilder {
    public JsonPatchBuilderImpl() {
        super();
    }

    public JsonPatchBuilderImpl(JsonArray initialData) {
        super();
    }



    @Override
    public JsonStructure apply(JsonStructure target) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonObject apply(JsonObject target) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonArray apply(JsonArray target) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder add(String path, JsonValue value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder add(String path, String value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder add(String path, int value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder add(String path, boolean value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder remove(String path) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder replace(String path, JsonValue value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder replace(String path, String value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder replace(String path, int value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder replace(String path, boolean value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder move(String path, String from) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder copy(String path, String from) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder test(String path, JsonValue value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder test(String path, String value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder test(String path, int value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatchBuilder test(String path, boolean value) {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }

    @Override
    public JsonPatch build() {
        throw new UnsupportedOperationException("JSON-P 1.1");
    }
}
