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
import java.io.Writer;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

public abstract class JsrEncoder<T> implements Encoder.TextStream<T> {
    private JsonWriterFactory factory;

    protected abstract void doWrite(JsonWriter writer, T t);

    @Override
    public void init(final EndpointConfig endpointConfig) {
        factory = FactoryLocator.writerLocate();
    }

    @Override
    public void destroy() {
        // no-op
    }

    @Override
    public void encode(final T t, final Writer writer) throws EncodeException, IOException {
        final JsonWriter jsonWriter = factory.createWriter(writer);
        try {
            doWrite(jsonWriter, t);
        } finally {
            jsonWriter.close();
        }
    }
}
