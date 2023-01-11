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

import jakarta.json.stream.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class JsonGeneratorFactoryImplTest {
    @Test
    public void boundedOutputStream() throws UnsupportedEncodingException {
        final Map<String, Object> boundedConfig = new HashMap<>();
        boundedConfig.put(JsonGeneratorFactoryImpl.BOUNDED_OUTPUT_STREAM_WRITER_LEN, 1);
        boundedConfig.put(JsonGeneratorFactoryImpl.GENERATOR_BUFFER_LENGTH, 1);

        final ByteArrayOutputStream bounded = new ByteArrayOutputStream();
        final ByteArrayOutputStream defaultOut = new ByteArrayOutputStream();

        try (final JsonGenerator boundedGenerator = new JsonGeneratorFactoryImpl(boundedConfig).createGenerator(bounded);
             final JsonGenerator defaultGenerator = new JsonGeneratorFactoryImpl(emptyMap()).createGenerator(defaultOut)) {
            assertEquals(0, defaultOut.size());
            assertEquals(0, bounded.size());

            boundedGenerator.writeStartObject();
            defaultGenerator.writeStartObject();
            assertEquals(0, defaultOut.size());
            assertEquals(0, bounded.size());

            boundedGenerator.write("k", "val");
            defaultGenerator.write("k", "val");
            assertEquals(0, defaultOut.size());
            assertEquals(8, bounded.size());
            // this is the interesting part, there is still some buffering in the StreamEncoder due to
            // encoding logic but it flushes "often enough" for our usage
            assertEquals("{\"k\":\"va", bounded.toString("UTF-8"));

            boundedGenerator.writeEnd();
            defaultGenerator.writeEnd();
            assertEquals(0, defaultOut.size());
            assertEquals(9, bounded.size());
        }

        assertArrayEquals(bounded.toByteArray(), defaultOut.toByteArray());
        assertEquals("{\"k\":\"val\"}", bounded.toString("UTF-8"));
    }
}
