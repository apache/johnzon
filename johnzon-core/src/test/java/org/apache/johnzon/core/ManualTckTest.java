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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ManualTckTest {


    @Test
    public void jsonParser11Test() throws Exception {
        Class parserClass = loadClass("com.sun.ts.tests.jsonp.api.jsonparsertests.Parser");
        if (parserClass == null) {
            // no TCK available, so we skip the test
            return;
        }

        Constructor constructor = parserClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object parserTest = constructor.newInstance();
        assertNotNull(parserTest);

        Method testMethod = parserClass.getDeclaredMethod("test");
        testMethod.setAccessible(true);
        final Object result = testMethod.invoke(parserTest);

        Class testResultClass = loadClass("com.sun.ts.tests.jsonp.api.common.TestResult");
        Method evalMethod = testResultClass.getDeclaredMethod("eval");
        evalMethod.invoke(result);
    }

    private Class loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException cnfe) {
            return null;
        }
    }
}
