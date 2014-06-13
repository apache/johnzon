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
package org.apache.fleece.jaxrs;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.transport.local.LocalConduit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FleeceProviderTest {
    private final static String ENDPOINT_ADDRESS = "local://fleece";
    private static Server server;

    @BeforeClass
    public static void bindEndpoint() throws Exception {
        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(FleeceResource.class);
        sf.setProviders(asList(new FleeceProvider<Object>()));
        sf.setResourceProvider(FleeceResource.class, new SingletonResourceProvider(new FleeceResource(), false));
        sf.setAddress(ENDPOINT_ADDRESS);
        server = sf.create();
    }

    @AfterClass
    public static void unbind() throws Exception {
        server.stop();
        server.destroy();
    }

    @Test
    public void asParam() {
        final String result = client().path("fleece").type(MediaType.APPLICATION_JSON_TYPE).post(new Fleece("client")).readEntity(String.class);
        assertTrue(Boolean.parseBoolean(result));
    }

    @Test
    public void object() {
        final Fleece fleece = client().path("fleece").get(Fleece.class);
        assertEquals("fleece", fleece.getName());
    }

    @Test
    public void array() {
        final Fleece[] fleece = client().path("fleece/all1").get(Fleece[].class);
        assertEquals(2, fleece.length);
        for (int i = 0; i < fleece.length; i++) {
            assertEquals("fleece" + (i + 1), fleece[i].getName());
        }
    }

    @Test
    public void list() {
        final ParameterizedType list = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{Fleece.class};
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
        final List<Fleece> fleeces = client().path("fleece/all2").get(new GenericType<List<Fleece>>(list));
        assertEquals(2, fleeces.size());
        int i = 1;
        for (final Fleece f : fleeces) {
            assertEquals("fleece" + i, f.getName());
            i++;
        }
    }

    private static WebClient client() {
        final WebClient client = WebClient.create(ENDPOINT_ADDRESS, asList(new FleeceProvider<Object>())).accept(MediaType.APPLICATION_JSON_TYPE);
        WebClient.getConfig(client).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        return client;
    }

    public static class Fleece {
        private String name;

        public Fleece(final String name) {
            this.name = name;
        }

        public Fleece() {
            // no-op
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Path("fleece")
    public static class FleeceResource {
        @GET
        public Fleece fleece() {
            return new Fleece("fleece");
        }

        @GET
        @Path("all1")
        public Fleece[] fleeces1() {
            return new Fleece[] { new Fleece("fleece1"), new Fleece("fleece2") };
        }

        @GET
        @Path("all2")
        public List<Fleece> fleeces2() {
            return asList(fleeces1());
        }

        @POST
        public String asParam(final Fleece f) {
            return Boolean.toString("client".equals(f.getName()));
        }
    }
}
