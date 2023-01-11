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
import java.util.function.Supplier;

import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonLocation;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;

import org.apache.johnzon.mapper.jsonp.RewindableJsonParser;

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
    
    public static JsonParser createFor(final JsonValue jsonValue,
                                       final Supplier<JsonParserFactory> parserFactoryProvider) {
        return new RewindableJsonParser(doCreate(jsonValue, parserFactoryProvider));
    }

    private static JsonParser doCreate(final JsonValue jsonValue,
                                       final Supplier<JsonParserFactory> parserFactoryProvider) {
        switch (jsonValue.getValueType()) {
            case OBJECT: return parserFactoryProvider.get().createParser(jsonValue.asJsonObject());
            case ARRAY: return parserFactoryProvider.get().createParser(jsonValue.asJsonArray());
            case STRING: return new JsonStringParserAdapter((JsonString) jsonValue);
            case NUMBER: return new JsonNumberParserAdapter((JsonNumber) jsonValue);
            default: return new JsonValueParserAdapter<>(jsonValue);
        }
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
