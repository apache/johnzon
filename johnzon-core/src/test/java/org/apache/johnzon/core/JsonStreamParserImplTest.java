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

import javax.json.stream.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;

public class JsonStreamParserImplTest {
    @Test
    public void ensureNoArrayBoundErrorWhenOverflow() throws IOException {
        final String json = new JsonObjectBuilderImpl(
            emptyMap(),
            BufferStrategyFactory.valueOf("QUEUE").newCharProvider(100),
            RejectDuplicateKeysMode.TRUE, (JsonProviderImpl) JsonProviderImpl.provider())
                .add("content", "{\"foo\":\"barbar\\barbarbar\"}")
                .build()
                .toString();
        final JsonParser parser = new JsonStreamParserImpl(new ByteArrayInputStream(json
                .getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8,
                10,
                BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10),
                BufferStrategyFactory.valueOf("QUEUE").newCharProvider(10),
                true, (JsonProviderImpl) JsonProviderImpl.provider());
        final List<String> events = new ArrayList<>();
        while (parser.hasNext()) {
            final JsonParser.Event event = parser.next();
            events.add(event.name());
            switch (event) {
                case VALUE_STRING:
                    events.add(parser.getString());
                    break;
                default:
            }
        }
        parser.close();
        assertEquals(
                asList("START_OBJECT", "KEY_NAME", "VALUE_STRING", "{\"foo\":\"barbar\\barbarbar\"}", "END_OBJECT"),
                events);
    }
}
