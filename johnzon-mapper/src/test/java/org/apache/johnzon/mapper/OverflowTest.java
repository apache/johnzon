/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.johnzon.mapper;

import org.junit.Test;

import javax.json.Json;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class OverflowTest {
    @Test
    public void overflow() {
        final Mapper mapper = new MapperBuilder()
                .setReaderFactory(Json.createReaderFactory(new HashMap<String, Object>() {{
                    put("org.apache.johnzon.max-string-length", 1024 * 1024);
                    put("org.apache.johnzon.default-char-buffer", 2); // we will overflow > 1
                }}))
                .setAccessModeName("field")
                .build();
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1024 * 1024; i++) {
            builder.append("a");
        }
        final String val = builder.toString();
        final Value value = mapper.readObject("{\"value\":\"" + val + "\"}", Value.class);
        assertEquals(val, Value.class.cast(value).value);
        assertEquals("{\"value\":\"" + val + "\"}", mapper.writeObjectAsString(value));
    }

    public static class Value {
        public String value;
    }
}
