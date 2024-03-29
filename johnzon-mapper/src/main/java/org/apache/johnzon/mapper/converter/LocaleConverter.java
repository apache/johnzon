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

import org.apache.johnzon.mapper.TypeAwareAdapter;
import org.apache.johnzon.mapper.internal.AdapterKey;

import java.lang.reflect.Type;
import java.util.Locale;

// from [lang]
public class LocaleConverter implements TypeAwareAdapter<Locale, String> {
    private final AdapterKey key = new AdapterKey(Locale.class, String.class);

    @Override
    public Type getTo() {
        return key.getTo();
    }

    @Override
    public Type getFrom() {
        return key.getFrom();
    }

    @Override
    public AdapterKey getKey() {
        return key;
    }

    @Override
    public String from(final Locale instance) {
        return instance.toLanguageTag();
    }

    @Override
    public Locale to(final String locale) {
        if (locale == null) {
            return null;
        }

        return Locale.forLanguageTag(locale);
    }
}
