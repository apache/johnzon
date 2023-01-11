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

// helper class to lazy trigger CDI deps
public final class CDIs {
    private final JohnzonCdiExtension tracker;

    public CDIs(final Object beanManager) {
        this.tracker = Lazy.load(beanManager);
    }

    public void track(final JohnzonJsonb johnzonJsonb) {
        tracker.track(johnzonJsonb);
    }

    public void untrack(final JohnzonJsonb johnzonJsonb) {
        tracker.untrack(johnzonJsonb);
    }

    public boolean isCanWrite() {
        return tracker.isCanWrite();
    }

    private static class Lazy {
        private Lazy() {
            // no-op
        }

        private static JohnzonCdiExtension load(final Object beanManager) {
            final jakarta.enterprise.inject.spi.BeanManager bm = jakarta.enterprise.inject.spi.BeanManager.class.cast(beanManager);
            return JohnzonCdiExtension.class.cast(
                bm.getReference(bm.resolve(bm.getBeans(JohnzonCdiExtension.class)), JohnzonCdiExtension.class, bm.createCreationalContext(null)));
        }
    }
}
