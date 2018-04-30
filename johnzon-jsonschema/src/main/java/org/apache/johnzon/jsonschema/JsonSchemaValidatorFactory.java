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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.regex.JavascriptRegex;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;
import org.apache.johnzon.jsonschema.spi.builtin.ContainsValidation;
import org.apache.johnzon.jsonschema.spi.builtin.EnumValidation;
import org.apache.johnzon.jsonschema.spi.builtin.ExclusiveMaximumValidation;
import org.apache.johnzon.jsonschema.spi.builtin.ExclusiveMinimumValidation;
import org.apache.johnzon.jsonschema.spi.builtin.ItemsValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MaxItemsValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MaxLengthValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MaxPropertiesValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MaximumValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MinItemsValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MinLengthValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MinPropertiesValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MinimumValidation;
import org.apache.johnzon.jsonschema.spi.builtin.MultipleOfValidation;
import org.apache.johnzon.jsonschema.spi.builtin.PatternValidation;
import org.apache.johnzon.jsonschema.spi.builtin.RequiredValidation;
import org.apache.johnzon.jsonschema.spi.builtin.TypeValidation;
import org.apache.johnzon.jsonschema.spi.builtin.UniqueItemsValidation;

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

    // js is closer to default and actually most used in the industry
    private final AtomicReference<Function<String, Predicate<CharSequence>>> regexFactory = new AtomicReference<>(JavascriptRegex::new);

    public JsonSchemaValidatorFactory() {
        extensions.addAll(createDefaultValidations());
        extensions.addAll(new ArrayList<>(StreamSupport.stream(ServiceLoader.load(ValidationExtension.class).spliterator(), false)
                .collect(toList())));
    }

    // see http://json-schema.org/latest/json-schema-validation.html
    public List<ValidationExtension> createDefaultValidations() {
        return asList(
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
                new PatternValidation(regexFactory.get()),
                new ItemsValidation(this),
                new MaxItemsValidation(),
                new MinItemsValidation(),
                new UniqueItemsValidation(),
                new ContainsValidation(this),
                new MaxPropertiesValidation(),
                new MinPropertiesValidation()
                // TODO: dependencies, propertyNames, if/then/else, allOf/anyOf/oneOf/not,
                //       format validations
        );
    }

    public JsonSchemaValidatorFactory appendExtensions(final ValidationExtension... extensions) {
        this.extensions.addAll(asList(extensions));
        return this;
    }

    public JsonSchemaValidatorFactory setExtensions(final ValidationExtension... extensions) {
        this.extensions.clear();
        return appendExtensions(extensions);
    }

    public JsonSchemaValidatorFactory setRegexFactory(final Function<String, Predicate<CharSequence>> factory) {
        regexFactory.set(factory);
        return this;
    }

    public JsonSchemaValidator newInstance(final JsonObject schema) {
        return new JsonSchemaValidator(buildValidator(ROOT_PATH, schema, null));
    }

    @Override
    public void close() {
        // no-op for now
    }

    private Function<JsonValue, Stream<ValidationResult.ValidationError>> buildValidator(final String[] path,
                                                                                         final JsonObject schema,
                                                                                         final Function<JsonValue, JsonValue> valueProvider) {
        final List<Function<JsonValue, Stream<ValidationResult.ValidationError>>> directValidations = buildDirectValidations(path, schema, valueProvider).collect(toList());
        final Function<JsonValue, Stream<ValidationResult.ValidationError>> nestedValidations = buildPropertiesValidations(path, schema, valueProvider);
        final Function<JsonValue, Stream<ValidationResult.ValidationError>> dynamicNestedValidations = buildPatternPropertiesValidations(path, schema, valueProvider);
        final Function<JsonValue, Stream<ValidationResult.ValidationError>> fallbackNestedValidations = buildAdditionalPropertiesValidations(path, schema, valueProvider);
        return new ValidationsFunction(
                Stream.concat(
                        directValidations.stream(),
                        Stream.of(nestedValidations, dynamicNestedValidations, fallbackNestedValidations))
                    .collect(toList()));
    }

    private Stream<Function<JsonValue, Stream<ValidationResult.ValidationError>>> buildDirectValidations(final String[] path,
                                                                                                         final JsonObject schema,
                                                                                                         final Function<JsonValue, JsonValue> valueProvider) {
        final ValidationContext model = new ValidationContext(path, schema, valueProvider);
        return extensions.stream()
                .map(e -> e.create(model))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Function<JsonValue, Stream<ValidationResult.ValidationError>> buildPropertiesValidations(final String[] path,
                                                                                                     final JsonObject schema,
                                                                                                     final Function<JsonValue, JsonValue> valueProvider) {
        return ofNullable(schema.get("properties"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.OBJECT)
                .map(it -> it.asJsonObject().entrySet().stream()
                        .filter(obj -> obj.getValue().getValueType() == JsonValue.ValueType.OBJECT)
                        .map(obj -> {
                            final String key = obj.getKey();
                            final String[] fieldPath = Stream.concat(Stream.of(path), Stream.of(key)).toArray(String[]::new);
                            return buildValidator(fieldPath, obj.getValue().asJsonObject(), new ChainedValueAccessor(valueProvider, key));
                        })
                        .collect(toList()))
                .map(this::toFunction)
                .orElse(NO_VALIDATION);
    }

    // not the best impl but is it really an important case?
    private Function<JsonValue, Stream<ValidationResult.ValidationError>> buildPatternPropertiesValidations(final String[] path,
                                                                                                     final JsonObject schema,
                                                                                                     final Function<JsonValue, JsonValue> valueProvider) {
        return ofNullable(schema.get("patternProperties"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.OBJECT)
                .map(it -> it.asJsonObject().entrySet().stream()
                        .filter(obj -> obj.getValue().getValueType() == JsonValue.ValueType.OBJECT)
                        .map(obj -> {
                            final Predicate<CharSequence> pattern = regexFactory.get().apply(obj.getKey());
                            final JsonObject currentSchema = obj.getValue().asJsonObject();
                            // no cache cause otherwise it could be in properties
                            return (Function<JsonValue, Stream<ValidationResult.ValidationError>>) validable -> {
                                if (validable.getValueType() != JsonValue.ValueType.OBJECT) {
                                    return Stream.empty();
                                }
                                return validable.asJsonObject().entrySet().stream()
                                        .filter(e -> pattern.test(e.getKey()))
                                        .flatMap(e -> buildValidator(
                                                Stream.concat(Stream.of(path), Stream.of(e.getKey())).toArray(String[]::new),
                                                currentSchema,
                                                new ChainedValueAccessor(valueProvider, e.getKey())).apply(e.getValue()));
                            };
                        })
                        .collect(toList()))
                .map(this::toFunction)
                .orElse(NO_VALIDATION);
    }

    private Function<JsonValue, Stream<ValidationResult.ValidationError>> buildAdditionalPropertiesValidations(final String[] path,
                                                                                                     final JsonObject schema,
                                                                                                     final Function<JsonValue, JsonValue> valueProvider) {
        return ofNullable(schema.get("additionalProperties"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.OBJECT)
                .map(it -> {
                    Predicate<String> excluded = s -> false;
                    if (schema.containsKey("properties")) {
                        final Set<String> properties = schema.getJsonObject("properties").keySet();
                        excluded = excluded.and(s -> !properties.contains(s));
                    }
                    if (schema.containsKey("patternProperties")) {
                        final List<Predicate<CharSequence>> properties = schema.getJsonObject("patternProperties").keySet().stream()
                                                                               .map(regexFactory.get())
                                                                               .collect(toList());
                        excluded = excluded.and(s -> properties.stream().noneMatch(p -> p.test(s)));
                    }
                    final Predicate<String> excludeAttrRef = excluded;
                    final JsonObject currentSchema = it.asJsonObject();
                    return (Function<JsonValue, Stream<ValidationResult.ValidationError>>) validable -> {
                        if (validable.getValueType() != JsonValue.ValueType.OBJECT) {
                            return Stream.empty();
                        }
                        return validable.asJsonObject().entrySet().stream()
                                        .filter(e -> excludeAttrRef.test(e.getKey()))
                                        .flatMap(e -> buildValidator(
                                                Stream.concat(Stream.of(path), Stream.of(e.getKey())).toArray(String[]::new),
                                                currentSchema,
                                                new ChainedValueAccessor(valueProvider, e.getKey())).apply(e.getValue()));
                    };
                })
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

    private static class ChainedValueAccessor implements Function<JsonValue, JsonValue> {
        private final Function<JsonValue, JsonValue> parent;
        private final String key;

        private ChainedValueAccessor(final Function<JsonValue, JsonValue> valueProvider, final String key) {
            this.parent = valueProvider;
            this.key = key;
        }

        @Override
        public JsonValue apply(final JsonValue value) {
            final JsonValue root = parent == null ? value : parent.apply(value);
            if (root != null && root.getValueType() != JsonValue.ValueType.NULL && root.getValueType() == JsonValue.ValueType.OBJECT) {
                return root.asJsonObject().get(key);
            }
            return JsonValue.NULL;
        }

        @Override
        public String toString() {
            return "ChainedValueAccessor{" +
                    "parent=" + parent +
                    ", key='" + key + '\'' +
                    '}';
        }
    }
}
