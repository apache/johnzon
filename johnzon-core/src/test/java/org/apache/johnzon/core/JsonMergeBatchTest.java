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
import org.junit.Ignore;
import org.junit.Test;

public class JsonMergeBatchTest {

    @Test
    @Ignore(value = "work in progress, TODO finish")
    public void testSimpleMergePatch() {
        // {"a":"xa","b","xb"}
        String source = "{\"a\":\"xa\",\"b\",\"xb\"}";

        // {"b":"bNew","c":"xc"}
        // the result after this patch gets applied to source should be {"a":"xa","b","bNew","c":"xc"}
        String patch = "{\"b\":\"bNew\",\"c\":\"xc\"}";

        //X TODO Json.createMergePatch();

        JsonMergePatch jsonMergePatch = Json.createMergePatch(Json.createReader(new StringReader(patch)).readObject());

        JsonObject jsonSource = Json.createReader(new StringReader(source)).readObject();

        JsonObject jsonTarget = jsonMergePatch.apply(jsonSource).asJsonObject();
        Assert.assertNotNull(jsonTarget);
        Assert.assertEquals(3, jsonTarget.entrySet().size());
        Assert.assertEquals("xa", jsonTarget.getString("a"));
        Assert.assertEquals("bNew", jsonTarget.getString("b"));
        Assert.assertEquals("xc", jsonTarget.getString("c"));
    }
}
