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

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;

public class JsonbLocatorDelegate implements ServletContextListener {
    private static final Map<ClassLoader, Jsonb> BY_LOADER = new ConcurrentHashMap<>();
    private static final String ATTRIBUTE = JsonbLocator.class.getName() + ".jsonb";

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        final ServletContext servletContext = servletContextEvent.getServletContext();
        final Jsonb instance = ofNullable(servletContext.getAttribute(ATTRIBUTE))
                .map(Jsonb.class::cast)
                .orElseGet(() -> {
                    final Jsonb jsonb = newInstance();
                    servletContext.setAttribute(ATTRIBUTE, jsonb);
                    return jsonb;
                });
        BY_LOADER.put(servletContext.getClassLoader(), instance);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        final Jsonb instance = BY_LOADER.remove(servletContextEvent.getServletContext().getClassLoader());
        if (instance != null) {
            try {
                instance.close();
            } catch (final Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    public static Jsonb locate() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = JsonbLocatorDelegate.class.getClassLoader();
        }
        Jsonb jsonb = BY_LOADER.get(loader);
        if (jsonb == null) {
            synchronized (BY_LOADER) {
                jsonb = BY_LOADER.get(loader);
                if (jsonb != null) {
                    return jsonb;
                }
                jsonb = newInstance();
                BY_LOADER.put(loader, jsonb);
                return jsonb;
            }
        }
        return jsonb;
    }

    private static Jsonb newInstance() {
        return JsonbBuilder.create();
    }
}
