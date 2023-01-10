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
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

public class ConstructorVisibilityTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    @Test(expected = JsonbException.class)
    public void packageConstructor() {
        jsonb.fromJson("{}", PackageCons.class);
    }

    @Test
    public void instantiablePackageConstructor() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .setProperty("johnzon.supportsPrivateAccess", "true"))) {
            assertEquals("ok", jsonb.fromJson("{\"foo\":\"ok\"}", InstantiablePackageCons.class).value);
        }
    }

    public static class PackageCons {
        public String value;

        PackageCons() {
            // no-op
        }
    }

    public static class InstantiablePackageCons {
        public String value;

        @JsonbCreator
        InstantiablePackageCons(@JsonbProperty("foo") final String foo) {
            this.value = foo;
        }
    }
}
