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
package org.apache.fleece.mapper.converter;

import org.apache.fleece.mapper.Converter;

import java.util.HashMap;
import java.util.Map;

public class EnumConverter<T extends Enum<T>> implements Converter<T> {
    private final Map<String, T> values;

    public EnumConverter(final Class<T> aClass) {
        final T[] enumConstants = aClass.getEnumConstants();
        values = new HashMap<String, T>(enumConstants.length);
        for (final T t : enumConstants) {
            values.put(t.name(), t);
        }
    }

    @Override // no need of cache here, it is already fast
    public String toString(final T instance) {
        return instance.name();
    }

    @Override
    public T fromString(final String text) {
        return values.get(text);
    }
}
