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
package org.apache.johnzon.jsonb;

import org.junit.Test;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.JsonbException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FailOnUnknownPropertiesTest {
    @Test
    public void failOnUnknownProperties() {
        final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty("johnzon.fail-on-unknown-properties", true));

        // valid
        assertEquals("ok", jsonb.fromJson("{\"known\":\"ok\"}", Model.class).known);

        try { // invalid
            assertEquals("ok", jsonb.fromJson("{\"known\":\"ok\",\"unknown\":\"whatever\"}", Model.class).known);
            fail();
        } catch (final JsonbException jsone) {
            assertEquals("(fail on unknown properties): [unknown]", jsone.getMessage());
        }
        try { // invalid but a missing key from the model
            assertEquals("ok", jsonb.fromJson("{\"unknown\":\"whatever\"}", Model.class).known);
            fail();
        } catch (final JsonbException jsone) {
            assertEquals("(fail on unknown properties): [unknown]", jsone.getMessage());
        }
    }

    public static class Model {
        public String known;
    }
}
