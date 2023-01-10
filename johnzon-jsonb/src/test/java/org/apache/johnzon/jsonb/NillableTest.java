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

import static org.junit.Assert.assertEquals;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

import org.apache.johnzon.jsonb.model.NillableType;
import org.apache.johnzon.jsonb.model.nillable.NotNillablePropertyModel;
import org.apache.johnzon.jsonb.model.nillable.notnillable.StringHolder;
import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

public class NillableTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    @Test
    public void propertyWinsOverPackage() {
        assertEquals("{}", jsonb.toJson(new NotNillablePropertyModel()));
    }

    @Test
    public void globalNillableConfigInNonNullablePackage() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withNullValues(true))) {
            assertEquals("{}", jsonb.toJson(new StringHolder()));
        }
    }

    @Test
    public void type() {
        assertEquals("{\"value\":null}", jsonb.toJson(new NillableType()));
    }
}
