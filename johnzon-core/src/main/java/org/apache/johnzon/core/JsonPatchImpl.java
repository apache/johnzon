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
import java.util.List;

import javax.json.JsonException;
import javax.json.JsonPatch;
import javax.json.JsonPointer;
import javax.json.JsonStructure;
import javax.json.JsonValue;

class JsonPatchImpl implements JsonPatch {

    private final List<PatchValue> patches;


    JsonPatchImpl(PatchValue... patches) {
        this.patches = Arrays.asList(patches);
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
                        throw new JsonException("JsonPatchOperation.TEST fails! Values are not equal");
                    }
                    break;
                default:
                    throw new IllegalStateException("unsupported operation: " + patch.operation);
            }
        }

        //X TODO dirty cast can be removed after JsonPointer uses generics like JsonPatch
        return (T) patched;
    }



    static class PatchValue {
        private final JsonPatchOperation operation;
        private final JsonPointer path;
        private final JsonPointer from;
        private final JsonValue value;

        PatchValue(JsonPatchOperation operation,
                   JsonPointer path,
                   JsonPointer from,
                   JsonValue value) {
            this.operation = operation;
            this.path = path;
            this.from = from;
            this.value = value;
        }
    }
}
