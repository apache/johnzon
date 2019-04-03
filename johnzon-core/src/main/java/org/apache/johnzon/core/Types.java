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
    
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

public class Types {

    /**
     * This method helps reconstructing the resulting ParameterizedType for a specific generic type across its hierarchy.
     *
     * Example: Let's create some interface Converter[X, Y] and try to determine the actual type arguments for that
     * interface from classes/interfaces which inherit from it (directly or indirectly).
     *
     * <ul>
     * <li>Converter[X, Y] will yield Converter[X, Y]</li>
     * <li>StringConverter[Z] extends Converter[Z, String] will yield Converter[Z, String]</li>
     * <li>AbstractStringConverter[A] implements StringConverter[A] will yield Converter[A, String]</li>
     * <li>UUIDStringConverter extends AbstractStringConverter[UUID] will yield Converter[UUID, String]</li>
     * </ul>
     *
     * For the last example (UUIDStringConverter), nowhere in its hierarchy is a type directly implementing
     * Converter[UUID, String] but this method is capable of reconstructing that information.
     */
    public static ParameterizedType findParameterizedType(Class<?> klass, Class<?> parameterizedClass) {
        return new ParameterizedTypeImpl(parameterizedClass, resolveArgumentTypes(klass, parameterizedClass));
    }

    private static Type[] resolveArgumentTypes(Type type, Class<?> parameterizedClass) {
        if (type instanceof Class<?>) {
            return resolveArgumentTypes((Class<?>) type, parameterizedClass);
        }
        if (type instanceof ParameterizedType) {
            return resolveArgumentTypes((ParameterizedType) type, parameterizedClass);
        }
        throw new IllegalArgumentException("Cannot resolve argument types from " + type.getClass().getSimpleName());
    }

    private static Type[] resolveArgumentTypes(Class<?> type, Class<?> parameterizedClass) {
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

    private static Type[] resolveArgumentTypes(ParameterizedType type, Class<?> parameterizedClass) {
        Class<?> rawType = (Class<?>) type.getRawType(); // always a Class
        TypeVariable<?>[] typeVariables = rawType.getTypeParameters();
        Type[] types = resolveArgumentTypes(rawType, parameterizedClass);
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

    private Types() {
        // no-op
    }
    
    private static class ParameterizedTypeImpl implements ParameterizedType {

        private final Type rawType;
        private final Type[] arguments;

        ParameterizedTypeImpl(Type rawType, Type... arguments) {
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

        // TODO equals, hashcode, toString if needed
    }
}
