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
package org.apache.johnzon.websocket.internal.jsr;

import java.io.IOException;
import java.io.Reader;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;

public abstract class JsrDecoder<T> implements Decoder.TextStream<T> {
    private JsonReaderFactory factory;

    protected abstract T doRead(JsonReader jsonReader);

    @Override
    public void init(final EndpointConfig endpointConfig) {
        factory = FactoryLocator.readerLocate();
    }

    @Override
    public T decode(final Reader reader) throws DecodeException, IOException {
        final JsonReader jsonReader = factory.createReader(reader);
        try {
            return doRead(jsonReader);
        } finally {
            jsonReader.close();
        }
    }

    @Override
    public void destroy() {
        // no-op
    }
}
