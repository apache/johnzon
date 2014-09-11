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
package org.apache.johnzon.mapper.converter;

import org.apache.johnzon.mapper.Converter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CachedDelegateConverter<T> implements Converter<T> {
    private final ConcurrentMap<T, String> strings = new ConcurrentHashMap<T, String>();
    private final ConcurrentMap<String, T> values = new ConcurrentHashMap<String, T>();
    private final Converter<T> delegate;

    public CachedDelegateConverter(final Converter<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public String toString(final T instance) {
        String v = strings.get(instance);
        if (v == null) {
            v = delegate.toString(instance);
            strings.putIfAbsent(instance, v);
        }
        return v;
    }

    @Override
    public T fromString(final String text) {
        T v = values.get(text);
        if (v == null) {
            v = delegate.fromString(text);
            values.putIfAbsent(text, v);
        }
        return v;
    }
}
