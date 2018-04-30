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

import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult;

abstract class BaseValidation implements Function<JsonValue, Stream<ValidationResult.ValidationError>> {
    protected final String pointer;
    protected final Function<JsonValue, JsonValue> extractor;
    private final JsonValue.ValueType validType;
    private final boolean rootCanBeNull;

    BaseValidation(final String pointer, final Function<JsonValue, JsonValue> extractor, final JsonValue.ValueType validType) {
        this.pointer = pointer;
        this.extractor = extractor != null ? extractor : v -> v;
        this.rootCanBeNull = extractor != null;
        this.validType = validType;
    }

    @Override
    public Stream<ValidationResult.ValidationError> apply(final JsonValue obj) {
        if (isNull(obj) && rootCanBeNull) {
            return Stream.empty();
        }

        final JsonValue value = extractor.apply(obj);
        if (value == null || JsonValue.ValueType.NULL == value.getValueType() || value.getValueType() != validType) {
            return Stream.empty();
        }

        switch (value.getValueType()) {
            case STRING:
                return onString(JsonString.class.cast(value));
            case TRUE:
            case FALSE:
                return onBoolean(JsonValue.TRUE.equals(value));
            case NUMBER:
                return onNumber(JsonNumber.class.cast(value));
            case OBJECT:
                return onObject(value.asJsonObject());
            case ARRAY:
                return onArray(value.asJsonArray());
            case NULL:
                return Stream.empty();
            default:
                throw new IllegalArgumentException("Unsupported value type: " + value);
        }
    }

    protected boolean isNull(final JsonValue obj) {
        return null == obj || obj.getValueType() == JsonValue.ValueType.NULL;
    }

    protected Stream<ValidationResult.ValidationError> onArray(final JsonArray array) {
        return Stream.empty();
    }

    protected Stream<ValidationResult.ValidationError> onObject(final JsonObject object) {
        return Stream.empty();
    }

    protected Stream<ValidationResult.ValidationError> onNumber(final JsonNumber number) {
        return Stream.empty();
    }

    protected Stream<ValidationResult.ValidationError> onBoolean(final boolean value) {
        return Stream.empty();
    }

    protected Stream<ValidationResult.ValidationError> onString(final JsonString cast) {
        return Stream.empty();
    }
}
