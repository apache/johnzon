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

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.JsonValue;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class JsonPatchDiffTest {

    @Test
    public void testAddDiff() {
        // {"a":"xa"}
        String jsonA = "{\"a\":\"xa\"}";

        // {"a":"xa","b":"xb"}
        String jsonB = "{\"a\":\"xa\",\"b\":\"xb\"}";

        // this results in 1 diff operations:
        // adding "b"
        JsonPatch jsonPatch = Json.createDiff(Json.createReader(new StringReader(jsonA)).readObject(),
                Json.createReader(new StringReader(jsonB)).readObject());
        Assert.assertNotNull(jsonPatch);
        JsonArray patchOperations = jsonPatch.toJsonArray();
        Assert.assertNotNull(patchOperations);
        Assert.assertEquals(1, patchOperations.size());
        containsOperation(patchOperations, JsonPatch.Operation.ADD, "/b", "xb");
    }

    @Test
    @Ignore //X TODO reinhard take over ;)
    public void testComplexDiff() {
        // {"a":"xa","b":2,"c":{"d":"xd"},"e":[1,2,3]}
        String jsonA = "{\"a\":\"xa\",\"b\":2,\"c\":{\"d\":\"xd\"},\"e\":[1,2,3]}";

        // {"a":"xa","c":{"d":"xd", "d2":"xd2"},"e":[1,3],"f":"xe"}
        String jsonB = "{\"a\":\"xa\",\"c\":{\"d\":\"xd\", \"d2\":\"xd2\"},\"e\":[1,3],\"f\":\"xe\"}";

        // this results in 4 diff operations:
        // removing b, adding d2, removing 2 from e, adding f
        JsonPatch jsonPatch = Json.createDiff(Json.createReader(new StringReader(jsonA)).readObject(),
                Json.createReader(new StringReader(jsonB)).readObject());
        Assert.assertNotNull(jsonPatch);
        JsonArray patchOperations = jsonPatch.toJsonArray();
        Assert.assertNotNull(patchOperations);
        Assert.assertEquals(4, patchOperations.size());
        containsOperation(patchOperations, JsonPatch.Operation.REMOVE, "/b", null);
        containsOperation(patchOperations, JsonPatch.Operation.ADD, "/c/d2", "xd2");
        containsOperation(patchOperations, JsonPatch.Operation.REMOVE, "/e/2", null);
        containsOperation(patchOperations, JsonPatch.Operation.ADD, "/f", "xe");
    }

    private void containsOperation(JsonArray patchOperations, JsonPatch.Operation patchOperation,
                                   String jsonPointer, String value) {
        for (JsonValue operation : patchOperations) {
            if (operation instanceof JsonObject &&
                    patchOperation.operationName().equalsIgnoreCase(((JsonObject) operation).getString("op"))) {
                Assert.assertEquals(jsonPointer, ((JsonObject) operation).getString("path"));

                if (value != null) {
                    Assert.assertEquals(value, ((JsonObject) operation).getString("value"));
                }

                return;
            }
        }
        Assert.fail("patchOperations does not contain " + patchOperation + " " + jsonPointer);
    }


}
