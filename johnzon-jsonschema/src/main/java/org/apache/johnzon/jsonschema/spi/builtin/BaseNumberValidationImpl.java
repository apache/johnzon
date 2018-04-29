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

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult;

abstract class BaseNumberValidationImpl implements Function<JsonValue, Stream<ValidationResult.ValidationError>> {
    protected final String pointer;
    protected final Function<JsonObject, JsonValue> extractor;
    protected final double bound;
    private final JsonValue.ValueType validType;

    BaseNumberValidationImpl(final String pointer, final Function<JsonObject, JsonValue> extractor, final double bound,
                             final JsonValue.ValueType validType) {
        this.bound = bound;
        this.pointer = pointer;
        this.extractor = extractor;
        this.validType = validType;
    }

    @Override
    public Stream<ValidationResult.ValidationError> apply(final JsonValue obj) {
        if (obj == null || obj == JsonValue.NULL) {
            return Stream.empty();
        }
        final JsonValue value = extractor.apply(obj.asJsonObject());
        if (value == null || value.getValueType() != validType) {
            return Stream.empty();
        }
        final double val = toNumber(value);
        if (val <= 0) {
            return toError(val);
        }
        if (isValid(val)) {
            return Stream.empty();
        }
        return toError(val);
    }

    protected double toNumber(final JsonValue value) {
        return JsonNumber.class.cast(value).doubleValue();
    }

    protected abstract boolean isValid(double val);

    protected abstract Stream<ValidationResult.ValidationError> toError(double val);
}
