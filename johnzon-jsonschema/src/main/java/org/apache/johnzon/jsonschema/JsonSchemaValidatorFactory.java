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
package org.apache.johnzon.jsonschema;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;
import org.apache.johnzon.jsonschema.spi.builtin.EnumValidation;
import org.apache.johnzon.jsonschema.spi.builtin.ExclusiveMaximumValidation;
import org.apache.johnzon.jsonschema.spi.builtin.ExclusiveMinimumValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MaxLengthValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MaximumValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MinLengthValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MinimumValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MultipleOfValidation;
import org.apache.johnzon.jsonschema.spi.builtin.PatternValidation;
import org.apache.johnzon.jsonschema.spi.builtin.RequiredValidation;
import org.apache.johnzon.jsonschema.spi.builtin.TypeValidation;

public class JsonSchemaValidatorFactory implements AutoCloseable {
    private static final String[] ROOT_PATH = new String[0];
    private static final Function<JsonValue, Stream<ValidationResult.ValidationError>> NO_VALIDATION = new Function<JsonValue, Stream<ValidationResult.ValidationError>>() {
        @Override
        public Stream<ValidationResult.ValidationError> apply(JsonValue jsonValue) {
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "NoValidation";
        }
    };

    private final List<ValidationExtension> extensions = new ArrayList<>();

    public JsonSchemaValidatorFactory() {
        extensions.addAll(asList(
                new RequiredValidation(),
                new TypeValidation(),
                new EnumValidation(),
                new MultipleOfValidation(),
                new MaximumValidation(),
                new MinimumValidation(),
                new ExclusiveMaximumValidation(),
                new ExclusiveMinimumValidation(),
                new MaxLengthValidation(),
                new MinLengthValidation(),
                new PatternValidation()
                // todo: http://json-schema.org/latest/json-schema-validation.html#rfc.section.6.4 and following
        ));
        extensions.addAll(new ArrayList<>(StreamSupport.stream(ServiceLoader.load(ValidationExtension.class).spliterator(), false)
                .collect(toList())));
    }

    public JsonSchemaValidatorFactory appendExtensions(final ValidationExtension... extensions) {
        this.extensions.addAll(asList(extensions));
        return this;
    }

    public JsonSchemaValidatorFactory setExtensions(final ValidationExtension... extensions) {
        this.extensions.clear();
        return appendExtensions(extensions);
    }

    public JsonSchemaValidator newInstance(final JsonObject schema) {
        return new JsonSchemaValidator(buildValidator(ROOT_PATH, schema));
    }

    @Override
    public void close() {
        // no-op for now
    }

    private Function<JsonValue, Stream<ValidationResult.ValidationError>> buildValidator(final String[] path,
                                                                                         final JsonObject schema) {
        final List<Function<JsonValue, Stream<ValidationResult.ValidationError>>> directValidations = buildDirectValidations(path, schema).collect(toList());
        final Function<JsonValue, Stream<ValidationResult.ValidationError>> nestedValidations = buildNestedValidations(path, schema);
        return new ValidationsFunction(Stream.concat(directValidations.stream(), Stream.of(nestedValidations)).collect(toList()));
    }

    private Stream<Function<JsonValue, Stream<ValidationResult.ValidationError>>> buildDirectValidations(final String[] path, final JsonObject schema) {
        final ValidationContext model = new ValidationContext(path, schema);
        return extensions.stream().map(e -> e.create(model)).filter(Optional::isPresent).map(Optional::get);
    }

    private Function<JsonValue, Stream<ValidationResult.ValidationError>> buildNestedValidations(final String[] path, final JsonObject schema) {
        return ofNullable(schema.get("properties"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.OBJECT)
                .map(it -> it.asJsonObject().entrySet().stream()
                        .filter(obj -> obj.getValue().getValueType() == JsonValue.ValueType.OBJECT)
                        .map(obj -> {
                            final String[] fieldPath = Stream.concat(Stream.of(path), Stream.of(obj.getKey())).toArray(String[]::new);
                            return buildValidator(fieldPath, obj.getValue().asJsonObject());
                        })
                        .collect(toList()))
                .map(this::toFunction)
                .orElse(NO_VALIDATION);
    }

    private Function<JsonValue, Stream<ValidationResult.ValidationError>> toFunction(
            final List<Function<JsonValue, Stream<ValidationResult.ValidationError>>> validations) {
        return new ValidationsFunction(validations);
    }

    private static class ValidationsFunction implements Function<JsonValue, Stream<ValidationResult.ValidationError>> {
        private final List<Function<JsonValue, Stream<ValidationResult.ValidationError>>> delegates;

        private ValidationsFunction(final List<Function<JsonValue, Stream<ValidationResult.ValidationError>>> validations) {
            // unwrap when possible to simplify the stack and make toString readable (debug)
            this.delegates = validations.stream()
                    .flatMap(it -> ValidationsFunction.class.isInstance(it) ? ValidationsFunction.class.cast(it).delegates.stream() : Stream.of(it))
                    .filter(it -> it != NO_VALIDATION)
                    .collect(toList());
        }

        @Override
        public Stream<ValidationResult.ValidationError> apply(final JsonValue jsonValue) {
            return delegates.stream().flatMap(v -> v.apply(jsonValue));
        }

        @Override
        public String toString() {
            return delegates.toString();
        }
    }
}
