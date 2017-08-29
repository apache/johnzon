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
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

class JsonPatchBuilderImpl implements JsonPatchBuilder {

    private final List<JsonPatchImpl.PatchValue> operations;


    JsonPatchBuilderImpl() {
        operations = new ArrayList<>();
    }

    JsonPatchBuilderImpl(JsonArray initialData) {
        operations = new ArrayList<>(initialData.size());

        for (JsonValue value : initialData) {

            JsonObject operation = (JsonObject) value;

            JsonPatch.Operation op = JsonPatch.Operation.fromOperationName(operation.getString("op"));
            String path = operation.getString("path");
            String from = operation.getString("from", null);
            JsonValue jsonValue = operation.get("value");

            operations.add(new JsonPatchImpl.PatchValue(op,
                                                        path,
                                                        from,
                                                        jsonValue));
        }
    }


    @Override
    public JsonPatchBuilder add(String path, JsonValue value) {
        return addOperation(new JsonPatchImpl.PatchValue(JsonPatch.Operation.ADD,
                                                         path,
                                                         null,
                                                         value));
    }

    @Override
    public JsonPatchBuilder add(String path, String value) {
        return add(path, toJsonString(value));
    }

    @Override
    public JsonPatchBuilder add(String path, int value) {
        return add(path, toJsonNumber(value));
    }

    @Override
    public JsonPatchBuilder add(String path, boolean value) {
        return add(path, toJsonBoolean(value));
    }


    @Override
    public JsonPatchBuilder remove(String path) {
        return addOperation(new JsonPatchImpl.PatchValue(JsonPatch.Operation.REMOVE,
                                                         path,
                                                         null,
                                                         null));
    }


    @Override
    public JsonPatchBuilder replace(String path, JsonValue value) {
        return addOperation(new JsonPatchImpl.PatchValue(JsonPatch.Operation.REPLACE,
                                                         path,
                                                         null,
                                                         value));
    }

    @Override
    public JsonPatchBuilder replace(String path, String value) {
        return replace(path, toJsonString(value));
    }

    @Override
    public JsonPatchBuilder replace(String path, int value) {
        return replace(path, toJsonNumber(value));
    }

    @Override
    public JsonPatchBuilder replace(String path, boolean value) {
        return replace(path, toJsonBoolean(value));
    }


    @Override
    public JsonPatchBuilder move(String path, String from) {
        return addOperation(new JsonPatchImpl.PatchValue(JsonPatch.Operation.MOVE,
                                                         path,
                                                         from,
                                                         null));
    }


    @Override
    public JsonPatchBuilder copy(String path, String from) {
        return addOperation(new JsonPatchImpl.PatchValue(JsonPatch.Operation.COPY,
                                                         path,
                                                         from,
                                                         null));
    }


    @Override
    public JsonPatchBuilder test(String path, JsonValue value) {
        return addOperation(new JsonPatchImpl.PatchValue(JsonPatch.Operation.TEST,
                                                         path,
                                                         null,
                                                         value));
    }

    @Override
    public JsonPatchBuilder test(String path, String value) {
        return test(path, toJsonString(value));
    }

    @Override
    public JsonPatchBuilder test(String path, int value) {
        return test(path, toJsonNumber(value));
    }

    @Override
    public JsonPatchBuilder test(String path, boolean value) {
        return test(path, toJsonBoolean(value));
    }


    @Override
    public JsonPatch build() {
        JsonPatchImpl patch = new JsonPatchImpl(new ArrayList<>(operations));

        return patch;

    }


    private JsonPatchBuilder addOperation(JsonPatchImpl.PatchValue operation) {
        operations.add(operation);
        return this;
    }

    private static JsonValue toJsonBoolean(boolean value) {
        return value ? JsonValue.TRUE : JsonValue.FALSE;
    }

    private static JsonValue toJsonString(String value) {
        return value == null ? JsonValue.NULL : new JsonStringImpl(value);
    }

    private static JsonValue toJsonNumber(int value) {
        return new JsonLongImpl(value);
    }

}
