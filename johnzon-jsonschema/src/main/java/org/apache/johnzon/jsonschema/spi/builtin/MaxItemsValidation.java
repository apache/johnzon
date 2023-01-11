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

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;

public class MaxItemsValidation implements ValidationExtension {
    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>> create(final ValidationContext model) {
        return Optional.ofNullable(model.getSchema().get("maxItems"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.NUMBER)
                .map(it -> JsonNumber.class.cast(it).intValue())
                .filter(it -> it >= 0)
                .map(max -> new Impl(model.toPointer(), model.getValueProvider(), max));
    }

    private static class Impl extends BaseValidation {
        private final int bound;

        private Impl(final String pointer,
                     final Function<JsonValue, JsonValue> extractor,
                     final int bound) {
            super(pointer, extractor, JsonValue.ValueType.ARRAY);
            this.bound = bound;
        }

        @Override
        protected Stream<ValidationResult.ValidationError> onArray(final JsonArray array) {
            if (array.size() > bound) {
                return Stream.of(new ValidationResult.ValidationError(pointer, "Too much items in the array (> " + bound + ")"));
            }
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "MaxItems{" +
                    "max=" + bound +
                    ", pointer='" + pointer + '\'' +
                    '}';
        }
    }
}
