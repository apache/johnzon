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

public final class Records {
    private static final Method IS_RECORD;
    private static final Class RECORD_CLASS;

    static {
        Method isRecord = null;
        try {
            isRecord = Class.class.getMethod("isRecord");
        } catch (final NoSuchMethodException e) {
            // no-op
        }
        IS_RECORD = isRecord;

        Class recordClass = null;
        try {
            recordClass = Class.forName("java.lang.Record");
        } catch (final ClassNotFoundException e) {
            // no-op
        }
        RECORD_CLASS = recordClass;
    }

    private Records() {
        // no-op
    }

    public static boolean isRecord(final Class<?> clazz) {
        if (IS_RECORD == null) {
            return false;
        }
        if (clazz.getSuperclass() != RECORD_CLASS) {
            return false;
        }

        try {
            return Boolean.class.cast(IS_RECORD.invoke(clazz));
        } catch (final InvocationTargetException | IllegalAccessException e) {
            return false;
        }
    }
}
