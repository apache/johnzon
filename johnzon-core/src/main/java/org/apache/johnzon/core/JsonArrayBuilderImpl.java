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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

class JsonArrayBuilderImpl implements JsonArrayBuilder, Serializable {
    private List<JsonValue> tmpList;

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
            tmpList=new ArrayList<JsonValue>();
        }
        
        tmpList.add(value);
    }
    
    
    private void addValue(int index, JsonValue value){
        if (value == null) {
            throw npe();
        }
        
        if(tmpList==null){
            tmpList=new ArrayList<JsonValue>();
        }
        
        tmpList.add(index, value);
    }
    
    private void setValue(int index, JsonValue value){
        if (value == null) {
            throw npe();
        }
        
        if(tmpList==null){
            tmpList=new ArrayList<JsonValue>();
        }
        
        tmpList.set(index, value);
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

    @Override
    public JsonArrayBuilder addAll(JsonArrayBuilder builder) {    	
        addValue(builder.build()); //TODO is it ok to call build() here and destry the builder?
        return this;
    }

    @Override
    public JsonArrayBuilder add(int index, JsonValue value) {
        addValue(index, value);
        return this;
        
    }

    @Override
    public JsonArrayBuilder add(int index, String value) {
    	addValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(int index, BigDecimal value) {
    	addValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(int index, BigInteger value) {
    	addValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(int index, int value) {
    	addValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(int index, long value) {
    	addValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(int index, double value) {
    	addValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder add(int index, boolean value) {
    	addValue(index, value?JsonValue.TRUE:JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonArrayBuilder addNull(int index) {
    	addValue(index, JsonValue.NULL);
        return this;
    }

    @Override
    public JsonArrayBuilder add(int index, JsonObjectBuilder builder) {
    	addValue(index, builder.build()); //TODO ok to call build()?
        return this;
    }

    @Override
    public JsonArrayBuilder add(int index, JsonArrayBuilder builder) {
    	addValue(index, builder.build()); //TODO ok to call build()?
        return this;
    }

    @Override
    public JsonArrayBuilder set(int index, JsonValue value) {
    	setValue(index, value);
        return this;
    }

    @Override
    public JsonArrayBuilder set(int index, String value) {
    	setValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder set(int index, BigDecimal value) {
    	setValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder set(int index, BigInteger value) {
    	setValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder set(int index, int value) {
    	setValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder set(int index, long value) {
    	setValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder set(int index, double value) {
    	setValue(index, Json.createValue(value));
        return this;
    }

    @Override
    public JsonArrayBuilder set(int index, boolean value) {
    	setValue(index, value?JsonValue.TRUE:JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonArrayBuilder setNull(int index) {
    	setValue(index, JsonValue.NULL);
        return this;
    }

    @Override
    public JsonArrayBuilder set(int index, JsonObjectBuilder builder) {
    	setValue(index, builder.build()); //TODO ok here to call build?
        return this;
    }

    @Override
    public JsonArrayBuilder set(int index, JsonArrayBuilder builder) {
    	setValue(index, builder.build()); //TODO ok here to call build?
        return this;
    }

    @Override
    public JsonArrayBuilder remove(int index) {
    	
    	if(tmpList == null) return this;
    	
        tmpList.remove(index);
        return this;
    }
}
