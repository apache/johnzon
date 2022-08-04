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

import java.beans.ConstructorProperties;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MapperBeanConstructorExceptionsTest {

    private static final RuntimeException USER_EXCEPTION = new RuntimeException("I am user, hear me roar");

    @Test
    public void singleExceptionMapperInCause() {
        try (final Mapper mapper = new MapperBuilder().setSnippetMaxLength(20).build()) {
            mapper.readObject("{ \"string\" : \"whatever\" }", Rectangle.class);
            fail("should have failed");
        } catch (final MapperException me) {
            final Collection<Throwable> exceptionStack = stream(spliteratorUnknownSize(new Iterator<Throwable>() {
                private Throwable current = me;

                @Override
                public boolean hasNext() {
                    return current != null;
                }

                @Override
                public Throwable next() {
                    final Throwable throwable = current;
                    current = current.getCause() == current ? null : current.getCause();
                    return throwable;
                }
            }, Spliterator.IMMUTABLE), false).collect(toList());
            assertEquals(2, exceptionStack.size());
            assertEquals(1, exceptionStack.stream().filter(MapperException.class::isInstance).count());
        }
    }

    @Test
    public void constructor() {
        ExceptionAsserts.fromMapperReadObject("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", Circle.class)
                .assertInstanceOf(MapperException.class)
                .assertCauseChain(USER_EXCEPTION)
                .assertMessage("Circle cannot be constructed to deserialize json object value: {\"string\":\"Supercali...\n" +
                        "java.lang.RuntimeException: I am user, hear me roar");
    }

    @Test
    public void constructorParametersWithNoAnnotations() {
        ExceptionAsserts.fromMapperReadObject("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", Square.class)
                .assertInstanceOf(MapperException.class)
                .assertMessage("Square has no suitable constructor or factory.  Cannot deserialize json object value: {\"string\":\"Supercali...\n" +
                        "Use Johnzon @ConstructorProperties or @JsonbCreator if constructor arguments are needed\n" +
                        "class org.apache.johnzon.mapper.MapperBeanConstructorExceptionsTest$Square not instantiable");
    }

    @Test
    public void constructorWithGenerics() {

        final Type type = new Oval<String>(true) {
        }.getClass().getGenericSuperclass();

        ExceptionAsserts.fromMapperReadObject("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", type)
                .assertInstanceOf(MapperException.class)
                .assertCauseChain(USER_EXCEPTION)
                .assertMessage("Oval<String> cannot be constructed to deserialize json object value: {\"string\":\"Supercali...\n" +
                        "java.lang.RuntimeException: I am user, hear me roar");
    }

    @Test
    public void constructorProperties() {
        ExceptionAsserts.fromMapperReadObject("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", Triangle.class)
                .assertInstanceOf(MapperException.class)
                .assertCauseChain(USER_EXCEPTION)
                .assertMessage("Triangle cannot be constructed to deserialize json object value: {\"string\":\"Supercali...\n" +
                        "java.lang.RuntimeException: I am user, hear me roar");
    }

    @Test
    public void noConstructors() {
        ExceptionAsserts.fromMapperReadObject("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", Sphere.class)
                .assertInstanceOf(MapperException.class)
                .assertMessage("Sphere is an interface and requires an adapter or factory.  Cannot deserialize json object value: {\"string\":\"Supercali...\n" +
                        "interface org.apache.johnzon.mapper.MapperBeanConstructorExceptionsTest$Sphere not instantiable");
    }

    public static class Circle {
        private String string;

        public Circle() {
            throw USER_EXCEPTION;
        }

        public String getString() {
            return string;
        }

        public void setString(final String string) {
            this.string = string;
        }
    }

    public static class Square {
        private String string;

        public Square(final String string) {
            throw USER_EXCEPTION;
        }

        public String getString() {
            return string;
        }

        public void setString(final String string) {
            this.string = string;
        }
    }

    public static class Rectangle {
        private String string;

        @ConstructorProperties({"string"})
        public Rectangle(@JohnzonConverter(FailingConverter.class) final String string) {
            fail("shouldn't be reached");
        }

        public String getString() {
            return string;
        }

        public void setString(final String string) {
            this.string = string;
        }
    }

    public static class Oval<T> {
        private String s;

        public Oval() {
            throw USER_EXCEPTION;
        }

        public Oval(final boolean ignored) {
        }
    }

    public static class Triangle {
        private String string;

        @ConstructorProperties("string")
        public Triangle(final String string) {
            throw USER_EXCEPTION;
        }
    }

    public interface Sphere {
    }

    public static class FailingConverter<T> implements Converter<T> {
        @Override
        public String toString(final T instance) {
            throw USER_EXCEPTION;
        }

        @Override
        public T fromString(final String text) {
            throw USER_EXCEPTION;
        }
    }
}
