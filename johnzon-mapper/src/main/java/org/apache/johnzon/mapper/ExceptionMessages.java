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

import javax.json.JsonValue;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static javax.json.JsonValue.ValueType.ARRAY;
import static javax.json.JsonValue.ValueType.FALSE;
import static javax.json.JsonValue.ValueType.NUMBER;
import static javax.json.JsonValue.ValueType.OBJECT;
import static javax.json.JsonValue.ValueType.STRING;
import static javax.json.JsonValue.ValueType.TRUE;

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
            final StringBuilder sb = new StringBuilder();
            sb.append(simpleName(parameterizedType.getRawType()));
            sb.append("<");

            final Type[] args = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < args.length; i++) {
                final Type arg = args[i];
                sb.append(simpleName(arg));
                if (i < args.length - 1) {
                    sb.append(",");
                }
            }
            sb.append(">");
            return sb.toString();
        }
        return type.getTypeName();
    }

    public static String description(final JsonValue value) {
        return description(value == null ? null : value.getValueType());
    }

    public static String description(final JsonValue.ValueType type) {
        if (type == OBJECT || type == ARRAY || type == STRING) {
            return "json " + type.toString().toLowerCase() + " value";
        }
        if (type == NUMBER) {
            return "json numeric value";
        }
        if (type == TRUE || type == FALSE) {
            return "json boolean value";
        }

        return "json value";
    }
}
