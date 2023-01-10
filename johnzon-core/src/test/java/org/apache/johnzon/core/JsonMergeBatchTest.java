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

import jakarta.json.Json;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

import org.junit.Assert;
import org.junit.Test;

public class JsonMergeBatchTest {

    private JsonProvider json = JsonProvider.provider();

    @Test
    public void testApplyValueOnObject() {
        // {"a":"xa","b":"xb"}
        JsonObject source = jsonObjectFrom("{\"a\":\"xa\",\"b\":\"xb\"}");

        // {"b":"bNew","c":"xc"}
        // the result after this patch gets applied to source should be {"a":"xa","b","bNew","c":"xc"}
        JsonValue patch = json.createValue(4711);

        JsonMergePatch jsonMergePatch = Json.createMergePatch(patch);
        JsonValue patchedValue = jsonMergePatch.apply(source);
        Assert.assertEquals(4711, ((JsonNumber) patchedValue).intValue());
    }

    @Test
    public void testApplyObjectOnValue() {
        // {"a":"xa","b":"xb"}
        JsonValue source = json.createValue(4711);

        // {"b":"bNew","c":"xc"}
        // the result after this patch gets applied to source should be {"a":"xa","b","bNew","c":"xc"}
        JsonObject patch = jsonObjectFrom("{\"a\":\"xa\",\"b\":\"xb\"}");

        JsonMergePatch jsonMergePatch = Json.createMergePatch(patch);
        JsonValue patchedValue = jsonMergePatch.apply(source);
        Assert.assertTrue(patchedValue instanceof JsonObject);
        Assert.assertEquals("xa", patchedValue.asJsonObject().getString("a"));
        Assert.assertEquals("xb", patchedValue.asJsonObject().getString("b"));
    }


    @Test
    public void testSimpleJsonObjectMergePatch() {
        // {"a":"xa","b":"xb"}
        JsonObject source = jsonObjectFrom("{\"a\":\"xa\",\"b\":\"xb\"}");

        // {"b":"bNew","c":"xc"}
        // the result after this patch gets applied to source should be {"a":"xa","b","bNew","c":"xc"}
        JsonObject patch = jsonObjectFrom("{\"b\":\"bNew\",\"c\":\"xc\"}");

        JsonMergePatch jsonMergePatch = Json.createMergePatch(patch);


        JsonObject jsonTarget = jsonMergePatch.apply(source).asJsonObject();
        Assert.assertNotNull(jsonTarget);
        Assert.assertEquals(3, jsonTarget.entrySet().size());
        Assert.assertEquals("xa", jsonTarget.getString("a"));
        Assert.assertEquals("bNew", jsonTarget.getString("b"));
        Assert.assertEquals("xc", jsonTarget.getString("c"));
    }


    private JsonObject jsonObjectFrom(String val) {
        return json.createReader(new StringReader(val)).readObject();
    }
}
