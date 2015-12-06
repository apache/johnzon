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
package javax.json.bind.spi;

import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;
import java.util.Iterator;
import java.util.ServiceLoader;

public abstract class JsonbProvider {
    private static final String DEFAULT_PROVIDER = "org.apache.johnzon.jsonb.JohnzonProvider";

    public static JsonbProvider provider() {
        final Iterator<JsonbProvider> it = ServiceLoader.load(JsonbProvider.class).iterator();
        if (it.hasNext()) {
            return it.next();
        }

        try {
            return JsonbProvider.class.cast(Thread.currentThread().getContextClassLoader().loadClass(DEFAULT_PROVIDER).newInstance());
        } catch (final ClassNotFoundException cnfe) {
            throw new JsonbException(DEFAULT_PROVIDER + " not found", cnfe);
        } catch (final Exception x) {
            throw new JsonbException(DEFAULT_PROVIDER + " couldn't be instantiated: " + x, x);
        }
    }

    public static JsonbProvider provider(final String providerFqn) {
        if (providerFqn == null) {
            throw new IllegalArgumentException();
        }
        for (final JsonbProvider provider : ServiceLoader.load(JsonbProvider.class)) {
            if (providerFqn.equals(provider.getClass().getName())) {
                return provider;
            }
        }

        final String msg = providerFqn + " not found";
        throw new JsonbException(msg, new ClassNotFoundException(msg));
    }

    public abstract JsonbBuilder create();
}
