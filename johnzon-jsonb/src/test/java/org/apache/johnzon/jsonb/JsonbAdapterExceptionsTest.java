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

import org.junit.Test;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JsonbAdapterExceptionsTest {

    private static final Exception ADAPT_TO_JSON_EXCEPTION = new Exception("I am user, hear me roar");
    private static final Exception ADAPT_FROM_JSON_EXCEPTION = new Exception("I am user, hear me roar");
    private static final Exception CONSTRUCTOR_EXCEPTION = new Exception("I am user, hear me roar");

    @Test
    public void adaptToJsonRuntimeException() {

        final Object object = new WidgetWithFailedAdapter(new Color("red"));

        ExceptionAsserts.toJson(object)
                .assertInstanceOf(JsonbException.class)
                .assertCauseChain(ADAPT_TO_JSON_EXCEPTION)
                .assertMessage("I am user, hear me roar");
    }

    @Test
    public void adaptFromJsonRuntimeException() {
        final String json = "{\"color\":{\"red\":2550,\"green\":0,\"blue\":0}}";

        ExceptionAsserts.fromJson(json, WidgetWithFailedAdapter.class)
                .assertInstanceOf(JsonbException.class)
                .assertCauseChain(ADAPT_FROM_JSON_EXCEPTION)
                .assertMessage("WidgetWithFailedAdapter property 'color' of type Color cannot be" +
                        " mapped to json object value: {\"red\":2550,\"green\":...\n" +
                        "I am user, hear me roar");
    }

    @Test
    public void fromJsonConstructorRuntimeException() {
        final String json = "{\"color\":{\"red\":2550,\"green\":0,\"blue\":0}}";

        ExceptionAsserts.fromJson(json, WidgetWithFailedConstructorAdapter.class)
                // TODO Review: shouldn't this be wrapped in a JsonbException?
                .assertSame(CONSTRUCTOR_EXCEPTION);
    }

    @Test
    public void toJsonConstructorRuntimeException() {

        final Object object = new WidgetWithFailedConstructorAdapter(new Color("red"));

        ExceptionAsserts.toJson(object)
                // TODO Review: shouldn't this be wrapped in a JsonbException?
                .assertSame(CONSTRUCTOR_EXCEPTION);
    }

    @Test
    public void adaptToJsonReturnNull() {

        final Object object = new WidgetWithReturnNullAdapter(new Color("red"));

        ExceptionAsserts.toJson(object)
                // TODO Review potential symmetry issue: adaptFromJson() can return null, but adaptToJson() cannot
                .assertInstanceOf(NullPointerException.class);
    }

    @Test
    public void adaptFromJsonReturnNull() throws Exception {
        final String json = "{\"color\":{\"red\":2550,\"green\":0,\"blue\":0}}";

        final JsonbConfig config = new JsonbConfig();
        config.setProperty("johnzon.snippetMaxLength", 20);

        final WidgetWithReturnNullAdapter widget = ((Callable<WidgetWithReturnNullAdapter>) () -> {
            try (final Jsonb jsonb = JsonbBuilder.create(config)) {
                return jsonb.fromJson(json, WidgetWithReturnNullAdapter.class);
            }
        }).call();

        assertNotNull(widget);
        assertNull(widget.getColor());
    }


    public static class FailedAdapter implements JsonbAdapter<Color, RGB> {

        @Override
        public RGB adaptToJson(final Color color) throws Exception {
            throw ADAPT_TO_JSON_EXCEPTION;
        }

        @Override
        public Color adaptFromJson(final RGB rgb) throws Exception {
            throw ADAPT_FROM_JSON_EXCEPTION;
        }
    }

    public static class FailedConstructorAdapter implements JsonbAdapter<Color, RGB> {
        public FailedConstructorAdapter() throws Exception {
            throw CONSTRUCTOR_EXCEPTION;
        }

        @Override
        public RGB adaptToJson(final Color color) throws Exception {
            return null;
        }

        @Override
        public Color adaptFromJson(final RGB rgb) throws Exception {
            return null;
        }
    }

    public static class ReturnNullAdapter implements JsonbAdapter<Color, RGB> {
        @Override
        public RGB adaptToJson(final Color color) throws Exception {
            return null;
        }

        @Override
        public Color adaptFromJson(final RGB rgb) throws Exception {
            return null;
        }
    }

    public static class WidgetWithReturnNullAdapter {
        @JsonbTypeAdapter(ReturnNullAdapter.class)
        private Color color;

        public WidgetWithReturnNullAdapter() {
        }

        public WidgetWithReturnNullAdapter(final Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(final Color color) {
            this.color = color;
        }
    }

    public static class WidgetWithFailedConstructorAdapter {
        @JsonbTypeAdapter(FailedConstructorAdapter.class)
        private Color color;

        public WidgetWithFailedConstructorAdapter() {
        }

        public WidgetWithFailedConstructorAdapter(final Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(final Color color) {
            this.color = color;
        }
    }

    public static class WidgetWithFailedAdapter {
        @JsonbTypeAdapter(FailedAdapter.class)
        private Color color;

        public WidgetWithFailedAdapter() {
        }

        public WidgetWithFailedAdapter(final Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(final Color color) {
            this.color = color;
        }
    }

    public static class Color {
        private final String name;

        public Color(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class RGB {
        private int red;
        private int green;
        private int blue;

        public RGB() {
        }

        public RGB(final int red, final int green, final int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public int getRed() {
            return red;
        }

        public void setRed(final int red) {
            this.red = red;
        }

        public int getGreen() {
            return green;
        }

        public void setGreen(final int green) {
            this.green = green;
        }

        public int getBlue() {
            return blue;
        }

        public void setBlue(final int blue) {
            this.blue = blue;
        }

    }

}
