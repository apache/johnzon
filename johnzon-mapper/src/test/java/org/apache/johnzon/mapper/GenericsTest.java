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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.superbiz.Model;

import java.util.Comparator;

public class GenericsTest {
    @Test
    public void typeVariableMultiLevel() {
        final String input = "{\"aalist\":[{\"detail\":\"something2\",\"name\":\"Na2\"}]," +
                "\"childA\":{\"detail\":\"something\",\"name\":\"Na\"},\"childB\":{}}";

        final Mapper mapper = new MapperBuilder().setAttributeOrder(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        }).build();

        final Model model = mapper.readObject(input, Model.class);
        assertNotNull(model.getChildA());
        assertNotNull(model.getChildB());
        assertNotNull(model.getAalist());
        assertEquals("something", model.getChildA().detail);
        assertEquals("Na", model.getChildA().name);
        assertEquals(1, model.getAalist().size());
        assertEquals("something2", model.getAalist().iterator().next().detail);
        assertEquals(input, mapper.writeObjectAsString(model));
    }
}
