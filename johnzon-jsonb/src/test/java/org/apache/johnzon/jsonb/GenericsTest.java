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

import org.apache.johnzon.jsonb.model.Holder;
import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

public class GenericsTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    @Test
    public void genericModel() {
        final String json = jsonb.toJson(new StillGeneric<String>() {{ setInstance("Test String"); } });
        assertEquals("{\"instance\":\"Test String\"}", json);
        final StillGeneric<String> deserialized = jsonb.fromJson(json, new StillGeneric<String>() {
        }.getClass().getGenericSuperclass());
        assertEquals("Test String", deserialized.getInstance());
    }

    public static class StillGeneric<T> implements Holder<T> {
        private T value;

        @Override
        public T getInstance() {
            return value;
        }

        @Override
        public void setInstance(final T instance) {
            this.value = instance;
        }
    }
}
