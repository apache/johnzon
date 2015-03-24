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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class MapperLocator implements ServletContextListener {
    private static final Map<ClassLoader, Mapper> MAPPER_BY_LOADER = new ConcurrentHashMap<ClassLoader, Mapper>();
    private static final String ATTRIBUTE = MapperLocator.class.getName() + ".mapper";

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        final Mapper build = newMapper();
        MAPPER_BY_LOADER.put(servletContextEvent.getServletContext().getClassLoader(), build);
        servletContextEvent.getServletContext().setAttribute(ATTRIBUTE, build);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        MAPPER_BY_LOADER.remove(servletContextEvent.getServletContext().getClassLoader());
    }

    public static Mapper locate() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = MapperLocator.class.getClassLoader();
        }
        final Mapper mapper = MAPPER_BY_LOADER.get(loader);
        if (mapper == null) {
            return newMapper();
        }
        return mapper;
    }

    private static Mapper newMapper() {
        return new MapperBuilder().build();
    }
}
