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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.json.Json;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonWriterFactory;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class FactoryLocator implements ServletContextListener {
    private static final Map<ClassLoader, JsonReaderFactory> READER_FACTORY_BY_LOADER = new ConcurrentHashMap<ClassLoader, JsonReaderFactory>();
    private static final Map<ClassLoader, JsonWriterFactory> WRITER_FACTORY_BY_LOADER = new ConcurrentHashMap<ClassLoader, JsonWriterFactory>();
    private static final String READER_ATTRIBUTE = FactoryLocator.class.getName() + ".readerFactory";
    private static final String WRITER_ATTRIBUTE = FactoryLocator.class.getName() + ".writerFactory";

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        final ClassLoader classLoader = servletContextEvent.getServletContext().getClassLoader();

        final JsonReaderFactory reader = newReadFactory();
        READER_FACTORY_BY_LOADER.put(classLoader, reader);
        servletContextEvent.getServletContext().setAttribute(READER_ATTRIBUTE, reader);

        final JsonWriterFactory writer = newWriterFactory();
        WRITER_FACTORY_BY_LOADER.put(classLoader, writer);
        servletContextEvent.getServletContext().setAttribute(WRITER_ATTRIBUTE, reader);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        final ClassLoader classLoader = servletContextEvent.getServletContext().getClassLoader();
        READER_FACTORY_BY_LOADER.remove(classLoader);
        WRITER_FACTORY_BY_LOADER.remove(classLoader);
    }

    public static JsonReaderFactory readerLocate() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = FactoryLocator.class.getClassLoader();
        }
        final JsonReaderFactory factory = READER_FACTORY_BY_LOADER.get(loader);
        if (factory == null) {
            return newReadFactory();
        }
        return factory;
    }

    public static JsonWriterFactory writerLocate() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = FactoryLocator.class.getClassLoader();
        }
        final JsonWriterFactory factory = WRITER_FACTORY_BY_LOADER.get(loader);
        if (factory == null) {
            return newWriterFactory();
        }
        return factory;
    }

    private static JsonReaderFactory newReadFactory() {
        return Json.createReaderFactory(Collections.<String, Object>emptyMap());
    }

    private static JsonWriterFactory newWriterFactory() {
        return Json.createWriterFactory(Collections.<String, Object>emptyMap());
    }
}
