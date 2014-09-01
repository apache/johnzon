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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

class JsonObjectBuilderImpl implements JsonObjectBuilder, Serializable {
    private Map<String, JsonValue> tmpMap;

    @Override
    public JsonObjectBuilder add(final String name, final JsonValue value) {
        putValue(name, value);
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final String value) {
        putValue(name, new JsonStringImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final BigInteger value) {
        putValue(name, new JsonNumberImpl(new BigDecimal(value)));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final BigDecimal value) {
        putValue(name, new JsonNumberImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final int value) {
        putValue(name, new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final long value) {
        putValue(name, new JsonLongImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final double value) {
        putValue(name, new JsonDoubleImpl(value));
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final boolean value) {
        putValue(name, value ? JsonValue.TRUE : JsonValue.FALSE);
        return this;
    }

    @Override
    public JsonObjectBuilder addNull(final String name) {
        putValue(name, JsonValue.NULL);
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final JsonObjectBuilder builder) {
        putValue(name, builder.build());
        return this;
    }

    @Override
    public JsonObjectBuilder add(final String name, final JsonArrayBuilder builder) {
        putValue(name, builder.build());
        return this;
    }
    
    private void putValue(String name, JsonValue value){
        if(name == null || value == null) {
            throw npe();
        }
        
        if(tmpMap==null){
            tmpMap=new LinkedHashMap<String, JsonValue>();
        }
        
        tmpMap.put(name, value);
    }
    
    private static NullPointerException npe() {
        return new NullPointerException("name or value/builder must not be null");
    }

    @Override
    public JsonObject build() {
        
        if(tmpMap==null) {
            return new JsonObjectImpl(Collections.EMPTY_MAP);
        } else {
            Map<String, JsonValue> dump = (Collections.unmodifiableMap(tmpMap));
            tmpMap=null;
            return new JsonObjectImpl(dump);
        }
        
        
    }
}
