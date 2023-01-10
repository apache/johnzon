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
package org.apache.johnzon.jsonschema.spi.builtin;

import org.apache.johnzon.jsonschema.ValidationResult;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class EnumValidationTest {
    @Test
    public void nullIsValidIfNullable() {
        final Function<JsonValue, Stream<ValidationResult.ValidationError>> validator = new EnumValidation()
                .create(new ValidationContext(
                        new String[]{"/test"},
                        Json.createObjectBuilder()
                                .add("type", "string")
                                .add("nullable", true)
                                .add("enum", Json.createArrayBuilder()
                                        .add("A")
                                        .add("B"))
                                .build(),
                        v -> v.asJsonObject().get("test")))
                .orElseThrow(IllegalStateException::new);
        assertEquals(0, validator
                .apply(Json.createObjectBuilder().build())
                .count());
        assertEquals(0, validator
                .apply(Json.createObjectBuilder().addNull("test").build())
                .count());
        assertEquals(0, validator
                .apply(Json.createObjectBuilder().add("test", "A").build())
                .count());
        assertEquals(1, validator
                .apply(Json.createObjectBuilder().add("test", "C").build())
                .count());
    }
}
