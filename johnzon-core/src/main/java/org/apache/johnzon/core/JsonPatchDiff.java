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

import java.util.Map;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;

/**
 * Create a diff from a source and target JsonStructure
 */
class JsonPatchDiff {

    private final JsonStructure source;
    private final JsonStructure target;

    JsonPatchDiff(JsonStructure source, JsonStructure target) {
        this.source = source;
        this.target = target;
    }

    JsonPatch calculateDiff() {
        JsonPatchBuilder patchBuilder = new JsonPatchBuilderImpl();

        diff(patchBuilder, "/", source, target);

        return patchBuilder.build();
    }

    private void diff(JsonPatchBuilder patchBuilder, String basePath, JsonStructure source, JsonStructure target) {
        if (isJsonObject(source) && isJsonObject(target)) {
            // handle JsonObjects
            diffJsonObjects(patchBuilder, basePath, (JsonObject) source, (JsonObject) target);

        } else if (source instanceof JsonArray && target instanceof JsonArray) {
            // handle JsonArray
            //X TODO
            throw new UnsupportedOperationException("not yet implemented.");
        } else {
            throw new UnsupportedOperationException("not yet implemented.");
        }
    }

    private void diffJsonObjects(JsonPatchBuilder patchBuilder, String basePath, JsonObject source, JsonObject target) {
        Set<Map.Entry<String, JsonValue>> sourceEntries = source.entrySet();

        for (Map.Entry<String, JsonValue> sourceEntry : sourceEntries) {
            String attributeName = sourceEntry.getKey();
            if (target.containsKey(attributeName)) {
                JsonValue targetValue = target.get(attributeName);

                if (isJsonObject(targetValue) && isJsonObject(targetValue)) {
                    diffJsonObjects(patchBuilder, basePath + attributeName + "/", (JsonObject) sourceEntry.getValue(), (JsonObject) targetValue);
                } else if (!sourceEntry.getValue().equals(targetValue)) {
                    // replace the original value
                    patchBuilder.replace(basePath + attributeName, targetValue);
                }
            } else {
                // the value got removed
                patchBuilder.remove(basePath + attributeName);
            }
        }

        Set<Map.Entry<String, JsonValue>> targetEntries = target.entrySet();
        for (Map.Entry<String, JsonValue> targetEntry : targetEntries) {
            if (!source.containsKey(targetEntry.getKey())) {
                patchBuilder.add(basePath + targetEntry.getKey(), targetEntry.getValue());
            }
        }

    }


    private static boolean isJsonObject(JsonValue jsonValue) {
        return jsonValue instanceof JsonObject;
    }
}
