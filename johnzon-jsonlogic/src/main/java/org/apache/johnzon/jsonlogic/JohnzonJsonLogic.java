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
package org.apache.johnzon.jsonlogic;

import org.apache.johnzon.jsonlogic.spi.Operator;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonException;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonPatch;
import jakarta.json.JsonPointer;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.joining;

public class JohnzonJsonLogic {
    private final JsonProvider provider;
    private final Map<String, Operator> operators = new HashMap<>();
    private final Map<String, JsonPointer> pointers = new ConcurrentHashMap<>();
    private final Map<JsonArray, JsonPatch> jsonPatches = new ConcurrentHashMap<>();
    private final Map<JsonValue, JsonMergePatch> jsonMergePatches = new ConcurrentHashMap<>();
    private final JsonBuilderFactory builderFactory;
    private boolean cachePointers;
    private boolean cacheJsonPatches;
    private boolean cacheJsonMergePatches;

    public JohnzonJsonLogic() {
        this(JsonProvider.provider());
        registerDefaultOperators();
        registerExtensionsOperators();
    }

    public JohnzonJsonLogic(final JsonProvider provider) {
        this.provider = provider;
        this.builderFactory = provider.createBuilderFactory(emptyMap());
    }

    public JohnzonJsonLogic cachePointers() {
        this.cachePointers = true;
        return this;
    }

    public JohnzonJsonLogic cacheJsonPatches() {
        this.cacheJsonPatches = true;
        return this;
    }

    public JohnzonJsonLogic cacheJsonMergePatches() {
        this.cacheJsonMergePatches = true;
        return this;
    }

    public JohnzonJsonLogic registerOperator(final String name, final Operator impl) {
        operators.put(name, impl);
        return this;
    }

    public JsonValue apply(final JsonValue logic, final JsonValue args) {
        if (logic.getValueType() != JsonValue.ValueType.OBJECT) {
            return logic;
        }

        final JsonObject object = logic.asJsonObject();
        if (object.size() > 1) {
            return object;
        }

        final Set<String> keys = object.keySet();
        if (keys.size() != 1) {
            throw invalidArgument(keys);
        }
        final String operator = keys.iterator().next();
        final Operator impl = operators.get(operator);
        if (impl == null) {
            throw missingOperator(operator);
        }
        return impl.apply(this, object.get(operator), args);
    }

    public CompletionStage<JsonValue> applyStage(final JsonValue logic, final JsonValue args) {
        if (logic.getValueType() != JsonValue.ValueType.OBJECT) {
            return completedFuture(logic);
        }

        final JsonObject object = logic.asJsonObject();
        if (object.size() > 1) {
            return completedFuture(object);
        }

        final Set<String> keys = object.keySet();
        if (keys.size() != 1) {
            final CompletableFuture<JsonValue> promise = new CompletableFuture<>();
            promise.completeExceptionally(invalidArgument(keys));
            return promise;
        }
        final String operator = keys.iterator().next();
        final Operator impl = operators.get(operator);
        if (impl == null) {
            final CompletableFuture<JsonValue> promise = new CompletableFuture<>();
            promise.completeExceptionally(missingOperator(operator));
            return promise;
        }
        return impl.applyStage(this, object.get(operator), args);
    }

    public boolean isTruthy(final JsonValue value) {
        return !isFalsy(value);
    }

    public boolean isFalsy(final JsonValue value) {
        switch (value.getValueType()) {
            case NUMBER:
                return JsonNumber.class.cast(value).intValue() == 0;
            case ARRAY:
                return value.asJsonArray().isEmpty();
            case STRING:
                return JsonString.class.cast(value).getString().isEmpty();
            case FALSE:
            case NULL:
                return true;
            default:
                return false;
        }
    }

