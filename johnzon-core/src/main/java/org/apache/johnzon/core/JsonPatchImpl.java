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


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonPatch;
import javax.json.JsonStructure;
import javax.json.JsonValue;

class JsonPatchImpl implements JsonPatch {

    private final List<PatchValue> patches;


    JsonPatchImpl(PatchValue... patches) {
        this.patches = Arrays.asList(patches);
    }

    JsonPatchImpl(List<PatchValue> patches) {
        if (patches == null) {
            this.patches = Collections.emptyList();
        } else {
            this.patches = Collections.unmodifiableList(patches);
        }
    }


    @Override
    public <T extends JsonStructure> T apply(T target) {

        //X TODO JsonPointer should use generics like JsonPatch
        JsonStructure patched = target;

        for (PatchValue patch : patches) {

            switch (patch.operation) {
                case ADD:
                    patched = patch.path.add(patched, patch.value);
                    break;
                case REMOVE:
                    patched = patch.path.remove(patched);
                    break;
                case REPLACE:
                    // first remove the existing element and then add the new value
                    patched = patch.path.add(patch.path.remove(patched), patch.value);
                    break;
                case MOVE:
                    JsonValue valueToMove = patch.from.getValue(patched);
                    patched = patch.path.add(patch.from.remove(patched), valueToMove);
                    break;
                case COPY:
                    JsonValue toCopy = patch.from.getValue(patched);
                    patched = patch.path.add(patched, toCopy);
                    break;
                case TEST:
                    JsonValue toTest = patch.path.getValue(patched);
                    if (!toTest.equals(patch.value)) {
                        throw new JsonException("JsonPatch.Operation.TEST fails! Values are not equal");
                    }
                    break;
                default:
                    throw new IllegalStateException("unsupported operation: " + patch.operation);
            }
        }

        //X TODO dirty cast can be removed after JsonPointer uses generics like JsonPatch
        return (T) patched;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JsonPatchImpl jsonPatch = (JsonPatchImpl) o;

        return patches.equals(jsonPatch.patches);
    }

    @Override
    public int hashCode() {
        return patches.hashCode();
    }


    @Override
    public JsonArray toJsonArray() {

        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (PatchValue patch : patches) {
            builder.add(patch.toJson());
        }

        return builder.build();
    }



    static class PatchValue {
        private final JsonPatch.Operation operation;
        private final JsonPointerImpl path;
        private final JsonPointerImpl from;
        private final JsonValue value;

        PatchValue(JsonPatch.Operation operation,
                   String path,
                   String from,
                   JsonValue value) {
            this.operation = operation;
            this.path = new JsonPointerImpl(path);

            // ignore from if we do not need it
            if (operation == JsonPatch.Operation.MOVE || operation == JsonPatch.Operation.COPY) {
                this.from = new JsonPointerImpl(from);
            } else {
                this.from = null;
            }

            this.value = value;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PatchValue that = (PatchValue) o;

            if (operation != that.operation) {
                return false;
            }
            if (!path.equals(that.path)) {
                return false;
            }
            if (from != null ? !from.equals(that.from) : that.from != null) {
                return false;
            }
            return value != null ? value.equals(that.value) : that.value == null;
        }

        @Override
        public int hashCode() {
            int result = operation.hashCode();
            result = 31 * result + path.hashCode();
            result = 31 * result + (from != null ? from.hashCode() : 0);
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }


        @Override
        public String toString() {
            return "{" +
                   "op: " + operation +
                   ", path: " + path +
                   ", from: " + from +
                   ", value: " + value +
                   '}';
        }

        JsonObject toJson() {
            JsonObjectBuilder builder = Json.createObjectBuilder()
                                            .add("op", operation.name().toLowerCase())
                                            .add("path", path.getJsonPointer());

            if (from != null) {
                builder.add("from", from.getJsonPointer());
            }

            if (value != null) {
                builder.add("value", value);
            }

            return builder.build();
        }
    }
}
