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

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.JsonbException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class DeserializationUserExceptionMessagesTest {

    @Test
    public void objectFromObject() throws Exception {
        assertMessage("{ \"object\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Error calling public void org.apache.johnzon.jsonb.DeserializationUserExceptionsTest$Widget.setObjec" +
                        "t(org.apache.johnzon.jsonb.DeserializationUserExceptionsTest$Color)");
    }

    @Test
    public void stringFromString() throws Exception {
        assertMessage("{ \"string\" : \"Supercalifragilisticexpialidocious\" }",
                "Error calling public void org.apache.johnzon.jsonb.DeserializationUserExceptionsTest$Widget.setStrin" +
                        "g(java.lang.String)");
    }

    @Test
    public void numberFromNumber() throws Exception {
        assertMessage("{ \"number\" : 122333444455555.666666777777788888888 }",
                "Not an int/long, use other value readers");
    }

    @Test
    public void booleanFromBoolean() throws Exception {
        assertMessage("{ \"bool\" : true }",
                "Error calling public void org.apache.johnzon.jsonb.DeserializationUserExceptionsTest$Widget.setBool(" +
                        "java.lang.Boolean)");
    }

    @Test
    public void arrayOfObjectFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfObject\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Error calling public void org.apache.johnzon.jsonb.DeserializationUserExceptionsTest$Widget.setArray" +
                        "OfObject(org.apache.johnzon.jsonb.DeserializationUserExceptionsTest$Color[])");
    }

    @Test
    public void arrayOfStringFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfString\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Error calling public void org.apache.johnzon.jsonb.DeserializationUserExceptionsTest$Widget.setArray" +
                        "OfString(java.lang.String[])");
    }

    @Test
    public void arrayOfNumberFromArrayOfNumber() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Error calling public void org.apache.johnzon.jsonb.DeserializationUserExceptionsTest$Widget.setArray" +
                        "OfNumber(java.lang.Number[])");
    }

    @Test
    public void arrayOfBooleanFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : [true,false,true,true,false] }",
                "Error calling public void org.apache.johnzon.jsonb.DeserializationUserExceptionsTest$Widget.setArray" +
                        "OfBoolean(java.lang.Boolean[])");
    }

    private void assertMessage(final String json, final String expected) throws Exception {

        final String message = getExceptionMessage(json);
        assertEquals(expected, message);
    }

    public static String getExceptionMessage(final String json) {
        final JsonbConfig config = new JsonbConfig();
        config.setProperty("johnzon.snippetMaxLength", 20);

        try (final Jsonb jsonb = JsonbBuilder.create(config)) {
            final Widget widget = jsonb.fromJson(json, Widget.class);

            throw new AssertionError("No exception occured");
        } catch (JsonbException e) {
            return e.getMessage();
        } catch (AssertionError assertionError) {
            throw assertionError;
        } catch (Exception e) {
            throw new AssertionError("Unexpected failure", e);
        }
    }


    public static class Widget {
        private Color[] arrayOfObject;
        private String[] arrayOfString;
        private Number[] arrayOfNumber;
        private int[] arrayOfint;
        private Boolean[] arrayOfBoolean;
        private boolean[] arrayOfBooleanPrimitive;
        private Color object;
        private String string;
        private Integer number;
        private int intPrimitive;
        private Boolean bool;
        private boolean boolPrimitive;
        private Date date;
        private TimeUnit unit;

        public Color[] getArrayOfObject() {
            return arrayOfObject;
        }

        public void setArrayOfObject(final Color[] arrayOfObject) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public String[] getArrayOfString() {
            return arrayOfString;
        }

        public void setArrayOfString(final String[] arrayOfString) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public Number[] getArrayOfNumber() {
            return arrayOfNumber;
        }

        public void setArrayOfNumber(final Number[] arrayOfNumber) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public int[] getArrayOfint() {
            return arrayOfint;
        }

        public void setArrayOfint(final int[] arrayOfint) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public Boolean[] getArrayOfBoolean() {
            return arrayOfBoolean;
        }

        public void setArrayOfBoolean(final Boolean[] arrayOfBoolean) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public boolean[] getArrayOfBooleanPrimitive() {
            return arrayOfBooleanPrimitive;
        }

        public void setArrayOfBooleanPrimitive(final boolean[] arrayOfBooleanPrimitive) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public Color getObject() {
            return object;
        }

        public void setObject(final Color object) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public String getString() {
            return string;
        }

        public void setString(final String string) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public Integer getNumber() {
            return number;
        }

        public void setNumber(final Integer number) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public int getIntPrimitive() {
            return intPrimitive;
        }

        public void setIntPrimitive(final int intPrimitive) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public Boolean getBool() {
            return bool;
        }

        public void setBool(final Boolean bool) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public boolean isBoolPrimitive() {
            return boolPrimitive;
        }

        public void setBoolPrimitive(final boolean boolPrimitive) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public Date getDate() {
            return date;
        }

        public void setDate(final Date date) {
            throw new RuntimeException("I am user, hear me roar");
        }

        public TimeUnit getUnit() {
            return unit;
        }

        public void setUnit(final TimeUnit unit) {
            throw new RuntimeException("I am user, hear me roar");
        }
    }

    public static class Color {
        int red;
        int green;
        int blue;

        public Color() {
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
