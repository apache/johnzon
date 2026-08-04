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

import java.util.stream.Stream;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Reads the {@code type} keyword which is either a plain string or - the idiomatic way to declare
 * a nullable value - an array of strings, ex: {@code "type": ["string", "null"]}.
 * Reading it with {@code schema.getString("type", ...)} silently falls back on the default value
 * for the array form so the sibling constraints (pattern, bounds, lengths) must not rely on it.
 */
public final class SchemaType {
    private SchemaType() {
        // no-op
    }

    public static boolean isString(final JsonObject schema) {
        return declaredTypes(schema).anyMatch("string"::equals);
    }

    /**
     * @return true when the schema declares a numeric type, {@code integer} included since the
     * numeric keywords (maximum, multipleOf, ...) apply to any JSON number.
     */
    public static boolean isNumber(final JsonObject schema) {
        return declaredTypes(schema).anyMatch(it -> "number".equals(it) || "integer".equals(it));
    }

    public static boolean isInteger(final JsonObject schema) {
        return declaredTypes(schema).anyMatch("integer"::equals);
    }

    public static boolean isNullable(final JsonObject schema) {
        return declaredTypes(schema).anyMatch("null"::equals);
    }

    /**
     * @return the declared {@code type} values, handling both the plain string and the array forms.
     */
    public static Stream<String> declaredTypes(final JsonObject schema) {
        final JsonValue type = schema.get("type");
        if (JsonString.class.isInstance(type)) {
            return Stream.of(JsonString.class.cast(type).getString());
        }
        if (type != null && type.getValueType() == JsonValue.ValueType.ARRAY) {
            return type.asJsonArray().stream()
                    .filter(JsonString.class::isInstance)
                    .map(it -> JsonString.class.cast(it).getString());
        }
        return Stream.empty();
    }
}
