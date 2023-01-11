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
package org.apache.johnzon.websocket.endpoint;

import org.apache.johnzon.websocket.jsr.JsrObjectDecoder;
import org.apache.johnzon.websocket.jsr.JsrObjectEncoder;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/jsrserver", encoders = JsrObjectEncoder.class, decoders = JsrObjectDecoder.class)
public class JsrServerEndpointImpl {
    public static final List<JsonObject> MESSAGES = new LinkedList<JsonObject>();
    public static final Semaphore SEMAPHORE = new Semaphore(0);

    @OnMessage
    public synchronized void on(final Session session, final JsonObject message) throws IOException, EncodeException {
        MESSAGES.add(message);
        SEMAPHORE.release();

        final JsonObjectBuilder builder = Json.createBuilderFactory(Collections.<String, Object>emptyMap()).createObjectBuilder();
        session.getBasicRemote().sendObject(builder.add("value", "jsr@server").build());
    }
}
