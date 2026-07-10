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
package org.apache.johnzon.mapper.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public final class Records {
    private static final Method IS_RECORD;
    private static final Method GET_RECORD_COMPONENTS;
    private static final Method GET_NAME;

    static {
        Method isRecord = null;
        Method getRecordComponents = null;
        Method getName = null;
        try {
            isRecord = Class.class.getMethod("isRecord");
            getRecordComponents = Class.class.getMethod("getRecordComponents");
            getName = Class.forName("java.lang.reflect.RecordComponent").getMethod("getName");
        } catch (final NoSuchMethodException | ClassNotFoundException e) {
            // no-op
        }
        IS_RECORD = isRecord;
        GET_RECORD_COMPONENTS = getRecordComponents;
        GET_NAME = getName;
    }

    private Records() {
        // no-op
    }

    public static boolean isRecord(final Class<?> clazz) {
        try {
            return IS_RECORD != null && Boolean.class.cast(IS_RECORD.invoke(clazz));
        } catch (final InvocationTargetException | IllegalAccessException e) {
            return false;
        }
    }

    /**
     * @return the record component names of {@code clazz}, or {@code null} if it is not a record.
     */
    public static Set<String> componentNames(final Class<?> clazz) {
        if (!isRecord(clazz)) {
            return null;
        }
        try {
            final Object[] components = Object[].class.cast(GET_RECORD_COMPONENTS.invoke(clazz));
            final Set<String> names = new HashSet<>(components.length);
            for (final Object component : components) {
                names.add(String.class.cast(GET_NAME.invoke(component)));
            }
            return names;
        } catch (final InvocationTargetException | IllegalAccessException e) {
            return null;
        }
    }
}
