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

import org.junit.Ignore;
import org.junit.Test;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.JsonbException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class DeserializationExceptionMessagesTest {

    @Test
    public void objectFromString() throws Exception {
        assertMessage("{ \"object\" : \"Supercalifragilisticexpialidocious\" }",
                "Missing a Converter for type class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Col" +
                        "or to convert the JSON String 'Supercalifragilisticexpialidocious' . Please register a custom conver" +
                        "ter for it.");
    }

    @Test
    public void objectFromNumber() throws Exception {
        assertMessage("{ \"object\" : 122333444455555.666666777777788888888 }",
                "Unable to parse 122333444455555.666666777777788888888 to class org.apache.johnzon.jsonb.Deserializat" +
                        "ionExceptionMessagesTest$Color");
    }

    @Test
    public void objectFromBoolean() throws Exception {
        assertMessage("{ \"object\" : true }",
                "Unable to parse true to class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color");
    }

    @Test
    public void objectFromArrayOfObject() throws Exception {
        assertMessage("{ \"object\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "type class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color not supported");
    }

    @Test
    public void objectFromArrayOfString() throws Exception {
        assertMessage("{ \"object\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "type class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color not supported");
    }

    @Test
    public void objectFromArrayOfNumber() throws Exception {
        assertMessage("{ \"object\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "type class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color not supported");
    }

    @Test
    public void objectFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"object\" : [true,false,true,true,false] }",
                "type class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color not supported");
    }

    @Test
    public void stringFromArrayOfObject() throws Exception {
        assertMessage("{ \"string\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Unsupported [{\"red\":255,\"green\":165,\"blue\":0},{\"red\":0,\"green\":45,\"blue\":127}] for type class java.l" +
                        "ang.String");
    }

    @Test
    public void stringFromArrayOfString() throws Exception {
        assertMessage("{ \"string\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Unsupported [\"Klaatu\",\"barada\",\"nikto\"] for type class java.lang.String");
    }

    @Test
    public void stringFromArrayOfNumber() throws Exception {
        assertMessage("{ \"string\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Unsupported [2,3,5,7,11,13,17,19,23,29] for type class java.lang.String");
    }

    @Test
    public void stringFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"string\" : [true,false,true,true,false] }",
                "Unsupported [true,false,true,true,false] for type class java.lang.String");
    }

    @Test
    public void numberFromObject() throws Exception {
        assertMessage("{ \"number\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Can't map JSON Object to class java.lang.Integer: {\"red\":255,\"green\":1...");
    }

    @Test
    public void numberFromString() throws Exception {
        assertMessage("{ \"number\" : \"Supercalifragilisticexpialidocious\" }",
                "For input string: \"Supercalifragilisticexpialidocious\"");
    }

    @Test
    public void numberFromBoolean() throws Exception {
        assertMessage("{ \"number\" : true }",
                "Unable to parse true to class java.lang.Integer");
    }

    @Test
    public void numberFromArrayOfObject() throws Exception {
        assertMessage("{ \"number\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "type class java.lang.Integer not supported");
    }

    @Test
    public void numberFromArrayOfString() throws Exception {
        assertMessage("{ \"number\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "type class java.lang.Integer not supported");
    }

    @Test
    public void numberFromArrayOfNumber() throws Exception {
        assertMessage("{ \"number\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "type class java.lang.Integer not supported");
    }

    @Test
    public void numberFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"number\" : [true,false,true,true,false] }",
                "type class java.lang.Integer not supported");
    }

    @Test
    public void intPrimitiveFromObject() throws Exception {
        assertMessage("{ \"intPrimitive\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Can't map JSON Object to int: {\"red\":255,\"green\":1...");
    }

    @Test
    public void intPrimitiveFromString() throws Exception {
        assertMessage("{ \"intPrimitive\" : \"Supercalifragilisticexpialidocious\" }",
                "Missing a Converter for type int to convert the JSON String 'Supercalifragilisticexpialidocious' . P" +
                        "lease register a custom converter for it.");
    }

    @Test
    public void intPrimitiveFromNumber() throws Exception {
        assertMessage("{ \"intPrimitive\" : 122333444455555.666666777777788888888 }",
                "Not an int/long, use other value readers");
    }

    @Test
    public void intPrimitiveFromBoolean() throws Exception {
        assertMessage("{ \"intPrimitive\" : true }",
                "Unable to parse true to int");
    }

    @Test
    public void intPrimitiveFromNull() throws Exception {
        assertMessage("{ \"intPrimitive\" : null }",
                "Error calling public void org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Widget.setIn" +
                        "tPrimitive(int)");
    }

    @Test
    public void intPrimitiveFromArrayOfObject() throws Exception {
        assertMessage("{ \"intPrimitive\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "type int not supported");
    }

    @Test
    public void intPrimitiveFromArrayOfString() throws Exception {
        assertMessage("{ \"intPrimitive\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "type int not supported");
    }

    @Test
    public void intPrimitiveFromArrayOfNumber() throws Exception {
        assertMessage("{ \"intPrimitive\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "type int not supported");
    }

    @Test
    public void intPrimitiveFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"intPrimitive\" : [true,false,true,true,false] }",
                "type int not supported");
    }

    @Test
    public void booleanFromObject() throws Exception {
        assertMessage("{ \"bool\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Unable to parse {\"red\":255,\"green\":165,\"blue\":0} to boolean");
    }

    @Test
    public void booleanFromString() throws Exception {
        assertMessage("{ \"bool\" : \"Supercalifragilisticexpialidocious\" }",
                "Unable to parse \"Supercalifragilisticexpialidocious\" to boolean");
    }

    @Test
    public void booleanFromNumber() throws Exception {
        assertMessage("{ \"bool\" : 122333444455555.666666777777788888888 }",
                "Unable to parse 122333444455555.666666777777788888888 to boolean");
    }

    @Test
    public void booleanFromArrayOfObject() throws Exception {
        assertMessage("{ \"bool\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Unable to parse [{\"red\":255,\"green\":165,\"blue\":0},{\"red\":0,\"green\":45,\"blue\":127}] to boolean");
    }

    @Test
    public void booleanFromArrayOfString() throws Exception {
        assertMessage("{ \"bool\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Unable to parse [\"Klaatu\",\"barada\",\"nikto\"] to boolean");
    }

    @Test
    public void booleanFromArrayOfNumber() throws Exception {
        assertMessage("{ \"bool\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Unable to parse [2,3,5,7,11,13,17,19,23,29] to boolean");
    }

    @Test
    public void booleanFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"bool\" : [true,false,true,true,false] }",
                "Unable to parse [true,false,true,true,false] to boolean");
    }

    @Test
    public void boolPrimitiveFromObject() throws Exception {
        assertMessage("{ \"boolPrimitive\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Unable to parse {\"red\":255,\"green\":165,\"blue\":0} to boolean");
    }

    @Test
    public void boolPrimitiveFromString() throws Exception {
        assertMessage("{ \"boolPrimitive\" : \"Supercalifragilisticexpialidocious\" }",
                "Unable to parse \"Supercalifragilisticexpialidocious\" to boolean");
    }

    @Test
    public void boolPrimitiveFromNumber() throws Exception {
        assertMessage("{ \"boolPrimitive\" : 122333444455555.666666777777788888888 }",
                "Unable to parse 122333444455555.666666777777788888888 to boolean");
    }

    @Test
    public void boolPrimitiveFromNull() throws Exception {
        assertMessage("{ \"boolPrimitive\" : null }",
                "Error calling public void org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Widget.setBo" +
                        "olPrimitive(boolean)");
    }

    @Test
    public void boolPrimitiveFromArrayOfObject() throws Exception {
        assertMessage("{ \"boolPrimitive\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Unable to parse [{\"red\":255,\"green\":165,\"blue\":0},{\"red\":0,\"green\":45,\"blue\":127}] to boolean");
    }

    @Test
    public void boolPrimitiveFromArrayOfString() throws Exception {
        assertMessage("{ \"boolPrimitive\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Unable to parse [\"Klaatu\",\"barada\",\"nikto\"] to boolean");
    }

    @Test
    public void boolPrimitiveFromArrayOfNumber() throws Exception {
        assertMessage("{ \"boolPrimitive\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Unable to parse [2,3,5,7,11,13,17,19,23,29] to boolean");
    }

    @Test
    public void boolPrimitiveFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"boolPrimitive\" : [true,false,true,true,false] }",
                "Unable to parse [true,false,true,true,false] to boolean");
    }

    @Test
    public void enumFromObject() throws Exception {
        assertMessage("{ \"unit\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Can't map JSON Object to class java.lang.String: {\"red\":255,\"green\":1...");
    }

    @Test
    public void enumFromString() throws Exception {
        assertMessage("{ \"unit\" : \"Supercalifragilisticexpialidocious\" }",
                "Illegal class java.util.concurrent.TimeUnit enum value: Supercalifragilisticexpialidocious, known va" +
                        "lues: [MILLISECONDS, MICROSECONDS, HOURS, SECONDS, NANOSECONDS, DAYS, MINUTES]");
    }

    @Test
    public void enumFromNumber() throws Exception {
        assertMessage("{ \"unit\" : 122333444455555.666666777777788888888 }",
                "Illegal class java.util.concurrent.TimeUnit enum value: 122333444455555.666666777777788888888, known" +
                        " values: [MILLISECONDS, MICROSECONDS, HOURS, SECONDS, NANOSECONDS, DAYS, MINUTES]");
    }

    @Test
    public void enumFromBoolean() throws Exception {
        assertMessage("{ \"unit\" : true }",
                "Illegal class java.util.concurrent.TimeUnit enum value: true, known values: [MILLISECONDS, MICROSECO" +
                        "NDS, HOURS, SECONDS, NANOSECONDS, DAYS, MINUTES]");
    }

    @Test
    public void enumFromArrayOfObject() throws Exception {
        assertMessage("{ \"unit\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Unsupported [{\"red\":255,\"green\":165,\"blue\":0},{\"red\":0,\"green\":45,\"blue\":127}] for type class java.l" +
                        "ang.String");
    }

    @Test
    public void enumFromArrayOfString() throws Exception {
        assertMessage("{ \"unit\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Unsupported [\"Klaatu\",\"barada\",\"nikto\"] for type class java.lang.String");
    }

    @Test
    public void enumFromArrayOfNumber() throws Exception {
        assertMessage("{ \"unit\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Unsupported [2,3,5,7,11,13,17,19,23,29] for type class java.lang.String");
    }

    @Test
    public void enumFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"unit\" : [true,false,true,true,false] }",
                "Unsupported [true,false,true,true,false] for type class java.lang.String");
    }

    @Test
    public void dateFromObject() throws Exception {
        assertMessage("{ \"date\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "Can't map JSON Object to class java.lang.String: {\"red\":255,\"green\":1...");
    }

    @Test
    public void dateFromString() throws Exception {
        assertMessage("{ \"date\" : \"Supercalifragilisticexpialidocious\" }",
                "Text 'Supercalifragilisticexpialidocious' could not be parsed at index 0");
    }

    @Test
    public void dateFromNumber() throws Exception {
        assertMessage("{ \"date\" : 122333444455555.666666777777788888888 }",
                "Text '122333444455555.666666777777788888888' could not be parsed at index 0");
    }

    @Test
    public void dateFromBoolean() throws Exception {
        assertMessage("{ \"date\" : true }",
                "Text 'true' could not be parsed at index 0");
    }

    @Test
    public void dateFromArrayOfObject() throws Exception {
        assertMessage("{ \"date\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Unsupported [{\"red\":255,\"green\":165,\"blue\":0},{\"red\":0,\"green\":45,\"blue\":127}] for type class java.l" +
                        "ang.String");
    }

    @Test
    public void dateFromArrayOfString() throws Exception {
        assertMessage("{ \"date\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Unsupported [\"Klaatu\",\"barada\",\"nikto\"] for type class java.lang.String");
    }

    @Test
    public void dateFromArrayOfNumber() throws Exception {
        assertMessage("{ \"date\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Unsupported [2,3,5,7,11,13,17,19,23,29] for type class java.lang.String");
    }

    @Test
    public void dateFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"date\" : [true,false,true,true,false] }",
                "Unsupported [true,false,true,true,false] for type class java.lang.String");
    }

    @Test
    public void arrayOfObjectFromObject() throws Exception {
        assertMessage("{ \"arrayOfObject\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "class [Lorg.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color; not instantiable");
    }

    @Test
    public void arrayOfObjectFromString() throws Exception {
        assertMessage("{ \"arrayOfObject\" : \"Supercalifragilisticexpialidocious\" }",
                "Missing a Converter for type class [Lorg.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$C" +
                        "olor; to convert the JSON String 'Supercalifragilisticexpialidocious' . Please register a custom con" +
                        "verter for it.");
    }

    @Test
    public void arrayOfObjectFromNumber() throws Exception {
        assertMessage("{ \"arrayOfObject\" : 122333444455555.666666777777788888888 }",
                "Unable to parse 122333444455555.666666777777788888888 to class [Lorg.apache.johnzon.jsonb.Deserializ" +
                        "ationExceptionMessagesTest$Color;");
    }

    @Test
    public void arrayOfObjectFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfObject\" : true }",
                "Unable to parse true to class [Lorg.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color;");
    }

    @Test
    public void arrayOfObjectFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfObject\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Missing a Converter for type class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Col" +
                        "or to convert the JSON String 'Klaatu' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfObjectFromArrayOfNumber() throws Exception {
        assertMessage("{ \"arrayOfObject\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Unable to parse 2 to class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color");
    }

    @Test
    public void arrayOfObjectFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfObject\" : [true,false,true,true,false] }",
                "Unable to parse true to class org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest$Color");
    }

    @Test
    public void arrayOfStringFromObject() throws Exception {
        assertMessage("{ \"arrayOfString\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "class [Ljava.lang.String; not instantiable");
    }

    @Test
    public void arrayOfStringFromString() throws Exception {
        assertMessage("{ \"arrayOfString\" : \"Supercalifragilisticexpialidocious\" }",
                "Missing a Converter for type class [Ljava.lang.String; to convert the JSON String 'Supercalifragilis" +
                        "ticexpialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfStringFromNumber() throws Exception {
        assertMessage("{ \"arrayOfString\" : 122333444455555.666666777777788888888 }",
                "Unable to parse 122333444455555.666666777777788888888 to class [Ljava.lang.String;");
    }

    @Test
    public void arrayOfStringFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfString\" : true }",
                "Unable to parse true to class [Ljava.lang.String;");
    }

    @Test
    public void arrayOfStringFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfString\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Can't map JSON Object to class java.lang.String: {\"red\":255,\"green\":1...");
    }

    @Test
    public void arrayOfStringFromArrayOfNumber() throws Exception {
        assertMessage("{ \"arrayOfString\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Unable to parse 2 to class java.lang.String");
    }

    @Test
    public void arrayOfStringFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfString\" : [true,false,true,true,false] }",
                "Unable to parse true to class java.lang.String");
    }

    @Test
    public void arrayOfNumberFromObject() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "class [Ljava.lang.Number; not instantiable");
    }

    @Test
    public void arrayOfNumberFromString() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : \"Supercalifragilisticexpialidocious\" }",
                "Missing a Converter for type class [Ljava.lang.Number; to convert the JSON String 'Supercalifragilis" +
                        "ticexpialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfNumberFromNumber() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : 122333444455555.666666777777788888888 }",
                "Unable to parse 122333444455555.666666777777788888888 to class [Ljava.lang.Number;");
    }

    @Test
    public void arrayOfNumberFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : true }",
                "Unable to parse true to class [Ljava.lang.Number;");
    }

    @Test
    public void arrayOfNumberFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "java.lang.InstantiationException");
    }

    @Test
    public void arrayOfNumberFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Missing a Converter for type class java.lang.Number to convert the JSON String 'Klaatu' . Please reg" +
                        "ister a custom converter for it.");
    }

    @Test
    public void arrayOfNumberFromArrayOfBoolean() throws Exception {
        assertMessage("{ \"arrayOfNumber\" : [true,false,true,true,false] }",
                "Unable to parse true to class java.lang.Number");
    }

    @Test
    public void arrayOfBooleanFromObject() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "class [Ljava.lang.Boolean; not instantiable");
    }

    @Test
    public void arrayOfBooleanFromString() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : \"Supercalifragilisticexpialidocious\" }",
                "Missing a Converter for type class [Ljava.lang.Boolean; to convert the JSON String 'Supercalifragili" +
                        "sticexpialidocious' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfBooleanFromNumber() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : 122333444455555.666666777777788888888 }",
                "Unable to parse 122333444455555.666666777777788888888 to class [Ljava.lang.Boolean;");
    }

    @Test
    public void arrayOfBooleanFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : true }",
                "Unable to parse true to class [Ljava.lang.Boolean;");
    }

    @Test
    public void arrayOfBooleanFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Unable to parse {\"red\":255,\"green\":165,\"blue\":0} to boolean");
    }

    @Test
    public void arrayOfBooleanFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Unable to parse \"Klaatu\" to boolean");
    }

    @Test
    public void arrayOfBooleanFromArrayOfNumber() throws Exception {
        assertMessage("{ \"arrayOfBoolean\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Unable to parse 2 to boolean");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromObject() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : {\"red\": 255, \"green\": 165, \"blue\":0} }",
                "class [Z not instantiable");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromString() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : \"Supercalifragilisticexpialidocious\" }",
                "Missing a Converter for type class [Z to convert the JSON String 'Supercalifragilisticexpialidocious" +
                        "' . Please register a custom converter for it.");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromNumber() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : 122333444455555.666666777777788888888 }",
                "Unable to parse 122333444455555.666666777777788888888 to class [Z");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromBoolean() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : true }",
                "Unable to parse true to class [Z");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromArrayOfObject() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : [{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}] }",
                "Unable to parse {\"red\":255,\"green\":165,\"blue\":0} to boolean");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromArrayOfString() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : [\"Klaatu\", \"barada\", \"nikto\"] }",
                "Unable to parse \"Klaatu\" to boolean");
    }

    @Test
    public void arrayOfBooleanPrimitiveFromArrayOfNumber() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : [2, 3, 5, 7, 11, 13, 17, 19, 23, 29] }",
                "Unable to parse 2 to boolean");
    }

    @Ignore("JOHNZON-371")
    @Test
    public void arrayOfBooleanPrimitiveFromArrayOfNull() throws Exception {
        assertMessage("{ \"arrayOfBooleanPrimitive\" : [null,null,null,null,null,null] }",
                "Cannot invoke \"java.lang.Boolean.booleanValue()\" because the return value of \"org.apache.johnzon.map" +
                        "per.MappingParserImpl.toObject(Object, javax.json.JsonValue, java.lang.reflect.Type, org.apache.john" +
                        "zon.mapper.Adapter, org.apache.johnzon.mapper.internal.JsonPointerTracker, java.lang.reflect.Type)\" " +
                        "is null");
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

        public int[] getArrayOfint() {
            return arrayOfint;
        }

        public void setArrayOfint(final int[] arrayOfint) {
            this.arrayOfint = arrayOfint;
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
