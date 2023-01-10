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

import jakarta.json.bind.JsonbException;
import java.beans.ConstructorProperties;
import java.lang.reflect.Type;

public class JsonbBeanConstructorExceptionsTest {

    private static final RuntimeException USER_EXCEPTION = new RuntimeException("I am user, hear me roar");

    @Test
    public void constructor() {
        ExceptionAsserts.fromJson("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", Circle.class)
                .assertInstanceOf(JsonbException.class)
                .assertCauseChain(USER_EXCEPTION)
                .assertMessage("Circle cannot be constructed to deserialize json object value: {\"string\":\"Supercali...\n" +
                        "java.lang.RuntimeException: I am user, hear me roar");
    }

    @Test
    public void constructorParametersWithNoAnnotations() {
        ExceptionAsserts.fromJson("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", Square.class)
                .assertInstanceOf(JsonbException.class)
                .assertMessage("Square has no suitable constructor or factory.  Cannot deserialize json object value: {\"string\":\"Supercali...\n" +
                        "Use Johnzon @ConstructorProperties or @JsonbCreator if constructor arguments are needed\n" +
                        "class org.apache.johnzon.jsonb.JsonbBeanConstructorExceptionsTest$Square not instantiable");
    }

    @Test
    public void constructorWithGenerics() {

        final Type type = new Oval<String>(true) {
        }.getClass().getGenericSuperclass();

        ExceptionAsserts.fromJson("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", type)
                .assertInstanceOf(JsonbException.class)
                .assertCauseChain(USER_EXCEPTION)
                .assertMessage("Oval<String> cannot be constructed to deserialize json object value: {\"string\":\"Supercali...\n" +
                        "java.lang.RuntimeException: I am user, hear me roar");
    }

    @Test
    public void constructorProperties() {
        ExceptionAsserts.fromJson("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", Triangle.class)
                .assertInstanceOf(JsonbException.class)
                .assertCauseChain(USER_EXCEPTION)
                .assertMessage("Triangle cannot be constructed to deserialize json object value: {\"string\":\"Supercali...\n" +
                        "java.lang.RuntimeException: I am user, hear me roar");
    }

    @Test
    public void noConstructors() {
        ExceptionAsserts.fromJson("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", Sphere.class)
                .assertInstanceOf(JsonbException.class)
                .assertMessage("Sphere is an interface and requires an adapter or factory.  Cannot deserialize json object value: {\"string\":\"Supercali...\n" +
                        "interface org.apache.johnzon.jsonb.JsonbBeanConstructorExceptionsTest$Sphere not instantiable");
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


}
