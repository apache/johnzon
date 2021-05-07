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

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PerHierarchyAndLexicographicalOrderFieldComparator implements Comparator<String> {
    private final Class<?> clazz;
    private final Map<String, Integer> distances = new ConcurrentHashMap<>();

    public PerHierarchyAndLexicographicalOrderFieldComparator(final Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public int compare(final String o1, final String o2) {
        if (o1.equals(o2)) {
            return 0;
        }
        final int d1 = distance(o1);
        final int d2 = distance(o2);
        final int res = d2 - d1; // reversed!
        if (res == 0) {
            return o1.compareTo(o2);
        }
        return res;
    }

    private int distance(final String o1) {
        return distances.computeIfAbsent(o1, this::slowDistance);
    }

    private int slowDistance(String o1) {
        Class<?> current = clazz;
        int i = 0;
        while (current != null && current != Object.class) {
            try {
                current.getDeclaredField(o1);
                return i;
            } catch (final NoSuchFieldException e) {
                // no-op
            }
            final String methodSuffix = Character.toUpperCase(o1.charAt(0)) + (o1.length() > 1 ? o1.substring(1) : "");
            try {
                current.getDeclaredMethod("get" + methodSuffix);
                return i;
            } catch (final Exception e) {
                // no-op
            }
            try {
                current.getDeclaredMethod("is" + methodSuffix);
                return i;
            } catch (final Exception e) {
                // no-op
            }
            i++;
            current = current.getSuperclass();
        }
        return i;
    }
}
