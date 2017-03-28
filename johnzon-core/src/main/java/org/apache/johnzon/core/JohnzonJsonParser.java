/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.core;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;

/**
 * JsonParser with extended functionality
 */
public interface JohnzonJsonParser extends JsonParser {

    boolean isNotTooLong();


    public static class JohnzonJsonParserWrapper implements JohnzonJsonParser {
        private final JsonParser jsonParser;

        public JohnzonJsonParserWrapper(JsonParser jsonParser) {
            this.jsonParser = jsonParser;
        }

        @Override
        public boolean isNotTooLong() {
            return true;
        }

        @Override
        public boolean hasNext() {
            return jsonParser.hasNext();
        }

        @Override
        public Event next() {
            return jsonParser.next();
        }

        @Override
        public String getString() {
            return jsonParser.getString();
        }

        @Override
        public boolean isIntegralNumber() {
            return jsonParser.isIntegralNumber();
        }

        @Override
        public int getInt() {
            return jsonParser.getInt();
        }

        @Override
        public long getLong() {
            return jsonParser.getLong();
        }

        @Override
        public BigDecimal getBigDecimal() {
            return jsonParser.getBigDecimal();
        }

        @Override
        public JsonLocation getLocation() {
            return jsonParser.getLocation();
        }

        @Override
        public void close() {
            jsonParser.close();
        }

        @Override
        public JsonObject getObject() {
            return jsonParser.getObject();
        }

        @Override
        public JsonValue getValue() {
            return jsonParser.getValue();
        }

        @Override
        public JsonArray getArray() {
            return jsonParser.getArray();
        }

        @Override
        public Stream<JsonValue> getArrayStream() {
            return jsonParser.getArrayStream();
        }

        @Override
        public Stream<Map.Entry<String, JsonValue>> getObjectStream() {
            return jsonParser.getObjectStream();
        }

        @Override
        public Stream<JsonValue> getValueStream() {
            return jsonParser.getValueStream();
        }

        @Override
        public void skipArray() {
            jsonParser.skipArray();
        }

        @Override
        public void skipObject() {
            jsonParser.skipObject();
        }
    }
}
