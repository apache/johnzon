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

import javax.json.JsonMergePatch;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * Creates a JsonMergePatch as diff between two JsonValues
 */
class JsonMergePatchDiff extends DiffBase {
    private final JsonValue source;
    private final JsonValue target;

    public JsonMergePatchDiff(JsonValue source, JsonValue target) {
        this.source = source;
        this.target = target;
    }

    public JsonMergePatch calculateDiff() {
        return new JsonMergePatchImpl(diff(source, target));
    }

    private JsonValue diff(JsonValue source, JsonValue target) {
        JsonObjectBuilder builder = new JsonObjectBuilderImpl();

        if (isJsonObject(source) && isJsonObject(target)) {
            JsonObject srcObj = source.asJsonObject();
            JsonObject targetObj = target.asJsonObject();
            for (Map.Entry<String, JsonValue> sourceEntry : srcObj.entrySet()) {
                String attributeName = sourceEntry.getKey();
                if (targetObj.containsKey(attributeName)) {
                    // compare the attribute values
                    JsonValue attribDiff = diff(sourceEntry.getValue(), targetObj.get(attributeName));
                    if (!JsonValue.EMPTY_JSON_OBJECT.equals(attribDiff)) {
                        builder.add(attributeName, attribDiff);
                    }
                } else {
                    // attribute got removed
                    builder.add(attributeName, JsonValue.NULL);
                }
            }

            for (Map.Entry<String, JsonValue> targetEntry : targetObj.entrySet()) {
                String attributeName = targetEntry.getKey();
                if (!srcObj.containsKey(attributeName)) {
                    // add operation
                    builder.add(attributeName, targetEntry.getValue());
                }
            }

            return builder.build();
        } else if (source.equals(target)) {
            // if the two objects are identical, then return an empty patch
            return JsonValue.EMPTY_JSON_OBJECT;
        } else {
            // as defined in the RFC anything else than comparing JsonObjects will result
            // in completely replacing the source with the target
            // That means our target is the patch.
            return target;
        }
    }
}
