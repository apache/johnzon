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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.json.bind.config.BinaryDataStrategy;

import org.junit.Test;

public class DynamicBufferResizingTest {
    @Test
    public void main() throws Exception {
        final JsonbConfig config = new JsonbConfig()
                .withFormatting(Boolean.TRUE)
                .withBinaryDataStrategy(BinaryDataStrategy.BASE_64);
        Jsonb jsonb = JsonbBuilder.create(config);

        final Request request = new Request("Screenshot.png", "image/png", new byte[558140]);
        final String json = jsonb.toJson(request);

        // the first call works
        for (int i = 0; i < 10; i++) { // originally the second call was failling
            final Request fromJson = jsonb.fromJson(json, Request.class);
            assertEquals("Screenshot.png", fromJson.name);
            assertEquals("image/png", fromJson.mimeType);
            assertEquals(558140, fromJson.body.length);
        }

        jsonb.close();
    }

    @Test(expected = JsonbException.class)
    public void failOnMissingProp() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty("jsonb.creator-parameters-required", true))) {
            jsonb.fromJson(jsonb.toJson(new Request("Screenshot.png", null, null)), Request.class);
        }
    }

    @Test
    public void dontFailOnMissingPropsIfConfiguredAsSuch() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty("johnzon.failOnMissingCreatorValues", false))) {
            final Request request = new Request("Screenshot.png", null, null);
            final String json = jsonb.toJson(request);

            final Request fromJson = jsonb.fromJson(json, Request.class);
            assertEquals("Screenshot.png", fromJson.name);
            assertNull(fromJson.mimeType);
            assertNull(fromJson.body);
        }
    }

    @JsonbPropertyOrder(value = {"name", "mimeType"})
    public static class Request {
        private String name;
        private String mimeType;
        private byte[] body;

        @JsonbCreator
        public Request(
                final @JsonbProperty("name") String name,
                final @JsonbProperty("mimeType") String mimeType,
                final @JsonbProperty("body") byte[] body) {
            this.name = name;
            this.mimeType = mimeType;
            this.body = body;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public byte[] getBody() {
            return body;
        }

        public void setBody(byte[] body) {
            this.body = body;
        }
    }
}
