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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringReader;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

public class JsonPatchTest {

    @Test
    public void testAddObjectMember() {

        JsonObject object = Json.createReader(new StringReader("{ \"foo\": \"bar\" }"))
                                .readObject();


        JsonPatchImpl patch = new JsonPatchImpl(new JsonPatchImpl.PatchValue(JsonPatchOperation.ADD,
                                                                           Json.createJsonPointer("/baz"),
                                                                           null, // no from
                                                                           new JsonStringImpl("qux")));

        JsonObject patched = patch.apply(object);
        assertNotNull(patched);
        assertEquals("bar", patched.getString("foo"));
        assertEquals("qux", patched.getString("baz"));

        StringWriter writer = new StringWriter();
        Json.createWriter(writer).write(patched);

        assertEquals("{\"foo\":\"bar\",\"baz\":\"qux\"}", writer.toString());
    }

}
