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
package org.apache.johnzon.websocket.internal.mapper;

import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperBuilder;
import org.apache.johnzon.websocket.internal.lazy.LazySupplier;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

public class MapperLocatorDelegate implements ServletContextListener {
    private static final Map<ClassLoader, Supplier<Mapper>> MAPPER_BY_LOADER = new ConcurrentHashMap<ClassLoader, Supplier<Mapper>>();
    private static final String ATTRIBUTE = MapperLocator.class.getName() + ".mapper";

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        final ServletContext servletContext = servletContextEvent.getServletContext();
        final Supplier<Mapper> supplier = ofNullable(servletContext.getAttribute(ATTRIBUTE))
                .map(it -> (Supplier<Mapper>) it)
                .orElseGet(() -> {
                    final LazySupplier<Mapper> lazySupplier = new LazySupplier<>(MapperLocatorDelegate::newMapper);
                    servletContext.setAttribute(ATTRIBUTE, lazySupplier);
                    return lazySupplier;
                });
        MAPPER_BY_LOADER.put(servletContext.getClassLoader(), supplier);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        final Supplier<Mapper> supplier = MAPPER_BY_LOADER.remove(servletContextEvent.getServletContext().getClassLoader());
        if (LazySupplier.class.isInstance(supplier)) {
            final Object mapper = LazySupplier.class.cast(supplier).getInstance();
            if (mapper != null) {
                Mapper.class.cast(mapper).close();
            }
        }
    }

    public static Mapper locate() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = MapperLocatorDelegate.class.getClassLoader();
        }
        Supplier<Mapper> mapper = MAPPER_BY_LOADER.get(loader);
        if (mapper == null) {
            synchronized (MAPPER_BY_LOADER) {
                mapper = MAPPER_BY_LOADER.get(loader);
                if (mapper != null) {
                    return mapper.get();
                }
                final Mapper instance = newMapper();
                mapper = () -> instance;
                MAPPER_BY_LOADER.put(loader, mapper);
                return mapper.get();
            }
        }
        return mapper.get();
    }

    private static Mapper newMapper() {
        return new MapperBuilder().build();
    }
}
