/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.core;

import javax.json.stream.JsonParser.Event;

public interface JsonChars {
    char EOF = Character.MIN_VALUE;

    char START_OBJECT_CHAR = '{';
    char END_OBJECT_CHAR = '}';
    char START_ARRAY_CHAR = '[';
    char END_ARRAY_CHAR = ']';
    char QUOTE_CHAR = '"';
    char COMMA_CHAR = ',';
    char KEY_SEPARATOR = ':';
    
    char EOL = '\n';
    char SPACE = ' ';
    
    char TRUE_T = 't';
    char TRUE_R = 'r';
    char TRUE_U = 'u';
    char TRUE_E = 'e';
    char FALSE_F = 'f';
    char FALSE_A = 'a';
    char FALSE_L = 'l';
    char FALSE_S = 's';
    char FALSE_E = 'e';
    char NULL_N = 'n';
    char NULL_U = 'u';
    char NULL_L = 'l';
 
    char ZERO = '0';
    char NINE = '9';
    char DOT = '.';
    char MINUS = '-';
    char PLUS = '+';
    char EXP_LOWERCASE = 'e';
    char EXP_UPPERCASE = 'E';
    char ESCAPE_CHAR = '\\';
    
    char TAB = '\t';
    char BACKSPACE = '\b';
    char FORMFEED = '\f';
    char CR = '\r';

    String NULL = "null".intern();
    
    static final byte START_ARRAY = (byte) Event.START_ARRAY.ordinal();
    static final byte START_OBJECT = (byte) Event.START_OBJECT.ordinal();
    static final byte KEY_NAME=(byte) Event.KEY_NAME.ordinal();
    static final byte VALUE_STRING=(byte) Event.VALUE_STRING.ordinal(); 
    static final byte VALUE_NUMBER=(byte) Event.VALUE_NUMBER.ordinal(); 
    static final byte VALUE_TRUE=(byte) Event.VALUE_TRUE.ordinal();
    static final byte VALUE_FALSE=(byte) Event.VALUE_FALSE.ordinal(); 
    static final byte VALUE_NULL=(byte) Event.VALUE_NULL.ordinal();
    static final byte END_OBJECT=(byte) Event.END_OBJECT.ordinal();
    static final byte END_ARRAY=(byte) Event.END_ARRAY.ordinal();
    
    static final byte COMMA_EVENT=Byte.MAX_VALUE;
    static final byte KEY_SEPARATOR_EVENT=Byte.MIN_VALUE;
    
    static final Event[] EVT_MAP =Event.values();
    
}
