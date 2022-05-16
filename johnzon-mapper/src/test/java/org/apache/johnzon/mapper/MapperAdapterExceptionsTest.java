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
package org.apache.johnzon.mapper;

import org.junit.Test;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MapperAdapterExceptionsTest {

    private static final RuntimeException FROM_EXCEPTION = new RuntimeException("I am user, hear me roar");
    private static final RuntimeException TO_EXCEPTION = new RuntimeException("I am user, hear me roar");

    @Test
    public void adapterFromRuntimeException() {

        final Runnable adapterFrom = () -> {
            try (final Mapper mapper = new MapperBuilder().addAdapter(new FailingAdapter()).setSnippetMaxLength(20).build()) {
                mapper.writeObjectAsString(new Widget(new Color("red")));
            }
        };

        ExceptionAsserts.from(adapterFrom)
                // TODO: not consistent with how getter user exceptions are handled
                .assertSame(FROM_EXCEPTION);
    }

    @Test
    public void adapterToRuntimeException() {
        
        final Runnable adapterTo = () -> {
            try (final Mapper mapper = new MapperBuilder().addAdapter(new FailingAdapter()).setSnippetMaxLength(20).build()) {
                mapper.readObject("{\"color\":{\"red\":2550,\"green\":0,\"blue\":0}}", Widget.class);
            }
        };
        
        ExceptionAsserts.from(adapterTo)
                .assertCauseChain(TO_EXCEPTION)
                .assertInstanceOf(MapperException.class)
                .assertMessage("Widget property 'color' of type Color cannot be mapped to json object value: {\"red\":2550,\"green\":...\n" +
                        "I am user, hear me roar");
    }

    @Test
    public void adapterFromNulValue() {

        final Runnable adapterFrom = () -> {
            try (final Mapper mapper = new MapperBuilder().addAdapter(new ReturnNull()).setSnippetMaxLength(20).build()) {
                mapper.writeObjectAsString(new Widget(new Color("red")));
            }
        };

        ExceptionAsserts.from(adapterFrom)
                // TODO Review: to() can return null, but from() cannot
                .assertInstanceOf(NullPointerException.class);

    }

    @Test
    public void adapterToNullValue() throws Exception {

        final Callable<Widget> adapterTo = () -> {
            try (final Mapper mapper = new MapperBuilder().addAdapter(new ReturnNull()).setSnippetMaxLength(20).build()) {
                return mapper.readObject("{\"color\":{\"red\":2550,\"green\":0,\"blue\":0}}", Widget.class);
            }
        };

        final Widget widget = adapterTo.call();

        assertNotNull(widget);
        assertNull(widget.getColor());
    }

    public static class FailingAdapter implements Adapter<Color, RGB> {

        // writeObject
        @Override
        public RGB from(final Color color) {
            throw FROM_EXCEPTION;
        }

        // readObject
        @Override
        public Color to(final RGB rgb) {
            throw TO_EXCEPTION;
        }

    }

    public static class ReturnNull implements Adapter<Color, RGB> {

        // writeObject
        @Override
        public RGB from(final Color color) {
            return null;
        }

        // readObject
        @Override
        public Color to(final RGB rgb) {
            return null;
        }
    }

    public static class Widget {
        private Color color;

        public Widget() {
        }

        public Widget(final Color color) {
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
