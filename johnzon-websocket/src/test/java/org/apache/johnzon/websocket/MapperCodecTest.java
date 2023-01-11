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

import org.apache.johnzon.mapper.MapperBuilder;
import org.apache.johnzon.websocket.endpoint.ClientEndpointImpl;
import org.apache.johnzon.websocket.endpoint.Message;
import org.apache.johnzon.websocket.endpoint.ServerEndpointImpl;
import org.apache.johnzon.websocket.endpoint.ServerReport;
import org.apache.johnzon.websocket.internal.mapper.MapperLocator;
import org.apache.johnzon.websocket.internal.mapper.MapperLocatorDelegate;
import org.apache.johnzon.websocket.internal.servlet.IgnoreIfMissing;
import org.apache.johnzon.websocket.mapper.JohnzonTextDecoder;
import org.apache.johnzon.websocket.mapper.JohnzonTextEncoder;
import org.apache.openejb.arquillian.common.IO;
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
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class MapperCodecTest {
    @Deployment(testable = false)
    public static WebArchive war() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "codec.war")
                                                .addClasses(ServerEndpointImpl.class, ServerReport.class, Message.class)
                                                .addAsLibrary(
                                                    ShrinkWrap.create(JavaArchive.class, "johnzon-websocket.jar")
                                                              .addClasses(MapperLocator.class,
                                                                          MapperLocatorDelegate.class,
                                                                          IgnoreIfMissing.class,
                                                                          JohnzonTextDecoder.class,
                                                                          JohnzonTextEncoder.class));

        System.out.println(war.toString(true));
        return war;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void codec() throws Exception {
        ClientEndpointImpl.MESSAGES.clear();
        ClientEndpointImpl.SEMAPHORE.acquire(ClientEndpointImpl.SEMAPHORE.availablePermits());

        final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        final Session session = container.connectToServer(
                ClientEndpointImpl.class,
                new URI("ws://localhost:" + url.getPort() + url.getPath() + "server"));

        session.getBasicRemote().sendObject(new Message("client"));

        ClientEndpointImpl.SEMAPHORE.acquire();

        // it does wait for the server, using same jaxrs provider to match format, it uses jettison which is weird but we don't care for that part of test
        final Message serverMessage = new MapperBuilder().build().readObject(IO.slurp(new URL(url.toExternalForm() + "report/annotation")), Message.class);

        session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "bye"));

        assertNotNull(serverMessage);
        assertEquals("client", serverMessage.getValue());
        assertEquals(1, ClientEndpointImpl.MESSAGES.size());
        assertEquals("server", ClientEndpointImpl.MESSAGES.iterator().next().getValue());
    }
}
