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
package org.apache.johnzon.jaxrs;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.transport.local.LocalConduit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.util.Iterator;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class JsrProviderTest {
    private final static String ENDPOINT_ADDRESS = "local://johnzon";
    private static Server server;

    @BeforeClass
    public static void bindEndpoint() throws Exception {
        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(JohnzonResource.class);
        sf.setProviders(asList(new JsrProvider()));
        sf.setResourceProvider(JohnzonResource.class, new SingletonResourceProvider(new JohnzonResource(), false));
        sf.setAddress(ENDPOINT_ADDRESS);
        server = sf.create();
    }

    @AfterClass
    public static void unbind() throws Exception {
        server.stop();
        server.destroy();
    }

    @Test
    public void object() {
        final JsonObject object = client().path("johnzon/object").get(JsonObject.class);
        assertEquals(2, object.size());
        for (int i = 1; i <= 2; i++) {
            assertEquals(i, object.getInt(Character.toString((char) ('a' + i - 1))));
        }
    }

    @Test
    public void array() {
        final JsonArray array = client().path("johnzon/array").get(JsonArray.class);
        assertEquals(2, array.size());
        final Iterator<JsonValue> ints = array.iterator();
        for (int i = 1; i <= 2; i++) {
            final JsonValue next = ints.next();
            assertEquals(JsonValue.ValueType.NUMBER, next.getValueType());
            assertEquals(i, JsonNumber.class.cast(next).intValue());
        }
    }

    private static WebClient client() {
        final WebClient client = WebClient.create(ENDPOINT_ADDRESS, asList(new JsrProvider())).accept(MediaType.APPLICATION_JSON_TYPE);
        WebClient.getConfig(client).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        return client;
    }

    @Path("johnzon")
    public static class JohnzonResource {
        @GET
        @Path("array")
        public JsonArray array() {
            return Json.createArrayBuilder().add(1).add(2).build();
        }

        @GET
        @Path("object")
        public JsonObject object() {
            return Json.createObjectBuilder().add("a", 1).add("b", 2).build();
        }
    }
}
