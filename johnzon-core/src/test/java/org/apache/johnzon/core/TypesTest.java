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

import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.UUID;

public class TypesTest {
    
    interface Converter<From, To> {}
    
    interface ToStringConverter<X> extends Serializable, Converter<X, String> {}
    
    static abstract class AbstractToStringConverter<NotUsed, Z> implements ToStringConverter<Z> {}
    
    static class UUIDToStringConverter extends AbstractToStringConverter<Void, UUID> {}
    
    @Test
    public void test() {
        assertTypeParameters(Converter.class, Converter.class, variable("From"), variable("To"));
        assertTypeParameters(ToStringConverter.class, Converter.class, variable("X"), String.class);
        assertTypeParameters(AbstractToStringConverter.class, Converter.class, variable("Z"), String.class);
        assertTypeParameters(UUIDToStringConverter.class, Converter.class, UUID.class, String.class);
    }

    private static void assertTypeParameters(final Class<?> klass,
                                             final Class<?> parameterizedClass,
                                             final Type... types) {
        ParameterizedType parameterizedType = new Types().findParameterizedType(klass, parameterizedClass);
        Assert.assertNotNull(parameterizedType);
        Assert.assertEquals(parameterizedType.getRawType(), parameterizedClass);
        Assert.assertArrayEquals(types, parameterizedType.getActualTypeArguments());
    }

    private static Type variable(String name) {
        return new SimplifiedTypeVariable(name);
    }

    // Serves as a placeholder to hold variable name (and not reimplementing the whole TypeVariable)
    private static class SimplifiedTypeVariable implements Type {

        private final String name;

        public SimplifiedTypeVariable(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TypeVariable<?> && ((TypeVariable<?>) obj).getName().equals(this.name);
        }
    }
}
