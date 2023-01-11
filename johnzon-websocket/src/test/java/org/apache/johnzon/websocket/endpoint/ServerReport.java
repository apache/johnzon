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

import org.apache.johnzon.mapper.MapperBuilder;

import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import jakarta.json.Json;
import jakarta.json.JsonWriter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("report")
@Produces(MediaType.APPLICATION_JSON)
public class ServerReport {
    @GET
    @Path("annotation")
    public String amessage() {
        try {
            if (!ServerEndpointImpl.SEMAPHORE.tryAcquire(1, TimeUnit.MINUTES)) {
                throw new IllegalStateException("acquire failed");
            }
        } catch (final InterruptedException e) {
            Thread.interrupted();
            return null;
        }

        // don't setup (+dependency) for just this method the mapper jaxrs provider
        return new MapperBuilder().build().writeObjectAsString(ServerEndpointImpl.MESSAGES.iterator().next());
    }

    @GET
    @Path("jsr")
    @Produces(MediaType.APPLICATION_JSON)
    public String pmessage() {
        try {
            if (!JsrServerEndpointImpl.SEMAPHORE.tryAcquire(1, TimeUnit.MINUTES)) {
                throw new IllegalStateException("acquire failed");
            }
        } catch (final InterruptedException e) {
            Thread.interrupted();
            return null;
        }

        // don't setup (+dependency) for just this method the jsr jaxrs provider
        final StringWriter output = new StringWriter();
        final JsonWriter writer = Json.createWriter(output);
        writer.write(JsrServerEndpointImpl.MESSAGES.iterator().next());
        writer.close();
        return output.toString();
    }
}
