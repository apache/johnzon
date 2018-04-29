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

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;

public class RequiredValidation implements ValidationExtension {
    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>> create(final ValidationContext model) {
        return ofNullable(model.getSchema().get("required"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.ARRAY)
                .map(JsonValue::asJsonArray)
                .filter(arr -> arr.stream().allMatch(it -> it.getValueType() == JsonValue.ValueType.STRING))
                .map(arr -> arr.stream().map(it -> JsonString.class.cast(it).getString()).collect(toSet()))
                .map(required -> new Impl(required, model.getValueProvider(), model.toPointer()));
    }

    private static class Impl extends BaseValidation {
        private final Collection<String> required;

        private Impl(final Collection<String> required, final Function<JsonValue, JsonValue> extractor, final String pointer) {
            super(pointer, extractor, JsonValue.ValueType.OBJECT);
            this.required = required;
        }

        @Override
        public Stream<ValidationResult.ValidationError> onObject(final JsonObject obj) {
            if (obj == null || obj == JsonValue.NULL) {
                return toErrors(required.stream());
            }
            return toErrors(required.stream().filter(name -> isNull(obj.get(name))));
        }

        private Stream<ValidationResult.ValidationError> toErrors(final Stream<String> fields) {
            return fields.map(name -> new ValidationResult.ValidationError(pointer, name + " is required and is not present"));
        }

        @Override
        public String toString() {
            return "Required{" +
                    "required=" + required +
                    ", pointer='" + pointer + '\'' +
                    '}';
        }
    }
}
