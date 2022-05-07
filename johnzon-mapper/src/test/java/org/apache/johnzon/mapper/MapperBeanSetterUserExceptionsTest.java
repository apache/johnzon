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

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapperBeanSetterUserExceptionsTest {

    private static final RuntimeException USER_EXCEPTION = new RuntimeException("I am user, hear me roar");

    @Test
    public void object() {
        assertException("{ \"object\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'object' of type Color cannot be mapped to json object value: {\"red\":255,\"green\":1.." +
                        ".\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.setObjec" +
                        "t(org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Color)");
    }

    @Test
    public void string() {
        assertException("{ \"string\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'string' of type String cannot be mapped to json string value: \"Supercalifragilisti." +
                        "..\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.setStri" +
                        "ng(java.lang.String)");
    }

    @Test
    public void number() {
        assertException("{ \"number\" : 122333444455555.666666777777788888888 }",
                "Widget property 'number' of type Double cannot be mapped to json numeric value: 122333444455555.6666" +
                        "...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.setNum" +
                        "ber(java.lang.Double)");
    }

    @Test
    public void intPrimitive() {
        assertException("{ \"intPrimitive\" : 42 }",
                "Widget property 'intPrimitive' of type int cannot be mapped to json numeric value: 42\nError calling " +
                        "public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.setIntPrimitive(int)");
    }

    @Test
    public void booleanValue() {
        assertException("{ \"bool\" : true }",
                "Widget property 'bool' of type Boolean cannot be mapped to json boolean value: true\nError calling pu" +
                        "blic void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.setBool(java.lang.Boolean)");
    }

    @Test
    public void boolPrimitive() {
        assertException("{ \"boolPrimitive\" : true }",
                "Widget property 'boolPrimitive' of type boolean cannot be mapped to json boolean value: true\nError c" +
                        "alling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.setBoolPrimitive(" +
                        "boolean)");
    }

    @Test
    public void enumeration() {
        assertException("{ \"unit\" : \"SECONDS\" }",
                "Widget property 'unit' of type TimeUnit cannot be mapped to json string value: \"SECONDS\"\nError calli" +
                        "ng public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.setUnit(java.util.con" +
                        "current.TimeUnit)");
    }

    @Test
    public void date() {
        assertException("{ \"date\" : \"20220503123456UTC\" }",
                "Widget property 'date' of type Date cannot be mapped to json string value: \"20220503123456UTC\"\nError" +
                        " calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.setDate(java.ut" +
                        "il.Date)");
    }

    @Test
    public void arrayOfObject() {
        assertException("{ \"arrayOfObject\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfObject' of type Color[] cannot be mapped to json array value: [{\"red\":255,\"g" +
                        "reen\":...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget." +
                        "setArrayOfObject(org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Color[])");
    }

    @Test
    public void arrayOfString() {
        assertException("{ \"arrayOfString\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'arrayOfString' of type String[] cannot be mapped to json array value: [\"Klaatu\",\"ba" +
                        "rada\",\"...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget" +
                        ".setArrayOfString(java.lang.String[])");
    }

    @Test
    public void arrayOfNumber() {
        assertException("{ \"arrayOfNumber\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'arrayOfNumber' of type Number[] cannot be mapped to json array value: [2,3,5,7,11,1" +
                        "3,17,19...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget" +
                        ".setArrayOfNumber(java.lang.Number[])");
    }

    @Test
    public void arrayOfBoolean() {
        assertException("{ \"arrayOfBoolean\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfBoolean' of type Boolean[] cannot be mapped to json array value: [true,false" +
                        ",true,tru...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widg" +
                        "et.setArrayOfBoolean(java.lang.Boolean[])");
    }

    @Test
    public void arrayOfInt() {
        assertException("{ \"arrayOfInt\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'arrayOfInt' of type int[] cannot be mapped to json array value: [2,3,5,7,11,13,17,1" +
                        "9...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.setAr" +
                        "rayOfInt(int[])");
    }

    @Test
    public void arrayOfByte() {
        assertException("{ \"arrayOfByte\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'arrayOfByte' of type byte[] cannot be mapped to json array value: [2,3,5,7,11,13,17" +
                        ",19...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.set" +
                        "ArrayOfByte(byte[])");
    }

    @Test
    public void arrayOfChar() {
        assertException("{ \"arrayOfChar\" : [\"a\",\"a\",\"a\",\"a\",\"a\",\"a\",\"a\",\"a\"] }",
                "Widget property 'arrayOfChar' of type char[] cannot be mapped to json array value: [\"a\",\"a\",\"a\",\"a\"," +
                        "\"a\"...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.set" +
                        "ArrayOfChar(char[])");
    }

    @Test
    public void arrayOfShort() {
        assertException("{ \"arrayOfShort\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'arrayOfShort' of type short[] cannot be mapped to json array value: [2,3,5,7,11,13," +
                        "17,19...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.s" +
                        "etArrayOfShort(short[])");
    }

    @Test
    public void arrayOfLong() {
        assertException("{ \"arrayOfLong\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'arrayOfLong' of type long[] cannot be mapped to json array value: [2,3,5,7,11,13,17" +
                        ",19...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.set" +
                        "ArrayOfLong(long[])");
    }

    @Test
    public void arrayOfFloat() {
        assertException("{ \"arrayOfFloat\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'arrayOfFloat' of type float[] cannot be mapped to json array value: [2,3,5,7,11,13," +
                        "17,19...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget.s" +
                        "etArrayOfFloat(float[])");
    }

    @Test
    public void arrayOfDouble() {
        assertException("{ \"arrayOfDouble\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'arrayOfDouble' of type double[] cannot be mapped to json array value: [2,3,5,7,11,1" +
                        "3,17,19...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widget" +
                        ".setArrayOfDouble(double[])");
    }

    @Test
    public void arrayOfBooleanPrimitive() {
        assertException("{ \"arrayOfBooleanPrimitive\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfBooleanPrimitive' of type boolean[] cannot be mapped to json array value: [t" +
                        "rue,false,true,tru...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptions" +
                        "Test$Widget.setArrayOfBooleanPrimitive(boolean[])");
    }

    @Test
    public void listOfObject() {
        assertException("{ \"listOfObject\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'listOfObject' of type List<Color> cannot be mapped to json array value: [{\"red\":255" +
                        ",\"green\":...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Widg" +
                        "et.setListOfObject(java.util.List)");
    }

    @Test
    public void listOfString() {
        assertException("{ \"listOfString\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'listOfString' of type List<String> cannot be mapped to json array value: [\"Klaatu\"," +
                        "\"barada\",\"...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Wid" +
                        "get.setListOfString(java.util.List)");
    }

    @Test
    public void listOfNumber() {
        assertException("{ \"listOfNumber\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'listOfNumber' of type List<Number> cannot be mapped to json array value: [2,3,5,7,1" +
                        "1,13,17,19...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$Wid" +
                        "get.setListOfNumber(java.util.List)");
    }

    @Test
    public void listOfBoolean() {
        assertException("{ \"listOfBoolean\" : [true,false,true,true,false] }",
                "Widget property 'listOfBoolean' of type List<Boolean> cannot be mapped to json array value: [true,fa" +
                        "lse,true,tru...\nError calling public void org.apache.johnzon.mapper.MapperBeanSetterUserExceptionsTest$W" +
                        "idget.setListOfBoolean(java.util.List)");
    }

    private void assertException(final String json, final String expected) {
        ExceptionAsserts.fromMapperReadObject(json, Widget.class)
                .assertInstanceOf(MapperException.class)
                .assertMessage(expected)
                .assertCauseChain(USER_EXCEPTION);
    }

    public static class Widget {
        private Color[] arrayOfObject;
        private String[] arrayOfString;
        private Number[] arrayOfNumber;
        private int[] arrayOfInt;
        private byte[] arrayOfByte;
        private char[] arrayOfChar;
        private short[] arrayOfShort;
        private long[] arrayOfLong;
        private float[] arrayOfFloat;
        private double[] arrayOfDouble;
        private Boolean[] arrayOfBoolean;
        private boolean[] arrayOfBooleanPrimitive;
        private List<Color> listOfObject;
        private List<String> listOfString;
        private List<Number> listOfNumber;
        private List<Boolean> listOfBoolean;
        private Color object;
        private String string;
        private Double number;
        private int intPrimitive;
        private Boolean bool;
        private boolean boolPrimitive;
        private Date date;
        private TimeUnit unit;

        public Color[] getArrayOfObject() {
            return arrayOfObject;
        }

        public void setArrayOfObject(final Color[] arrayOfObject) {
            throw USER_EXCEPTION;
        }

        public String[] getArrayOfString() {
            return arrayOfString;
        }

        public void setArrayOfString(final String[] arrayOfString) {
            throw USER_EXCEPTION;
        }

        public Number[] getArrayOfNumber() {
            return arrayOfNumber;
        }

        public void setArrayOfNumber(final Number[] arrayOfNumber) {
            throw USER_EXCEPTION;
        }

        public int[] getArrayOfInt() {
            return arrayOfInt;
        }

        public void setArrayOfInt(final int[] arrayOfint) {
            throw USER_EXCEPTION;
        }

        public Boolean[] getArrayOfBoolean() {
            return arrayOfBoolean;
        }

        public void setArrayOfBoolean(final Boolean[] arrayOfBoolean) {
            throw USER_EXCEPTION;
        }

        public boolean[] getArrayOfBooleanPrimitive() {
            return arrayOfBooleanPrimitive;
        }

        public void setArrayOfBooleanPrimitive(final boolean[] arrayOfBooleanPrimitive) {
            throw USER_EXCEPTION;
        }

        public Color getObject() {
            return object;
        }

        public void setObject(final Color object) {
            throw USER_EXCEPTION;
        }

        public String getString() {
            return string;
        }

        public void setString(final String string) {
            throw USER_EXCEPTION;
        }

        public Double getNumber() {
            return number;
        }

        public void setNumber(final Double number) {
            throw USER_EXCEPTION;
        }

        public int getIntPrimitive() {
            return intPrimitive;
        }

        public void setIntPrimitive(final int intPrimitive) {
            throw USER_EXCEPTION;
        }

        public Boolean getBool() {
            return bool;
        }

        public void setBool(final Boolean bool) {
            throw USER_EXCEPTION;
        }

        public boolean isBoolPrimitive() {
            return boolPrimitive;
        }

        public void setBoolPrimitive(final boolean boolPrimitive) {
            throw USER_EXCEPTION;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(final Date date) {
            throw USER_EXCEPTION;
        }

        public TimeUnit getUnit() {
            return unit;
        }

        public void setUnit(final TimeUnit unit) {
            throw USER_EXCEPTION;
        }

        public byte[] getArrayOfByte() {
            return arrayOfByte;
        }

        public void setArrayOfByte(final byte[] arrayOfByte) {
            throw USER_EXCEPTION;
        }

        public char[] getArrayOfChar() {
            return arrayOfChar;
        }

        public void setArrayOfChar(final char[] arrayOfChar) {
            throw USER_EXCEPTION;
        }

        public short[] getArrayOfShort() {
            return arrayOfShort;
        }

        public void setArrayOfShort(final short[] arrayOfShort) {
            throw USER_EXCEPTION;
        }

        public long[] getArrayOfLong() {
            return arrayOfLong;
        }

        public void setArrayOfLong(final long[] arrayOfLong) {
            throw USER_EXCEPTION;
        }

        public float[] getArrayOfFloat() {
            return arrayOfFloat;
        }

        public void setArrayOfFloat(final float[] arrayOfFloat) {
            throw USER_EXCEPTION;
        }

        public double[] getArrayOfDouble() {
            return arrayOfDouble;
        }

        public void setArrayOfDouble(final double[] arrayOfDouble) {
            throw USER_EXCEPTION;
        }

        public List<Color> getListOfObject() {
            return listOfObject;
        }

        public void setListOfObject(final List<Color> listOfObject) {
            throw USER_EXCEPTION;
        }

        public List<String> getListOfString() {
            return listOfString;
        }

        public void setListOfString(final List<String> listOfString) {
            throw USER_EXCEPTION;
        }

        public List<Number> getListOfNumber() {
            return listOfNumber;
        }

        public void setListOfNumber(final List<Number> listOfNumber) {
            throw USER_EXCEPTION;
        }

        public List<Boolean> getListOfBoolean() {
            return listOfBoolean;
        }

        public void setListOfBoolean(final List<Boolean> listOfBoolean) {
            throw USER_EXCEPTION;
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
