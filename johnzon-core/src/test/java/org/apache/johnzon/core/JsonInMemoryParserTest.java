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

import org.junit.Test;

import javax.json.JsonArray;
import javax.json.stream.JsonParser;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class JsonInMemoryParserTest {
    @Test
    public void getBigDecimalChecksReceivingProviderScale() {
        final JsonProviderImpl permissiveProvider = new JsonProviderImpl();
        permissiveProvider.setMaxBigDecimalScale(4);
        final JsonArray array = permissiveProvider.createArrayBuilder()
                .add(new BigDecimal("1e2"))
                .add(new BigDecimal("1e3"))
                .build();

        final JsonProviderImpl strictProvider = new JsonProviderImpl();
        strictProvider.setMaxBigDecimalScale(2);
        try (JsonParser parser = strictProvider.createParserFactory(Collections.emptyMap()).createParser(array)) {
            assertEquals(JsonParser.Event.START_ARRAY, parser.next());
            assertEquals(JsonParser.Event.VALUE_NUMBER, parser.next());
            assertEquals(new BigDecimal("1e2"), parser.getBigDecimal());
            assertEquals(JsonParser.Event.VALUE_NUMBER, parser.next());
            assertThrows(ArithmeticException.class, parser::getBigDecimal);
        }
    }
}
