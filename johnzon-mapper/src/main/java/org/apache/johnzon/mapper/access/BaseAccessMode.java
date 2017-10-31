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

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.JohnzonAny;
import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.JohnzonConverter;
import org.apache.johnzon.mapper.MapperConverter;
import org.apache.johnzon.mapper.ObjectConverter;
import org.apache.johnzon.mapper.internal.ConverterAdapter;
import org.apache.johnzon.mapper.reflection.JohnzonParameterizedType;

import java.beans.ConstructorProperties;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.apache.johnzon.mapper.reflection.Converters.matches;

// handle some specific types
public abstract class BaseAccessMode implements AccessMode {
    private static final Type[] NO_PARAMS = new Type[0];

    private final Map<Class<?>, String[]> fieldsToRemove = new HashMap<Class<?>, String[]>();
    private final boolean acceptHiddenConstructor;
    private final boolean useConstructor;

    protected BaseAccessMode(final boolean useConstructor, final boolean acceptHiddenConstructor) {
        this.useConstructor = useConstructor;
        this.acceptHiddenConstructor = acceptHiddenConstructor;

        // mainly built it in the JVM types == user cant handle them
        fieldsToRemove.put(Throwable.class, new String[]{"suppressedExceptions", "cause"});
    }

    protected abstract Map<String,Reader> doFindReaders(Class<?> clazz);
    protected abstract Map<String,Writer> doFindWriters(Class<?> clazz);

    @Override
    public Comparator<String> fieldComparator(final Class<?> clazz) {
        return null;
    }

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

    @Override
    public ObjectConverter.Reader<?> findReader(final Class<?> clazz) {
        return null; // TODO: converter?
    }

    @Override
    public ObjectConverter.Writer<?> findWriter(final Class<?> clazz) {
        return null; // TODO: converter?
    }

    @Override
    public Adapter<?, ?> findAdapter(Class<?> clazz) {
        return null; // TODO: converter?
    }

    @Override
    public void afterParsed(final Class<?> clazz) {
        // no-op
    }

    @Override
    public Factory findFactory(final Class<?> clazz) {
        Constructor<?> constructor = null;
        for (final Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (c.getParameterTypes().length == 0) {
                if (!Modifier.isPublic(c.getModifiers()) && acceptHiddenConstructor) {
                    c.setAccessible(true);
                }
                constructor = c;
                if (!useConstructor) {
                    break;
                }
            } else if (c.getAnnotation(ConstructorProperties.class) != null) {
                constructor = c;
                break;
            }
        }
        if (constructor == null) {
            try {
                constructor = clazz.getConstructor();
            } catch (final NoSuchMethodException e) {
                return null; // readOnly class
            }
        }

        final boolean constructorHasArguments = constructor != null && constructor.getGenericParameterTypes().length > 0;
        final Type[] factoryParameterTypes;
        final String[] constructorParameters;
        final Adapter<?, ?>[] constructorParameterConverters;
        final Adapter<?, ?>[] constructorItemParameterConverters;
        final ObjectConverter.Codec<?>[] objectConverters;
        if (constructorHasArguments) {
            factoryParameterTypes = constructor.getGenericParameterTypes();

            constructorParameters = new String[constructor.getGenericParameterTypes().length];
            final ConstructorProperties constructorProperties = constructor.getAnnotation(ConstructorProperties.class);
            System.arraycopy(constructorProperties.value(), 0, constructorParameters, 0, constructorParameters.length);

            constructorParameterConverters = new Adapter<?, ?>[constructor.getGenericParameterTypes().length];
            constructorItemParameterConverters = new Adapter<?, ?>[constructorParameterConverters.length];
            objectConverters = new ObjectConverter.Codec[constructorParameterConverters.length];
            for (int i = 0; i < constructorParameters.length; i++) {
                for (final Annotation a : constructor.getParameterAnnotations()[i]) {
                    if (a.annotationType() == JohnzonConverter.class) {
                        try {
                            MapperConverter mapperConverter = JohnzonConverter.class.cast(a).value().newInstance();
                            if (mapperConverter instanceof Converter) {
                                final Adapter<?, ?> converter = new ConverterAdapter((Converter) mapperConverter);
                                if (matches(constructor.getParameterTypes()[i], converter)) {
                                    constructorParameterConverters[i] = converter;
                                    constructorItemParameterConverters[i] = null;
                                } else {
                                    constructorParameterConverters[i] = null;
                                    constructorItemParameterConverters[i] = converter;
                                }
                            } else {
                                objectConverters[i] = (ObjectConverter.Codec<?>) mapperConverter;
                            }
                        } catch (final Exception e) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                }
            }
        } else {
            factoryParameterTypes = NO_PARAMS;
            constructorParameters = null;
            constructorParameterConverters = null;
            constructorItemParameterConverters = null;
            objectConverters = null;
        }

        final Constructor<?> cons = constructor;
        if (cons != null && !cons.isAccessible()) {
            cons.setAccessible(true);
        }
        return new Factory() {
            @Override
            public Object create(final Object[] params) {
                if (cons == null) {
                    throw new IllegalArgumentException(clazz.getName() + " can't be instantiated by Johnzon, this is a write only class");
                }
                try {
                    return params == null ? cons.newInstance() : cons.newInstance(params);
                } catch (final InstantiationException e) {
                    throw new IllegalStateException(e);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException(e.getCause());
                }
            }

            @Override
            public Type[] getParameterTypes() {
                return factoryParameterTypes;
            }

            @Override
            public String[] getParameterNames() {
                return constructorParameters;
            }

            @Override
            public Adapter<?, ?>[] getParameterConverter() {
                return constructorParameterConverters;
            }

            @Override
            public Adapter<?, ?>[] getParameterItemConverter() {
                return constructorItemParameterConverters;
            }

            @Override
            public ObjectConverter.Codec<?>[] getObjectConverter() {
                return objectConverters;
            }
        };
    }

    @Override
    public Method findAnyGetter(final Class<?> clazz) {
        Method m = null;
        for (final Method current : clazz.getMethods()) {
            if (current.getAnnotation(JohnzonAny.class) != null) {
                if (current.getParameterTypes().length == 0) {
                    if (!Map.class.isAssignableFrom(current.getReturnType())) {
                        throw new IllegalArgumentException("@JohnzonAny getters can only return a Map<String, Object>");
                    }
                    if (m != null) {
                        throw new IllegalArgumentException("Ambiguous @JohnzonAny on " + m + " and " + current);
                    }
                    m = current;
                }
            }
        }
        return m;
    }

    @Override
    public Method findAnySetter(final Class<?> clazz) {
        Method m = null;
        for (final Method current : clazz.getMethods()) {
            if (current.getAnnotation(JohnzonAny.class) != null) {
                final Class<?>[] parameterTypes = current.getParameterTypes();
                if (parameterTypes.length == 2 && parameterTypes[0] == String.class && parameterTypes[1] == Object.class) {
                    if (m != null) {
                        throw new IllegalArgumentException("Ambiguous @JohnzonAny on " + m + " and " + current);
                    }
                    m = current;
                }
            }
        }
        return m;
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

        if (clazz == null || clazz == genericDeclaration) {
            return clazz;
        }

        final Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return findClass(superclass, genericDeclaration);
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
