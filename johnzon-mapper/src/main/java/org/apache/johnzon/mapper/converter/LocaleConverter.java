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

import org.apache.johnzon.mapper.Adapter;

import java.util.Locale;

// from [lang]
public class LocaleConverter implements Adapter<Locale, String> {
    @Override
    public String from(final Locale instance) {
        return instance.toString();
    }

    @Override
    public Locale to(final String locale) {
        if (locale == null) {
            return null;
        }
        final int len = locale.length();
        if (len != 2 && len != 5 && len < 7) {
            throw new IllegalArgumentException("Invalid locale format: " + locale);
        }
        final char ch0 = locale.charAt(0);
        final char ch1 = locale.charAt(1);
        if (ch0 < 'a' || ch0 > 'z' || ch1 < 'a' || ch1 > 'z') {
            throw new IllegalArgumentException("Invalid locale format: " + locale);
        }
        if (len == 2) {
            return new Locale(locale, "");
        }
        if (locale.charAt(2) != '_') {
            throw new IllegalArgumentException("Invalid locale format: " + locale);
        }
        final char ch3 = locale.charAt(3);
        if (ch3 == '_') {
            return new Locale(locale.substring(0, 2), "", locale.substring(4));
        }
        final char ch4 = locale.charAt(4);
        if (ch3 < 'A' || ch3 > 'Z' || ch4 < 'A' || ch4 > 'Z') {
            throw new IllegalArgumentException("Invalid locale format: " + locale);
        }
        if (len == 5) {
            return new Locale(locale.substring(0, 2), locale.substring(3, 5));
        }
        if (locale.charAt(5) != '_') {
            throw new IllegalArgumentException("Invalid locale format: " + locale);
        }
        return new Locale(locale.substring(0, 2), locale.substring(3, 5), locale.substring(6));
    }
}