    public boolean areEqualsWithCoercion(final JsonValue a, final JsonValue b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return false;
        }
        if (b == null) {
            return false;
        }
        if (a.getValueType() == b.getValueType()) {
            return a.equals(b);
        }
        switch (a.getValueType()) {
            case STRING:
                switch (b.getValueType()) {
                    case NUMBER:
                        try {
                            return Double.parseDouble(JsonString.class.cast(a).getString()) == JsonNumber.class.cast(b).doubleValue();
                        } catch (final NumberFormatException nfe) {
                            return false;
                        }
                    case TRUE:
                    case FALSE:
                        return isFalsy(a) == isFalsy(b);
                    default:
                        return false;
                }
            case NUMBER:
                switch (b.getValueType()) {
                    case STRING:
                        try {
                            return Double.parseDouble(JsonString.class.cast(b).getString()) == JsonNumber.class.cast(a).doubleValue();
                        } catch (final NumberFormatException nfe) {
                            return false;
                        }
                    case TRUE:
                    case FALSE:
                    default:
                        return isFalsy(a) == isFalsy(b);
                }
            case TRUE:
            case FALSE:
                return isFalsy(a) == isFalsy(b);
            default:
                return false;
        }
    }

    public JohnzonJsonLogic registerExtensionsOperators() {
        registerOperator("jsonpatch", (logic, config, params) -> getJsonPatch(config)
                .apply(JsonStructure.class.cast(params)));
        registerOperator("jsonmergepatch", (logic, config, params) -> getJsonMergePatch(config)
                .apply(params));
        registerOperator("jsonmergediff", (logic, config, params) -> {
            final JsonArray array = params.asJsonArray();
            if (array.size() != 2) {
                throw new IllegalArgumentException("jsonmergediff should have 2 parameters (in an array): " + array);
            }
            return provider.createMergeDiff(config, array.get(0)).apply(array.get(1));
        });
        return this;
    }

    private JsonPatch getJsonPatch(final JsonValue config) {
        if (!cacheJsonPatches) {
            return provider.createPatch(config.asJsonArray());
        }
        return jsonPatches.computeIfAbsent(config.asJsonArray(), provider::createPatch);
    }

    private JsonMergePatch getJsonMergePatch(final JsonValue config) {
        if (!cacheJsonPatches) {
            return provider.createMergePatch(config);
        }
        return jsonMergePatches.computeIfAbsent(config, provider::createMergePatch);
    }

    // to not depend on a logger we don't register "log" operation but it is trivial to do:
    public JohnzonJsonLogic registerDefaultOperators() {
        registerOperator("log", (logic, config, params) -> {
            throw new UnsupportedOperationException("Log is not supported by default, register the following operator with your preferred logger:\n\n" +
                    "jsonLogic.registerOperator(\"log\", (l, c, p) -> log.info(String.valueOf(l.apply(c, p)));\n");
        });
        registerOperator("var", (logic, config, params) -> varImpl(config, params));
        registerOperator("missing", this::missingImpl);
        registerOperator("missing_some", this::missingSomeImpl);
        registerOperator("if", this::ifImpl);
        registerOperator("<", (logic, config, params) -> numericComparison((a, b) -> a < b, config, logic, params));
        registerOperator(">", (logic, config, params) -> numericComparison((a, b) -> a > b, config, logic, params));
        registerOperator("<=", (logic, config, params) -> numericComparison((a, b) -> a <= b, config, logic, params));
        registerOperator(">=", (logic, config, params) -> numericComparison((a, b) -> a >= b, config, logic, params));
        registerOperator("==", (logic, config, params) -> comparison(this::areEqualsWithCoercion, config, logic, params));
        registerOperator("!=", (logic, config, params) -> comparison((a, b) -> !areEqualsWithCoercion(a, b), config, logic, params));
        registerOperator("===", (logic, config, params) -> comparison(Objects::equals, config, logic, params));
        registerOperator("!==", (logic, config, params) -> comparison((a, b) -> !Objects.equals(a, b), config, logic, params));
        registerOperator("!", this::notImpl);
        registerOperator("!!", this::toBooleanImpl);
        registerOperator("or", this::orImpl);
        registerOperator("and", this::andImpl);
        registerOperator("min", this::minImpl);
        registerOperator("max", this::maxImpl);
        registerOperator("+", this::plusImpl);
        registerOperator("*", this::multiplyImpl);
        registerOperator("-", this::minusImpl);
        registerOperator("/", this::divideImpl);
        registerOperator("%", this::moduloImpl);
        registerOperator("map", this::mapImpl);
        registerOperator("filter", this::filterImpl);
        registerOperator("reduce", this::reduceImpl);
        registerOperator("all", (logic, config, params) ->
                arrayTest(logic, config, params, (subConf, stream) -> stream.allMatch(it -> isTruthy(logic.apply(subConf, it)))));
        registerOperator("some", (logic, config, params) ->
                arrayTest(logic, config, params, (subConf, stream) -> stream.anyMatch(it -> isTruthy(logic.apply(subConf, it)))));
        registerOperator("none", (logic, config, params) ->
                arrayTest(logic, config, params, (subConf, stream) -> stream.noneMatch(it -> isTruthy(logic.apply(subConf, it)))));
        registerOperator("merge", (logic, config, params) -> mergeImpl(config));
        registerOperator("in", this::inImpl);
        registerOperator("cat", this::catImpl);
        registerOperator("substr", this::substrImpl);
        return this;
    }

    private IllegalArgumentException invalidArgument(final Set<String> keys) {
        return new IllegalArgumentException("Invalid argument, multiple keys found: " + keys);
    }

    private IllegalArgumentException missingOperator(final String operator) {
        return new IllegalArgumentException("Missing operator '" + operator + "'");
    }

    private JsonValue minImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("min only supports arrays: '" + config + "'");
        }
        return provider.createValue(mapToDouble(logic, config, params).min().orElse(0));
    }

    private JsonValue maxImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("max only supports arrays: '" + config + "'");
        }
        return provider.createValue(mapToDouble(logic, config, params).max().orElse(0));
    }

    private JsonValue plusImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            return castToNumber(logic.apply(config, params));
        }
        if (config.asJsonArray().isEmpty()) {
            return provider.createValue(0);
        }
        return provider.createValue(mapToDouble(logic, config, params).sum());
    }

    private JsonValue multiplyImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("* only supports arrays: '" + config + "'");
        }
        if (config.asJsonArray().isEmpty()) {
            return provider.createValue(0);
        }
        return provider.createValue(mapToDouble(logic, config, params)
                .reduce(1, (a, b) -> a * b));
    }

    private JsonValue minusImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() == JsonValue.ValueType.ARRAY) {
            final JsonArray array = config.asJsonArray();
            if (array.size() != 2) {
                throw new IllegalArgumentException("- only supports arrays with 2 elements: '" + config + "'");
            }
            return provider.createValue(JsonNumber.class.cast(logic.apply(array.get(0), params)).doubleValue() -
                    JsonNumber.class.cast(logic.apply(array.get(1), params)).doubleValue());
        }
        final JsonValue applied = logic.apply(config, params);
        if (applied.getValueType() == JsonValue.ValueType.NUMBER) {
            return provider.createValue(-1 * JsonNumber.class.cast(applied).doubleValue());
        }
        throw new IllegalArgumentException("Unsupported - operation: '" + config + "'");
    }

    private JsonValue divideImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() == JsonValue.ValueType.ARRAY) {
            final JsonArray array = config.asJsonArray();
            if (array.size() != 2) {
                throw new IllegalArgumentException("/ only supports arrays with 2 elements: '" + config + "'");
            }
            return provider.createValue(JsonNumber.class.cast(logic.apply(array.get(0), params)).doubleValue() /
                    JsonNumber.class.cast(logic.apply(array.get(1), params)).doubleValue());
        }
        throw new IllegalArgumentException("Unsupported / operation: '" + config + "'");
    }

    private JsonValue moduloImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() == JsonValue.ValueType.ARRAY) {
            final JsonArray array = config.asJsonArray();
            if (array.size() != 2) {
                throw new IllegalArgumentException("% only supports arrays with 2 elements: '" + config + "'");
            }
            return provider.createValue(JsonNumber.class.cast(logic.apply(array.get(0), params)).doubleValue() %
                    JsonNumber.class.cast(logic.apply(array.get(1), params)).doubleValue());
        }
        throw new IllegalArgumentException("Unsupported % operation: '" + config + "'");
    }

    private JsonValue mapImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() == JsonValue.ValueType.ARRAY) {
            final JsonArray array = config.asJsonArray();
            if (array.size() != 2) {
                throw new IllegalArgumentException("map only supports arrays with 2 elements: '" + config + "'");
            }
            final JsonValue items = logic.apply(array.get(0), params);
            if (items.getValueType() != JsonValue.ValueType.ARRAY) {
                throw new IllegalArgumentException("Expected '" + array.get(0) + "' to be an array, got " + items.getValueType());
            }
            final JsonValue subLogic = array.get(1);
            return items.asJsonArray().stream()
                    .map(it -> logic.apply(subLogic, it))
                    .collect(toArray());
        }
        throw new IllegalArgumentException("Unsupported map operation: '" + config + "'");
    }

    private JsonValue filterImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() == JsonValue.ValueType.ARRAY) {
            final JsonArray array = config.asJsonArray();
            if (array.size() != 2) {
                throw new IllegalArgumentException("filter only supports arrays with 2 elements: '" + config + "'");
            }
            final JsonValue items = logic.apply(array.get(0), params);
            if (items.getValueType() != JsonValue.ValueType.ARRAY) {
                throw new IllegalArgumentException("Expected '" + array.get(0) + "' to be an array, got " + items.getValueType());
            }
            final JsonValue subLogic = array.get(1);
            return items.asJsonArray().stream()
                    .filter(it -> isTruthy(logic.apply(subLogic, it)))
                    .collect(toArray());
        }
        throw new IllegalArgumentException("Unsupported filter operation: '" + config + "'");
    }

    private JsonValue mergeImpl(final JsonValue config) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("merge only support an array as configuration, got '" + config + "'");
        }
        return config.asJsonArray().stream()
                .flatMap(it -> it.getValueType() == JsonValue.ValueType.ARRAY ?
                        it.asJsonArray().stream() : builderFactory.createArrayBuilder().add(it).build().stream())
                .collect(toArray());
    }

    private JsonValue substrImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY || config.asJsonArray().size() < 2) {
            throw new IllegalArgumentException("substr only support an array as configuration, got '" + config + "'");
        }
        final JsonArray array = config.asJsonArray();
        final JsonValue value = logic.apply(array.get(0), params);
        if (value.getValueType() != JsonValue.ValueType.STRING) {
            throw new IllegalArgumentException("expected a string for substr, got '" + value + "'");
        }
        final String valueStr = JsonString.class.cast(value).getString();
        final JsonValue from = logic.apply(array.get(1), params);
        if (from.getValueType() != JsonValue.ValueType.NUMBER) {
            throw new IllegalArgumentException("expected a number for substr, got '" + from + "'");
        }
        final int fromIdx = JsonNumber.class.cast(from).intValue();
        final int start;
        if (fromIdx < 0) {
            start = valueStr.length() + fromIdx;
        } else {
            start = fromIdx;
        }
        final int end;
        if (array.size() == 3) {
            final JsonValue to = logic.apply(array.get(2), params);
            if (to.getValueType() != JsonValue.ValueType.NUMBER) {
                throw new IllegalArgumentException("expected a number for substr, got '" + to + "'");
            }
            final int length = JsonNumber.class.cast(to).intValue();
            end = length < 0 ? valueStr.length() + length : start + length;
        } else {
            end = valueStr.length();
        }
        return provider.createValue(valueStr.substring(start, end));
    }

    private JsonValue catImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("cat only support an array of string elements as configuration, got '" + config + "'");
        }
        return provider.createValue(config.asJsonArray().stream()
                .map(it -> logic.apply(it, params))
                .filter(it -> it.getValueType() == JsonValue.ValueType.STRING)
                .map(it -> JsonString.class.cast(it).getString())
                .collect(joining()));
    }

    private JsonValue inImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY || config.asJsonArray().size() != 2) {
            throw new IllegalArgumentException("in only support an array of 2 elements as configuration, got '" + config + "'");
        }
        final JsonArray array = config.asJsonArray();
        final JsonValue expected = logic.apply(array.get(0), params);
        final JsonValue value = logic.apply(array.get(1), params);
        switch (value.getValueType()) {
            case STRING:
                return expected.getValueType() == JsonValue.ValueType.STRING && JsonString.class.cast(value).getString()
                        .contains(JsonString.class.cast(expected).getString()) ? JsonValue.TRUE : JsonValue.FALSE;
            case ARRAY:
                return value.getValueType() == JsonValue.ValueType.ARRAY && value.asJsonArray().stream()
                        .anyMatch(it -> Objects.equals(it, expected)) ? JsonValue.TRUE : JsonValue.FALSE;
            default:
                return JsonValue.FALSE;
        }
    }

    private JsonValue reduceImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() == JsonValue.ValueType.ARRAY) {
            final JsonArray array = config.asJsonArray();
            if (array.size() < 2 || array.size() > 3) {
                throw new IllegalArgumentException("filter only supports arrays with 2 or 3 elements: '" + config + "'");
            }
            final JsonValue items = logic.apply(array.get(0), params);
            if (items.getValueType() != JsonValue.ValueType.ARRAY) {
                throw new IllegalArgumentException("Expected '" + array.get(0) + "' to be an array, got " + items.getValueType());
            }
            final JsonValue subLogic = array.get(1);
            return items.asJsonArray().stream()
                    .reduce(
                            array.size() == 3 ? array.get(2) : JsonValue.NULL,
                            (accumulator, current) -> logic.apply(subLogic, builderFactory.createObjectBuilder()
                                    .add("accumulator", accumulator)
                                    .add("current", current)
                                    .build()));
        }
        throw new IllegalArgumentException("Unsupported reduce operation: '" + config + "'");
    }

    private JsonValue andImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("and only supports arrays: '" + config + "'");
        }
        final JsonArray array = config.asJsonArray();
        return array.stream()
                .map(it -> logic.apply(it, params))
                .filter(this::isFalsy)
                .findFirst()
                .orElseGet(() -> array.isEmpty() ? JsonValue.FALSE : array.get(array.size() - 1));
    }

    private JsonValue orImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("or only supports arrays: '" + config + "'");
        }
        final JsonArray array = config.asJsonArray();
        return array.stream()
                .map(it -> logic.apply(it, params))
                .filter(this::isTruthy)
                .findFirst()
                .orElseGet(() -> array.isEmpty() ? JsonValue.FALSE : array.get(array.size() - 1));
    }

    private JsonValue toBooleanImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() == JsonValue.ValueType.ARRAY) {
            final JsonArray array = config.asJsonArray();
            if (array.size() != 1) {
                throw new IllegalArgumentException("!! takes only one parameter '" + config + "'");
            }
            return isTruthy(logic.apply(array.get(0), params)) ? JsonValue.TRUE : JsonValue.FALSE;
        }
        return isTruthy(logic.apply(config, params)) ? JsonValue.TRUE : JsonValue.FALSE;
    }

    private JsonValue notImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() == JsonValue.ValueType.ARRAY) {
            final JsonArray array = config.asJsonArray();
            if (array.size() != 1) {
                throw new IllegalArgumentException("! takes only one parameter '" + config + "'");
            }
            return isFalsy(logic.apply(array.get(0), params)) ? JsonValue.TRUE : JsonValue.FALSE;
        }
        return isFalsy(logic.apply(config, params)) ? JsonValue.TRUE : JsonValue.FALSE;
    }

    private JsonValue ifImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("if config must be an array");
        }
        final JsonArray configArray = config.asJsonArray();
        if (configArray.size() < 2) {
            throw new IllegalArgumentException("if config must be an array >= 2 elements");
        }
        for (int i = 0; i < configArray.size() - 1; i += 2) {
            if (isTruthy(logic.apply(configArray.get(i), params))) {
                return logic.apply(configArray.get(i + 1), params);
            }
        }
        if (configArray.size() % 2 == 1) {
            return configArray.get(configArray.size() - 1);
        }
        return JsonValue.FALSE;
    }

    private JsonValue missingSomeImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("missing_some takes an array as parameter: '" + config + "'");
        }
        final JsonArray configArray = config.asJsonArray();
        if (configArray.size() != 2) {
            throw new IllegalArgumentException("missing_some takes an array with a number and a path array as parameter: '" + config + "'");
        }
        final JsonArray tested = configArray.get(1).asJsonArray();
        final JsonArray missing = tested.stream()
                .filter(it -> varImpl(logic.apply(it, params), params) == JsonValue.NULL)
                .collect(toArray());
        if ((tested.size() - missing.size()) < JsonNumber.class.cast(logic.apply(configArray.get(0), params)).intValue()) {
            return missing;
        }
        return JsonValue.EMPTY_JSON_ARRAY;
    }

    private JsonValue missingImpl(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("missing takes an array as parameter: '" + config + "'");
        }
        return config.asJsonArray().stream()
                .filter(it -> varImpl(logic.apply(it, params), params) == JsonValue.NULL)
                .collect(toArray());
    }

    private JsonValue arrayTest(final JohnzonJsonLogic self, final JsonValue config, final JsonValue params,
                                final BiPredicate<JsonValue, Stream<JsonValue>> tester) {
        if (config.getValueType() == JsonValue.ValueType.ARRAY) {
            final JsonArray array = config.asJsonArray();
            if (array.size() != 2) {
                throw new IllegalArgumentException("array test only supports arrays with 2: '" + config + "'");
            }
            final JsonValue items = self.apply(array.get(0), params);
            if (items.getValueType() != JsonValue.ValueType.ARRAY) {
                throw new IllegalArgumentException("Expected '" + array.get(0) + "' to be an array, got " + items.getValueType());
            }
            final JsonValue subLogic = array.get(1);
            return tester.test(subLogic, items.asJsonArray().stream()) ? JsonValue.TRUE : JsonValue.FALSE;
        }
        throw new IllegalArgumentException("Unsupported array test operation: '" + config + "'");
    }

    private JsonValue castToNumber(final JsonValue value) {
        switch (value.getValueType()) {
            case NUMBER:
                return value;
            case STRING:
                return provider.createValue(Double.parseDouble(JsonString.class.cast(value).getString()));
            default:
                throw new IllegalArgumentException("Unsupported value to number: '" + value + "'");
        }
    }

    private DoubleStream mapToDouble(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
        return config.asJsonArray().stream()
                .map(it -> logic.apply(it, params))
                .filter(it -> it.getValueType() == JsonValue.ValueType.NUMBER)
                .mapToDouble(it -> JsonNumber.class.cast(it).doubleValue());
    }

    private JsonValue comparison(final BiPredicate<JsonValue, JsonValue> comparator,
                                 final JsonValue config, final JohnzonJsonLogic self,
                                 final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("comparison config must be an array");
        }
        final JsonArray values = config.asJsonArray();
        if (values.size() != 2) {
            throw new IllegalArgumentException("comparison requires 2 arguments");
        }
        final JsonValue first = self.apply(values.get(0), params);
        final JsonValue second = self.apply(values.get(1), params);
        return comparator.test(first, second) ? JsonValue.TRUE : JsonValue.FALSE;
    }

    private JsonValue numericComparison(final BiPredicate<Double, Double> comparator,
                                        final JsonValue config, final JohnzonJsonLogic self,
                                        final JsonValue params) {
        if (config.getValueType() != JsonValue.ValueType.ARRAY) {
            throw new IllegalArgumentException("numeric comparison config must be an array");
        }
        final JsonArray configArray = config.asJsonArray();
        switch (configArray.size()) {
            case 2: {
                final JsonValue first = self.apply(configArray.get(0), params);
                final JsonValue second = self.apply(configArray.get(1), params);
                if (Stream.of(first, second).anyMatch(it -> it.getValueType() != JsonValue.ValueType.NUMBER)) {
                    throw new IllegalArgumentException("Only numbers can be compared: " + first + " / " + second);
                }
                return comparator.test(JsonNumber.class.cast(first).doubleValue(), JsonNumber.class.cast(second).doubleValue()) ?
                        JsonValue.TRUE : JsonValue.FALSE;
            }
            case 3: { // between
                final JsonValue first = self.apply(configArray.get(0), params);
                final JsonValue second = self.apply(configArray.get(1), params);
                final JsonValue third = self.apply(configArray.get(1), params);
                if (Stream.of(first, second, third).anyMatch(it -> it.getValueType() != JsonValue.ValueType.NUMBER)) {
                    throw new IllegalArgumentException("Only numbers can be compared");
                }
                return comparator.test(JsonNumber.class.cast(first).doubleValue(), JsonNumber.class.cast(second).doubleValue()) &&
                        comparator.test(JsonNumber.class.cast(second).doubleValue(), JsonNumber.class.cast(third).doubleValue()) ?
                        JsonValue.TRUE : JsonValue.FALSE;
            }
            default:
                throw new IllegalArgumentException("numeric comparison config must be an array >= 2 elements");
        }
    }

    private JsonValue varImpl(final JsonValue config, final JsonValue params) {
        switch (config.getValueType()) {
            case ARRAY:
                final JsonArray values = config.asJsonArray();
                if (values.isEmpty()) {
                    throw new IllegalArgumentException("var should have at least one parameter");
                }
                final JsonValue accessor = apply(values.get(0), params);
                switch (accessor.getValueType()) {
                    case NUMBER:
                        final int index = JsonNumber.class.cast(accessor).intValue();
                        final JsonArray array = params.asJsonArray();
                        final JsonValue arrayAttribute = index >= array.size() ? null : array.get(index);
                        return arrayAttribute == null ? (values.size() > 1 ? apply(values.get(1), params) : JsonValue.NULL) : arrayAttribute;
                    case STRING:
                        final JsonValue objectAttribute = extractValue(params, JsonString.class.cast(accessor).getString());
                        return objectAttribute == JsonValue.NULL && values.size() > 1 ? apply(values.get(1), params) : objectAttribute;
                    default:
                        throw new IllegalArgumentException("Unsupported var first paraemter: '" + accessor + "', should be string or number");
                }
            case STRING:
                return extractValue(params, JsonString.class.cast(config).getString());
            case NUMBER:
                final int index = JsonNumber.class.cast(config).intValue();
                final JsonArray array = params.asJsonArray();
                final JsonValue arrayAttribute = array.size() <= index ? null : array.get(index);
                return arrayAttribute == null ? JsonValue.NULL : arrayAttribute;
            case OBJECT:
                return varImpl(apply(config, params), params);
            default:
                throw new IllegalArgumentException("Unsupported configuration for var: '" + config + "'");
        }
    }

    private JsonValue extractValue(final JsonValue params, final String string) {
        if (string.isEmpty()) {
            return params;
        }
        final JsonValue objectAttribute;
        if (string.contains(".")) {
            try {
                objectAttribute = toPointer(string).getValue(JsonStructure.class.cast(params));
            } catch (final JsonException je) { // missing
                return JsonValue.NULL;
            }
        } else if (params.getValueType() == JsonValue.ValueType.OBJECT) {
            objectAttribute = params.asJsonObject().get(string);
        } else if (params.getValueType() == JsonValue.ValueType.ARRAY) {
            objectAttribute = params.asJsonArray().get(Integer.parseInt(string.trim()));
        } else {
            objectAttribute = null;
        }
        return objectAttribute == null ? JsonValue.NULL : objectAttribute;
    }

    // cache?
    private JsonPointer toPointer(final String string) {
        if (cachePointers) {
            return pointers.computeIfAbsent(string, this::doToPointer);
        }
        return doToPointer(string);
    }

    private JsonPointer doToPointer(final String string) {
        return provider.createPointer(
                (!string.startsWith("/") ? "/" : "") +
                        string.replace('.', '/'));
    }

    // same as JsonCollector one except it uses this builderFactory instead of default one which goes through the SPI
    private Collector<JsonValue, JsonArrayBuilder, JsonArray> toArray() {
        return Collector.of(builderFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::addAll, JsonArrayBuilder::build);
    }
}
