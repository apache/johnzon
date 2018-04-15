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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;

public final class Generics {
    private Generics() {
        // no-op
    }

    // todo: this piece of code needs to be enhanced a lot:
    // - better handling of the hierarchy
    // - wildcard support?
    // - cycle handling (Foo<Foo>)
    // - ....
    public static Type resolve(final Type value, final Class<?> rootClass) {
        if (TypeVariable.class.isInstance(value)) {
            return resolveTypeVariable(value, rootClass);
        }
        if (ParameterizedType.class.isInstance(value)) {
            return resolveParameterizedType(value, rootClass);
        }
        return value;
    }

    private static Type resolveParameterizedType(final Type value, final Class<?> rootClass) {
        Collection<Type> args = null;
        final ParameterizedType parameterizedType = ParameterizedType.class.cast(value);
        int index = 0;
        for (final Type arg : parameterizedType.getActualTypeArguments()) {
            final Type type = resolve(arg, rootClass);
            if (type != arg) {
                if (args == null) {
                    args = new ArrayList<Type>();
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
    private static Type resolveTypeVariable(final Type value, final Class<?> rootClass) {
        final TypeVariable<?> tv = TypeVariable.class.cast(value);
        Type parent = rootClass == null ? null : rootClass.getGenericSuperclass();
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
                    return resolveTypeVariable(type, rootClass);
                }
                return type;
            }
        }
        return value;
    }
}
