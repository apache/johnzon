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
package org.apache.johnzon.jsonb.generator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GeneratedJsonbTest {
    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void empty() throws IOException {
        final Path output = temp.getRoot().toPath();
        new JsonbMapperGenerator(new JsonbMapperGenerator.Configuration()
                .setClasses(singleton(Empty.class))
                .setOutput(output))
                .run();
        final Path result = output.resolve("org/apache/johnzon/jsonb/generator/GeneratedJsonbTest$Empty$$JohnzonJsonb.class");
        assertTrue(Files.exists(result));
        assertEquals("" +
                "" +
                "", new String(Files.readAllBytes(result), UTF_8));
    }

    @Test
    public void simplePOJO() throws IOException {
        final Path output = temp.getRoot().toPath();
        new JsonbMapperGenerator(new JsonbMapperGenerator.Configuration()
                .setClasses(singleton(Simple.class))
                .setOutput(output))
                .run();
        final Path result = output.resolve("org/apache/johnzon/jsonb/generator/GeneratedJsonbTest$Simple$$JohnzonJsonb.class");
        assertTrue(Files.exists(result));
        assertEquals("" +
                "" +
                "", new String(Files.readAllBytes(result), UTF_8));
    }

    public static class Empty {
    }

    public static class Simple {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }
    }
}
