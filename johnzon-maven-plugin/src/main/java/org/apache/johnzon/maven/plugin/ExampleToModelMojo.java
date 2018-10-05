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

import static java.util.Arrays.asList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.lang.model.element.Modifier;

import org.apache.commons.lang3.StringUtils;
import org.apache.johnzon.mapper.JohnzonProperty;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

@Mojo(name = "example-to-model", defaultPhase = GENERATE_SOURCES)
public class ExampleToModelMojo extends AbstractMojo {
    // not strictly forbidden but kind of file to java convertion
    private static final List<Character> FORBIDDEN_JAVA_NAMES = asList('-', '_', '.');

    @Parameter(property = "johnzon.source", defaultValue = "${project.basedir}/src/main/johnzon")
    protected File source;

    @Parameter(property = "johnzon.target", defaultValue = "${project.build.directory}/generated-sources/johnzon")
    protected File target;

    @Parameter(property = "johnzon.package", defaultValue = "com.johnzon.generated")
    protected String packageBase;

    @Parameter
    protected String header;

    @Parameter
    protected MavenProject project;

    @Parameter(property = "johnzon.attach", defaultValue = "true")
    protected boolean attach;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final JsonReaderFactory readerFactory = Json.createReaderFactory(Collections.<String, Object>emptyMap());
        if (source.isFile()) {
            generateFile(readerFactory, source);
        } else {
            final File[] children = source.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    return name.endsWith(".json");
                }
            });
            if (children == null || children.length == 0) {
                throw new MojoExecutionException("No json file found in " + source);
            }
            for (final File child : children) {
                generateFile(readerFactory, child);
            }
        }
        if (attach && project != null) {
            project.addCompileSourceRoot(target.getAbsolutePath());
        }
    }

    // TODO: unicity of field name, better nested array/object handling
    private void generate(final JsonReaderFactory readerFactory, final File source, final TypeSpec.Builder targetType) throws MojoExecutionException {
        JsonReader reader = null;
        try {
            reader = readerFactory.createReader(new FileReader(source));
            final JsonStructure structure = reader.read();
            if (JsonArray.class.isInstance(structure) || !JsonObject.class.isInstance(structure)) { // quite redundant for now but to avoid surprises in future
                throw new MojoExecutionException("This plugin doesn't support array generation, generate the model (generic) and handle arrays in your code");
            }

            final JsonObject object = JsonObject.class.cast(structure);

            if (header != null) {
                targetType.addJavadoc(header);
            }

            generateFieldsAndMethods(object, targetType);
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void generateFieldsAndMethods(final JsonObject object, TypeSpec.Builder targetType) {
        final Map<String, JsonObject> nestedTypes = new TreeMap<String, JsonObject>();
        {
            for (final Map.Entry<String, JsonValue> entry : object.entrySet()) {
                final String key = entry.getKey();
                final String fieldName = toJavaFieldName(key);
                switch (entry.getValue().getValueType()) {
                    case ARRAY:
                        handleArray(targetType, nestedTypes, entry.getValue(), key, fieldName, 1);
                        break;
                    case OBJECT:
                        final String type = toJavaName(fieldName);
                        nestedTypes.put(type, JsonObject.class.cast(entry.getValue()));
                        fieldGetSetMethods(targetType, key, fieldName, type, 0);
                        break;
                    case TRUE:
                    case FALSE:
                        fieldGetSetMethods(targetType, key, fieldName, "Boolean", 0);
                        break;
                    case NUMBER:
                        fieldGetSetMethods(targetType, key, fieldName, "Double", 0);
                        break;
                    case STRING:
                        fieldGetSetMethods(targetType, key, fieldName, "String", 0);
                        break;
                    case NULL:
                    default:
                        throw new UnsupportedOperationException("Unsupported " + entry.getValue() + ".");
                }
            }
        }

        for (final Map.Entry<String, JsonObject> entry : nestedTypes.entrySet()) {
            TypeSpec.Builder nestedType = TypeSpec.classBuilder(entry.getKey())
                                                  .addModifiers( Modifier.PUBLIC, Modifier.STATIC );

            generateFieldsAndMethods(entry.getValue(), nestedType);

            targetType.addType(nestedType.build());
        }
    }

    private void handleArray(final TypeSpec.Builder targetType,
                             final Map<String, JsonObject> nestedTypes,
                             final JsonValue value,
                             final String jsonField,
                             final String fieldName,
                             final int arrayLevel) {
        final JsonArray array = JsonArray.class.cast(value);
        if (array.size() > 0) { // keep it simple for now - 1 level, we can have an awesome recursive algo later if needed
            final JsonValue jsonValue = array.get(0);
            switch (jsonValue.getValueType()) {
                case OBJECT:
                    final String javaName = toJavaName(fieldName);
                    nestedTypes.put(javaName, JsonObject.class.cast(jsonValue));
                    fieldGetSetMethods(targetType, jsonField, fieldName, javaName, arrayLevel);
                    break;
                case TRUE:
                case FALSE:
                    fieldGetSetMethods(targetType, jsonField, fieldName, "Boolean", arrayLevel);
                    break;
                case NUMBER:
                    fieldGetSetMethods(targetType, jsonField, fieldName, "Double", arrayLevel);
                    break;
                case STRING:
                    fieldGetSetMethods(targetType, jsonField, fieldName, "String", arrayLevel);
                    break;
                case ARRAY:
                    handleArray(targetType, nestedTypes, jsonValue, jsonField, fieldName, arrayLevel + 1);
                    break;
                case NULL:
                default:
                    throw new UnsupportedOperationException("Unsupported " + value + ".");
            }
        } else {
            getLog().warn("Empty arrays are ignored");
        }
    }

    private void fieldGetSetMethods(final TypeSpec.Builder targetType,
                                    final String jsonField,
                                    final String field,
                                    final String type,
                                    final int arrayLevel) {
        final TypeName actualType =  buildArrayType(arrayLevel, type);
        final String actualField = buildValidFieldName(jsonField);
        final String methodName = StringUtils.capitalize(actualField);

        FieldSpec.Builder fieldBuilder = FieldSpec.builder(actualType, actualField, Modifier.PRIVATE);

        if (!jsonField.equals(field)) {
            fieldBuilder.addAnnotation(AnnotationSpec.builder(JohnzonProperty.class)
                                                     .addMember("value", "$S", jsonField)
                                                     .build());
        }

        targetType.addField(fieldBuilder.build());

        targetType.addMethod(MethodSpec.methodBuilder("get" + methodName)
                                       .addModifiers(Modifier.PUBLIC)
                                       .returns(actualType)
                                       .addStatement("return $L", actualField)
                                       .build());

        targetType.addMethod(MethodSpec.methodBuilder("set" + methodName)
                                       .addModifiers(Modifier.PUBLIC)
                                       .addParameter(actualType, actualField, Modifier.FINAL)
                                       .addStatement("this.$1L = $1L", actualField)
                                       .build());
    }

    private TypeName buildArrayType(final int arrayLevel, final String type) {
        ClassName typeArgument = ClassName.bestGuess(type);

        if (arrayLevel == 0) { // quick exit
            return typeArgument;
        }

        return ParameterizedTypeName.get(ClassName.get(List.class), typeArgument);
    }

    private void visit(final JsonStructure structure, final Visitor visitor) {
        if (JsonObject.class.isInstance(structure)) {
            visitor.visitObject(JsonObject.class.cast(structure));
        } else if (JsonArray.class.isInstance(structure)) {
            visitor.visitArray(JsonArray.class.cast(structure));
        } else {
            throw new UnsupportedOperationException("Can't visit " + structure);
        }
    }

    private void generateFile(final JsonReaderFactory readerFactory, final File source) throws MojoExecutionException {
        final String javaName = StringUtils.capitalize(toJavaName(source.getName()));

        TypeSpec.Builder targetType = TypeSpec.classBuilder(javaName).addModifiers(Modifier.PUBLIC);

        generate(readerFactory, source, targetType);

        try {
            JavaFile.builder(packageBase, targetType.build()).build().writeTo(target);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private String buildValidFieldName(final String jsonField) {
        String val = jsonField;
        if (Character.isDigit(jsonField.charAt(0))) {
            val = "_" + jsonField;
        }
        return val.replace(".", "");
    }

    private String toJavaFieldName(final String key) {
        return StringUtils.uncapitalize(toJavaName(key));
    }

    private String toJavaName(final String file) {
        final StringBuilder builder = new StringBuilder();
        boolean nextUpper = false;
        for (final char anIn : file.replace(".json", "").toCharArray()) {
            if (FORBIDDEN_JAVA_NAMES.contains(anIn)) {
                nextUpper = true;
            } else if (nextUpper) {
                builder.append(Character.toUpperCase(anIn));
                nextUpper = false;
            } else {
                builder.append(anIn);
            }
        }
        return StringUtils.capitalize(builder.toString());
    }

    private interface Visitor {
        void visitObject(JsonObject structure);

        void visitArray(JsonArray structure);
    }
}
