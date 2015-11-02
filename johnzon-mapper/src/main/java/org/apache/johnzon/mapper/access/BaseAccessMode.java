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

import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;

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

    protected Type fixType(final Class<?> clazz, final Type type) { // to enhance
        if (TypeVariable.class.isInstance(type)) { // we need to handle it on deserialization side, not needed on serialization side
            return fixTypeVariable(clazz, type);
        }
        if (ParameterizedType.class.isInstance(type)) {
            final ParameterizedType pt = ParameterizedType.class.cast(type);
            final Type[] actualTypeArguments = pt.getActualTypeArguments();
            if (actualTypeArguments.length == 1 && Class.class.isInstance(pt.getRawType())
                && Collection.class.isAssignableFrom(Class.class.cast(pt.getRawType()))
                && Class.class.cast(pt.getRawType()).getName().startsWith("java.util.")
                && TypeVariable.class.isInstance(actualTypeArguments[0])) {
                return new JohnzonParameterizedType(pt.getRawType(), fixTypeVariable(clazz, actualTypeArguments[0]));
            } else if (actualTypeArguments.length == 2 && Class.class.isInstance(pt.getRawType())
                && Map.class.isAssignableFrom(Class.class.cast(pt.getRawType()))
                && Class.class.cast(pt.getRawType()).getName().startsWith("java.util.")
                && TypeVariable.class.isInstance(actualTypeArguments[1])) {
                return new JohnzonParameterizedType(pt.getRawType(), actualTypeArguments[0], fixTypeVariable(clazz, actualTypeArguments[1]));
            }
        }
        return type;
    }

    private Type fixTypeVariable(final Class<?> clazz, final Type type) {
        final TypeVariable typeVariable = TypeVariable.class.cast(type);
        final Class<?> classWithDeclaration = findClass(clazz.getSuperclass(), typeVariable.getGenericDeclaration());

        if (classWithDeclaration != null) {
            // try to match generic
            final TypeVariable<? extends Class<?>>[] typeParameters = classWithDeclaration.getTypeParameters();
            final int idx = asList(typeParameters).indexOf(typeVariable);
            if (idx >= 0) {

                ParameterizedType pt = findParameterizedType(clazz, classWithDeclaration);
                if (pt != null) {
                    if (pt.getActualTypeArguments().length == typeParameters.length) {
                        return pt.getActualTypeArguments()[idx];
                    }
                }
            }
        }
        return type;
    }

    private Class<?> findClass(final Class<?> clazz, final GenericDeclaration genericDeclaration) {

        if (clazz == genericDeclaration) {
            return clazz;
        }

        if (clazz.getSuperclass() != Object.class) {
            return findClass(clazz.getSuperclass(), genericDeclaration);
        }

        return null;
    }

    private ParameterizedType findParameterizedType(Class<?> clazz, Class<?> classWithDeclaration) {

        if (clazz == Object.class) {
            return null;
        }

        Type genericSuperclass = clazz.getGenericSuperclass();

        if (genericSuperclass instanceof ParameterizedType &&
            ((ParameterizedType) genericSuperclass).getRawType() == classWithDeclaration) {

                return (ParameterizedType) genericSuperclass;
        }

        return findParameterizedType(clazz.getSuperclass(), classWithDeclaration);
    }
}
