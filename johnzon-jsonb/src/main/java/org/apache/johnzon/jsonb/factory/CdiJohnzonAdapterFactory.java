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
package org.apache.johnzon.jsonb.factory;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.util.Set;

public class CdiJohnzonAdapterFactory extends SimpleJohnzonAdapterFactory {
    private final BeanManager bm;

    public CdiJohnzonAdapterFactory(final Object bm) {
        this.bm = BeanManager.class.cast(bm);
    }

    @Override
    public <T> Instance<T> create(final Class<T> type) {
        try {
            final Set<Bean<?>> beans = bm.getBeans(type);
            final Bean<?> bean = bm.resolve(beans);
            if (bean != null) {
                final CreationalContext<Object> creationalContext = bm.createCreationalContext(null);
                final T instance = (T) bm.getReference(bean, type, creationalContext);
                if (bm.isNormalScope(bean.getScope())) {
                    return new ConstantInstance<>((T) bm.getReference(bean, type, creationalContext));
                }
                return new CdiInstance<T>(instance, creationalContext);
            }
        } catch (final Exception e) {
            // fallback
        }
        return super.create(type);
    }

    private static class CdiInstance<T> implements Instance<T> {
        private final T value;
        private final CreationalContext<Object> context;

        private CdiInstance(final T instance, final CreationalContext<Object> creationalContext) {
            this.value = instance;
            this.context = creationalContext;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public void release() {
            context.release();
        }
    }
}
