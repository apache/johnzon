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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.json.stream.JsonParsingException;

class Strings implements JsonChars {

    private static final String UNICODE_PREFIX = "\\u";
    private static final String UNICODE_PREFIX_HELPER = "000";
    private static final ConcurrentMap<Character, String> UNICODE_CACHE = new ConcurrentHashMap<Character, String>();

    static char asEscapedChar(final char current) {
        switch (current) {
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'n':
                return '\n';
            case '"':
                return '\"';
            case '\\':
                return '\\';
            case '/':
                return '/';
            case '[': // todo: check - needed in tck
                return '[';
            case ']': // todo: check - needed in tck
                return ']';
            default:
                if(Character.isHighSurrogate(current) || Character.isLowSurrogate(current)) {
                    return current;
                }
                throw new JsonParsingException("Invalid escape sequence '"+current +"' (Codepoint: "+String.valueOf(current).
                        codePointAt(0),JsonLocationImpl.UNKNOWN_LOCATION);
        }

    }

    static void appendEscaped(final String value, final StringBuilder builder) {
        final int length = value.length();
        int nextStart = 0;
        for (int i = 0; i < length; i++) {
            final char c = value.charAt(i);
            if (c < SPACE || c == QUOTE_CHAR || c == ESCAPE_CHAR) {
                if (nextStart < i) {
                    builder.append(value, nextStart, i);
                }
                nextStart = i + 1;
                switch (c) {
                case QUOTE_CHAR:
                case ESCAPE_CHAR:
                    builder.append(ESCAPE_CHAR).append(c);
                    break;
                case EOL:
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                default:
                    builder.append(toUnicode(c));
                    break;
                }
            }
        }
        if (nextStart < length) {
            builder.append(value, nextStart, length);
        }
    }

    private static String toUnicode(final char c) {
        final String found = UNICODE_CACHE.get(c);
        if (found != null) {
            return found;
        }

        final String hex = UNICODE_PREFIX_HELPER + Integer.toHexString(c);
        final String s = UNICODE_PREFIX + hex.substring(hex.length() - 4);
        UNICODE_CACHE.putIfAbsent(c, s);
        return s;
    }

    private Strings() {
        // no-op
    }
}
