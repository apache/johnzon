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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;

public class UniqueItemsValidation implements ValidationExtension {
    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>> create(final ValidationContext model) {
        return Optional.ofNullable(model.getSchema().get("uniqueItems"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.TRUE)
                .map(max -> new Impl(model.toPointer(), model.getValueProvider()));
    }

    private static class Impl extends BaseValidation {
        private Impl(final String pointer,
                     final Function<JsonValue, JsonValue> extractor) {
            super(pointer, extractor, JsonValue.ValueType.ARRAY);
        }

        @Override
        protected Stream<ValidationResult.ValidationError> onArray(final JsonArray array) {
            final Collection<JsonValue> uniques = new HashSet<>(array);
            if (array.size() != uniques.size()) {
                final Collection<JsonValue> duplicated = new ArrayList<>(array);
                duplicated.removeAll(uniques);
                return Stream.of(new ValidationResult.ValidationError(pointer, "duplicated items: " + duplicated));
            }
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "UniqueItems{" +
                    "pointer='" + pointer + '\'' +
                    '}';
        }
    }
}
