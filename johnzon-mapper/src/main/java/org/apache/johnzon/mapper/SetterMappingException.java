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
import java.lang.reflect.Type;

public class SetterMappingException extends MapperException {

    public SetterMappingException(final Class<?> clazz, final String entryName, final Type type,
                                  final JsonValue.ValueType valueType, final String jsonValue, final Exception cause) {
        super(message(clazz, entryName, type, valueType, jsonValue, cause), cause);
    }

    private static String message(final Class<?> clazz, final String entryName, final Type type,
                                  final JsonValue.ValueType valueType, final String jsonValue, final Exception cause) {

        return String.format("%s property '%s' of type %s cannot be mapped to %s: %s%n%s",
                clazz.getSimpleName(),
                entryName,
                ExceptionMessages.simpleName(type),
                ExceptionMessages.description(valueType),
                jsonValue,
                cause.getMessage()
        );
    }

}
