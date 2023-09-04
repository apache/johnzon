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

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonPatch;
import jakarta.json.JsonPointer;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

import static java.util.Locale.ROOT;

class JsonPatchImpl implements JsonPatch {

    private final JsonProvider provider;
    private final List<PatchValue> patches;

    private volatile JsonArray json;

    JsonPatchImpl(final JsonProvider provider, final PatchValue... patches) {
        this(provider, Arrays.asList(patches));
    }

    JsonPatchImpl(final JsonProvider provider, final List<PatchValue> patches) {
        this.provider = provider;
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
                    patched = patch.pathPointer.add(patched, patch.value);
                    break;
                case REMOVE:
                    patched = patch.pathPointer.remove(patched);
                    break;
                case REPLACE:
                    // first remove the existing element and then add the new value
                    patched = patch.pathPointer.add(patch.pathPointer.remove(patched), patch.value);
                    break;
                case MOVE:
                    JsonValue valueToMove = patch.fromPointer.getValue(patched);
                    patched = patch.pathPointer.add(patch.fromPointer.remove(patched), valueToMove);
                    break;
                case COPY:
                    JsonValue toCopy = patch.fromPointer.getValue(patched);
                    patched = patch.pathPointer.add(patched, toCopy);
                    break;
                case TEST:
                    JsonValue toTest = patch.pathPointer.getValue(patched);
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
        if (patches.isEmpty()) {
            return JsonValue.EMPTY_JSON_ARRAY;
        }
        if (json == null) {
            synchronized (this) {
                if (json == null) {
                    final JsonArrayBuilder builder = provider.createArrayBuilder();
                    for (final PatchValue patch : patches) {
                        builder.add(patch.toJson());
                    }
                    json = builder.build();
                }
            }
        }
        return json;
    }

    @Override
    public String toString() {
        if (patches.isEmpty()) {
            return "[]";
        }
        return toJsonArray().toString();
    }

    static class PatchValue {
        private final JsonProvider provider;
        private final JsonPatch.Operation operation;
        private String path;
        private String from;
        private final JsonPointer pathPointer;
        private final JsonPointer fromPointer;
        private final JsonValue value;

        private volatile String str;
        private volatile JsonObject json;
        private volatile Integer hash;

        PatchValue(final JsonProvider provider,
                   final JsonPatch.Operation operation,
                   final String path,
                   final String from,
                   final JsonValue value) {
            this.provider = provider;
            this.operation = operation;
            this.path = path;
            this.from = from;
            this.pathPointer = provider.createPointer(path);

            // ignore from if we do not need it
            if (operation == JsonPatch.Operation.MOVE || operation == JsonPatch.Operation.COPY) {
                this.fromPointer = provider.createPointer(from);
            } else {
                this.fromPointer = null;
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
            if (hash == null) {
                synchronized (this) {
                    if (hash == null) {
                        int result = operation.hashCode();
                        result = 31 * result + path.hashCode();
                        result = 31 * result + (from != null ? from.hashCode() : 0);
                        result = 31 * result + (value != null ? value.hashCode() : 0);
                        hash = result;
                    }
                }
            }
            return hash;
        }


        @Override
        public String toString() {
            if (str == null) {
                synchronized (this) {
                    if (str == null) {
                        str = "{op: " + operation + ", path: " +
                              path + ", from: " + from + ", value: " + value + '}';
                    }
                }
            }
            return str;
        }

        JsonObject toJson() {
            if (json == null) {
                synchronized (this) {
                    if (json == null) {
                        JsonObjectBuilder builder = provider.createObjectBuilder()
                                .add("op", operation.name().toLowerCase(ROOT))
                                .add("path", path);

                        if (fromPointer != null) {
                            builder.add("from", from);
                        }

                        if (value != null) {
                            builder.add("value", value);
                        }

                        json = builder.build();
                    }
                }
            }
            return json;
        }
    }

}
