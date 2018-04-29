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

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.johnzon.jsonschema.ValidationResult;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;

public class PatternValidation implements ValidationExtension {
    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>> create(final ValidationContext model) {
        if (model.getSchema().getString("type", "object").equals("string")) {
            return Optional.ofNullable(model.getSchema().get("pattern"))
                    .filter(val -> val.getValueType() == JsonValue.ValueType.STRING)
                    .map(pattern -> new Impl(model.toPointer(), model::readValue, JsonString.class.cast(pattern).getString()));
        }
        return Optional.empty();
    }

    private static class Impl implements Function<JsonValue, Stream<ValidationResult.ValidationError>> {
        private final String pointer;
        private final Function<JsonObject, JsonValue> extractor;
        private final JsRegex jsRegex;

        private Impl(final String pointer, final Function<JsonObject, JsonValue> extractor, final String pattern) {
            this.jsRegex = new JsRegex(pattern);
            this.pointer = pointer;
            this.extractor = extractor;
        }

        @Override
        public Stream<ValidationResult.ValidationError> apply(final JsonValue obj) {
            if (obj == null || obj == JsonValue.NULL) {
                return Stream.empty();
            }
            final JsonValue value = extractor.apply(obj.asJsonObject());
            if (value == null || value.getValueType() != JsonValue.ValueType.STRING || JsonValue.NULL.equals(value)) {
                return Stream.empty();
            }
            if (!jsRegex.test(JsonString.class.cast(value).getString())) {
                return Stream.of(new ValidationResult.ValidationError(pointer, value + " doesn't match " + jsRegex));
            }
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "Pattern{" +
                    "regex=" + jsRegex +
                    ", pointer='" + pointer + '\'' +
                    '}';
        }
    }

    private static class JsRegex implements Predicate<CharSequence> {

        private static final ScriptEngine ENGINE;

        static {
            ENGINE = new ScriptEngineManager().getEngineByName("javascript");
        }

        private final String regex;

        private final String indicators;

        private JsRegex(final String regex) {
            if (regex.startsWith("/") && regex.length() > 1) {
                final int end = regex.lastIndexOf('/');
                if (end < 0) {
                    this.regex = regex;
                    this.indicators = "";
                } else {
                    this.regex = regex.substring(1, end);
                    this.indicators = regex.substring(end + 1);
                }
            } else {
                this.regex = regex;
                this.indicators = "";
            }
        }

        @Override
        public boolean test(final CharSequence string) {
            final Bindings bindings = ENGINE.createBindings();
            bindings.put("text", string);
            bindings.put("regex", regex);
            bindings.put("indicators", indicators);
            try {
                return Boolean.class.cast(ENGINE.eval("new RegExp(regex, indicators).test(text)", bindings));
            } catch (final ScriptException e) {
                return false;
            }
        }
    }
}
