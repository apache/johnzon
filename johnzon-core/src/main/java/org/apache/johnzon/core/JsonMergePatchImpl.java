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
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class JsonMergePatchImpl implements JsonMergePatch {
    private final JsonValue patch;

    public JsonMergePatchImpl(JsonValue patch) {
        this.patch = patch;
    }

    @Override
    public JsonValue apply(JsonValue valueToApplyPatchOn) {
        return applyPatch(valueToApplyPatchOn, patch);
    }

    private JsonValue applyPatch(JsonValue valueToApplyPatchOn, JsonValue patch) {
        if (patch == null) {
            return JsonValue.NULL;
        } else if (patch instanceof JsonObject && valueToApplyPatchOn instanceof JsonObject) {
            // we only apply an actual patch IF both sides are a JsonObject
            JsonObject patchObject = patch.asJsonObject();

            return applyJsonObjectPatch(valueToApplyPatchOn.asJsonObject(), patchObject);
        } else {
            // this must be a native JsonValue or JsonObject, so we just replace the
            // the whole original valueToApplyPatchOn with the new jsonValue
            return patch;
        }
    }

    private JsonValue applyJsonObjectPatch(JsonObject jsonObject, JsonObject patch) {
        JsonObjectBuilder builder = new JsonObjectBuilderImpl(jsonObject);

        for (Map.Entry<String, JsonValue> patchAttrib : patch.entrySet()) {
            String attribName = patchAttrib.getKey();
            if (patchAttrib.getValue().equals(JsonValue.NULL)) {
                builder.remove(attribName);
            } else {
                JsonValue originalAttrib = jsonObject.get(attribName);
                if (originalAttrib == null) {
                    builder.add(attribName, patchAttrib.getValue());
                } else {
                    builder.add(attribName, applyPatch(originalAttrib, patchAttrib.getValue()));
                }
            }
        }
        return builder.build();
    }

    @Override
    public JsonValue toJsonValue() {
        return patch;
    }
}
