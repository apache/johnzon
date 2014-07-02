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
package org.apache.fleece.core;

import javax.json.JsonString;

public class JsonStringImpl implements JsonString {
    private final String value;
    private String escape;
    private Integer hashCode = null;

    public JsonStringImpl(final String value) {
        this(value, null);
    }

    public JsonStringImpl(final String value, final String escaped) {
        this.value = value;
        this.escape = escaped;
    }

    @Override
    public String getString() {
        return value;
    }

    @Override
    public CharSequence getChars() {
        return value;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.STRING;
    }

    @Override
    public String toString() {
        if (escape == null) {
            escape = Strings.escape(value);
        }
        return escape;
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = value.hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        return JsonString.class.isInstance(obj) && JsonString.class.cast(obj).getString().equals(value);
    }
}
