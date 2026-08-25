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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PerHierarchyAndLexicographicalOrderFieldComparator implements Comparator<String> {
    private final Class<?> clazz;
    private final Map<String, Integer> distances = new ConcurrentHashMap<>();
    private final AtomicBoolean populated = new AtomicBoolean();

    public PerHierarchyAndLexicographicalOrderFieldComparator(final Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public int compare(final String o1, final String o2) {
        if (o1 != null ? o1.equals(o2) : o2 == null) {
            return 0;
        }
        populateDistances();
        final Integer d1 = o1 == null ? null : distances.get(o1);
        final Integer d2 = o2 == null ? null : distances.get(o2);
        if (d1 == null || d2 == null) {
            return compareStrings(o1, o2);
        }
        final int res = d2 - d1; // reversed!
        if (res == 0) {
            return compareStrings(o1, o2);
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
                                distances.putIfAbsent(decapitalize(name.substring(3)), level);
                            } else if (name.length() > 2 && name.startsWith("is")
                                    && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
                                distances.putIfAbsent(decapitalize(name.substring(2)), level);
                            }
                        }
                        level++;
                        current = current.getSuperclass();
                    }
                }
            }
        }
    }

    private String decapitalize(final String value) {
        if (value.isEmpty()) {
            return value;
        }
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private int compareStrings(final String o1, final String o2) {
        if (o1 == null) {
            return o2 == null ? 0 : 1;
        }
        if (o2 == null) {
            return -1;
        }
        return o1.compareTo(o2);
    }
}