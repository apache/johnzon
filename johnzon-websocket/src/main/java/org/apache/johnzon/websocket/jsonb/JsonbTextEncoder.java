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
package org.apache.johnzon.websocket.jsonb;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;
import java.io.Writer;

public class JsonbTextEncoder implements Encoder.TextStream<Object> {
    private Jsonb jsonb;

    @Override
    public void init(final EndpointConfig endpointConfig) {
        jsonb = JsonbLocatorDelegate.locate();
    }

    @Override
    public void destroy() {
        // no-op
    }

    @Override
    public void encode(final Object object, final Writer writer) throws EncodeException {
        try {
            jsonb.toJson(object, writer);
        } catch (final JsonbException je) {
            throw new EncodeException(object, je.getMessage(), je);
        }
    }
}
