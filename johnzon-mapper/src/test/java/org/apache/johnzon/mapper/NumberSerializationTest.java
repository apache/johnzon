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
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.junit.Test;

public class NumberSerializationTest {
    @Test
    public void toJson() {
        final Mapper mapper = new MapperBuilder().setUseJsRange(true).build();
        final Holder holder = new Holder();
        holder.value = 1;
        assertEquals("{\"value\":1}", mapper.writeObjectAsString(holder));
        holder.value = Long.MAX_VALUE;
        assertEquals("{\"value\":\"9223372036854775807\"}", mapper.writeObjectAsString(holder));
        mapper.close();
    }

    @Test
    public void numberFromJson() {
        final Mapper mapper = new MapperBuilder().build();
        final Num num = mapper.readObject("{\"value\":0}", Num.class);
        assertTrue(BigDecimal.class.isInstance(num.value));
        assertEquals(0, num.value.intValue());
        mapper.close();
    }

    public static class Holder {
        public long value;
    }

    public static class Num {
        public Number value;
    }
}
