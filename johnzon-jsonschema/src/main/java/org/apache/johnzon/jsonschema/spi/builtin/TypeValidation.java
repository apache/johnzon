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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;

public class TypeValidation implements ValidationExtension {
    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>> create(final ValidationContext model) {
        final JsonString value = model.getSchema().getJsonString("type");
        if (value == null) {
            return Optional.empty();
        }
        switch (JsonString.class.cast(value).getString()) {
            case "string":
                return Optional.of(new Impl(model.toPointer(), model.getValueProvider(), JsonValue.ValueType.STRING));
            case "number":
                return Optional.of(new Impl(model.toPointer(), model.getValueProvider(), JsonValue.ValueType.NUMBER));
            case "array":
                return Optional.of(new Impl(model.toPointer(), model.getValueProvider(), JsonValue.ValueType.ARRAY));
            case "boolean":
                return Optional.of(new Impl(model.toPointer(), model.getValueProvider(), JsonValue.ValueType.FALSE, JsonValue.ValueType.TRUE));
            case "object":
            default:
                return Optional.of(new Impl(model.toPointer(), model.getValueProvider(), JsonValue.ValueType.OBJECT));
        }
    }

    private static class Impl extends BaseValidation {
        private final Collection<JsonValue.ValueType> types;

        private Impl(final String pointer, final Function<JsonValue, JsonValue> extractor, final JsonValue.ValueType... types) {
            super(pointer, extractor, types[0] /*ignored anyway*/);
            this.types = Stream.concat(Stream.of(types), Stream.of(JsonValue.ValueType.NULL))
                    .distinct()
                    .sorted(comparing(JsonValue.ValueType::name))
                    .collect(toList());
        }

        @Override
        public Stream<ValidationResult.ValidationError> apply(final JsonValue root) {
            if (isNull(root)) {
                return Stream.empty();
            }
            final JsonValue value = extractor.apply(root);
            if (value == null || types.contains(value.getValueType())) {
                return Stream.empty();
            }
            return Stream.of(new ValidationResult.ValidationError(
                    pointer,
                    "Expected " + types + " but got " + value.getValueType()));
        }

        @Override
        public String toString() {
            return "Type{" +
                    "type=" + types +
                    ", pointer='" + pointer + '\'' +
                    '}';
        }
    }
}
