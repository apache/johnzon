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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        assertNotNull(jsonPatch);
        JsonArray patchOperations = jsonPatch.toJsonArray();
        assertNotNull(patchOperations);
        assertEquals(1, patchOperations.size());
        containsOperation(patchOperations, JsonPatch.Operation.ADD, "/b", Json.createValue("xb"));
    }

    @Test
    public void testAddDiffNewObject() {

        JsonObject target = Json.createObjectBuilder()
                .add("a", Json.createObjectBuilder()
                        .add("aa", "value")
                        .add("ab", "another"))
                .build();

        JsonPatch patch = Json.createDiff(JsonValue.EMPTY_JSON_OBJECT, target);
        assertNotNull(patch);

        JsonArray operations = patch.toJsonArray();
        assertEquals(1, operations.size());

        containsOperation(operations, JsonPatch.Operation.ADD, "/a", target.get("a"));

        // now try to apply that patch.
        JsonObject patched = patch.apply(JsonValue.EMPTY_JSON_OBJECT);
        Assert.assertEquals(target, patched);
    }

    //X TODO
    @Test
    @Ignore("TODO define how escaping must get handled")
    public void testAddDiffNewObjectWithEscaping() {

        JsonObject target = Json.createObjectBuilder()
                .add("a~/", Json.createObjectBuilder()
                        .add("esc/aped", "value")
                        .add("tilde", "another"))
                .build();

        JsonPatch patch = Json.createDiff(JsonValue.EMPTY_JSON_OBJECT, target);
        assertNotNull(patch);

        JsonArray operations = patch.toJsonArray();
        assertEquals(1, operations.size());

        containsOperation(operations, JsonPatch.Operation.ADD, "/a~/", target.get("a"));

        // now try to apply that patch.
        JsonObject patched = patch.apply(JsonValue.EMPTY_JSON_OBJECT);
        Assert.assertEquals(target, patched);
    }

    @Test
    public void testAddDiffInNestedObject() {

        JsonObject source = Json.createObjectBuilder()
                .add("a", Json.createObjectBuilder()
                        .add("aa", "value"))
                .build();

        JsonObject target = Json.createObjectBuilder()
                .add("a", Json.createObjectBuilder()
                        .add("aa", "value")
                        .add("bb", "another value"))
                .build();

        JsonPatch patch = Json.createDiff(source, target);
        assertNotNull(patch);

        JsonArray operations = patch.toJsonArray();
        assertEquals(1, operations.size());

        containsOperation(operations, JsonPatch.Operation.ADD, "/a/bb", Json.createValue("another value"));
    }

    @Test
    public void testRemoveDiffObject() {

        JsonObject source = Json.createObjectBuilder()
                .add("a", "value")
                .build();

        JsonPatch patch = Json.createDiff(source, JsonValue.EMPTY_JSON_OBJECT);
        assertNotNull(patch);

        JsonArray operations = patch.toJsonArray();
        assertEquals(1, operations.size());

        containsOperation(operations, JsonPatch.Operation.REMOVE, "/a");
    }

    @Test
    public void testRemoveDiffNestedObject() {

        JsonObject source = Json.createObjectBuilder()
                .add("a", "value")
                .add("nested", Json.createObjectBuilder()
                        .add("1", 1)
                        .add("2", 2))
                .build();

        {
            JsonPatch patch = Json.createDiff(source, JsonValue.EMPTY_JSON_OBJECT);
            assertNotNull(patch);

            JsonArray operations = patch.toJsonArray();
            assertEquals(2, operations.size());

            containsOperation(operations, JsonPatch.Operation.REMOVE, "/a");
            containsOperation(operations, JsonPatch.Operation.REMOVE, "/nested");
        }

        {
            JsonObject target = Json.createObjectBuilder()
                    .add("a", "value")
                    .add("nested", Json.createObjectBuilder()
                            .add("1", 1))
                    .build();

            JsonPatch patch = Json.createDiff(source, target);
            assertNotNull(patch);

            JsonArray operations = patch.toJsonArray();
            assertEquals(1, operations.size());

            containsOperation(operations, JsonPatch.Operation.REMOVE, "/nested/2");
        }
    }

    @Test
    public void testDiffEqualObjects() {

        JsonObject source = Json.createObjectBuilder()
                .add("a", "value")
                .build();

        JsonObject target = Json.createObjectBuilder()
                .add("a", "value")
                .build();

        JsonPatch patch = Json.createDiff(source, target);
        assertNotNull(patch);
        assertEquals(0, patch.toJsonArray().size());
    }


    //X TODO arrays...
    //X TODO test add/remove JsonArray
    //X TODO test add object to JsonArray
    //X TODO test remove object to JsonArray



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
        assertNotNull(jsonPatch);
        JsonArray patchOperations = jsonPatch.toJsonArray();
        assertNotNull(patchOperations);
        assertEquals(4, patchOperations.size());
        containsOperation(patchOperations, JsonPatch.Operation.REMOVE, "/b");
        containsOperation(patchOperations, JsonPatch.Operation.ADD, "/c/d2", Json.createValue("xd2"));
        containsOperation(patchOperations, JsonPatch.Operation.REMOVE, "/e/2");
        containsOperation(patchOperations, JsonPatch.Operation.ADD, "/f", Json.createValue("xe"));
    }

    private void containsOperation(JsonArray patchOperations,
                                   JsonPatch.Operation patchOperation,
                                   String jsonPointer) {

        containsOperation(patchOperations, patchOperation, jsonPointer, null);
    }

    private void containsOperation(JsonArray patchOperations,
                                   JsonPatch.Operation patchOperation,
                                   String jsonPointer,
                                   JsonValue value) {

        for (JsonValue operation : patchOperations) {
            if (operation instanceof JsonObject &&
                    patchOperation.operationName().equalsIgnoreCase(((JsonObject) operation).getString("op")) &&
                    ((JsonObject) operation).getString("path").equals(jsonPointer)) {

                if (value != null) {
                    assertEquals(value, ((JsonObject) operation).get("value"));
                }

                return;
            }
        }
        Assert.fail("patchOperations does not contain " + patchOperation + " " + jsonPointer);
    }


}
