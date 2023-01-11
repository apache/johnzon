/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.johnzon.jsonb;

import org.junit.Assert;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExceptionAsserts {

    private final Throwable throwable;

    public ExceptionAsserts(final Throwable throwable) {
        this.throwable = throwable;
    }

    public <T extends Throwable> ExceptionAsserts assertInstanceOf(final Class<T> expected) {
        final String message = String.format("%s not an instance of %s",
                throwable.getClass().getSimpleName(),
                expected.getSimpleName());
        assertTrue(message, expected.isAssignableFrom(throwable.getClass()));
        return this;
    }

    public ExceptionAsserts assertSame(final Throwable expected) {
        Assert.assertSame(expected, throwable);
        return this;
    }

    public ExceptionAsserts assertCauseChain(final Throwable expected) {
        Throwable cause = throwable;
        while ((cause = cause.getCause()) != null) {
            if (cause == expected) {
                return this;
            }
        }

        throw new AssertionError("Throwable " + throwable.getClass().getSimpleName() +
                " cause chain does not contain exception:" + expected.getMessage(), throwable);
    }

    public ExceptionAsserts assertMessage(final String expected) {
        assertEquals(expected, throwable.getMessage());
        return this;
    }

    /**
     * Useful for debugging tests
     */
    public ExceptionAsserts printStackTrace() {
        throwable.printStackTrace();
        return this;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public static ExceptionAsserts from(final Runnable runnable) {
        return from(runnable);
    }

    public static ExceptionAsserts from(final Callable<?> runnable) {
        try {

            runnable.call();

            throw new AssertionError("No exception occurred");

        } catch (AssertionError assertionError) {
            throw assertionError;
        } catch (Throwable throwable) {
            return new ExceptionAsserts(throwable);
        }
    }

    public static ExceptionAsserts fromJson(final String json, final Type clazz) {

        final JsonbConfig config = new JsonbConfig();
        config.setProperty("johnzon.snippetMaxLength", 20);

        return from(() -> {
            try (final Jsonb jsonb = JsonbBuilder.create(config)) {
                return jsonb.fromJson(json, clazz);
            }
        });
    }

    public static ExceptionAsserts toJson(final Object object) {

        final JsonbConfig config = new JsonbConfig();
        config.setProperty("johnzon.snippetMaxLength", 20);

        return from(() -> {
            try (final Jsonb jsonb = JsonbBuilder.create(config)) {
                jsonb.toJson(object, new ByteArrayOutputStream());
                return null;
            }
        });
    }
}
