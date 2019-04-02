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
package org.apache.johnzon.jsonb;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.function.Supplier;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;

class JsonValueParserAdapter<T extends JsonValue> implements JsonParser {
    
    private static class JsonStringParserAdapter extends JsonValueParserAdapter<JsonString> {

        public JsonStringParserAdapter(JsonString jsonValue) {
            super(jsonValue);
        }
        
        @Override
        public String getString() {
            return getValue().getString();
        }
    }
    
    private static class JsonNumberParserAdapter extends JsonValueParserAdapter<JsonNumber> {
        
        public JsonNumberParserAdapter(JsonNumber jsonValue) {
            super(jsonValue);
        }

        @Override
        public boolean isIntegralNumber() {
            return getValue().isIntegral();
        }

        @Override
        public int getInt() {
            return getValue().intValueExact();
        }

        @Override
        public long getLong() {
            return getValue().longValueExact();
        }

        @Override
        public BigDecimal getBigDecimal() {
            return getValue().bigDecimalValue();
        }
    }
    
    public static JsonParser createFor(JsonValue jsonValue, Supplier<JsonParserFactory> parserFactoryProvider) {
        if (jsonValue instanceof JsonObject) {
            return parserFactoryProvider.get().createParser((JsonObject) jsonValue);
        } else if (jsonValue instanceof JsonArray) {
            return parserFactoryProvider.get().createParser((JsonArray) jsonValue);
        } else if (jsonValue instanceof JsonString) {
            return new JsonStringParserAdapter((JsonString) jsonValue);
        } else if (jsonValue instanceof JsonNumber) {
            return new JsonNumberParserAdapter((JsonNumber) jsonValue);
        } else if (EnumSet.of(ValueType.FALSE, ValueType.TRUE).contains(jsonValue.getValueType())) {
            return new JsonValueParserAdapter<>(jsonValue);
        }
        throw new IllegalArgumentException("Cannot create JsonParser for " + jsonValue.getValueType());
    }
    
    private final T jsonValue;
    
    JsonValueParserAdapter(T jsonValue) {
        this.jsonValue = jsonValue;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public Event next() {
        throw new UnsupportedOperationException("next() no supported for " + jsonValue.getValueType());
    }

    @Override
    public String getString() {
        throw new UnsupportedOperationException("next() no supported for " + jsonValue.getValueType());
    }

    @Override
    public boolean isIntegralNumber() {
        throw new UnsupportedOperationException("isIntegralNumber() not supported for " + jsonValue.getValueType());
    }

    @Override
    public int getInt() {
        throw new UnsupportedOperationException("getInt() not supported for " + jsonValue.getValueType());
    }

    @Override
    public long getLong() {
        throw new UnsupportedOperationException("getLong() not supported for " + jsonValue.getValueType());
    }

    @Override
    public BigDecimal getBigDecimal() {
        throw new UnsupportedOperationException("getBigDecimal() not supported for " + jsonValue.getValueType());
    }

    @Override
    public JsonLocation getLocation() {
        throw new UnsupportedOperationException("getLocation() not supported for " + jsonValue.getValueType());
    }

    @Override
    public void close() {
        // no-op
    }
    
    @Override
    public T getValue() {
        return jsonValue;
    }
}
