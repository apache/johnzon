/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.mapper.util;

import jakarta.json.spi.JsonProvider;
import org.apache.johnzon.core.JsonProviderImpl;

import java.lang.reflect.Method;

/**
 * ClassLoader related utils to avoid direct access to our JSON provider from the mapper
 */
public final class JsonProviderUtil {

    private final static Method SET_MAX_BIG_DECIMAL_SCALE;

    static {
        try {
            SET_MAX_BIG_DECIMAL_SCALE =
                JsonProviderImpl.class.getDeclaredMethod("setMaxBigDecimalScale", Integer.TYPE);
            SET_MAX_BIG_DECIMAL_SCALE.setAccessible(true);
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonProviderUtil() {
        // private utility class ct
    }

    /**
     * Sets the max big decimal scale property on the given provider instance.
     * <p>
     * This method is intentionally not receiving the property name, so we know exactly what will be passed in and what
     * the method is supposed to set on the provider.
     * <p>
     * If the provider is not an instance of our JohnzonProviderImpl (org.apache.johnzon.core.JsonProviderImpl), the
     * method is a noop.
     *
     * @param provider the provider to configure. Must be an instance of org.apache.johnzon.core.JsonProviderImpl
     * @param value the max big decimal scale to set on the provider
     */
    public static void setMaxBigDecimalScale(final JsonProvider provider, final int value) {
        if (!"org.apache.johnzon.core.JsonProviderImpl".equals(provider.getClass().getName())) {
            return;
        }

        try {
            SET_MAX_BIG_DECIMAL_SCALE.invoke(provider, value);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}