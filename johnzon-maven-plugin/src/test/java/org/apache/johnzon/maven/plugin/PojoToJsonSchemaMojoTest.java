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
package org.apache.johnzon.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;

public class PojoToJsonSchemaMojoTest {
    @Test
    public void generate() throws MojoExecutionException, IOException {
        final PojoToJsonSchemaMojo mojo = new PojoToJsonSchemaMojo();
        mojo.classesDir = new File("target/classes");
        mojo.target = new File("target/workdir-PojoToJsonSchemaMojoTest/output.json");
        mojo.description = "Test desc";
        mojo.title = "Test title";
        mojo.project = new MavenProject() {
            @Override
            public Set<Artifact> getArtifacts() {
                return emptySet();
            }
        };
        mojo.schemaClass = Foo.class.getName();
        if (mojo.target.exists()) {
            mojo.target.delete();
        }
        mojo.execute();
        assertEquals("" +
                        "{\n" +
                        "  \"$id\":\"org_apache_johnzon_maven_plugin_PojoToJsonSchemaMojoTest_Foo\",\n" +
                        "  \"type\":\"object\",\n" +
                        "  \"title\":\"Test title\",\n" +
                        "  \"description\":\"Test desc\",\n" +
                        "  \"properties\":{\n" +
                        "    \"nested\":{\n" +
                        "      \"$id\":\"org_apache_johnzon_maven_plugin_PojoToJsonSchemaMojoTest_Bar\",\n" +
                        "      \"type\":\"object\",\n" +
                        "      \"properties\":{\n" +
                        "        \"simple\":{\n" +
                        "          \"type\":\"string\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"other\":{\n" +
                        "      \"type\":\"array\",\n" +
                        "      \"items\":{\n" +
                        "        \"type\":\"string\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"simple\":{\n" +
                        "      \"type\":\"string\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}" +
                        "",
                Files.readAllLines(mojo.target.toPath()).stream().collect(joining("\n")));
    }

    public static class Foo {
        public String simple;
        public Bar nested;
        public List<String> other;
    }

    public static class Bar {
        public String simple;
    }
}
