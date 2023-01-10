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

import org.apache.johnzon.jsonlogic.spi.AsyncOperator;
import org.apache.johnzon.jsonlogic.spi.Operator;
import org.junit.Test;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonPatch;
import jakarta.json.JsonValue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;

public class JohnzonJsonLogicTest {
    private final JohnzonJsonLogic jsonLogic = new JohnzonJsonLogic();
    private final JsonBuilderFactory builderFactory = Json.createBuilderFactory(emptyMap());

    @Test
    public void stage() throws InterruptedException {
        // if the async exec is too immediate we will execute the thenAccept callback in main thread
        // which is not the goal of this test so let's ensure we are in the expected case
        final CountDownLatch waitChainReady = new CountDownLatch(1);

        final JohnzonJsonLogic jsonLogic = new JohnzonJsonLogic()
                .registerOperator("async", new Operator() {
                    @Override
                    public CompletionStage<JsonValue> applyStage(final JohnzonJsonLogic logic,
                                                                 final JsonValue config,
                                                                 final JsonValue params) {
                        return logic.applyStage(
                                builderFactory.createObjectBuilder().add("async2", "ok").build(),
                                builderFactory.createObjectBuilder().add("p2", "1").build())
                                .thenApplyAsync(
                                        previous -> builderFactory.createObjectBuilder()
                                                .add("thread", Thread.currentThread().getName())
                                                .add("config", config)
                                                .add("params", params)
                                                .add("exec2", previous)
                                                .build(),
                                        r -> new Thread(r, "async").start());
                    }

                    @Override
                    public JsonValue apply(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
                        throw new UnsupportedOperationException();
                    }
                })
                .registerOperator("async2", new AsyncOperator() {
                    @Override
                    public CompletionStage<JsonValue> applyStage(final JohnzonJsonLogic logic,
                                                                 final JsonValue config,
                                                                 final JsonValue params) {
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                waitChainReady.await();
                            } catch (final InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return builderFactory.createObjectBuilder()
                                    .add("thread2", Thread.currentThread().getName())
                                    .add("config2", config)
                                    .add("params2", params)
                                    .build();
                        }, r -> new Thread(r, "async2").start());
                    }

                    @Override
                    public JsonValue apply(final JohnzonJsonLogic logic, final JsonValue config, final JsonValue params) {
                        throw new UnsupportedOperationException();
                    }
                });

        final CountDownLatch latch = new CountDownLatch(1); // if we use Future.get we break stage threading
        final AtomicReference<JsonValue> output = new AtomicReference<>();
        jsonLogic
                .applyStage(
                        builderFactory.createObjectBuilder()
                                .add("async", "a")
                                .build(),
                        builderFactory.createObjectBuilder()
                                .add("p1", "0")
                                .build())
                .thenAccept(result -> {
                    output.set(builderFactory.createObjectBuilder()
                            .add("exec1", result)
                            .add("thenSyncThread", Thread.currentThread().getName())
                            .build());
                    latch.countDown();
                });
        waitChainReady.countDown();
        latch.await();
        assertEquals("" +
                        "{" +
                        "\"exec1\":{\"thread\":\"async\",\"config\":\"a\",\"params\":{\"p1\":\"0\"}," +
                        "\"exec2\":{\"thread2\":\"async2\",\"config2\":\"ok\",\"params2\":{\"p2\":\"1\"}}}," +
                        "\"thenSyncThread\":\"async\"" +
                        "}" +
                        "",
                output.get().toString());
    }

    @Test
    public void jsonPatch() {
        assertEquals(
                builderFactory.createObjectBuilder()
                        .add("dummy", builderFactory.createObjectBuilder().add("added", true))
                        .add("foo", "replaced")
                        .build(),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("jsonpatch", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("op", JsonPatch.Operation.ADD.operationName())
                                                .add("path", "/dummy")
                                                .add("value", builderFactory.createObjectBuilder()
                                                        .add("added", true)))
                                        .add(builderFactory.createObjectBuilder()
                                                .add("op", JsonPatch.Operation.REPLACE.operationName())
                                                .add("path", "/foo")
                                                .add("value", "replaced")))
                                .build(),
                        Json.createObjectBuilder().add("foo", "bar").build()));
    }

    @Test
    public void jsonMergePatch() {
        assertEquals(
                builderFactory.createObjectBuilder()
                        .add("a", "z")
                        .add("c", builderFactory.createObjectBuilder()
                                .add("d", "e"))
                        .build(),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("jsonmergepatch", builderFactory.createObjectBuilder()
                                        .add("a", "z")
                                        .add("c", builderFactory.createObjectBuilder()
                                                .addNull("f"))
                                        .build())
                                .build(),
                        builderFactory.createObjectBuilder()
                                .add("a", "b")
                                .add("c", builderFactory.createObjectBuilder()
                                        .add("d", "e")
                                        .add("f", "g"))
                                .build()));
    }

    @Test
    public void jsonMergeDiff() {
        assertEquals(
                builderFactory.createObjectBuilder()
                        .add("a", "z")
                        .add("c", builderFactory.createObjectBuilder()
                                .addNull("f"))
                        .build(),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("jsonmergediff", builderFactory.createObjectBuilder()
                                        .add("a", "b")
                                        .add("c", builderFactory.createObjectBuilder()
                                                .add("d", "e")
                                                .add("f", "g")))
                                .build(),
                        builderFactory.createArrayBuilder()
                                .add(builderFactory.createObjectBuilder()
                                        .add("a", "z")
                                        .add("c", builderFactory.createObjectBuilder()
                                                .add("d", "e"))
                                        .build())
                                .add(builderFactory.createObjectBuilder())
                                .build()));
    }

    @Test
    public void varObjectString() {
        assertEquals(Json.createValue("b"), jsonLogic.apply(
                builderFactory.createObjectBuilder()
                        .add("var", "a")
                        .build(),
                builderFactory.createObjectBuilder()
                        .add("a", "b")
                        .add("c", "d")
                        .build()));
    }

    @Test
    public void varObjectPtr() {
        assertEquals(Json.createValue("ok"), jsonLogic.apply(
                builderFactory.createObjectBuilder()
                        .add("var", "a.b.0")
                        .build(),
                builderFactory.createObjectBuilder()
                        .add("a", builderFactory.createObjectBuilder()
                                .add("b", builderFactory.createArrayBuilder()
                                        .add("ok")))
                        .build()));
    }

    @Test
    public void varObjectStringMissing() {
        assertEquals(JsonValue.NULL, jsonLogic.apply(
                builderFactory.createObjectBuilder()
                        .add("var", "a")
                        .build(),
                builderFactory.createObjectBuilder()
                        .add("c", "d")
                        .build()));
    }

    @Test
    public void varArrayInt() {
        assertEquals(Json.createValue("b"), jsonLogic.apply(
                builderFactory.createObjectBuilder()
                        .add("var", 1)
                        .build(),
                builderFactory.createArrayBuilder()
                        .add("a")
                        .add("b")
                        .build()));
    }

    @Test
    public void varObjectDefault() {
        assertEquals(Json.createValue(26), jsonLogic.apply(
                builderFactory.createObjectBuilder()
                        .add("var", builderFactory.createArrayBuilder().add("z").add(26))
                        .build(),
                builderFactory.createObjectBuilder()
                        .add("a", "b")
                        .add("c", "d")
                        .build()));
    }

    @Test
    public void varArrayDefault() {
        assertEquals(Json.createValue(26), jsonLogic.apply(
                builderFactory.createObjectBuilder()
                        .add("var", builderFactory.createArrayBuilder().add(10).add(26))
                        .build(),
                builderFactory.createArrayBuilder()
                        .add("a")
                        .add("b")
                        .build()));
    }

    @Test
    public void missing() {
        final JsonObject value = builderFactory.createObjectBuilder()
                .add("a", 1)
                .add("b", 2)
                .build();
        assertEquals(
                JsonValue.EMPTY_JSON_ARRAY,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("missing",
                                        builderFactory.createArrayBuilder()
                                                .add("a")
                                                .add("b"))
                                .build(),
                        value));
        assertEquals(
                builderFactory.createArrayBuilder()
                        .add("c")
                        .build(),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("missing", builderFactory.createArrayBuilder()
                                        .add("a")
                                        .add("b")
                                        .add("c"))
                                .build(),
                        value));
    }

    @Test
    public void missingSome() {
        final JsonObject value = builderFactory.createObjectBuilder()
                .add("a", 1)
                .add("b", 2)
                .build();
        assertEquals(
                JsonValue.EMPTY_JSON_ARRAY,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("missing_some",
                                        builderFactory.createArrayBuilder()
                                                .add(1)
                                                .add(builderFactory.createArrayBuilder()
                                                        .add("a")
                                                        .add("c")
                                                        .add("d")))
                                .build(),
                        value));
        assertEquals(
                builderFactory.createArrayBuilder()
                        .add("c")
                        .add("d")
                        .build(),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("missing_some", builderFactory.createArrayBuilder()
                                        .add(2)
                                        .add(builderFactory.createArrayBuilder()
                                                .add("a")
                                                .add("c")
                                                .add("d")))
                                .build(),
                        value));
    }

    @Test
    public void ifStatic() {
        assertEquals(
                Json.createValue("yes"),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("if",
                                        builderFactory.createArrayBuilder()
                                                .add(true)
                                                .add("yes")
                                                .add("false"))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                Json.createValue("no"),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("if",
                                        builderFactory.createArrayBuilder()
                                                .add(false)
                                                .add("yes")
                                                .add("no"))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void ifElsIfElseWithVarEval() {
        assertEquals(
                Json.createValue("liquid"),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("if",
                                        builderFactory.createArrayBuilder()
                                                .add(builderFactory.createObjectBuilder()
                                                        .add("<", builderFactory.createArrayBuilder()
                                                                .add(builderFactory.createObjectBuilder()
                                                                        .add("var", "temp"))
                                                                .add(0)))
                                                .add("freezing")
                                                .add(builderFactory.createObjectBuilder()
                                                        .add("<", builderFactory.createArrayBuilder()
                                                                .add(builderFactory.createObjectBuilder()
                                                                        .add("var", "temp"))
                                                                .add(100)))
                                                .add("liquid")
                                                .add("gas"))
                                .build(),
                        builderFactory.createObjectBuilder().add("temp", 55).build()));
    }

    @Test
    public void lessThan() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("<", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("var", "temp"))
                                        .add(100))
                                .build(),
                        builderFactory.createObjectBuilder().add("temp", 99).build()));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("<", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("var", "temp"))
                                        .add(100))
                                .build(),
                        builderFactory.createObjectBuilder().add("temp", 100).build()));
    }

    @Test
    public void lessOrEqualsThan() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("<=", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("var", "temp"))
                                        .add(100))
                                .build(),
                        builderFactory.createObjectBuilder().add("temp", 100).build()));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("<=", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("var", "temp"))
                                        .add(100))
                                .build(),
                        builderFactory.createObjectBuilder().add("temp", 101).build()));
    }

    @Test
    public void greaterThan() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add(">", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("var", "temp"))
                                        .add(100))
                                .build(),
                        builderFactory.createObjectBuilder().add("temp", 101).build()));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add(">", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("var", "temp"))
                                        .add(100))
                                .build(),
                        builderFactory.createObjectBuilder().add("temp", 100).build()));
    }

    @Test
    public void greaterOrEqualsThan() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add(">=", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("var", "temp"))
                                        .add(100))
                                .build(),
                        builderFactory.createObjectBuilder().add("temp", 100).build()));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add(">=", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("var", "temp"))
                                        .add(100))
                                .build(),
                        builderFactory.createObjectBuilder().add("temp", 99).build()));
    }

    @Test
    public void equalsCoercion() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("==", builderFactory.createArrayBuilder()
                                        .add(1)
                                        .add(1))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("==", builderFactory.createArrayBuilder()
                                        .add("1")
                                        .add(1))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("==", builderFactory.createArrayBuilder()
                                        .add(1)
                                        .add("1"))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("==", builderFactory.createArrayBuilder()
                                        .add(0)
                                        .add(false))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("==", builderFactory.createArrayBuilder()
                                        .add(1)
                                        .add(false))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void equalsNoCoercion() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("===", builderFactory.createArrayBuilder()
                                        .add(1)
                                        .add(1))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("===", builderFactory.createArrayBuilder()
                                        .add("1")
                                        .add(1))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("===", builderFactory.createArrayBuilder()
                                        .add(1)
                                        .add("1"))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("===", builderFactory.createArrayBuilder()
                                        .add(0)
                                        .add(false))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("===", builderFactory.createArrayBuilder()
                                        .add(1)
                                        .add(false))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void negate() {
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("!", true)
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("!", builderFactory.createArrayBuilder().add(true).build())
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void booleanEvaluation() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("!!", true)
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("!!", "a")
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("!!", "")
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("!!", builderFactory.createArrayBuilder().add(true).build())
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void and() {
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("and", builderFactory.createArrayBuilder()
                                        .add(false)
                                        .add(true))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                Json.createValue("a"),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("and", builderFactory.createArrayBuilder()
                                        .add(true)
                                        .add("a"))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void or() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("or", builderFactory.createArrayBuilder()
                                        .add(false)
                                        .add(true))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("or", builderFactory.createArrayBuilder()
                                        .add(true)
                                        .add("a"))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                Json.createValue("a"),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("or", builderFactory.createArrayBuilder()
                                        .add("a")
                                        .add(true))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                Json.createValue("a"),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("or", builderFactory.createArrayBuilder()
                                        .add(false)
                                        .add(JsonValue.EMPTY_JSON_ARRAY)
                                        .add("a")
                                        .add(true))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void min() {
        assertEquals(
                Json.createValue(100.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("min", builderFactory.createArrayBuilder()
                                        .add(100)
                                        .add(200)
                                        .add(300))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void max() {
        assertEquals(
                Json.createValue(300.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("max", builderFactory.createArrayBuilder()
                                        .add(100)
                                        .add(200)
                                        .add(300))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void plus() {
        assertEquals(
                Json.createValue(6.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("+", builderFactory.createArrayBuilder()
                                        .add(4)
                                        .add(2))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                Json.createValue(3.14),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("+", "3.14")
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                Json.createValue(7.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("+", builderFactory.createArrayBuilder()
                                        .add(4)
                                        .add(2)
                                        .add(1))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void minus() {
        assertEquals(
                Json.createValue(2.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("-", builderFactory.createArrayBuilder()
                                        .add(4)
                                        .add(2))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                Json.createValue(-2.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("-", 2)
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void multiply() {
        assertEquals(
                Json.createValue(8.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("*", builderFactory.createArrayBuilder()
                                        .add(4)
                                        .add(2))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                Json.createValue(24.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("*", builderFactory.createArrayBuilder()
                                        .add(4)
                                        .add(3)
                                        .add(2))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void divide() {
        assertEquals(
                Json.createValue(2.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("/", builderFactory.createArrayBuilder()
                                        .add(4)
                                        .add(2))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void modulo() {
        assertEquals(
                Json.createValue(0.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("%", builderFactory.createArrayBuilder()
                                        .add(4)
                                        .add(2))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(
                Json.createValue(1.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("%", builderFactory.createArrayBuilder()
                                        .add(5)
                                        .add(2))
                                .build(),
                        JsonValue.EMPTY_JSON_OBJECT));
    }

    @Test
    public void map() {
        assertEquals(
                builderFactory.createArrayBuilder()
                        .add(2.)
                        .add(4.)
                        .add(6.)
                        .add(8.)
                        .add(10.)
                        .build(),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("map", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("var", "integers"))
                                        .add(builderFactory.createObjectBuilder()
                                                .add("*", builderFactory.createArrayBuilder()
                                                        .add(builderFactory.createObjectBuilder()
                                                                .add("var", ""))
                                                        .add(2))))
                                .build(),
                        builderFactory.createObjectBuilder()
                                .add("integers", builderFactory.createArrayBuilder()
                                        .add(1)
                                        .add(2)
                                        .add(3)
                                        .add(4)
                                        .add(5))
                                .build()));
    }

    @Test
    public void filter() {
        assertEquals(
                builderFactory.createArrayBuilder()
                        .add(1)
                        .add(3)
                        .add(5)
                        .build(),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("filter", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("var", "integers"))
                                        .add(builderFactory.createObjectBuilder()
                                                .add("%", builderFactory.createArrayBuilder()
                                                        .add(builderFactory.createObjectBuilder()
                                                                .add("var", ""))
                                                        .add(2))))
                                .build(),
                        builderFactory.createObjectBuilder()
                                .add("integers", builderFactory.createArrayBuilder()
                                        .add(1)
                                        .add(2)
                                        .add(3)
                                        .add(4)
                                        .add(5))
                                .build()));
    }

    @Test
    public void reduce() {
        assertEquals(
                Json.createValue(15.),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("reduce", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createObjectBuilder()
                                                .add("var", "integers"))
                                        .add(builderFactory.createObjectBuilder()
                                                .add("+", builderFactory.createArrayBuilder()
                                                        .add(builderFactory.createObjectBuilder()
                                                                .add("var", "current"))
                                                        .add(builderFactory.createObjectBuilder()
                                                                .add("var", "accumulator"))))
                                        .add(0))
                                .build(),
                        builderFactory.createObjectBuilder()
                                .add("integers", builderFactory.createArrayBuilder()
                                        .add(1)
                                        .add(2)
                                        .add(3)
                                        .add(4)
                                        .add(5))
                                .build()));
    }

    @Test
    public void all() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("all", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createArrayBuilder()
                                                .add(1)
                                                .add(2)
                                                .add(3))
                                        .add(builderFactory.createObjectBuilder()
                                                .add(">", builderFactory.createArrayBuilder()
                                                        .add(builderFactory.createObjectBuilder()
                                                                .add("var", ""))
                                                        .add(0))))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("all", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createArrayBuilder()
                                                .add(1)
                                                .add(2)
                                                .add(3))
                                        .add(builderFactory.createObjectBuilder()
                                                .add("<", builderFactory.createArrayBuilder()
                                                        .add(builderFactory.createObjectBuilder()
                                                                .add("var", ""))
                                                        .add(3))))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
    }

    @Test
    public void some() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("some", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createArrayBuilder()
                                                .add(1)
                                                .add(2)
                                                .add(3))
                                        .add(builderFactory.createObjectBuilder()
                                                .add(">", builderFactory.createArrayBuilder()
                                                        .add(builderFactory.createObjectBuilder()
                                                                .add("var", ""))
                                                        .add(2))))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("some", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createArrayBuilder()
                                                .add(1)
                                                .add(2)
                                                .add(3))
                                        .add(builderFactory.createObjectBuilder()
                                                .add(">", builderFactory.createArrayBuilder()
                                                        .add(builderFactory.createObjectBuilder()
                                                                .add("var", ""))
                                                        .add(3))))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
    }

    @Test
    public void none() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("none", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createArrayBuilder()
                                                .add(1)
                                                .add(2)
                                                .add(3))
                                        .add(builderFactory.createObjectBuilder()
                                                .add(">", builderFactory.createArrayBuilder()
                                                        .add(builderFactory.createObjectBuilder()
                                                                .add("var", ""))
                                                        .add(3))))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("none", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createArrayBuilder()
                                                .add(1)
                                                .add(2)
                                                .add(3))
                                        .add(builderFactory.createObjectBuilder()
                                                .add("<", builderFactory.createArrayBuilder()
                                                        .add(builderFactory.createObjectBuilder()
                                                                .add("var", ""))
                                                        .add(2))))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
    }

    @Test
    public void merge() {
        assertEquals(
                builderFactory.createArrayBuilder()
                        .add(1)
                        .add(2)
                        .add(3)
                        .add("4")
                        .build(),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("merge", builderFactory.createArrayBuilder()
                                        .add(builderFactory.createArrayBuilder()
                                                .add(1)
                                                .add(2))
                                        .add(3)
                                        .add("4"))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
    }

    @Test
    public void in() {
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("in", builderFactory.createArrayBuilder()
                                        .add(2)
                                        .add(builderFactory.createArrayBuilder()
                                                .add(1)
                                                .add(2)))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("in", builderFactory.createArrayBuilder()
                                        .add(3)
                                        .add(builderFactory.createArrayBuilder()
                                                .add(1)
                                                .add(2)))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
        assertEquals(
                JsonValue.TRUE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("in", builderFactory.createArrayBuilder()
                                        .add("ay")
                                        .add("may"))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
        assertEquals(
                JsonValue.FALSE,
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("in", builderFactory.createArrayBuilder()
                                        .add("cem")
                                        .add("may"))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
    }

    @Test
    public void cat() {
        assertEquals(
                Json.createValue("hello json"),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("cat", builderFactory.createArrayBuilder()
                                        .add("hell")
                                        .add("o json"))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
    }

    @Test
    public void substr() {
        assertEquals(
                Json.createValue("logic"),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("substr", builderFactory.createArrayBuilder()
                                        .add("jsonlogic")
                                        .add(4))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
        assertEquals(
                Json.createValue("logic"),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("substr", builderFactory.createArrayBuilder()
                                        .add("jsonlogic")
                                        .add(-5))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
        assertEquals(
                Json.createValue("son"),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("substr", builderFactory.createArrayBuilder()
                                        .add("jsonlogic")
                                        .add(1)
                                        .add(3))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
        assertEquals(
                Json.createValue("log"),
                jsonLogic.apply(
                        builderFactory.createObjectBuilder()
                                .add("substr", builderFactory.createArrayBuilder()
                                        .add("jsonlogic")
                                        .add(4)
                                        .add(-2))
                                .build(),
                        JsonValue.EMPTY_JSON_ARRAY));
    }
}
