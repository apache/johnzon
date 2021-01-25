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

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CircularExceptionTest {
    // note that with KnownNotOpenedJavaTypes this test will not test circular case anymore
    // but we still care to test exceptions don't loop so kept it
    @Test
    public void dontStackOverFlow() {
        final Throwable oopsImVicous = new Exception("circular");
        oopsImVicous.getStackTrace(); // fill it
        oopsImVicous.initCause(new IllegalArgumentException(oopsImVicous));
        final String serialized = new MapperBuilder().setAccessModeName("field").build().writeObjectAsString(oopsImVicous);
        assertTrue(serialized.contains("\"message\":\"circular\""));
        assertTrue(serialized.contains("\"stackTrace\":[{"));
    }

}
