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
import javax.json.JsonMergePatch;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonMergeBatchDiffTest {

    @Test
    public void testAddDiff() {
        // {"a":"xa"}
        String jsonA = "{\"a\":\"xa\"}";

        // {"a":"xa","b":"xb"}
        String jsonB = "{\"a\":\"xa\",\"b\":\"xb\"}";

        // this results in 1 diff operations:
        // adding "b"
        JsonMergePatch jsonMergePatch = Json.createMergeDiff(Json.createReader(new StringReader(jsonA)).readObject(),
                                                             Json.createReader(new StringReader(jsonB)).readObject());
        assertNotNull(jsonMergePatch);
        JsonObject patchJson = jsonMergePatch.toJsonValue().asJsonObject();
        assertNotNull(patchJson);
        assertEquals(1, patchJson.entrySet().size());
        Assert.assertEquals("xb", patchJson.getString("b"));
    }

}
