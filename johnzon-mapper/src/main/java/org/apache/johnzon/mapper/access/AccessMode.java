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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.ObjectConverter;

@FunctionalInterface
interface FindMethod {
    Method get(String name, Class<?> type, Class<?> param) throws NoSuchMethodException;
}

class MapHelper {
    private MapHelper() {
        // no-op
    }

    static Method find(final FindMethod finder, final Class<?> type) {
        return Stream.of(type.getGenericSuperclass())
                     .filter(ParameterizedType.class::isInstance)
                     .map(ParameterizedType.class::cast)
                     .filter(it -> Class.class.isInstance(it.getRawType()) && Map.class.isAssignableFrom(Class.class.cast(it.getRawType())))
                     .map(ParameterizedType::getActualTypeArguments)
                     .filter(a -> a.length == 2)
                     .map(a -> a[1])
                     .filter(Class.class::isInstance)
                     .map(Class.class::cast)
                     .flatMap(param -> {
                         final String simpleName = param.getSimpleName();
                         return Stream.of( // direct name or if the pattern is FoosImpl try addFoo
                                 simpleName,
                                 simpleName.replaceAll("Impl$" ,"").replaceAll("s$", ""))
                                      .map(it -> {
                                          try {
                                              return finder.get(it, type, param);
                                          } catch (final NoSuchMethodException e) {
                                              return null;
                                          }
                                      })
                                      .filter(Objects::nonNull);
                     })
                     .findFirst()
                     .orElse(null);
    }
}

public interface AccessMode {
    interface DecoratedType {
        Type getType();
        <T extends Annotation> T getAnnotation(Class<T> clazz);
        <T extends Annotation> T getClassOrPackageAnnotation(Class<T> clazz);
        Adapter<?, ?> findConverter();
        boolean isNillable(boolean globalConfig);
    }

    interface Writer extends DecoratedType {
        void write(Object instance, Object value);
        ObjectConverter.Reader<?> findObjectConverterReader();
    }

    interface Reader extends DecoratedType {
        Object read(Object instance);
        ObjectConverter.Writer<?> findObjectConverterWriter();
    }

    interface Factory {
        Object create(Object[] params);
        Type[] getParameterTypes();
        String[] getParameterNames();
        Adapter<?, ?>[] getParameterConverter();
        Adapter<?, ?>[] getParameterItemConverter();
        ObjectConverter.Codec<?>[] getObjectConverter();
    }

    Factory findFactory(Class<?> clazz, Function<AnnotatedElement, String>... parameterNameExtractors);

    default Factory findFactory(final Class<?> clazz) {
        return findFactory(clazz, null);
    }

    Comparator<String> fieldComparator(Class<?> clazz);
    Map<String, Reader> findReaders(Class<?> clazz);
    Map<String, Writer> findWriters(Class<?> clazz);
    ObjectConverter.Reader<?> findReader(Class<?> clazz);
    ObjectConverter.Writer<?> findWriter(Class<?> clazz);
    Adapter<?, ?> findAdapter(Class<?> clazz);
    Method findAnyGetter(Class<?> clazz);
    Method findAnySetter(Class<?> clazz);
    Field findAnyField(Class<?> clazz);

    default Method findMapAdder(final Class<?> clazz) {
        return MapHelper.find((name, type, param) -> type.getMethod("add" + name, String.class, param), clazz);
    }

    /**
     * Called once johnzon will not use AccessMode anymore. Can be used to clean up any local cache.
     *
     * @param clazz the parsed class.
     */
    void afterParsed(Class<?> clazz);
}
