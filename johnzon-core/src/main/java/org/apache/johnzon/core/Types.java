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
package org.apache.johnzon.core;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

public class Types {

    public static class TypeVisitor<T> {

        public T visit(Class<?> type) {
            throw new UnsupportedOperationException("Visiting Class not supported.");
        }

        public T visit(GenericArrayType type) {
            throw new UnsupportedOperationException("Visiting GenericArrayType not supported.");
        }

        public T visit(ParameterizedType type) {
            throw new UnsupportedOperationException("Visiting ParameterizedType not supported.");
        }

        public T visit(TypeVariable<?> type) {
            throw new UnsupportedOperationException("Visiting TypeVariable not supported.");
        }

        public T visit(WildcardType type) {
            throw new UnsupportedOperationException("Visiting WildcardType not supported.");
        }

        public final T visit(Type type) {
            if (type instanceof Class<?>) {
                return visit((Class<?>) type);
            }
            if (type instanceof ParameterizedType) {
                return visit((ParameterizedType) type);
            }
            if (type instanceof WildcardType) {
                return visit((WildcardType) type);
            }
            if (type instanceof TypeVariable<?>) {
                return visit((TypeVariable<?>) type);
            }
            if (type instanceof GenericArrayType) {
                return visit((GenericArrayType) type);
            }
            throw new IllegalArgumentException(String.format("Unknown type: %s", type.getClass()));
        }
    }

    private static class ArgumentTypeResolver extends TypeVisitor<Type[]> {

        private final Class<?> superType;

        public ArgumentTypeResolver(Class<?> superType) {
            this.superType = superType;
        }

        @Override
        public Type[] visit(Class<?> type) {
            if (this.superType.equals(type)) {
                // May return Class[] instead of Type[], so copy it as a Type[] to avoid
                // problems in visit(ParameterizedType)
                return Arrays.copyOf(type.getTypeParameters(), superType.getTypeParameters().length, Type[].class);
            }
            if (type.getSuperclass() != null && this.superType.isAssignableFrom(type.getSuperclass())) {
                return visit(type.getGenericSuperclass());
            }
            Class<?>[] interfaces = type.getInterfaces();
            Type[] genericInterfaces = type.getGenericInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                if (this.superType.isAssignableFrom(interfaces[i])) {
                    return visit(genericInterfaces[i]);
                }
            }
            throw new IllegalArgumentException(String.format("%s is not assignable from %s", type, this.superType));
        }

        @Override
        public Type[] visit(ParameterizedType type) {
            Class<?> rawType = (Class<?>) type.getRawType(); // always a Class
            TypeVariable<?>[] typeVariables = rawType.getTypeParameters();
            Type[] types = visit(rawType);
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
    }

    private static class ParameterizedTypeImpl implements ParameterizedType {

        private final Type rawType;
        private final Type[] arguments;

        public ParameterizedTypeImpl(Type rawType, Type... arguments) {
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

    }

    public static Type[] resolveArgumentTypes(Type type, Class<?> superClass) {
        return new ArgumentTypeResolver(superClass).visit(type);
    }

    public static ParameterizedType findParameterizedType(Type type, Class<?> superClass) {
        return new ParameterizedTypeImpl(superClass, resolveArgumentTypes(type, superClass));
    }

    private Types() {
        // no-op
    }
}
