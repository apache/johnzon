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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.JohnzonAny;
import org.apache.johnzon.mapper.JohnzonProperty;
import org.apache.johnzon.mapper.MapperException;
import org.apache.johnzon.mapper.ObjectConverter;

public class FieldAccessMode extends BaseAccessMode {
    public FieldAccessMode(final boolean useConstructor, final boolean acceptHiddenConstructor) {
        super(useConstructor, acceptHiddenConstructor);
    }

    @Override
    public Map<String, Reader> doFindReaders(final Class<?> clazz) {
        final Map<String, Reader> readers = new HashMap<String, Reader>();
        for (final Map.Entry<String, Field> f : fields(clazz, true).entrySet()) {
            final String key = f.getKey();
            if (isIgnored(key, f.getValue().getDeclaringClass()) || Meta.getAnnotation(f.getValue(), JohnzonAny.class) != null) {
                continue;
            }

            final Field field = f.getValue();
            readers.put(extractKey(field, key), new FieldReader(field, field.getGenericType()));
        }
        return readers;
    }

    @Override
    public Map<String, Writer> doFindWriters(final Class<?> clazz) {
        final Map<String, Writer> writers = new HashMap<String, Writer>();
        for (final Map.Entry<String, Field> f : fields(clazz, false).entrySet()) {
            final String key = f.getKey();
            if (isIgnored(key, f.getValue().getDeclaringClass())) {
                continue;
            }

            final Field field = f.getValue();
            writers.put(extractKey(field, key), new FieldWriter(field, field.getGenericType()));
        }
        return writers;
    }

    private String extractKey(final Field f, final String key) {
        final JohnzonProperty property = Meta.getAnnotation(f, JohnzonProperty.class);
        return property != null ? property.value() : key;
    }

    protected boolean isIgnored(final String key, final Class<?> clazz) {
        return isIgnored(key) || (clazz.getName().startsWith("java.") && (Map.class.isAssignableFrom(clazz) || List.class.isAssignableFrom(clazz)));
    }

    protected boolean isIgnored(final String key) {
        return key.contains("$");
    }

    protected Map<String, Field> fields(final Class<?> clazz, boolean includeFinalFields) {
        final Map<String, Field> fields = new HashMap<String, Field>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (final Field f : current.getDeclaredFields()) {
                final String name = f.getName();
                final int modifiers = f.getModifiers();
                if (fields.containsKey(name)
                        || Modifier.isStatic(modifiers)
                        || Modifier.isTransient(modifiers)
                        || (!includeFinalFields && Modifier.isFinal(modifiers))) {
                    continue;
                }
                fields.put(name, f);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    public static abstract class FieldDecoratedType implements DecoratedType {
        protected final Field field;
        protected final Type type;

        public FieldDecoratedType(final Field field, final Type type) {
            this.field = field;
            if (!field.isAccessible()) {
                this.field.setAccessible(true);
            }
            this.type = type;
        }

        @Override
        public <T extends Annotation> T getClassOrPackageAnnotation(final Class<T> clazz) {
            return Meta.getClassOrPackageAnnotation(field, clazz);
        }

        @Override
        public Adapter<?, ?> findConverter() {
            return null;
        }

        public Field getField() {
            return field;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> clazz) {
            return Meta.getAnnotation(field, clazz);
        }

        @Override
        public boolean isNillable(final boolean global) {
            return global;
        }

        @Override
        public String toString() {
            return "FieldDecoratedType{" +
                    "field=" + field +
                    '}';
        }
    }

    public static class FieldWriter extends FieldDecoratedType implements Writer {
        public FieldWriter(final Field field, final Type type) {
            super(field, type);
        }

        @Override
        public void write(final Object instance, final Object value) {
            try {
                field.set(instance, value);
            } catch (final Exception e) {
                throw new MapperException("Error setting " + field, e);
            }
        }

        @Override
        public ObjectConverter.Reader<?> findObjectConverterReader() {
            return null;
        }
    }

    public static class FieldReader extends FieldDecoratedType  implements Reader {
        public FieldReader(final Field field, final Type type) {
            super(field, type);
        }

        @Override
        public Object read(final Object instance) {
            try {
                return field.get(instance);
            } catch (final Exception e) {
                throw new MapperException("Error setting " + field, e);
            }
        }

        @Override
        public ObjectConverter.Writer<?> findObjectConverterWriter() {
            return null;
        }

        @Override
        public Type getType() {
            return type;
        }
    }
}
