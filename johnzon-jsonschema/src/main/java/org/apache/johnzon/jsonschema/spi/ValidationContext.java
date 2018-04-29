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
package org.apache.johnzon.jsonschema.spi;

import static java.util.stream.Collectors.joining;

import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

public class ValidationContext {
    private final String[] path;
    private final JsonObject schema;
    private final Function<JsonValue, JsonValue> valueProvider;

    public ValidationContext(final String[] path, final JsonObject schema, final Function<JsonValue, JsonValue> valueProvider) {
        this.path = path;
        this.schema = schema;
        this.valueProvider = valueProvider;
    }

    public Function<JsonValue, JsonValue> getValueProvider() {
        return valueProvider;
    }

    public String[] getPath() {
        return path;
    }

    public JsonObject getSchema() {
        return schema;
    }

    public String toPointer() {
        return Stream.of(path).collect(joining("/", "/", ""));
    }
}
