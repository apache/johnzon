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

import org.apache.johnzon.mapper.MapperException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class FieldAccessMode implements AccessMode {
    @Override
    public Map<String, Reader> findReaders(final Class<?> clazz) {
        final Map<String, Reader> readers = new HashMap<String, Reader>();
        for (final Map.Entry<String, Field> f : fields(clazz).entrySet()) {
            final String key = f.getKey();
            if (isIgnored(key)) {
                continue;
            }
            readers.put(key, new FieldReader(f.getValue()));
        }
        return readers;
    }

    @Override
    public Map<String, Writer> findWriters(final Class<?> clazz) {
        final Map<String, Writer> writers = new HashMap<String, Writer>();
        for (final Map.Entry<String, Field> f : fields(clazz).entrySet()) {
            final String key = f.getKey();
            if (isIgnored(key)) {
                continue;
            }
            writers.put(key, new FieldWriter(f.getValue()));
        }
        return writers;
    }

    protected boolean isIgnored(final String key) {
        return "$jacocoData".equals(key);
    }

    private Map<String, Field> fields(final Class<?> clazz) {
        final Map<String, Field> fields = new HashMap<String, Field>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (final Field f : current.getDeclaredFields()) {
                final String name = f.getName();
                if (fields.containsKey(name)) {
                    continue;
                }
                fields.put(name, f);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    public static abstract class FieldDecoratedType implements DecoratedType {
        public final Field field;

        public FieldDecoratedType(final Field field) {
            this.field = field;
            if (!field.isAccessible()) {
                this.field.setAccessible(true);
            }
        }

        @Override
        public Type getType() {
            return field.getGenericType();
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
            return field.getAnnotation(clazz);
        }
    }

    public static class FieldWriter extends FieldDecoratedType implements Writer {
        public FieldWriter(final Field field) {
            super(field);
        }

        @Override
        public void write(final Object instance, final Object value) {
            try {
                field.set(instance, value);
            } catch (final Exception e) {
                throw new MapperException(e);
            }
        }
    }

    public static class FieldReader extends FieldDecoratedType  implements Reader {
        public FieldReader(final Field field) {
            super(field);
        }

        @Override
        public Object read(final Object instance) {
            try {
                return field.get(instance);
            } catch (final Exception e) {
                throw new MapperException(e);
            }
        }
    }
}
