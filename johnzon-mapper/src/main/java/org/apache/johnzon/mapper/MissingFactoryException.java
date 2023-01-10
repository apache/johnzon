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

import jakarta.json.JsonObject;
import java.lang.reflect.Constructor;
import java.util.stream.Stream;


public class MissingFactoryException extends MapperException {
    public MissingFactoryException(final Class<?> clazz, final JsonObject object, final String json) {
        super(message(clazz, object, json));
    }

    private static String message(final Class<?> clazz, final JsonObject object, final String json) {
        if (clazz.isArray()) {
            return String.format("%s array not a suitable datatype for %s: %s",
                    clazz.getSimpleName(),
                    ExceptionMessages.description(object),
                    json);
        }

        if (clazz.isInterface()) {
            return String.format("%s is an interface and requires an adapter or factory.  Cannot deserialize %s: %s%n%s not instantiable",
                    clazz.getSimpleName(),
                    ExceptionMessages.description(object),
                    json,
                    clazz);
        }

        String message = String.format("%s has no suitable constructor or factory.  Cannot deserialize %s: %s",
                clazz.getSimpleName(),
                ExceptionMessages.description(object),
                json);

        final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        final long constructorsWithParameters = Stream.of(constructors)
                .filter(constructor -> constructor.getParameterTypes().length > 0)
                .count();

        // Was a constructor with parameters our only option?  If so, help people
        // learn how to properly use constructors with parameters
        if (constructorsWithParameters > 0 && constructors.length == constructorsWithParameters) {
            message += "\nUse Johnzon @ConstructorProperties or @JsonbCreator if constructor arguments are needed";
        }

        // Add full class name as final detail
        message += "\n" + clazz + " not instantiable";
        return message;
    }
}
