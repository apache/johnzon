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

import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.MapperConverter;
import org.apache.johnzon.mapper.TypeAwareAdapter;
import org.apache.johnzon.mapper.internal.ConverterAdapter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class Converters {
    private Converters() {
        // no-op
    }

    // TODO: more ParameterizedType and maybe TypeClosure support
    public static boolean matches(final Type type, final MapperConverter adapter) {
        Type convertType = null;
        if (ConverterAdapter.class.isInstance(adapter)) {
            final Converter delegate = ConverterAdapter.class.cast(adapter).getConverter();
            if (Converter.TypeAccess.class.isInstance(delegate)) {
                convertType = Converter.TypeAccess.class.cast(delegate).type();
            } else {
                for (final Type pt : delegate.getClass().getGenericInterfaces()) {
                    if (ParameterizedType.class.isInstance(pt) && ParameterizedType.class.cast(pt).getRawType() == Converter.class) {
                        convertType = ParameterizedType.class.cast(pt).getActualTypeArguments()[0];
                        break;
                    }
                }
            }
        } else if (TypeAwareAdapter.class.isInstance(adapter)) {
            convertType = TypeAwareAdapter.class.cast(adapter).getFrom();
        }

        if (convertType == null) { // compatibility, previously nested converter were not supported
            return true;
        }

        if (ParameterizedType.class.isInstance(type)) {
            final ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
            final Type rawType = parameterizedType.getRawType();
            if (Class.class.isInstance(rawType)) {
                final Class<?> clazz = Class.class.cast(rawType);
                if (Collection.class.isAssignableFrom(clazz) && parameterizedType.getActualTypeArguments().length == 1) {
                    final Type argType = parameterizedType.getActualTypeArguments()[0];
                    if (Class.class.isInstance(argType) && Class.class.isInstance(convertType)) {
                        return !Class.class.cast(convertType).isAssignableFrom(Class.class.cast(argType));
                    }
                } else if (Map.class.isAssignableFrom(clazz) && parameterizedType.getActualTypeArguments().length == 2) {
                    final Type argType = parameterizedType.getActualTypeArguments()[1];
                    if (Class.class.isInstance(argType) && Class.class.isInstance(convertType)) {
                        return !Class.class.cast(convertType).isAssignableFrom(Class.class.cast(argType));
                    }
                }
                return true; // actually here we suppose we dont know
            }
        }
        if (Class.class.isInstance(type)) {
            final Class<?> clazz = Class.class.cast(type);
            if (clazz.isArray()) {
                return !Class.class.cast(convertType).isAssignableFrom(clazz.getComponentType());
            }
        }

        return true;
    }
}
