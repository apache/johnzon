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

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class JsonArrayBuilderImpl implements JsonArrayBuilder, Serializable {
    private List<JsonValue> tmpList;

    public JsonArrayBuilderImpl() {
    }

    public JsonArrayBuilderImpl(JsonArray initialData) {
        tmpList = new ArrayList<>(initialData);
    }

    public JsonArrayBuilderImpl(Collection<Object> initialData) {
        tmpList = new ArrayList<>();
        for (Object initialValue : initialData) {
            add(initialValue);
        }
    }

    public JsonArrayBuilder add(final Object value) {
        if (value instanceof JsonValue) {
            add((JsonValue) value);
        } else if (value instanceof BigDecimal) {
            add((BigDecimal) value);
        } else if (value instanceof BigInteger) {
            add((BigInteger) value);
        } else if (value instanceof Boolean) {
            add((boolean) value);
        } else if (value instanceof Double) {
            add((double) value);
        } else if (value instanceof Integer) {
            add((int) value);
        } else if (value instanceof Long) {
            add((long) value);
        } else if (value instanceof String) {
            add((String) value);
        } else {
            throw new JsonException("Illegal JSON type! type=" + value.getClass());
        }

        return this;
    }

        @Override
    public JsonArrayBuilder add(final JsonValue value) {
        addValue(value);
        return this;
    }

    @Override
    public JsonArrayBuilder add(final String value) {
        addValue(new JsonStringImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final BigDecimal value) {
        addValue(new JsonNumberImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final BigInteger value) {
        addValue(new JsonNumberImpl(new BigDecimal(value)));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final int value) {
        addValue(new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final long value) {
        addValue(new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final double value) {
        addValue(new JsonDoubleImpl(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(final boolean value) {
        addValue(value ? JsonValue.TRUE : JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonArrayBuilder addNull() {
        addValue(JsonValue.NULL);
        return this;
    }

    @Override
    public JsonArrayBuilder add(final JsonObjectBuilder builder) {
        addValue(builder.build());
        return this;
    }

    @Override
    public JsonArrayBuilder add(final JsonArrayBuilder builder) {
        addValue(builder.build());
        return this;
    }
    
    private void addValue(JsonValue value){
        if (value == null) {
            throw npe();
        }
        
        if(tmpList==null){
            tmpList=new ArrayList<>();
        }
        
        tmpList.add(value);
    }

    @Override
    public JsonArray build() {
        
        if(tmpList == null) {
            return new JsonArrayImpl(Collections.EMPTY_LIST);
        } else {
            List<JsonValue> dump = (Collections.unmodifiableList(tmpList));
            tmpList=null;
            return new JsonArrayImpl(dump);
        }
        
    }

    private static NullPointerException npe() {
        throw new NullPointerException("value/builder must not be null");
    }
}
