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

import jakarta.json.stream.JsonParser;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class JsonInMemoryParserTest {
    @Test
    public void testSpecCurrentEvent() {
        JsonParser parser = new JsonInMemoryParser(
                new JsonObjectImpl(
                        Collections.emptyMap(),
                        BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10)),
                BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10),
                (JsonProviderImpl) JsonProviderImpl.provider());

        assertEquals(null, parser.currentEvent());

        parser.next();
        assertEquals(JsonParser.Event.START_OBJECT, parser.currentEvent());
    }


    @Test
    public void testJohnzonParserCurrent() {
        JohnzonJsonParser parser = new JsonInMemoryParser(
                new JsonObjectImpl(
                        Collections.emptyMap(),
                        BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10)),
                BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10),
                (JsonProviderImpl) JsonProviderImpl.provider());

        assertEquals(JsonParser.Event.START_OBJECT, parser.current());
    }
}
