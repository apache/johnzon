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

public class MapperConverterExceptionsTest {

    private static final RuntimeException FROM_STRING_EXCEPTION = new RuntimeException("I am user, hear me roar");
    private static final RuntimeException TO_STRING_EXCEPTION = new RuntimeException("I am user, hear me roar");
    private static final RuntimeException CONSTRUCTOR_EXCEPTION = new RuntimeException("I am user, hear me roar");

    @Test
    public void failedConstructorWriteObject() {
        final Object object = new WidgetWithFailedConstructorConverter(new Color("orange"));
        
        ExceptionAsserts.fromMapperWriteObject(object)
                .assertCauseChain(CONSTRUCTOR_EXCEPTION)
                // TODO wrapped, but not wrapped with MapperException
                .assertInstanceOf(IllegalArgumentException.class)
                .assertMessage("java.lang.RuntimeException: I am user, hear me roar");
    }

    @Test
    public void failedConstructorReadObject() {
        final String json = "{\"color\": \"Supercalifragilisticexpialidocious\" }";
        ExceptionAsserts.fromMapperReadObject(json, WidgetWithFailedConstructorConverter.class)
                .assertCauseChain(CONSTRUCTOR_EXCEPTION)
                // TODO Review: wrapped, but not wrapped with MapperException and no json is printed
                .assertInstanceOf(IllegalArgumentException.class)
                .assertMessage("java.lang.RuntimeException: I am user, hear me roar");
    }

    @Test
    public void failedToString() {
        final Object object = new WidgetWithFailedConverter(new Color("orange"));

        ExceptionAsserts.fromMapperWriteObject(object)
                // TODO Review
                .assertSame(TO_STRING_EXCEPTION);
    }

    @Test
    public void failedFromString() {
        final String json = "{\"color\": \"Supercalifragilisticexpialidocious\" }";

        ExceptionAsserts.fromMapperReadObject(json, WidgetWithFailedConverter.class)
                .assertCauseChain(FROM_STRING_EXCEPTION)
                .assertInstanceOf(MapperException.class)
                .assertMessage("WidgetWithFailedConverter property 'color' of type Color cannot be " +
                        "mapped to json string value: \"Supercalifragilisti...\n" +
                        "I am user, hear me roar");
    }

    @Test
    public void nullToString() {
        final Object object = new WidgetWithNullConverter(new Color("orange"));

        ExceptionAsserts.fromMapperWriteObject(object)
                // TODO Review: fromString can return null, but toString cannot
                .assertInstanceOf(NullPointerException.class);
    }

    @Test
    public void nullFromString() throws Exception {
        final String json = "{\"color\": \"Supercalifragilisticexpialidocious\" }";


        final Callable<WidgetWithNullConverter> fromString = () -> {
            try (final Mapper mapper = new MapperBuilder().setSnippetMaxLength(20).build()) {
                return mapper.readObject(json, WidgetWithNullConverter.class);
            }
        };

        final WidgetWithNullConverter widget = fromString.call();

        assertNotNull(widget);
        assertNull(widget.getColor());
    }

    public static class NullConverter implements Converter<Color> {
        @Override
        public String toString(final Color instance) {
            return null;
        }

        @Override
        public Color fromString(final String text) {
            return null;
        }
    }

    public static class FailedConverter implements Converter<Color> {
        @Override
        public String toString(final Color instance) {
            throw TO_STRING_EXCEPTION;
        }

        @Override
        public Color fromString(final String text) {
            throw FROM_STRING_EXCEPTION;
        }
    }

    public static class FailedConstructorConverter implements Converter<Color> {

        public FailedConstructorConverter() {
            throw CONSTRUCTOR_EXCEPTION;
        }

        @Override
        public String toString(final Color instance) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Color fromString(final String text) {
            throw new UnsupportedOperationException();
        }
    }

    public static class WidgetWithFailedConstructorConverter {

        @JohnzonConverter(FailedConstructorConverter.class)
        private Color color;

        public WidgetWithFailedConstructorConverter() {
        }

        public WidgetWithFailedConstructorConverter(final Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(final Color color) {
            this.color = color;
        }
    }

    public static class WidgetWithNullConverter {

        @JohnzonConverter(NullConverter.class)
        private Color color;

        public WidgetWithNullConverter() {
        }

        public WidgetWithNullConverter(final Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(final Color color) {
            this.color = color;
        }
    }

    public static class WidgetWithFailedConverter {

        @JohnzonConverter(FailedConverter.class)
        private Color color;

        public WidgetWithFailedConverter() {
        }

        public WidgetWithFailedConverter(final Color color) {
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
}
