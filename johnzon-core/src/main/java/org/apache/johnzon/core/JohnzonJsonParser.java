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
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;

/**
 * JsonParser with extended functionality
 */
public interface JohnzonJsonParser extends JsonParser {

    boolean isNotTooLong();

    /**
     * @return the _current_ Event. That's the one returned by the previous call to {@link #next()}
     *          but without propagating the Event pointer to the next entry.
     */
    Event current();

    public static class JohnzonJsonParserWrapper implements JohnzonJsonParser {
        public Event current() {
            throw new UnsupportedOperationException("getting the current JsonParser Event is not supported");
        }

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
    }
}
