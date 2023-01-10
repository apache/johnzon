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
package org.apache.johnzon.jsonb.adapter;

import org.apache.johnzon.mapper.MapperConfig;

import jakarta.json.bind.annotation.JsonbProperty;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class JsonbEnumAdapter<T extends Enum<T>> implements MapperConfig.CustomEnumConverter<T> {
    private final Map<String, T> values;
    private final Map<T, String> reversed;
    private final Class<T> enumType;

    public JsonbEnumAdapter(final Class<T> aClass) {
        this.enumType = aClass;

        final T[] enumConstants = aClass.isEnum() ?
                aClass.getEnumConstants() :
                (T[]) aClass.getSuperclass().getEnumConstants();
        values = new HashMap<>(enumConstants.length);
        reversed = new HashMap<>(enumConstants.length);
        for (final T t : enumConstants) {
            try {
                final Field field = findField(aClass, t.name());
                final JsonbProperty property = field.getAnnotation(JsonbProperty.class);
                final String name = property == null || property.value().isEmpty() ? t.name() : property.value();
                values.put(name, t);
                reversed.put(t, name);
            } catch (final Exception e) {
                values.put(t.name(), t);
                reversed.put(t, t.name());
            }
        }
    }

    @Override // no need of cache here, it is already fast
    public String toString(final T instance) {
        return instance != null ? reversed.get(instance) : null;
    }

    @Override
    public T fromString(final String text) {
        final T val = values.get(text);
        if (val == null) {
            throw new IllegalArgumentException("Illegal " + enumType + " enum value: " + text + ", known values: " + values.keySet());
        }
        return val;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + values.keySet();
    }

    @Override
    public Type type() {
        return enumType;
    }

    private Field findField(final Class<T> impl, final String field) {
        Class<?> type = impl;
        while (type != null && type != Object.class && type != Enum.class) {
            try {
                return type.getDeclaredField(field);
            } catch (final NoSuchFieldException e) {
                // continue
            }
            type = type.getSuperclass();
        }
        throw new IllegalArgumentException("Missing field: " + field);
    }
}
