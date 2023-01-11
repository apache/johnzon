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
package org.apache.johnzon.websocket;

import org.apache.johnzon.websocket.endpoint.JsrClientEndpointImpl;
import org.apache.johnzon.websocket.endpoint.JsrServerEndpointImpl;
import org.apache.johnzon.websocket.endpoint.Message;
import org.apache.johnzon.websocket.endpoint.ServerReport;
import org.apache.johnzon.websocket.internal.jsr.FactoryLocator;
import org.apache.johnzon.websocket.internal.jsr.JsrDecoder;
import org.apache.johnzon.websocket.internal.jsr.JsrEncoder;
import org.apache.johnzon.websocket.jsr.JsrObjectDecoder;
import org.apache.johnzon.websocket.jsr.JsrObjectEncoder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.net.URL;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class JsrCodecTest {
    @Deployment(testable = false)
    public static WebArchive war() {
        return ShrinkWrap.create(WebArchive.class, "jsr-codec.war")
                .addClasses(JsrServerEndpointImpl.class, ServerReport.class, Message.class /* for report endpoint */)
                .addAsLibraries(
                        ShrinkWrap.create(JavaArchive.class, "johnzon-websocket.jar")
                                .addClasses(FactoryLocator.class, JsrDecoder.class, JsrEncoder.class, JsrObjectDecoder.class, JsrObjectEncoder.class)
                                )
                ;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void codec() throws Exception {
        JsrClientEndpointImpl.MESSAGES.clear();

        final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        final Session session = container.connectToServer(
                JsrClientEndpointImpl.class,
                new URI("ws://localhost:" + url.getPort() + url.getPath() + "jsrserver"));

        session.getBasicRemote().sendObject(Json.createObjectBuilder().add("value", "jsr@client").build());

        JsrClientEndpointImpl.SEMAPHORE.acquire();

        // it does wait for the server, using same jaxrs provider to match format, it uses jettison which is weird but we don't care for that part of test
        final JsonObject serverMessage = Json.createReader(new URL(url.toExternalForm() + "report/jsr").openStream()).readObject();

        session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "bye"));

        assertNotNull(serverMessage);
        assertEquals("jsr@client", serverMessage.getString("value"));
        assertEquals(1, JsrClientEndpointImpl.MESSAGES.size());
        assertEquals("jsr@server", JsrClientEndpointImpl.MESSAGES.iterator().next().getString("value"));
    }
}
