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
package org.apache.johnzon.jsonb.order;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.johnzon.mapper.util.BeanUtil;

public class PerHierarchyAndLexicographicalOrderFieldComparator implements Comparator<String> {
    private static final Comparator<String> NULLS_LAST = Comparator.nullsLast(Comparator.naturalOrder());

    private final Class<?> clazz;
    private final Map<String, Integer> distances = new ConcurrentHashMap<>();
    private final AtomicBoolean populated = new AtomicBoolean();

    public PerHierarchyAndLexicographicalOrderFieldComparator(final Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public int compare(final String o1, final String o2) {
        if (Objects.equals(o1, o2)) {
            return 0;
        }
        populateDistances();
        final Integer d1 = o1 == null ? null : distances.get(o1);
        final Integer d2 = o2 == null ? null : distances.get(o2);
        if (d1 == null || d2 == null) {
            return NULLS_LAST.compare(o1, o2);
        }
        final int res = d2 - d1; // reversed!
        if (res == 0) {
            return NULLS_LAST.compare(o1, o2);
        }
        return res;
    }

    private void populateDistances() {
        if (!populated.get()) {
            synchronized (this) {
                if (populated.compareAndSet(false, true)) {
                    Class<?> current = clazz;
                    int level = 0;
                    while (current != null && current != Object.class) {
                        for (final Field field : current.getDeclaredFields()) {
                            distances.putIfAbsent(field.getName(), level);
                        }
                        for (final Method method : current.getDeclaredMethods()) {
                            if (method.getParameterCount() != 0 || method.getReturnType() == void.class) {
                                continue;
                            }
                            final String name = method.getName();
                            if (name.length() > 3 && name.startsWith("get")) {
                                distances.putIfAbsent(BeanUtil.decapitalize(name.substring(3)), level);
                            } else if (name.length() > 2 && name.startsWith("is")
                                    && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
                                distances.putIfAbsent(BeanUtil.decapitalize(name.substring(2)), level);
                            }
                        }
                        level++;
                        current = current.getSuperclass();
                    }
                }
            }
        }
    }
}
