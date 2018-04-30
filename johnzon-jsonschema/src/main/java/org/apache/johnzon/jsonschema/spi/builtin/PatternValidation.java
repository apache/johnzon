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
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;

public class PatternValidation implements ValidationExtension {
    private final Function<String, Predicate<CharSequence>> predicateFactory;

    public PatternValidation(final Function<String, Predicate<CharSequence>> predicateFactory) {
        this.predicateFactory = predicateFactory;
    }

    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>> create(final ValidationContext model) {
        if (model.getSchema().getString("type", "object").equals("string")) {
            return Optional.ofNullable(model.getSchema().get("pattern"))
                    .filter(val -> val.getValueType() == JsonValue.ValueType.STRING)
                    .map(pattern -> new Impl(model.toPointer(), model.getValueProvider(), predicateFactory.apply(JsonString.class.cast(pattern).getString())));
        }
        return Optional.empty();
    }

    private static class Impl extends BaseValidation {
        private final Predicate<CharSequence> matcher;

        private Impl(final String pointer, final Function<JsonValue, JsonValue> valueProvider,
                     final Predicate<CharSequence> matcher) {
            super(pointer, valueProvider, JsonValue.ValueType.STRING);
            this.matcher = matcher;
        }

        @Override
        public Stream<ValidationResult.ValidationError> onString(final JsonString value) {
            if (!matcher.test(value.getString())) {
                return Stream.of(new ValidationResult.ValidationError(pointer, value + " doesn't match " + matcher));
            }
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "Pattern{" +
                    "regex=" + matcher +
                    ", pointer='" + pointer + '\'' +
                    '}';
        }
    }
}
