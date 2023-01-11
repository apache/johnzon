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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonPatch;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

/**
 * Create a diff from a source and target JsonStructure
 */
class JsonPatchDiff extends DiffBase {

    private final JsonProvider provider;
    private final JsonStructure source;
    private final JsonStructure target;

    JsonPatchDiff(final JsonProvider provider, final JsonStructure source, final JsonStructure target) {
        this.provider = provider;
        this.source = source;
        this.target = target;
    }

    JsonPatch calculateDiff() {
        JsonPatchBuilder patchBuilder = new JsonPatchBuilderImpl(provider);

        diff(patchBuilder, "", source, target);

        return patchBuilder.build();
    }

    private void diff(JsonPatchBuilder patchBuilder, String basePath, JsonValue source, JsonValue target) {
        if (isJsonObject(source) && isJsonObject(target)) {
            diffJsonObjects(patchBuilder, basePath + "/", (JsonObject) source, (JsonObject) target);
        } else if (isJsonArray(source) && isJsonArray(target)) {
            diffJsonArray(patchBuilder, basePath + "/", (JsonArray) source, (JsonArray) target);
        } else if (!source.equals(target)){
            patchBuilder.replace(basePath, target);
        }
    }

    private void diffJsonArray(JsonPatchBuilder patchBuilder, String basePath, JsonArray source, JsonArray target) {
        for (int i = 0; i < source.size(); i++) {
            final JsonValue sourceValue = source.get(i);
            if (target.size() <= i) {
                patchBuilder.remove(basePath + i);
                continue;
            }
            diff(patchBuilder, basePath + i, sourceValue, target.get(i));
        }

        if (target.size() > source.size()) {
            for (int i = source.size(); i < target.size(); i++) {
                patchBuilder.add(basePath + i, target.get(i));
            }
        }

    }

    private void diffJsonObjects(JsonPatchBuilder patchBuilder, String basePath, JsonObject source, JsonObject target) {
        for (final Map.Entry<String, JsonValue> sourceEntry : source.entrySet()) {
            String attributeName = sourceEntry.getKey();

            if (target.containsKey(attributeName)) {
                diff(patchBuilder, basePath + JsonPointerUtil.encode(attributeName), sourceEntry.getValue(), target.get(attributeName));
            } else {
                // the value got removed
                patchBuilder.remove(basePath + JsonPointerUtil.encode(attributeName));
            }
        }

        for (final Map.Entry<String, JsonValue> targetEntry : target.entrySet()) {
            if (!source.containsKey(targetEntry.getKey())) {
                patchBuilder.add(basePath + JsonPointerUtil.encode(targetEntry.getKey()), targetEntry.getValue());
            }
        }
    }
}
