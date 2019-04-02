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

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class TypesTest {
    
    interface GenericInterface<X, Y> {}
    
    interface PartialInterface<X> extends Serializable, GenericInterface<Integer, X> {}
    
    static abstract class AbstractClass<Z, Y> implements PartialInterface<Y> {}
    
    static class ConcreteClass extends AbstractClass<String, Boolean> {}
    
    @Test
    public void test() {
        ParameterizedType parameterizedType = Types.findParameterizedType(ConcreteClass.class, GenericInterface.class);
        
        Assert.assertTrue(Arrays.deepEquals(parameterizedType.getActualTypeArguments(), new Type[] {
                Integer.class,
                Boolean.class
        }));
    }
}
