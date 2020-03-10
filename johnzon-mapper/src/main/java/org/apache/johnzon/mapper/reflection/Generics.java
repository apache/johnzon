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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class Generics {
    private Generics() {
        // no-op
    }

    public static Map<Type, Type> toResolvedTypes(final Type clazz) {
        if (ParameterizedType.class.isInstance(clazz)) {
            final ParameterizedType parameterizedType = ParameterizedType.class.cast(clazz);
            if (!Class.class.isInstance(parameterizedType.getRawType())) {
                return emptyMap(); // not yet supported
            }
            final Class<?> raw = Class.class.cast(parameterizedType.getRawType());
            final Type[] arguments = parameterizedType.getActualTypeArguments();
            if (arguments.length > 0) {
                final TypeVariable<? extends Class<?>>[] parameters = raw.getTypeParameters();
                final Map<Type, Type> map = new HashMap<>(parameters.length);
                for (int i = 0; i < parameters.length && i < arguments.length; i++) {
                    map.put(parameters[i], arguments[i]);
                }
                return map;
            }
        }
        return emptyMap();
    }

    // todo: this piece of code needs to be enhanced a lot:
    // - better handling of the hierarchy
    // - wildcard support?
    // - cycle handling (Foo<Foo>)
    // - ....
    public static Type resolve(final Type value, final Type rootClass,
                               final Map<Type, Type> resolved) {
        if (TypeVariable.class.isInstance(value)) {
            return resolveTypeVariable(value, rootClass, resolved);
        }
        if (ParameterizedType.class.isInstance(value)) {
            return resolveParameterizedType(value, rootClass, resolved);
        }
        if (WildcardType.class.isInstance(value)) {
            return resolveWildcardType(value);
        }
        return value;
    }

    private static Type resolveWildcardType(final Type value) {
        final WildcardType wildcardType = WildcardType.class.cast(value);
        if (Stream.of(wildcardType.getUpperBounds()).anyMatch(it -> it == Object.class) &&
                wildcardType.getLowerBounds().length == 0) {
            return Object.class;
        } // else todo
        return value;
    }

    private static Type resolveParameterizedType(final Type value, final Type rootClass,
                                                 final Map<Type, Type> resolved) {
        Collection<Type> args = null;
        final ParameterizedType parameterizedType = ParameterizedType.class.cast(value);
        int index = 0;
        for (final Type arg : parameterizedType.getActualTypeArguments()) {
            final Type type = resolve(arg, rootClass, resolved);
            if (type != arg) {
                if (args == null) {
                    args = new ArrayList<>();
                    if (index > 0) {
                        args.addAll(asList(parameterizedType.getActualTypeArguments()).subList(0, index));
                    }
                }
            }
            if (args != null) {
                args.add(type);
            }
            index++;
        }
        if (args != null) {
            return new JohnzonParameterizedType(parameterizedType.getRawType(), args.toArray(new Type[args.size()]));
        }
        return value;
    }

    // for now the level is hardcoded to 2 with generic > concrete
    private static Type resolveTypeVariable(final Type value, final Type rootClass,
                                            final Map<Type, Type> resolved) {
        final Type alreadyResolved = resolved.get(value);
        if (alreadyResolved != null) {
            return alreadyResolved;
        }

        final TypeVariable<?> tv = TypeVariable.class.cast(value);
        Type parent = rootClass;
        while (Class.class.isInstance(parent)) {
            parent = Class.class.cast(parent).getGenericSuperclass();
        }
        while (ParameterizedType.class.isInstance(parent) && ParameterizedType.class.cast(parent).getRawType() != tv.getGenericDeclaration()) {
            parent = Class.class.cast(ParameterizedType.class.cast(parent).getRawType()).getGenericSuperclass();
        }
        if (ParameterizedType.class.isInstance(parent)) {
            final ParameterizedType parentPt = ParameterizedType.class.cast(parent);
            final int argIndex = asList(Class.class.cast(parentPt.getRawType()).getTypeParameters()).indexOf(tv);
            if (argIndex >= 0) {
                final Type type = parentPt.getActualTypeArguments()[argIndex];
                if (TypeVariable.class.isInstance(type)) {
                    return resolveTypeVariable(type, rootClass, resolved);
                }
                return type;
            }
        }
        if (Class.class.isInstance(rootClass)) {
            return Object.class; // prefer a default over
        }
        return value;
    }
}
