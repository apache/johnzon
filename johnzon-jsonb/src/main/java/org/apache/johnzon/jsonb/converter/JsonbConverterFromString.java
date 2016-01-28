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
package org.apache.johnzon.jsonb.converter;

import org.apache.johnzon.mapper.Converter;

import javax.json.bind.JsonbException;
import javax.json.bind.adapter.JsonbAdapter;

public class JsonbConverterFromString<T> implements Converter<T> {
    private final JsonbAdapter<String, T> adapter;

    public JsonbConverterFromString(final JsonbAdapter<String, T> adapter) {
        this.adapter = adapter;
    }

    @Override
    public String toString(final T instance) {
        try {
            return adapter.adaptTo(instance);
        } catch (final Exception e) {
            throw new JsonbException(e.getMessage(), e);
        }
    }

    @Override
    public T fromString(final String text) {
        try {
            return adapter.adaptFrom(text);
        } catch (final Exception e) {
            throw new JsonbException(e.getMessage(), e);
        }
    }
}
