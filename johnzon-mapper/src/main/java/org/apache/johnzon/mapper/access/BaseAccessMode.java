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
package org.apache.johnzon.mapper.access;

import java.util.HashMap;
import java.util.Map;

// handle some specific types
public abstract class BaseAccessMode implements AccessMode {
    private final Map<Class<?>, String[]> fieldsToRemove = new HashMap<Class<?>, String[]>();

    public BaseAccessMode() { // mainly built it in the JVM types == user cant handle them
        fieldsToRemove.put(Throwable.class, new String[]{"suppressedExceptions", "cause"});
    }

    protected abstract Map<String,Reader> doFindReaders(Class<?> clazz);
    protected abstract Map<String,Writer> doFindWriters(Class<?> clazz);

    @Override
    public Map<String, Reader> findReaders(final Class<?> clazz) {
        return sanitize(clazz, doFindReaders(clazz));
    }

    @Override
    public Map<String, Writer> findWriters(final Class<?> clazz) {
        return sanitize(clazz, doFindWriters(clazz));
    }

    // editable during builder time, dont do it at runtime or you get no guarantee
    public Map<Class<?>, String[]> getFieldsToRemove() {
        return fieldsToRemove;
    }

    private <T> Map<String, T> sanitize(final Class<?> type, final Map<String, T> delegate) {
        for (final Map.Entry<Class<?>, String[]> entry : fieldsToRemove.entrySet()) {
            if (entry.getKey().isAssignableFrom(type)) {
                for (final String field : entry.getValue()) {
                    delegate.remove(field);
                }
                return delegate;
            }
        }
        return delegate;
    }
}
