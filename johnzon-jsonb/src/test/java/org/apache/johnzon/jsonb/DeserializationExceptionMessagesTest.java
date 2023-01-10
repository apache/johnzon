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
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeserializationExceptionMessagesTest {


    @Test
    public void objectFromString() throws Exception {
        assertMessage("{ \"object\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'object' of type Color cannot be mapped to json string value: \"Supercalifragilisti.." +
                        ".\nMissing a Converter for type class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$C" +
                        "olor to convert the JSON String 'Supercalifragilisticexpialidocious' . Please register a custom conv" +
                        "erter for it.");
    }

    @Test
    public void objectFromNumber() throws Exception {
        assertMessage("{ \"object\" : 122333444455555.666666777777788888888 }",
                "Widget property 'object' of type Color cannot be mapped to json numeric value: 122333444455555.6666." +
                        "..\nUnable to parse json numeric value to class org.apache.johnzon.jsonb.DeserializationExceptionMess" +
                        "agesTest$Color: 122333444455555.6666...");
    }

    @Test
    public void objectFromBoolean() throws Exception {
        assertMessage("{ \"object\" : true }",
                "Widget property 'object' of type Color cannot be mapped to json boolean value: true\nUnable to parse " +
                        "json boolean value to class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color: tru" +
                        "e");
    }

    @Test
    public void objectFromArrayOfObject() throws Exception {
        assertMessage("{ \"object\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'object' of type Color cannot be mapped to json array value: [{\"red\":255,\"green\":..." +
                        "\ntype class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color not supported");
    }

    @Test
    public void objectFromArrayOfString() throws Exception {
        assertMessage("{ \"object\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'object' of type Color cannot be mapped to json array value: [\"Klaatu\",\"barada\",\"..." +
                        "\ntype class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color not supported");
    }

    @Test
    public void objectFromArrayOfNumber() throws Exception {
        assertMessage("{ \"object\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'object' of type Color cannot be mapped to json array value: [2,3,5,7,11,13,17,19..." +
                        "\ntype class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color not supported");
    }

    @Test
    public void objectFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"object\" : [true,false,true,true,false] }",
                "Widget property 'object' of type Color cannot be mapped to json array value: [true,false,true,tru..." +
                        "\ntype class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color not supported");
    }

    @Test
    public void stringFromArrayOfObject() throws Exception {
        assertMessage("{ \"string\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'string' of type String cannot be mapped to json array value: [{\"red\":255,\"green\":.." +
                        ".\nclass java.lang.String does not support json array value: [{\"red\":255,\"green\":...");
    }

    @Test
    public void stringFromArrayOfString() throws Exception {
        assertMessage("{ \"string\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'string' of type String cannot be mapped to json array value: [\"Klaatu\",\"barada\",\".." +
                        ".\nclass java.lang.String does not support json array value: [\"Klaatu\",\"barada\",\"...");
    }

    @Test
    public void stringFromArrayOfNumber() throws Exception {
        assertMessage("{ \"string\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'string' of type String cannot be mapped to json array value: [2,3,5,7,11,13,17,19.." +
                        ".\nclass java.lang.String does not support json array value: [2,3,5,7,11,13,17,19...");
    }

    @Test
    public void stringFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"string\" : [true,false,true,true,false] }",
                "Widget property 'string' of type String cannot be mapped to json array value: [true,false,true,tru.." +
                        ".\nclass java.lang.String does not support json array value: [true,false,true,tru...");
    }

    @Test
    public void numberFromObject() throws Exception {
        assertMessage("{ \"number\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'number' of type Integer cannot be mapped to json object value: {\"red\":255,\"green\":1" +
                        "...\nUnable to map json object value to class java.lang.Integer: {\"red\":255,\"green\":1...");
    }

    @Test
    public void numberFromString() throws Exception {
        assertMessage("{ \"number\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'number' of type Integer cannot be mapped to json string value: \"Supercalifragilisti" +
                        "...\nFor input string: \"Supercalifragilisticexpialidocious\"");
    }

    @Test
    public void numberFromBoolean() throws Exception {
        assertMessage("{ \"number\" : true }",
                "Widget property 'number' of type Integer cannot be mapped to json boolean value: true\nUnable to pars" +
                        "e json boolean value to class java.lang.Integer: true");
    }

    @Test
    public void numberFromArrayOfObject() throws Exception {
        assertMessage("{ \"number\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'number' of type Integer cannot be mapped to json array value: [{\"red\":255,\"green\":." +
                        "..\ntype class java.lang.Integer not supported");
    }

    @Test
    public void numberFromArrayOfString() throws Exception {
        assertMessage("{ \"number\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'number' of type Integer cannot be mapped to json array value: [\"Klaatu\",\"barada\",\"." +
                        "..\ntype class java.lang.Integer not supported");
    }

    @Test
    public void numberFromArrayOfNumber() throws Exception {
        assertMessage("{ \"number\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'number' of type Integer cannot be mapped to json array value: [2,3,5,7,11,13,17,19." +
                        "..\ntype class java.lang.Integer not supported");
    }

    @Test
    public void numberFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"number\" : [true,false,true,true,false] }",
                "Widget property 'number' of type Integer cannot be mapped to json array value: [true,false,true,tru." +
                        "..\ntype class java.lang.Integer not supported");
    }

    @Test
    public void intPrimitiveFromObject() throws Exception {
        assertMessage("{ \"intPrimitive\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'intPrimitive' of type int cannot be mapped to json object value: {\"red\":255,\"green\"" +
                        ":1...\nUnable to map json object value to int: {\"red\":255,\"green\":1...");
    }

    @Test
    public void intPrimitiveFromString() throws Exception {
        assertMessage("{ \"intPrimitive\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'intPrimitive' of type int cannot be mapped to json string value: \"Supercalifragilis" +
                        "ti...\nMissing a Converter for type int to convert the JSON String 'Supercalifragilisticexpialidociou" +
                        "s' . Please register a custom converter for it.");
    }

    @Test
    public void intPrimitiveFromNumber() throws Exception {
        assertMessage("{ \"intPrimitive\" : 122333444455555.666666777777788888888 }",
                "Widget property 'intPrimitive' of type int cannot be mapped to json numeric value: 122333444455555.6" +
                        "666...\nNot an int/long, use other value readers");
    }

    @Test
    public void intPrimitiveFromBoolean() throws Exception {
        assertMessage("{ \"intPrimitive\" : true }",
                "Widget property 'intPrimitive' of type int cannot be mapped to json boolean value: true\nUnable to pa" +
                        "rse json boolean value to int: true");
    }

    @Test
    public void intPrimitiveFromNull() throws Exception {
        assertMessage("{ \"intPrimitive\" : null }",
                "Widget property 'intPrimitive' of type int cannot be mapped to json value: null\nError calling public" +
                        " void org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Widget.setIntPrimitive(int)");
    }

    @Test
    public void intPrimitiveFromArrayOfObject() throws Exception {
        assertMessage("{ \"intPrimitive\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'intPrimitive' of type int cannot be mapped to json array value: [{\"red\":255,\"green\"" +
                        ":...\ntype int not supported");
    }

    @Test
    public void intPrimitiveFromArrayOfString() throws Exception {
        assertMessage("{ \"intPrimitive\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'intPrimitive' of type int cannot be mapped to json array value: [\"Klaatu\",\"barada\"," +
                        "\"...\ntype int not supported");
    }

    @Test
    public void intPrimitiveFromArrayOfNumber() throws Exception {
        assertMessage("{ \"intPrimitive\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'intPrimitive' of type int cannot be mapped to json array value: [2,3,5,7,11,13,17,1" +
                        "9...\ntype int not supported");
    }

    @Test
    public void intPrimitiveFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"intPrimitive\" : [true,false,true,true,false] }",
                "Widget property 'intPrimitive' of type int cannot be mapped to json array value: [true,false,true,tr" +
                        "u...\ntype int not supported");
    }

    @Test
    public void booleanFromObject() throws Exception {
        assertMessage("{ \"bool\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'bool' of type Boolean cannot be mapped to json object value: {\"red\":255,\"green\":1.." +
                        ".\nUnable to parse json object value to boolean: {\"red\":255,\"green\":1...");
    }

    @Test
    public void booleanFromString() throws Exception {
        assertMessage("{ \"bool\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'bool' of type Boolean cannot be mapped to json string value: \"Supercalifragilisti.." +
                        ".\nUnable to parse json string value to boolean: \"Supercalifragilisti...");
    }

    @Test
    public void booleanFromNumber() throws Exception {
        assertMessage("{ \"bool\" : 122333444455555.666666777777788888888 }",
                "Widget property 'bool' of type Boolean cannot be mapped to json numeric value: 122333444455555.6666." +
                        "..\nUnable to parse json numeric value to boolean: 122333444455555.6666...");
    }

    @Test
    public void booleanFromArrayOfObject() throws Exception {
        assertMessage("{ \"bool\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'bool' of type Boolean cannot be mapped to json array value: [{\"red\":255,\"green\":..." +
                        "\nUnable to parse json array value to boolean: [{\"red\":255,\"green\":...");
    }

    @Test
    public void booleanFromArrayOfString() throws Exception {
        assertMessage("{ \"bool\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'bool' of type Boolean cannot be mapped to json array value: [\"Klaatu\",\"barada\",\"..." +
                        "\nUnable to parse json array value to boolean: [\"Klaatu\",\"barada\",\"...");
    }

    @Test
    public void booleanFromArrayOfNumber() throws Exception {
        assertMessage("{ \"bool\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'bool' of type Boolean cannot be mapped to json array value: [2,3,5,7,11,13,17,19..." +
                        "\nUnable to parse json array value to boolean: [2,3,5,7,11,13,17,19...");
    }

    @Test
    public void booleanFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"bool\" : [true,false,true,true,false] }",
                "Widget property 'bool' of type Boolean cannot be mapped to json array value: [true,false,true,tru..." +
                        "\nUnable to parse json array value to boolean: [true,false,true,tru...");
    }

    @Test
    public void boolPrimitiveFromObject() throws Exception {
        assertMessage("{ \"boolPrimitive\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'boolPrimitive' of type boolean cannot be mapped to json object value: {\"red\":255,\"g" +
                        "reen\":1...\nUnable to parse json object value to boolean: {\"red\":255,\"green\":1...");
    }

    @Test
    public void boolPrimitiveFromString() throws Exception {
        assertMessage("{ \"boolPrimitive\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'boolPrimitive' of type boolean cannot be mapped to json string value: \"Supercalifra" +
                        "gilisti...\nUnable to parse json string value to boolean: \"Supercalifragilisti...");
    }

    @Test
    public void boolPrimitiveFromNumber() throws Exception {
        assertMessage("{ \"boolPrimitive\" : 122333444455555.666666777777788888888 }",
                "Widget property 'boolPrimitive' of type boolean cannot be mapped to json numeric value: 122333444455" +
                        "555.6666...\nUnable to parse json numeric value to boolean: 122333444455555.6666...");
    }

    @Test
    public void boolPrimitiveFromNull() throws Exception {
        assertMessage("{ \"boolPrimitive\" : null }",
                "Widget property 'boolPrimitive' of type boolean cannot be mapped to json value: null\nError calling p" +
                        "ublic void org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Widget.setBoolPrimitive(boo" +
                        "lean)");
    }

    @Test
    public void boolPrimitiveFromArrayOfObject() throws Exception {
        assertMessage("{ \"boolPrimitive\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'boolPrimitive' of type boolean cannot be mapped to json array value: [{\"red\":255,\"g" +
                        "reen\":...\nUnable to parse json array value to boolean: [{\"red\":255,\"green\":...");
    }

    @Test
    public void boolPrimitiveFromArrayOfString() throws Exception {
        assertMessage("{ \"boolPrimitive\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'boolPrimitive' of type boolean cannot be mapped to json array value: [\"Klaatu\",\"bar" +
                        "ada\",\"...\nUnable to parse json array value to boolean: [\"Klaatu\",\"barada\",\"...");
    }

    @Test
    public void boolPrimitiveFromArrayOfNumber() throws Exception {
        assertMessage("{ \"boolPrimitive\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'boolPrimitive' of type boolean cannot be mapped to json array value: [2,3,5,7,11,13" +
                        ",17,19...\nUnable to parse json array value to boolean: [2,3,5,7,11,13,17,19...");
    }

    @Test
    public void boolPrimitiveFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"boolPrimitive\" : [true,false,true,true,false] }",
                "Widget property 'boolPrimitive' of type boolean cannot be mapped to json array value: [true,false,tr" +
                        "ue,tru...\nUnable to parse json array value to boolean: [true,false,true,tru...");
    }

    @Test
    public void enumFromObject() throws Exception {
        assertMessage("{ \"unit\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'unit' of type TimeUnit cannot be mapped to json object value: {\"red\":255,\"green\":1." +
                        "..\nUnable to map json object value to class java.lang.String: {\"red\":255,\"green\":1...");
    }

    @Test
    public void enumFromString() throws Exception {
        assertMessage("{ \"unit\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'unit' of type TimeUnit cannot be mapped to json string value: \"Supercalifragilisti." +
                        "..\nIllegal class java.util.concurrent.TimeUnit enum value: Supercalifragilisticexpialidocious, known" +
                        " values: [MILLISECONDS, MICROSECONDS, HOURS, SECONDS, NANOSECONDS, DAYS, MINUTES]");
    }

    @Test
    public void enumFromNumber() throws Exception {
        assertMessage("{ \"unit\" : 122333444455555.666666777777788888888 }",
                "Widget property 'unit' of type TimeUnit cannot be mapped to json numeric value: 122333444455555.6666" +
                        "...\nIllegal class java.util.concurrent.TimeUnit enum value: 122333444455555.666666777777788888888, k" +
                        "nown values: [MILLISECONDS, MICROSECONDS, HOURS, SECONDS, NANOSECONDS, DAYS, MINUTES]");
    }

    @Test
    public void enumFromBoolean() throws Exception {
        assertMessage("{ \"unit\" : true }",
                "Widget property 'unit' of type TimeUnit cannot be mapped to json boolean value: true\nIllegal class j" +
                        "ava.util.concurrent.TimeUnit enum value: true, known values: [MILLISECONDS, MICROSECONDS, HOURS, SEC" +
                        "ONDS, NANOSECONDS, DAYS, MINUTES]");
    }

    @Test
    public void enumFromArrayOfObject() throws Exception {
        assertMessage("{ \"unit\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'unit' of type TimeUnit cannot be mapped to json array value: [{\"red\":255,\"green\":.." +
                        ".\nclass java.lang.String does not support json array value: [{\"red\":255,\"green\":...");
    }

    @Test
    public void enumFromArrayOfString() throws Exception {
        assertMessage("{ \"unit\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'unit' of type TimeUnit cannot be mapped to json array value: [\"Klaatu\",\"barada\",\".." +
                        ".\nclass java.lang.String does not support json array value: [\"Klaatu\",\"barada\",\"...");
    }

    @Test
    public void enumFromArrayOfNumber() throws Exception {
        assertMessage("{ \"unit\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'unit' of type TimeUnit cannot be mapped to json array value: [2,3,5,7,11,13,17,19.." +
                        ".\nclass java.lang.String does not support json array value: [2,3,5,7,11,13,17,19...");
    }

    @Test
    public void enumFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"unit\" : [true,false,true,true,false] }",
                "Widget property 'unit' of type TimeUnit cannot be mapped to json array value: [true,false,true,tru.." +
                        ".\nclass java.lang.String does not support json array value: [true,false,true,tru...");
    }

    @Test
    public void dateFromObject() throws Exception {
        assertMessage("{ \"date\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'date' of type Date cannot be mapped to json object value: {\"red\":255,\"green\":1...\nU" +
                        "nable to map json object value to class java.lang.String: {\"red\":255,\"green\":1...");
    }

    @Test
    public void dateFromString() throws Exception {
        assertMessage("{ \"date\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'date' of type Date cannot be mapped to json string value: \"Supercalifragilisti...\nT" +
                        "ext 'Supercalifragilisticexpialidocious' could not be parsed at index 0");
    }

    @Test
    public void dateFromNumber() throws Exception {
        assertMessage("{ \"date\" : 122333444455555.666666777777788888888 }",
                "Widget property 'date' of type Date cannot be mapped to json numeric value: 122333444455555.6666...\n" +
                        "Text '122333444455555.666666777777788888888' could not be parsed at index 0");
    }

    @Test
    public void dateFromBoolean() throws Exception {
        assertMessage("{ \"date\" : true }",
                "Widget property 'date' of type Date cannot be mapped to json boolean value: true\nText 'true' could n" +
                        "ot be parsed at index 0");
    }

    @Test
    public void dateFromArrayOfObject() throws Exception {
        assertMessage("{ \"date\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'date' of type Date cannot be mapped to json array value: [{\"red\":255,\"green\":...\ncl" +
                        "ass java.lang.String does not support json array value: [{\"red\":255,\"green\":...");
    }

    @Test
    public void dateFromArrayOfString() throws Exception {
        assertMessage("{ \"date\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'date' of type Date cannot be mapped to json array value: [\"Klaatu\",\"barada\",\"...\ncl" +
                        "ass java.lang.String does not support json array value: [\"Klaatu\",\"barada\",\"...");
    }

    @Test
    public void dateFromArrayOfNumber() throws Exception {
        assertMessage("{ \"date\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'date' of type Date cannot be mapped to json array value: [2,3,5,7,11,13,17,19...\ncl" +
                        "ass java.lang.String does not support json array value: [2,3,5,7,11,13,17,19...");
    }

    @Test
    public void dateFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"date\" : [true,false,true,true,false] }",
                "Widget property 'date' of type Date cannot be mapped to json array value: [true,false,true,tru...\ncl" +
                        "ass java.lang.String does not support json array value: [true,false,true,tru...");
    }

    @Test
    public void arrayOfObjectFromObject() throws Exception {
        assertMessage("{ \"arrayOfObject\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfObject' of type Color[] cannot be mapped to json object value: {\"red\":255,\"g" +
                        "reen\":1...\nColor[] array not a suitable datatype for json object value: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfObjectFromString() throws Exception {
        assertMessage("{ \"arrayOfObject\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfObject' of type Color[] cannot be mapped to json string value: \"Supercalifra" +
                        "gilisti...\nMissing a Converter for type class [Lorg.apache.johnzon.jsonb.DeserializationExceptionMes" +
                        "sagesTest$Color; to convert the JSON String 'Supercalifragilisticexpialidocious' . Please register a" +
                        " custom converter for it.");
    }

    @Test
    public void arrayOfObjectFromNumber() throws Exception {
        assertMessage("{ \"arrayOfObject\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfObject' of type Color[] cannot be mapped to json numeric value: 122333444455" +
                        "555.6666...\nUnable to parse json numeric value to class [Lorg.apache.johnzon.jsonb.DeserializationEx" +
                        "ceptionMessagesTest$Color;: 122333444455555.6666...");
    }

    @Test
    public void arrayOfObjectFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfObject\" : true }",
                "Widget property 'arrayOfObject' of type Color[] cannot be mapped to json boolean value: true\nUnable " +
                        "to parse json boolean value to class [Lorg.apache.johnzon.jsonb.DeserializationExceptionMessagesTest" +
                        "$Color;: true");
    }

    @Test
    public void arrayOfObjectFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfObject\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'arrayOfObject' of type Color[] cannot be mapped to json array value: [\"Klaatu\",\"bar" +
                        "ada\",\"...\nMissing a Converter for type class org.apache.johnzon.jsonb.DeserializationExceptionMessag" +
                        "esTest$Color to convert the JSON String 'Klaatu' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfObjectFromArrayOfNumber() throws Exception {
        assertMessage("{ \"arrayOfObject\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'arrayOfObject' of type Color[] cannot be mapped to json array value: [2,3,5,7,11,13" +
                        ",17,19...\nUnable to parse json numeric value to class org.apache.johnzon.jsonb.DeserializationExcept" +
                        "ionMessagesTest$Color: 2");
    }

    @Test
    public void arrayOfObjectFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfObject\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfObject' of type Color[] cannot be mapped to json array value: [true,false,tr" +
                        "ue,tru...\nUnable to parse json boolean value to class org.apache.johnzon.jsonb.DeserializationExcept" +
                        "ionMessagesTest$Color: true");
    }

    @Test
    public void arrayOfStringFromObject() throws Exception {
        assertMessage("{ \"arrayOfString\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfString' of type String[] cannot be mapped to json object value: {\"red\":255,\"" +
                        "green\":1...\nString[] array not a suitable datatype for json object value: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfStringFromString() throws Exception {
        assertMessage("{ \"arrayOfString\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfString' of type String[] cannot be mapped to json string value: \"Supercalifr" +
                        "agilisti...\nMissing a Converter for type class [Ljava.lang.String; to convert the JSON String 'Super" +
                        "califragilisticexpialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfStringFromNumber() throws Exception {
        assertMessage("{ \"arrayOfString\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfString' of type String[] cannot be mapped to json numeric value: 12233344445" +
                        "5555.6666...\nUnable to parse json numeric value to class [Ljava.lang.String;: 122333444455555.6666.." +
                        ".");
    }

    @Test
    public void arrayOfStringFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfString\" : true }",
                "Widget property 'arrayOfString' of type String[] cannot be mapped to json boolean value: true\nUnable" +
                        " to parse json boolean value to class [Ljava.lang.String;: true");
    }

    @Test
    public void arrayOfStringFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfString\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfString' of type String[] cannot be mapped to json array value: [{\"red\":255,\"" +
                        "green\":...\nUnable to map json object value to class java.lang.String: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfStringFromArrayOfNumber() throws Exception {
        assertMessage("{ \"arrayOfString\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'arrayOfString' of type String[] cannot be mapped to json array value: [2,3,5,7,11,1" +
                        "3,17,19...\nUnable to parse json numeric value to class java.lang.String: 2");
    }

    @Test
    public void arrayOfStringFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfString\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfString' of type String[] cannot be mapped to json array value: [true,false,t" +
                        "rue,tru...\nUnable to parse json boolean value to class java.lang.String: true");
    }

    @Test
    public void arrayOfNumberFromObject() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfNumber' of type Number[] cannot be mapped to json object value: {\"red\":255,\"" +
                        "green\":1...\nNumber[] array not a suitable datatype for json object value: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfNumberFromString() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfNumber' of type Number[] cannot be mapped to json string value: \"Supercalifr" +
                        "agilisti...\nMissing a Converter for type class [Ljava.lang.Number; to convert the JSON String 'Super" +
                        "califragilisticexpialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfNumberFromNumber() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfNumber' of type Number[] cannot be mapped to json numeric value: 12233344445" +
                        "5555.6666...\nUnable to parse json numeric value to class [Ljava.lang.Number;: 122333444455555.6666.." +
                        ".");
    }

    @Test
    public void arrayOfNumberFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : true }",
                "Widget property 'arrayOfNumber' of type Number[] cannot be mapped to json boolean value: true\nUnable" +
                        " to parse json boolean value to class [Ljava.lang.Number;: true");
    }

    @Test
    public void arrayOfNumberFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfNumber' of type Number[] cannot be mapped to json array value: [{\"red\":255,\"" +
                        "green\":...\nNumber cannot be constructed to deserialize json object value: {\"red\":255,\"green\":1...\nja" +
                        "va.lang.InstantiationException");
    }

    @Test
    public void arrayOfNumberFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'arrayOfNumber' of type Number[] cannot be mapped to json array value: [\"Klaatu\",\"ba" +
                        "rada\",\"...\nMissing a Converter for type class java.lang.Number to convert the JSON String 'Klaatu' ." +
                        " Please register a custom converter for it.");
    }

    @Test
    public void arrayOfNumberFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfNumber' of type Number[] cannot be mapped to json array value: [true,false,t" +
                        "rue,tru...\nUnable to parse json boolean value to class java.lang.Number: true");
    }

    @Test
    public void arrayOfBooleanFromObject() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfBoolean' of type Boolean[] cannot be mapped to json object value: {\"red\":255" +
                        ",\"green\":1...\nBoolean[] array not a suitable datatype for json object value: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfBooleanFromString() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfBoolean' of type Boolean[] cannot be mapped to json string value: \"Supercali" +
                        "fragilisti...\nMissing a Converter for type class [Ljava.lang.Boolean; to convert the JSON String 'Su" +
                        "percalifragilisticexpialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfBooleanFromNumber() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfBoolean' of type Boolean[] cannot be mapped to json numeric value: 122333444" +
                        "455555.6666...\nUnable to parse json numeric value to class [Ljava.lang.Boolean;: 122333444455555.666" +
                        "6...");
    }

    @Test
    public void arrayOfBooleanFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : true }",
                "Widget property 'arrayOfBoolean' of type Boolean[] cannot be mapped to json boolean value: true\nUnab" +
                        "le to parse json boolean value to class [Ljava.lang.Boolean;: true");
    }

    @Test
    public void arrayOfBooleanFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfBoolean' of type Boolean[] cannot be mapped to json array value: [{\"red\":255" +
                        ",\"green\":...\nUnable to parse json object value to boolean: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfBooleanFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'arrayOfBoolean' of type Boolean[] cannot be mapped to json array value: [\"Klaatu\",\"" +
                        "barada\",\"...\nUnable to parse json string value to boolean: \"Klaatu\"");
    }

    @Test
    public void arrayOfBooleanFromArrayOfNumber() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'arrayOfBoolean' of type Boolean[] cannot be mapped to json array value: [2,3,5,7,11" +
                        ",13,17,19...\nUnable to parse json numeric value to boolean: 2");
    }

    @Test
    public void arrayOfIntFromObject() throws Exception {
        assertMessage("{ \"arrayOfInt\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfInt' of type int[] cannot be mapped to json object value: {\"red\":255,\"green\"" +
                        ":1...\nint[] array not a suitable datatype for json object value: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfIntFromString() throws Exception {
        assertMessage("{ \"arrayOfInt\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfInt' of type int[] cannot be mapped to json string value: \"Supercalifragilis" +
                        "ti...\nMissing a Converter for type class [I to convert the JSON String 'Supercalifragilisticexpialid" +
                        "ocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfIntFromNumber() throws Exception {
        assertMessage("{ \"arrayOfInt\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfInt' of type int[] cannot be mapped to json numeric value: 122333444455555.6" +
                        "666...\nUnable to parse json numeric value to class [I: 122333444455555.6666...");
    }

    @Test
    public void arrayOfIntFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfInt\" : true }",
                "Widget property 'arrayOfInt' of type int[] cannot be mapped to json boolean value: true\nUnable to pa" +
                        "rse json boolean value to class [I: true");
    }

    @Test
    public void arrayOfIntFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfInt\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfInt' of type int[] cannot be mapped to json array value: [{\"red\":255,\"green\"" +
                        ":...\nUnable to map json object value to int: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfIntFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfInt\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'arrayOfInt' of type int[] cannot be mapped to json array value: [\"Klaatu\",\"barada\"," +
                        "\"...\nMissing a Converter for type int to convert the JSON String 'Klaatu' . Please register a custom" +
                        " converter for it.");
    }

    @Test
    public void arrayOfIntFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfInt\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfInt' of type int[] cannot be mapped to json array value: [true,false,true,tr" +
                        "u...\nUnable to parse json boolean value to int: true");
    }

    @Test
    public void arrayOfIntFromArrayOfNull() throws Exception {
        assertMessage("{ \"arrayOfInt\" : [null,null,null,null,null,null] }",
                "Widget property 'arrayOfInt' of type int[] cannot be mapped to json array value: [null,null,null,nul" +
                        "l...\njson array mapped to int[] has null value at index 0");
    }

    @Test
    public void arrayOfByteFromObject() throws Exception {
        assertMessage("{ \"arrayOfByte\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfByte' of type byte[] cannot be mapped to json object value: {\"red\":255,\"gree" +
                        "n\":1...\nbyte[] array not a suitable datatype for json object value: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfByteFromString() throws Exception {
        assertMessage("{ \"arrayOfByte\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfByte' of type byte[] cannot be mapped to json string value: \"Supercalifragil" +
                        "isti...\nMissing a Converter for type class [B to convert the JSON String 'Supercalifragilisticexpial" +
                        "idocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfByteFromNumber() throws Exception {
        assertMessage("{ \"arrayOfByte\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfByte' of type byte[] cannot be mapped to json numeric value: 122333444455555" +
                        ".6666...\nUnable to parse json numeric value to class [B: 122333444455555.6666...");
    }

    @Test
    public void arrayOfByteFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfByte\" : true }",
                "Widget property 'arrayOfByte' of type byte[] cannot be mapped to json boolean value: true\nUnable to " +
                        "parse json boolean value to class [B: true");
    }

    @Test
    public void arrayOfByteFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfByte\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfByte' of type byte[] cannot be mapped to json array value: [{\"red\":255,\"gree" +
                        "n\":...\nUnable to map json object value to byte: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfByteFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfByte\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'arrayOfByte' of type byte[] cannot be mapped to json array value: [\"Klaatu\",\"barada" +
                        "\",\"...\nMissing a Converter for type byte to convert the JSON String 'Klaatu' . Please register a cus" +
                        "tom converter for it.");
    }

    @Test
    public void arrayOfByteFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfByte\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfByte' of type byte[] cannot be mapped to json array value: [true,false,true," +
                        "tru...\nUnable to parse json boolean value to byte: true");
    }

    @Test
    public void arrayOfCharFromObject() throws Exception {
        assertMessage("{ \"arrayOfChar\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfChar' of type char[] cannot be mapped to json object value: {\"red\":255,\"gree" +
                        "n\":1...\nchar[] array not a suitable datatype for json object value: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfCharFromString() throws Exception {
        assertMessage("{ \"arrayOfChar\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfChar' of type char[] cannot be mapped to json string value: \"Supercalifragil" +
                        "isti...\nMissing a Converter for type class [C to convert the JSON String 'Supercalifragilisticexpial" +
                        "idocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfCharFromNumber() throws Exception {
        assertMessage("{ \"arrayOfChar\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfChar' of type char[] cannot be mapped to json numeric value: 122333444455555" +
                        ".6666...\nUnable to parse json numeric value to class [C: 122333444455555.6666...");
    }

    @Test
    public void arrayOfCharFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfChar\" : true }",
                "Widget property 'arrayOfChar' of type char[] cannot be mapped to json boolean value: true\nUnable to " +
                        "parse json boolean value to class [C: true");
    }

    @Test
    public void arrayOfCharFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfChar\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfChar' of type char[] cannot be mapped to json array value: [{\"red\":255,\"gree" +
                        "n\":...\nCannot cast org.apache.johnzon.core.JsonObjectImpl to jakarta.json.JsonString");
    }

    @Test
    public void arrayOfCharFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfChar\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfChar' of type char[] cannot be mapped to json array value: [true,false,true," +
                        "tru...\nCannot cast jakarta.json.JsonValueImpl to jakarta.json.JsonString");
    }

    @Test
    public void arrayOfShortFromObject() throws Exception {
        assertMessage("{ \"arrayOfShort\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfShort' of type short[] cannot be mapped to json object value: {\"red\":255,\"gr" +
                        "een\":1...\nshort[] array not a suitable datatype for json object value: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfShortFromString() throws Exception {
        assertMessage("{ \"arrayOfShort\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfShort' of type short[] cannot be mapped to json string value: \"Supercalifrag" +
                        "ilisti...\nMissing a Converter for type class [S to convert the JSON String 'Supercalifragilisticexpi" +
                        "alidocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfShortFromNumber() throws Exception {
        assertMessage("{ \"arrayOfShort\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfShort' of type short[] cannot be mapped to json numeric value: 1223334444555" +
                        "55.6666...\nUnable to parse json numeric value to class [S: 122333444455555.6666...");
    }

    @Test
    public void arrayOfShortFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfShort\" : true }",
                "Widget property 'arrayOfShort' of type short[] cannot be mapped to json boolean value: true\nUnable t" +
                        "o parse json boolean value to class [S: true");
    }

    @Test
    public void arrayOfShortFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfShort\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfShort' of type short[] cannot be mapped to json array value: [{\"red\":255,\"gr" +
                        "een\":...\nUnable to map json object value to short: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfShortFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfShort\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'arrayOfShort' of type short[] cannot be mapped to json array value: [\"Klaatu\",\"bara" +
                        "da\",\"...\nMissing a Converter for type short to convert the JSON String 'Klaatu' . Please register a " +
                        "custom converter for it.");
    }

    @Test
    public void arrayOfShortFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfShort\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfShort' of type short[] cannot be mapped to json array value: [true,false,tru" +
                        "e,tru...\nUnable to parse json boolean value to short: true");
    }

    @Test
    public void arrayOfLongFromObject() throws Exception {
        assertMessage("{ \"arrayOfLong\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfLong' of type long[] cannot be mapped to json object value: {\"red\":255,\"gree" +
                        "n\":1...\nlong[] array not a suitable datatype for json object value: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfLongFromString() throws Exception {
        assertMessage("{ \"arrayOfLong\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfLong' of type long[] cannot be mapped to json string value: \"Supercalifragil" +
                        "isti...\nMissing a Converter for type class [J to convert the JSON String 'Supercalifragilisticexpial" +
                        "idocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfLongFromNumber() throws Exception {
        assertMessage("{ \"arrayOfLong\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfLong' of type long[] cannot be mapped to json numeric value: 122333444455555" +
                        ".6666...\nUnable to parse json numeric value to class [J: 122333444455555.6666...");
    }

    @Test
    public void arrayOfLongFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfLong\" : true }",
                "Widget property 'arrayOfLong' of type long[] cannot be mapped to json boolean value: true\nUnable to " +
                        "parse json boolean value to class [J: true");
    }

    @Test
    public void arrayOfLongFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfLong\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfLong' of type long[] cannot be mapped to json array value: [{\"red\":255,\"gree" +
                        "n\":...\nUnable to map json object value to long: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfLongFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfLong\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'arrayOfLong' of type long[] cannot be mapped to json array value: [\"Klaatu\",\"barada" +
                        "\",\"...\nMissing a Converter for type long to convert the JSON String 'Klaatu' . Please register a cus" +
                        "tom converter for it.");
    }

    @Test
    public void arrayOfLongFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfLong\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfLong' of type long[] cannot be mapped to json array value: [true,false,true," +
                        "tru...\nUnable to parse json boolean value to long: true");
    }

    @Test
    public void arrayOfFloatFromObject() throws Exception {
        assertMessage("{ \"arrayOfFloat\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfFloat' of type float[] cannot be mapped to json object value: {\"red\":255,\"gr" +
                        "een\":1...\nfloat[] array not a suitable datatype for json object value: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfFloatFromString() throws Exception {
        assertMessage("{ \"arrayOfFloat\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfFloat' of type float[] cannot be mapped to json string value: \"Supercalifrag" +
                        "ilisti...\nMissing a Converter for type class [F to convert the JSON String 'Supercalifragilisticexpi" +
                        "alidocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfFloatFromNumber() throws Exception {
        assertMessage("{ \"arrayOfFloat\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfFloat' of type float[] cannot be mapped to json numeric value: 1223334444555" +
                        "55.6666...\nUnable to parse json numeric value to class [F: 122333444455555.6666...");
    }

    @Test
    public void arrayOfFloatFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfFloat\" : true }",
                "Widget property 'arrayOfFloat' of type float[] cannot be mapped to json boolean value: true\nUnable t" +
                        "o parse json boolean value to class [F: true");
    }

    @Test
    public void arrayOfFloatFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfFloat\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfFloat' of type float[] cannot be mapped to json array value: [{\"red\":255,\"gr" +
                        "een\":...\nUnable to map json object value to float: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfFloatFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfFloat\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'arrayOfFloat' of type float[] cannot be mapped to json array value: [\"Klaatu\",\"bara" +
                        "da\",\"...\nMissing a Converter for type float to convert the JSON String 'Klaatu' . Please register a " +
                        "custom converter for it.");
    }

    @Test
    public void arrayOfFloatFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfFloat\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfFloat' of type float[] cannot be mapped to json array value: [true,false,tru" +
                        "e,tru...\nUnable to parse json boolean value to float: true");
    }

    @Test
    public void arrayOfDoubleFromObject() throws Exception {
        assertMessage("{ \"arrayOfDouble\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfDouble' of type double[] cannot be mapped to json object value: {\"red\":255,\"" +
                        "green\":1...\ndouble[] array not a suitable datatype for json object value: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfDoubleFromString() throws Exception {
        assertMessage("{ \"arrayOfDouble\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfDouble' of type double[] cannot be mapped to json string value: \"Supercalifr" +
                        "agilisti...\nMissing a Converter for type class [D to convert the JSON String 'Supercalifragilisticex" +
                        "pialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfDoubleFromNumber() throws Exception {
        assertMessage("{ \"arrayOfDouble\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfDouble' of type double[] cannot be mapped to json numeric value: 12233344445" +
                        "5555.6666...\nUnable to parse json numeric value to class [D: 122333444455555.6666...");
    }

    @Test
    public void arrayOfDoubleFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfDouble\" : true }",
                "Widget property 'arrayOfDouble' of type double[] cannot be mapped to json boolean value: true\nUnable" +
                        " to parse json boolean value to class [D: true");
    }

    @Test
    public void arrayOfDoubleFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfDouble\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfDouble' of type double[] cannot be mapped to json array value: [{\"red\":255,\"" +
                        "green\":...\nUnable to map json object value to double: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfDoubleFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfDouble\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'arrayOfDouble' of type double[] cannot be mapped to json array value: [\"Klaatu\",\"ba" +
                        "rada\",\"...\nMissing a Converter for type double to convert the JSON String 'Klaatu' . Please register" +
                        " a custom converter for it.");
    }

    @Test
    public void arrayOfDoubleFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfDouble\" : [true,false,true,true,false] }",
                "Widget property 'arrayOfDouble' of type double[] cannot be mapped to json array value: [true,false,t" +
                        "rue,tru...\nUnable to parse json boolean value to double: true");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromObject() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'arrayOfBooleanPrimitive' of type boolean[] cannot be mapped to json object value: {" +
                        "\"red\":255,\"green\":1...\nboolean[] array not a suitable datatype for json object value: {\"red\":255,\"gr" +
                        "een\":1...");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromString() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'arrayOfBooleanPrimitive' of type boolean[] cannot be mapped to json string value: \"" +
                        "Supercalifragilisti...\nMissing a Converter for type class [Z to convert the JSON String 'Supercalifr" +
                        "agilisticexpialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromNumber() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : 122333444455555.666666777777788888888 }",
                "Widget property 'arrayOfBooleanPrimitive' of type boolean[] cannot be mapped to json numeric value: " +
                        "122333444455555.6666...\nUnable to parse json numeric value to class [Z: 122333444455555.6666...");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : true }",
                "Widget property 'arrayOfBooleanPrimitive' of type boolean[] cannot be mapped to json boolean value: " +
                        "true\nUnable to parse json boolean value to class [Z: true");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'arrayOfBooleanPrimitive' of type boolean[] cannot be mapped to json array value: [{" +
                        "\"red\":255,\"green\":...\nUnable to parse json object value to boolean: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'arrayOfBooleanPrimitive' of type boolean[] cannot be mapped to json array value: [\"" +
                        "Klaatu\",\"barada\",\"...\nUnable to parse json string value to boolean: \"Klaatu\"");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromArrayOfNumber() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'arrayOfBooleanPrimitive' of type boolean[] cannot be mapped to json array value: [2" +
                        ",3,5,7,11,13,17,19...\nUnable to parse json numeric value to boolean: 2");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromArrayOfNull() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : [null,null,null,null,null,null] }",
                "Widget property 'arrayOfBooleanPrimitive' of type boolean[] cannot be mapped to json array value: [n" +
                        "ull,null,null,null...\njson array mapped to boolean[] has null value at index 0");
    }

    @Test
    public void listOfObjectFromObject() throws Exception {
        assertMessage("{ \"listOfObject\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'listOfObject' of type List<Color> cannot be mapped to json object value: {\"red\":255" +
                        ",\"green\":1...\nUnable to map json object value to java.util.List<org.apache.johnzon.jsonb.Deserializa" +
                        "tionExceptionMessagesTest$Color>: {\"red\":255,\"green\":1...");
    }

    @Test
    public void listOfObjectFromString() throws Exception {
        assertMessage("{ \"listOfObject\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'listOfObject' of type List<Color> cannot be mapped to json string value: \"Supercali" +
                        "fragilisti...\nMissing a Converter for type interface java.util.List to convert the JSON String 'Supe" +
                        "rcalifragilisticexpialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void listOfObjectFromNumber() throws Exception {
        assertMessage("{ \"listOfObject\" : 122333444455555.666666777777788888888 }",
                "Widget property 'listOfObject' of type List<Color> cannot be mapped to json numeric value: 122333444" +
                        "455555.6666...\nUnable to parse json numeric value to java.util.List<org.apache.johnzon.jsonb.Deseria" +
                        "lizationExceptionMessagesTest$Color>: 122333444455555.6666...");
    }

    @Test
    public void listOfObjectFromBoolean() throws Exception {
        assertMessage("{ \"listOfObject\" : true }",
                "Widget property 'listOfObject' of type List<Color> cannot be mapped to json boolean value: true\nUnab" +
                        "le to parse json boolean value to java.util.List<org.apache.johnzon.jsonb.DeserializationExceptionMe" +
                        "ssagesTest$Color>: true");
    }

    @Test
    public void listOfObjectFromArrayOfString() throws Exception {
        assertMessage("{ \"listOfObject\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'listOfObject' of type List<Color> cannot be mapped to json array value: [\"Klaatu\",\"" +
                        "barada\",\"...\nMissing a Converter for type class org.apache.johnzon.jsonb.DeserializationExceptionMes" +
                        "sagesTest$Color to convert the JSON String 'Klaatu' . Please register a custom converter for it.");
    }

    @Test
    public void listOfObjectFromArrayOfNumber() throws Exception {
        assertMessage("{ \"listOfObject\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'listOfObject' of type List<Color> cannot be mapped to json array value: [2,3,5,7,11" +
                        ",13,17,19...\nUnable to parse json numeric value to class org.apache.johnzon.jsonb.DeserializationExc" +
                        "eptionMessagesTest$Color: 2");
    }

    @Test
    public void listOfObjectFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"listOfObject\" : [true,false,true,true,false] }",
                "Widget property 'listOfObject' of type List<Color> cannot be mapped to json array value: [true,false" +
                        ",true,tru...\nUnable to parse json boolean value to class org.apache.johnzon.jsonb.DeserializationExc" +
                        "eptionMessagesTest$Color: true");
    }

    @Test
    public void listOfStringFromObject() throws Exception {
        assertMessage("{ \"listOfString\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'listOfString' of type List<String> cannot be mapped to json object value: {\"red\":25" +
                        "5,\"green\":1...\nUnable to map json object value to java.util.List<java.lang.String>: {\"red\":255,\"gree" +
                        "n\":1...");
    }

    @Test
    public void listOfStringFromString() throws Exception {
        assertMessage("{ \"listOfString\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'listOfString' of type List<String> cannot be mapped to json string value: \"Supercal" +
                        "ifragilisti...\nMissing a Converter for type interface java.util.List to convert the JSON String 'Sup" +
                        "ercalifragilisticexpialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void listOfStringFromNumber() throws Exception {
        assertMessage("{ \"listOfString\" : 122333444455555.666666777777788888888 }",
                "Widget property 'listOfString' of type List<String> cannot be mapped to json numeric value: 12233344" +
                        "4455555.6666...\nUnable to parse json numeric value to java.util.List<java.lang.String>: 122333444455" +
                        "555.6666...");
    }

    @Test
    public void listOfStringFromBoolean() throws Exception {
        assertMessage("{ \"listOfString\" : true }",
                "Widget property 'listOfString' of type List<String> cannot be mapped to json boolean value: true\nUna" +
                        "ble to parse json boolean value to java.util.List<java.lang.String>: true");
    }

    @Test
    public void listOfStringFromArrayOfObject() throws Exception {
        assertMessage("{ \"listOfString\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'listOfString' of type List<String> cannot be mapped to json array value: [{\"red\":25" +
                        "5,\"green\":...\nUnable to map json object value to class java.lang.String: {\"red\":255,\"green\":1...");
    }

    @Test
    public void listOfStringFromArrayOfNumber() throws Exception {
        assertMessage("{ \"listOfString\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'listOfString' of type List<String> cannot be mapped to json array value: [2,3,5,7,1" +
                        "1,13,17,19...\nUnable to parse json numeric value to class java.lang.String: 2");
    }

    @Test
    public void listOfStringFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"listOfString\" : [true,false,true,true,false] }",
                "Widget property 'listOfString' of type List<String> cannot be mapped to json array value: [true,fals" +
                        "e,true,tru...\nUnable to parse json boolean value to class java.lang.String: true");
    }

    @Test
    public void listOfNumberFromObject() throws Exception {
        assertMessage("{ \"listOfNumber\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'listOfNumber' of type List<Number> cannot be mapped to json object value: {\"red\":25" +
                        "5,\"green\":1...\nUnable to map json object value to java.util.List<java.lang.Number>: {\"red\":255,\"gree" +
                        "n\":1...");
    }

    @Test
    public void listOfNumberFromString() throws Exception {
        assertMessage("{ \"listOfNumber\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'listOfNumber' of type List<Number> cannot be mapped to json string value: \"Supercal" +
                        "ifragilisti...\nMissing a Converter for type interface java.util.List to convert the JSON String 'Sup" +
                        "ercalifragilisticexpialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void listOfNumberFromNumber() throws Exception {
        assertMessage("{ \"listOfNumber\" : 122333444455555.666666777777788888888 }",
                "Widget property 'listOfNumber' of type List<Number> cannot be mapped to json numeric value: 12233344" +
                        "4455555.6666...\nUnable to parse json numeric value to java.util.List<java.lang.Number>: 122333444455" +
                        "555.6666...");
    }

    @Test
    public void listOfNumberFromBoolean() throws Exception {
        assertMessage("{ \"listOfNumber\" : true }",
                "Widget property 'listOfNumber' of type List<Number> cannot be mapped to json boolean value: true\nUna" +
                        "ble to parse json boolean value to java.util.List<java.lang.Number>: true");
    }

    @Test
    public void listOfNumberFromArrayOfObject() throws Exception {
        assertMessage("{ \"listOfNumber\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'listOfNumber' of type List<Number> cannot be mapped to json array value: [{\"red\":25" +
                        "5,\"green\":...\nNumber cannot be constructed to deserialize json object value: {\"red\":255,\"green\":1..." +
                        "\njava.lang.InstantiationException");
    }

    @Test
    public void listOfNumberFromArrayOfString() throws Exception {
        assertMessage("{ \"listOfNumber\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'listOfNumber' of type List<Number> cannot be mapped to json array value: [\"Klaatu\"," +
                        "\"barada\",\"...\nMissing a Converter for type class java.lang.Number to convert the JSON String 'Klaatu" +
                        "' . Please register a custom converter for it.");
    }

    @Test
    public void listOfNumberFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"listOfNumber\" : [true,false,true,true,false] }",
                "Widget property 'listOfNumber' of type List<Number> cannot be mapped to json array value: [true,fals" +
                        "e,true,tru...\nUnable to parse json boolean value to class java.lang.Number: true");
    }

    @Test
    public void listOfBooleanFromObject() throws Exception {
        assertMessage("{ \"listOfBoolean\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Widget property 'listOfBoolean' of type List<Boolean> cannot be mapped to json object value: {\"red\":" +
                        "255,\"green\":1...\nUnable to map json object value to java.util.List<java.lang.Boolean>: {\"red\":255,\"g" +
                        "reen\":1...");
    }

    @Test
    public void listOfBooleanFromString() throws Exception {
        assertMessage("{ \"listOfBoolean\" : \"Supercalifragilisticexpialidocious\" }",
                "Widget property 'listOfBoolean' of type List<Boolean> cannot be mapped to json string value: \"Superc" +
                        "alifragilisti...\nMissing a Converter for type interface java.util.List to convert the JSON String 'S" +
                        "upercalifragilisticexpialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void listOfBooleanFromNumber() throws Exception {
        assertMessage("{ \"listOfBoolean\" : 122333444455555.666666777777788888888 }",
                "Widget property 'listOfBoolean' of type List<Boolean> cannot be mapped to json numeric value: 122333" +
                        "444455555.6666...\nUnable to parse json numeric value to java.util.List<java.lang.Boolean>: 122333444" +
                        "455555.6666...");
    }

    @Test
    public void listOfBooleanFromBoolean() throws Exception {
        assertMessage("{ \"listOfBoolean\" : true }",
                "Widget property 'listOfBoolean' of type List<Boolean> cannot be mapped to json boolean value: true\nU" +
                        "nable to parse json boolean value to java.util.List<java.lang.Boolean>: true");
    }

    @Test
    public void listOfBooleanFromArrayOfObject() throws Exception {
        assertMessage("{ \"listOfBoolean\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Widget property 'listOfBoolean' of type List<Boolean> cannot be mapped to json array value: [{\"red\":" +
                        "255,\"green\":...\nUnable to parse json object value to boolean: {\"red\":255,\"green\":1...");
    }

    @Test
    public void listOfBooleanFromArrayOfString() throws Exception {
        assertMessage("{ \"listOfBoolean\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Widget property 'listOfBoolean' of type List<Boolean> cannot be mapped to json array value: [\"Klaatu" +
                        "\",\"barada\",\"...\nUnable to parse json string value to boolean: \"Klaatu\"");
    }

    @Test
    public void listOfBooleanFromArrayOfNumber() throws Exception {
        assertMessage("{ \"listOfBoolean\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Widget property 'listOfBoolean' of type List<Boolean> cannot be mapped to json array value: [2,3,5,7" +
                        ",11,13,17,19...\nUnable to parse json numeric value to boolean: 2");
    }

    private void assertMessage(final String json, final String expected) throws Exception {
        ExceptionAsserts.fromJson(json, Widget.class)
                .assertInstanceOf(JsonbException.class)
                .assertMessage(expected);
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
            this.arrayOfObject = arrayOfObject;
        }

        public String[] getArrayOfString() {
            return arrayOfString;
        }

        public void setArrayOfString(final String[] arrayOfString) {
            this.arrayOfString = arrayOfString;
        }

        public Number[] getArrayOfNumber() {
            return arrayOfNumber;
        }

        public void setArrayOfNumber(final Number[] arrayOfNumber) {
            this.arrayOfNumber = arrayOfNumber;
        }

        public int[] getArrayOfInt() {
            return arrayOfInt;
        }

        public void setArrayOfInt(final int[] arrayOfInt) {
            this.arrayOfInt = arrayOfInt;
        }

        public byte[] getArrayOfByte() {
            return arrayOfByte;
        }

        public void setArrayOfByte(final byte[] arrayOfByte) {
            this.arrayOfByte = arrayOfByte;
        }

        public char[] getArrayOfChar() {
            return arrayOfChar;
        }

        public void setArrayOfChar(final char[] arrayOfChar) {
            this.arrayOfChar = arrayOfChar;
        }

        public short[] getArrayOfShort() {
            return arrayOfShort;
        }

        public void setArrayOfShort(final short[] arrayOfShort) {
            this.arrayOfShort = arrayOfShort;
        }

        public long[] getArrayOfLong() {
            return arrayOfLong;
        }

        public void setArrayOfLong(final long[] arrayOfLong) {
            this.arrayOfLong = arrayOfLong;
        }

        public float[] getArrayOfFloat() {
            return arrayOfFloat;
        }

        public void setArrayOfFloat(final float[] arrayOfFloat) {
            this.arrayOfFloat = arrayOfFloat;
        }

        public double[] getArrayOfDouble() {
            return arrayOfDouble;
        }

        public void setArrayOfDouble(final double[] arrayOfDouble) {
            this.arrayOfDouble = arrayOfDouble;
        }

        public Boolean[] getArrayOfBoolean() {
            return arrayOfBoolean;
        }

        public void setArrayOfBoolean(final Boolean[] arrayOfBoolean) {
            this.arrayOfBoolean = arrayOfBoolean;
        }

        public boolean[] getArrayOfBooleanPrimitive() {
            return arrayOfBooleanPrimitive;
        }

        public void setArrayOfBooleanPrimitive(final boolean[] arrayOfBooleanPrimitive) {
            this.arrayOfBooleanPrimitive = arrayOfBooleanPrimitive;
        }

        public List<Color> getListOfObject() {
            return listOfObject;
        }

        public void setListOfObject(final List<Color> listOfObject) {
            this.listOfObject = listOfObject;
        }

        public List<String> getListOfString() {
            return listOfString;
        }

        public void setListOfString(final List<String> listOfString) {
            this.listOfString = listOfString;
        }

        public List<Number> getListOfNumber() {
            return listOfNumber;
        }

        public void setListOfNumber(final List<Number> listOfNumber) {
            this.listOfNumber = listOfNumber;
        }

        public List<Boolean> getListOfBoolean() {
            return listOfBoolean;
        }

        public void setListOfBoolean(final List<Boolean> listOfBoolean) {
            this.listOfBoolean = listOfBoolean;
        }

        public Color getObject() {
            return object;
        }

        public void setObject(final Color object) {
            this.object = object;
        }

        public String getString() {
            return string;
        }

        public void setString(final String string) {
            this.string = string;
        }

        public Integer getNumber() {
            return number;
        }

        public void setNumber(final Integer number) {
            this.number = number;
        }

        public int getIntPrimitive() {
            return intPrimitive;
        }

        public void setIntPrimitive(final int intPrimitive) {
            this.intPrimitive = intPrimitive;
        }

        public Boolean getBool() {
            return bool;
        }

        public void setBool(final Boolean bool) {
            this.bool = bool;
        }

        public boolean isBoolPrimitive() {
            return boolPrimitive;
        }

        public void setBoolPrimitive(final boolean boolPrimitive) {
            this.boolPrimitive = boolPrimitive;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(final Date date) {
            this.date = date;
        }

        public TimeUnit getUnit() {
            return unit;
        }

        public void setUnit(final TimeUnit unit) {
            this.unit = unit;
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
