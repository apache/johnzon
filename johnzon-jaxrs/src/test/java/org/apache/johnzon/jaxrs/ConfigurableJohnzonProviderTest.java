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

public class ConfigurableJohnzonProviderTest {
    private final static String ENDPOINT_ADDRESS = "local://johnzon";
    private static Server server;

    @BeforeClass
    public static void bindEndpoint() throws Exception {
        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(JohnzonResource.class);
        sf.setProviders(asList(new ConfigurableJohnzonProvider()));
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
    public void asParam() {
        final String result = client().path("johnzon").type(MediaType.APPLICATION_JSON_TYPE).post(new Johnzon("client")).readEntity(String.class);
        assertTrue(Boolean.parseBoolean(result));
    }

    @Test
    public void object() {
        final Johnzon johnzon = client().path("johnzon").get(Johnzon.class);
        assertEquals("johnzon", johnzon.getName());
    }

    @Test
    public void array() {
        final Johnzon[] johnzon = client().path("johnzon/all1").get(Johnzon[].class);
        assertEquals(2, johnzon.length);
        for (int i = 0; i < johnzon.length; i++) {
            assertEquals("johnzon" + (i + 1), johnzon[i].getName());
        }
    }

    @Test
    public void list() {
        final ParameterizedType list = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{Johnzon.class};
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
        final List<Johnzon> johnzons = client().path("johnzon/all2").get(new GenericType<List<Johnzon>>(list));
        assertEquals(2, johnzons.size());
        int i = 1;
        for (final Johnzon f : johnzons) {
            assertEquals("johnzon" + i, f.getName());
            i++;
        }
    }

    private static WebClient client() {
        final WebClient client = WebClient.create(ENDPOINT_ADDRESS, asList(new JohnzonProvider<Object>())).accept(MediaType.APPLICATION_JSON_TYPE);
        WebClient.getConfig(client).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        return client;
    }

    public static class Johnzon {
        private String name;

        public Johnzon(final String name) {
            this.name = name;
        }

        public Johnzon() {
            // no-op
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Path("johnzon")
    public static class JohnzonResource {
        @GET
        public Johnzon johnzon() {
            return new Johnzon("johnzon");
        }

        @GET
        @Path("all1")
        public Johnzon[] johnzons1() {
            return new Johnzon[] { new Johnzon("johnzon1"), new Johnzon("johnzon2") };
        }

        @GET
        @Path("all2")
        public List<Johnzon> johnzons2() {
            return asList(johnzons1());
        }

        @POST
        public String asParam(final Johnzon f) {
            return Boolean.toString("client".equals(f.getName()));
        }
    }
}
