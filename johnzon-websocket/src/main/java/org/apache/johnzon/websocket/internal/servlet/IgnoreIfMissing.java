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
package org.apache.johnzon.websocket.internal.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.function.Supplier;

public class IgnoreIfMissing implements ServletContextListener {
    private final Supplier<ServletContextListener> delegate;
    private boolean skipped;

    public IgnoreIfMissing(final Supplier<ServletContextListener> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        try {
            delegate.get().contextInitialized(sce);
        } catch (final Error | RuntimeException re) {
            skipped = true;
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        if (!skipped) {
            delegate.get().contextDestroyed(sce);
        }
    }
}
