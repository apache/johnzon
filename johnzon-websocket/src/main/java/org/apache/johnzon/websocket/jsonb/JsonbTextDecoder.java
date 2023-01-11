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

import org.apache.johnzon.websocket.internal.TypeAwareDecoder;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;
import java.io.Reader;
import java.lang.reflect.Type;

public class JsonbTextDecoder extends TypeAwareDecoder implements Decoder.TextStream<Object> {
    protected Jsonb mapper;
    protected Type type;

    public JsonbTextDecoder() {
        // no-op
    }

    // for client side no way to guess the type so let the user provide it easily
    public JsonbTextDecoder(final Type type) {
        this(null, type);
    }

    public JsonbTextDecoder(final Jsonb jsonb, final Type type) {
        super(type);
        this.mapper = jsonb;
    }

    @Override
    public Object decode(final Reader stream) throws DecodeException {
        try {
            return mapper.fromJson(stream, type);
        } catch (final JsonbException je) {
            throw new DecodeException("", je.getMessage(), je);
        }
    }

    @Override
    public void init(final EndpointConfig endpointConfig) {
        if (mapper == null) {
            mapper = JsonbLocatorDelegate.locate();
        }
        super.init(endpointConfig);
    }

    @Override
    public void destroy() {
        // no-op
    }
}
