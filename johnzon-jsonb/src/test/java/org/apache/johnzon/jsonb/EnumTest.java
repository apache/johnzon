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
package org.apache.johnzon.jsonb;

import static org.junit.Assert.assertEquals;

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

public class EnumTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    @Test
    public void noDeclaringClass() { // JOHNZON-292
        assertEquals("\"A\"", jsonb.toJson(E.A));

        final Container c2 = new Container();
        c2.e = E.A;
        assertEquals("{\"e\":\"A\"}", jsonb.toJson(c2));
    }

    public static class Container {
        public E e;
    }

    public enum E {
        A {}
    }
}
