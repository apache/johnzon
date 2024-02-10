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

import org.junit.Ignore;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonReader;
import java.io.StringReader;

@Ignore
public class HugeStringTest {
    @Test
    public void test() {
        StringBuilder jsonBuilder = new StringBuilder("{\"data\":\"");
        for (int i = 0; i < 50 * 1024 * 1024 + 1; i++) {
            jsonBuilder.append("a");
        }
        jsonBuilder.append("\"}");
        String json = jsonBuilder.toString();

        // Warmup
        for (int i = 0; i < 10; i++) {
            try (JsonReader reader = Json.createReader(new StringReader(json))) {
                reader.readObject();
            }
        }

        long start = System.currentTimeMillis();
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            reader.readObject();
        }
        System.err.println("Took " + (System.currentTimeMillis() - start) + "ms");
    }
}
