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
package org.apache.johnzon.jsonb.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

// forked from johnzon-core to ensure the independency without creating a new module just for that
public class Types {
    public ParameterizedType findParameterizedType(final Class<?> klass, final Class<?> parameterizedClass) {
        return new ParameterizedTypeImpl(parameterizedClass, resolveArgumentTypes(klass, parameterizedClass));
    }

    public Class<?> findParamType(final ParameterizedType type, final Class<?> expectedWrapper) {
        if (type.getActualTypeArguments().length != 1) {
            return null;
        }
        final Class<?> asClass = asClass(type.getRawType());
        if (asClass == null || !expectedWrapper.isAssignableFrom(asClass)) {
            return null;
        }
        return asClass(type.getActualTypeArguments()[0]);
    }

    public Class<?> asClass(final Type type) {
        return Class.class.isInstance(type) ? Class.class.cast(type) : null;
    }

    private Type[] resolveArgumentTypes(final Type type, final Class<?> parameterizedClass) {
        if (type instanceof Class<?>) {
            return resolveArgumentTypes((Class<?>) type, parameterizedClass);
        }
        if (type instanceof ParameterizedType) {
            return resolveArgumentTypes((ParameterizedType) type, parameterizedClass);
        }
        throw new IllegalArgumentException("Cannot resolve argument types from " + type.getClass().getSimpleName());
    }

    private Type[] resolveArgumentTypes(final Class<?> type, final Class<?> parameterizedClass) {
        if (parameterizedClass.equals(type)) {
            // May return Class[] instead of Type[], so copy it as a Type[] to avoid
            // problems in visit(ParameterizedType)
            return Arrays.copyOf(type.getTypeParameters(), parameterizedClass.getTypeParameters().length, Type[].class);
        }
        if (type.getSuperclass() != null && parameterizedClass.isAssignableFrom(type.getSuperclass())) {
            return resolveArgumentTypes(type.getGenericSuperclass(), parameterizedClass);
        }
        Class<?>[] interfaces = type.getInterfaces();
        Type[] genericInterfaces = type.getGenericInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (parameterizedClass.isAssignableFrom(interfaces[i])) {
                return resolveArgumentTypes(genericInterfaces[i], parameterizedClass);
            }
        }
        throw new IllegalArgumentException(String.format("%s is not assignable from %s", type, parameterizedClass));
    }

    private Type[] resolveArgumentTypes(final ParameterizedType type, final Class<?> parameterizedClass) {
        final Class<?> rawType = (Class<?>) type.getRawType(); // always a Class
        final TypeVariable<?>[] typeVariables = rawType.getTypeParameters();
        final Type[] types = resolveArgumentTypes(rawType, parameterizedClass);
        for (int i = 0; i < types.length; i++) {
            if (types[i] instanceof TypeVariable<?>) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) types[i];
                for (int j = 0; j < typeVariables.length; j++) {
                    if (typeVariables[j].getName().equals(typeVariable.getName())) {
                        types[i] = type.getActualTypeArguments()[j];
                    }
                }
            }
        }
        return types;
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {
        private final Type rawType;
        private final Type[] arguments;

        private ParameterizedTypeImpl(final Type rawType, final Type... arguments) {
            this.rawType = rawType;
            this.arguments = arguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return arguments;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arguments) ^ (rawType == null ? 0 : rawType.hashCode());
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ParameterizedType) {
                final ParameterizedType that = (ParameterizedType) obj;
                final Type thatRawType = that.getRawType();
                return that.getOwnerType() == null
                        && (rawType == null ? thatRawType == null : rawType.equals(thatRawType))
                        && Arrays.equals(arguments, that.getActualTypeArguments());
            }
            return false;
        }

        @Override
        public String toString() {
            final StringBuilder buffer = new StringBuilder();
            buffer.append(((Class<?>) rawType).getSimpleName());
            final Type[] actualTypes = getActualTypeArguments();
            if (actualTypes.length > 0) {
                buffer.append("<");
                int length = actualTypes.length;
                for (int i = 0; i < length; i++) {
                    buffer.append(actualTypes[i].toString());
                    if (i != actualTypes.length - 1) {
                        buffer.append(",");
                    }
                }

                buffer.append(">");
            }
            return buffer.toString();
        }
    }
}
