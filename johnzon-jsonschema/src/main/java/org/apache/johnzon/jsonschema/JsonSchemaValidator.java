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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonValue;

public class JsonSchemaValidator implements Function<JsonValue, ValidationResult>, AutoCloseable {
    private static final ValidationResult SUCCESS = new ValidationResult(emptyList());

    private final Function<JsonValue, Stream<ValidationResult.ValidationError>> validationFunction;

    JsonSchemaValidator(final Function<JsonValue, Stream<ValidationResult.ValidationError>> validationFunction) {
        this.validationFunction = validationFunction;
    }

    @Override
    public ValidationResult apply(final JsonValue object) {
        final Collection<ValidationResult.ValidationError> errors = validationFunction.apply(object).collect(toList());
        if (!errors.isEmpty()) {
            return new ValidationResult(errors);
        }
        return SUCCESS;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String toString() {
        return "JsonSchemaValidator{" +
                "validationFunction=" + validationFunction +
                '}';
    }
}
