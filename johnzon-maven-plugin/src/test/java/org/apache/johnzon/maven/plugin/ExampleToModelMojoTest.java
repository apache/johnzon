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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExampleToModelMojoTest {
    @Test
    public void generate() throws MojoFailureException, MojoExecutionException, IOException {
        final File sourceFolder = new File("target/ExampleToModelMojoTest/source/");
        final File targetFolder = new File("target/ExampleToModelMojoTest/target/");
        final ExampleToModelMojo mojo = new ExampleToModelMojo() {{
            source = sourceFolder;
            target = targetFolder;
            packageBase = "org.test.apache.johnzon.mojo";
            header =
                "/*\n" +
                " * Licensed to the Apache Software Foundation (ASF) under one\n" +
                " * or more contributor license agreements. See the NOTICE file\n" +
                " * distributed with this work for additional information\n" +
                " * regarding copyright ownership. The ASF licenses this file\n" +
                " * to you under the Apache License, Version 2.0 (the\n" +
                " * \"License\"); you may not use this file except in compliance\n" +
                " * with the License. You may obtain a copy of the License at\n" +
                " *\n" +
                " * http://www.apache.org/licenses/LICENSE-2.0\n" +
                " *\n" +
                " * Unless required by applicable law or agreed to in writing,\n" +
                " * software distributed under the License is distributed on an\n" +
                " * \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
                " * KIND, either express or implied. See the License for the\n" +
                " * specific language governing permissions and limitations\n" +
                " * under the License.\n" +
                " */";
        }};

        sourceFolder.mkdirs();
        final FileWriter writer = new FileWriter(new File(sourceFolder, "some-value.json"));
        writer.write( // using openjmh as sample data
            "    {\n" +
            "        \"benchmark\" : \"com.sample.Perf.method\",\n" +
            "        \"mode\" : \"sample\",\n" +
            "        \"threads\" : 32,\n" +
            "        \"forks\" : 1,\n" +
            "        \"warmupIterations\" : 2,\n" +
            "        \"warmupTime\" : \"1 s\",\n" +
            "        \"measurementIterations\" : 3,\n" +
            "        \"measurementTime\" : \"1 s\",\n" +
            "        \"primaryMetric\" : {\n" +
            "            \"score\" : 6.951927808,\n" +
            "            \"scoreError\" : 0.7251433665600178,\n" +
            "            \"scoreConfidence\" : [\n" +
            "                6.226784441439982,\n" +
            "                7.677071174560018\n" +
            "            ],\n" +
            "            \"scorePercentiles\" : {\n" +
            "                \"0.0\" : 3.9468400640000003,\n" +
            "                \"50.0\" : 6.593445888000001,\n" +
            "                \"90.0\" : 9.925400985600001,\n" +
            "                \"95.0\" : 11.301132697600002,\n" +
            "                \"99.0\" : 11.844714496,\n" +
            "                \"99.9\" : 11.844714496,\n" +
            "                \"99.99\" : 11.844714496,\n" +
            "                \"99.999\" : 11.844714496,\n" +
            "                \"99.9999\" : 11.844714496,\n" +
            "                \"100.0\" : 11.844714496\n" +
            "            },\n" +
            "            \"scoreUnit\" : \"s/op\",\n" +
            "            \"rawData\" : [\n" +
            "                [\n" +
            "                    6.687817728,\n" +
            "                    7.169245184,\n" +
            "                    6.998720512\n" +
            "                ]\n" +
            "            ]\n" +
            "        },\n" +
            "        \"secondaryMetrics\" : {\n" +
            "        }\n" +
            "    }\n");
        writer.close();

        mojo.execute();

        final File output = new File(targetFolder, "org/test/apache/johnzon/mojo/SomeValue.java");
        assertTrue(output.isFile());
        assertEquals(
            new String(IOUtil.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream("SomeValue.java"))),
            new String(IOUtil.toByteArray(new FileReader(output))));
    }
}
