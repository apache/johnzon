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
package org.apache.johnzon.jsonb.cdi;

import org.apache.johnzon.jsonb.JohnzonJsonb;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

public class JohnzonCdiExtension implements Extension {
    private final Collection<JohnzonJsonb> jsonbs = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean canWrite = false;

    public boolean isCanWrite() {
        return canWrite;
    }

    public void track(final JohnzonJsonb jsonb) {
        if (!canWrite) {
            throw new IllegalStateException("CDI context already shutdown");
        }
        lock.lock();
        try {
            jsonbs.add(jsonb);
        } finally {
            lock.unlock();
        }
    }

    public void untrack(final JohnzonJsonb jsonb) {
        synchronized (this) {
            lock.lock();
            try {
                if (!jsonbs.remove(jsonb)) {
                    return;
                }
            } finally {
                lock.unlock();
            }
        }
        jsonb.close();
    }

    void started(@Observes final BeforeBeanDiscovery beforeBeanDiscovery) {
        canWrite = true;
    }

    void cleanUp(@Observes final BeforeShutdown beforeShutdown) {
        canWrite = false;
        new ArrayList<>(jsonbs).forEach(this::untrack);
        jsonbs.clear();
    }
}
