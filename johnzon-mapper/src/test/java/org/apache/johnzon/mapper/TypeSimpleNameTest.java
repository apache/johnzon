/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.johnzon.mapper;

import org.junit.Test;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.function.Function;

import static org.apache.johnzon.mapper.ExceptionMessages.simpleName;
import static org.junit.Assert.assertEquals;

public class TypeSimpleNameTest {

    @Test
    public void clazz() {
        assertEquals("URI", simpleName(URI.class));
    }

    @Test
    public void innerClazz() {
        assertEquals("Simple$Nam3", simpleName(Simple$Nam3.class));
    }

    @Test
    public void parameterizedType() throws NoSuchFieldException {
        class Foo {
            Function<Simple$Nam3, Simple$Nam3> field;
        }
        final Type type = Foo.class.getDeclaredField("field").getGenericType();

        assertEquals("Function<Simple$Nam3,Simple$Nam3>", simpleName(type));
    }

    @Test
    public void genericArrayType() throws Exception {
        class Foo {
            Function<Simple$Nam3, Simple$Nam3>[] field;
        }
        final Type type = Foo.class.getDeclaredField("field").getGenericType();

        assertEquals("Function<Simple$Nam3,Simple$Nam3>[]", simpleName(type));
    }

    @Test
    public void wildcardArrayType() throws Exception {
        class Foo {
            Function<? extends Simple$Nam3, ? super Simple$Nam3> field;
        }
        final Type type = Foo.class.getDeclaredField("field").getGenericType();

        final String actual = simpleName(type);
        assertEquals("Function<? extends Simple$Nam3,? super Simple$Nam3>", actual);
    }

    @Test
    public void fallback() {
        final Type unsupportedType = new Type() {
            @Override
            public String getTypeName() {
                return "we.don't.know.what.it.might$Contain.123";
            }
        };

        assertEquals("we.don't.know.what.it.might$Contain.123", simpleName(unsupportedType));
    }

    //CHECKSTYLE:OFF
    public static class Simple$Nam3 {

    }
    //CHECKSTYLE:ON
}
