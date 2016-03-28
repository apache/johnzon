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
package org.apache.johnzon.mapper;

import java.lang.reflect.Type;

/**
 * Convert a given Java Type to it's JSON String representation.
 * And the other way around.
 *
 * An example would be to convert joda LocalDate into Strings and back.
 *
 * @param <T>
 */
public interface Converter<T> extends MapperConverter {
    String toString(T instance);
    T fromString(String text);

    // for generic converters it allows to explicitely provide the converted type (ex: enum converter)
    // typically useful when generic type get resolved to a TypeVariable
    interface TypeAccess {
        Type type();
    }
}
