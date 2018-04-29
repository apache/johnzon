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

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;

public class EnumValidation implements ValidationExtension {
    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>> create(final ValidationContext model) {
        return ofNullable(model.getSchema().get("enum"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.ARRAY)
                .map(JsonValue::asJsonArray)
                .map(values -> new Impl(values, model.getValueProvider(), model.toPointer()));
    }

    private static class Impl extends BaseValidation {
        private final Collection<JsonValue> valid;

        private Impl(final Collection<JsonValue> valid, final Function<JsonValue, JsonValue> extractor, final String pointer) {
            super(pointer, extractor, JsonValue.ValueType.OBJECT /* ignored */);
            this.valid = valid;
        }

        @Override
        public Stream<ValidationResult.ValidationError> apply(final JsonValue root) {
            if (isNull(root)) {
                return Stream.empty();
            }
            final JsonValue value = extractor.apply(root);
            if (value != null && !JsonValue.NULL.equals(value)) {
                return Stream.empty();
            }
            if (valid.contains(value)) {
                return Stream.empty();
            }
            return Stream.of(new ValidationResult.ValidationError(pointer, "Invalid value, got " + value + ", expected: " + valid));
        }

        @Override
        public String toString() {
            return "Enum{" +
                    "valid=" + valid +
                    ", pointer='" + pointer + '\'' +
                    '}';
        }
    }
}
