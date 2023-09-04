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

import jakarta.json.JsonValue;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.FALSE;
import static jakarta.json.JsonValue.ValueType.NUMBER;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static jakarta.json.JsonValue.ValueType.STRING;
import static jakarta.json.JsonValue.ValueType.TRUE;
import static java.util.Locale.ROOT;

public final class ExceptionMessages {

    private ExceptionMessages() {
    }

    public static String simpleName(final Type type) {
        if (type instanceof Class) {
            final Class<?> clazz = (Class<?>) type;
            return clazz.getSimpleName();
        }
        
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            return simpleName(parameterizedType.getRawType()) +
                    "<" +
                    join(",", parameterizedType.getActualTypeArguments()) +
                    ">";
        }
        
        if (type instanceof GenericArrayType) {
            final GenericArrayType genericArrayType = (GenericArrayType) type;
            return simpleName(genericArrayType.getGenericComponentType()) + "[]";
        }

        if (type instanceof WildcardType) {
            final WildcardType wildcardType = (WildcardType) type;

            if (wildcardType.getLowerBounds().length > 0) {
                return "? super " + join(" & ", wildcardType.getLowerBounds());
            }

            if (wildcardType.getUpperBounds().length > 0) {
                return "? extends " + join(" & ", wildcardType.getUpperBounds());
            }

            // This should never happen, but it's always safe to fallback on getTypeName()
            // so we do that instead of throwing an exception
            return wildcardType.getTypeName();
        }

        // Some Type derivative we've never seen.  Fallback on getTypeName()
        return type.getTypeName();
    }

    public static String description(final JsonValue value) {
        return description(value == null ? null : value.getValueType());
    }

    public static String description(final JsonValue.ValueType type) {
        if (type == OBJECT || type == ARRAY || type == STRING) {
            return "json " + type.toString().toLowerCase(ROOT) + " value";
        }
        if (type == NUMBER) {
            return "json numeric value";
        }
        if (type == TRUE || type == FALSE) {
            return "json boolean value";
        }

        return "json value";
    }

    private static String join(final String delimiter, final Type[] args) {
        if (args.length == 0) {
            return "";
        }

        final StringBuilder sb = new StringBuilder(simpleName(args[0]));

        for (int i = 1; i < args.length; i++) {
            sb.append(delimiter);
            sb.append(simpleName(args[i]));
        }

        return sb.toString();
    }
}
