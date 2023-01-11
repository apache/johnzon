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
package org.apache.johnzon.core;

import jakarta.json.JsonString;
import java.io.Serializable;

final class JsonStringImpl implements JsonString, Serializable {
    private final String value;
    private String escape;
    private transient Integer hashCode = null;


    JsonStringImpl(final String value) {
        if(value == null) {
            throw new NullPointerException("value must not be null");
        }

        this.value = value;
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
        String s = escape;
        if (s == null) {
            final StringBuilder builder = new StringBuilder();
            Strings.appendEscaped(value, builder);
            s =  JsonChars.QUOTE_CHAR + builder.toString() + JsonChars.QUOTE_CHAR;
            escape=s;
        }
        return s;
    }

    @Override
    public int hashCode() {
        Integer h = hashCode;
        if (h == null) {
            h = value.hashCode();
            hashCode=h;
        }
        return h;
    }

    @Override
    public boolean equals(final Object obj) {
        return JsonString.class.isInstance(obj) && JsonString.class.cast(obj).getString().equals(value);
    }
}
