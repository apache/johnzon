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
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

public class JsonbCreatorExceptionsTest {

    private static final RuntimeException CONSTRUCTOR_EXCEPTION = new RuntimeException("I am user, hear me roar");
    private static final RuntimeException FACTORY_EXCEPTION = new RuntimeException("I am user, hear me roar");

    @Test
    public void constructor() {
        ExceptionAsserts.fromJson("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", Circle.class)
                .assertInstanceOf(JsonbException.class)
                .assertCauseChain(CONSTRUCTOR_EXCEPTION)
                .assertMessage("Circle cannot be constructed to deserialize json object value: {\"string\":\"Supercali...\n" +
                        "java.lang.RuntimeException: I am user, hear me roar");
    }


    @Test
    public void factory() {
        ExceptionAsserts.fromJson("{ \"string\" : \"Supercalifragilisticexpialidocious\" }", Square.class)
                .assertInstanceOf(JsonbException.class)
                .assertCauseChain(FACTORY_EXCEPTION)
                .assertMessage("Square cannot be constructed to deserialize json object value: {\"string\":\"Supercali...\n" +
                        "java.lang.RuntimeException: I am user, hear me roar");
    }

    public static class Circle {
        private String string;

        @JsonbCreator
        public Circle(@JsonbProperty("string") final String string) {
            throw CONSTRUCTOR_EXCEPTION;
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

        public String getString() {
            return string;
        }

        public void setString(final String string) {
            this.string = string;
        }

        @JsonbCreator
        public static Square square(@JsonbProperty("string") final String string) {
            throw FACTORY_EXCEPTION;
        }
    }
}
