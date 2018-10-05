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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ExampleToModelMojoTest {
    @Test
    public void generate() throws Exception {
        final File sourceFolder = new File("target/ExampleToModelMojoTest/source/");
        final File targetFolder = new File("target/ExampleToModelMojoTest/target/");
        final ExampleToModelMojo mojo = new ExampleToModelMojo() {{
            source = sourceFolder;
            target = targetFolder;
            packageBase = "org.test.apache.johnzon.mojo";
            header =
                "Licensed to the Apache Software Foundation (ASF) under one\n" +
                "or more contributor license agreements. See the NOTICE file\n" +
                "distributed with this work for additional information\n" +
                "regarding copyright ownership. The ASF licenses this file\n" +
                "to you under the Apache License, Version 2.0 (the\n" +
                "\"License\"); you may not use this file except in compliance\n" +
                "with the License. You may obtain a copy of the License at\n" +
                "\n" +
                "  http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "Unless required by applicable law or agreed to in writing,\n" +
                "software distributed under the License is distributed on an\n" +
                "\"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
                "KIND, either express or implied. See the License for the\n" +
                "specific language governing permissions and limitations\n" +
                "under the License.\n";
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
        assertTrue(output.exists());
        assertTrue(output.isFile());

        File input = new File(getClass().getResource("/SomeValue.java").toURI());
        CompilationUnit expected = JavaParser.parse(input);

        CompilationUnit actual = JavaParser.parse(output);

        assertEquals(expected.getPackageDeclaration(), actual.getPackageDeclaration());
        assertEquals(expected.getImports(), actual.getImports());
        assertEquals(expected.getPrimaryTypeName(), actual.getPrimaryTypeName());

        //assertEquals( expected.accept( v, arg );, actual );
        CollectorVisitor expectedVisitor = visit(expected);
        CollectorVisitor actualVisitor = visit(actual);

        expectedVisitor.getTypes()
                       .forEach(type -> assertThat(actualVisitor.getTypes(), hasItem(type)));
        expectedVisitor.getFields()
                       .forEach(field -> assertThat(actualVisitor.getFields(), hasItem(field)));
        expectedVisitor.getAnnotations()
                       .forEach(annotation -> assertThat(actualVisitor.getAnnotations(), hasItem(annotation)));
        expectedVisitor.getMethods()
                       .forEach(method -> assertThat(actualVisitor.getMethods(), hasItem(method)));
    }

    private static CollectorVisitor visit(CompilationUnit unit) {
        CollectorVisitor visitor = new CollectorVisitor();
        unit.accept(visitor, null);
        return visitor;
    }

    private static final class CollectorVisitor extends VoidVisitorAdapter<Void> {

        public List<String> getTypes() {
            return types;
        }

        private final List<String> fields = new LinkedList<>();

        private final List<String> annotations = new LinkedList<>();

        private final List<String> methods = new LinkedList<>();

        private final List<String> types = new LinkedList<>();

        public List<String> getFields() {
            return fields;
        }

        public List<String> getAnnotations() {
            return annotations;
        }

        public List<String> getMethods() {
            return methods;
        }

        @Override
        public void visit(FieldDeclaration field, Void arg) {
            field.getVariables().forEach(current -> fields.add(current.getNameAsString()));
            super.visit(field, arg);
        }

        @Override
        public void visit(AnnotationMemberDeclaration annotation, Void arg) {
            annotations.add(annotation.getNameAsString());
            super.visit(annotation, arg);
        }

        @Override
        public void visit(MethodDeclaration method, Void arg) {
            methods.add(method.getNameAsString());
            super.visit(method, arg);
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration type, Void arg) {
            types.add(type.getNameAsString());
            super.visit(type, arg);
        }

    }

}
