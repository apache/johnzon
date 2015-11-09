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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static java.util.Arrays.asList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

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
    private void generate(final JsonReaderFactory readerFactory, final File source, final Writer writer, final String javaName) throws MojoExecutionException {
        JsonReader reader = null;
        try {
            reader = readerFactory.createReader(new FileReader(source));
            final JsonStructure structure = reader.read();
            if (JsonArray.class.isInstance(structure) || !JsonObject.class.isInstance(structure)) { // quite redundant for now but to avoid surprises in future
                throw new MojoExecutionException("This plugin doesn't support array generation, generate the model (generic) and handle arrays in your code");
            }

            final JsonObject object = JsonObject.class.cast(structure);
            final Collection<String> imports = new TreeSet<String>();

            // while we browse the example tree just store imports as well, avoids a 2 passes processing duplicating imports logic
            final StringWriter memBuffer = new StringWriter();
            generateFieldsAndMethods(memBuffer, object, "    ", imports);

            if (header != null) {
                writer.write(header);
                writer.write('\n');
            }

            writer.write("package " + packageBase + ";\n\n");

            if (!imports.isEmpty()) {
                for (final String imp : imports) {
                    writer.write("import " + imp + ";\n");
                }
                writer.write('\n');
            }

            writer.write("public class " + javaName + " {\n");
            writer.write(memBuffer.toString());
            writer.write("}\n");
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void generateFieldsAndMethods(final Writer writer, final JsonObject object, final String prefix,
                                          final Collection<String> imports) throws IOException {
        final Map<String, JsonObject> nestedTypes = new TreeMap<String, JsonObject>();
        {
            final Iterator<Map.Entry<String, JsonValue>> iterator = object.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<String, JsonValue> entry = iterator.next();
                final String key = entry.getKey();
                final String fieldName = toJavaFieldName(key);
                switch (entry.getValue().getValueType()) {
                    case ARRAY:
                        imports.add("java.util.List");
                        handleArray(writer, prefix, nestedTypes, entry.getValue(), key, fieldName, 1, imports);
                        break;
                    case OBJECT:
                        final String type = toJavaName(fieldName);
                        nestedTypes.put(type, JsonObject.class.cast(entry.getValue()));
                        fieldGetSetMethods(writer, key, fieldName, type, prefix, 0, imports);
                        break;
                    case TRUE:
                    case FALSE:
                        fieldGetSetMethods(writer, key, fieldName, "Boolean", prefix, 0, imports);
                        break;
                    case NUMBER:
                        fieldGetSetMethods(writer, key, fieldName, "Double", prefix, 0, imports);
                        break;
                    case STRING:
                        fieldGetSetMethods(writer, key, fieldName, "String", prefix, 0, imports);
                        break;
                    case NULL:
                    default:
                        throw new UnsupportedOperationException("Unsupported " + entry.getValue() + ".");
                }
                if (iterator.hasNext()) {
                    writer.write("\n");
                }
            }
        }

        if (!object.isEmpty() && !nestedTypes.isEmpty()) {
            writer.write("\n");
        }

        final Iterator<Map.Entry<String, JsonObject>> entries = nestedTypes.entrySet().iterator();
        while (entries.hasNext()) {
            final Map.Entry<String, JsonObject> entry = entries.next();
            writer.write(prefix + "public static class " + entry.getKey() + " {\n");
            generateFieldsAndMethods(writer, entry.getValue(), "    " + prefix, imports);
            writer.write(prefix + "}\n");
            if (entries.hasNext()) {
                writer.write("\n");
            }
        }
    }

    private void handleArray(final Writer writer, final String prefix,
                             final Map<String, JsonObject> nestedTypes,
                             final JsonValue value,
                             final String jsonField,final String fieldName,
                             final int arrayLevel,
                             final Collection<String> imports) throws IOException {
        final JsonArray array = JsonArray.class.cast(value);
        if (array.size() > 0) { // keep it simple for now - 1 level, we can have an awesome recursive algo later if needed
            final JsonValue jsonValue = array.get(0);
            switch (jsonValue.getValueType()) {
                case OBJECT:
                    final String javaName = toJavaName(fieldName);
                    nestedTypes.put(javaName, JsonObject.class.cast(jsonValue));
                    fieldGetSetMethods(writer, jsonField, fieldName, javaName, prefix, arrayLevel, imports);
                    break;
                case TRUE:
                case FALSE:
                    fieldGetSetMethods(writer, jsonField, fieldName, "Boolean", prefix, arrayLevel, imports);
                    break;
                case NUMBER:
                    fieldGetSetMethods(writer, jsonField, fieldName, "Double", prefix, arrayLevel, imports);
                    break;
                case STRING:
                    fieldGetSetMethods(writer, jsonField, fieldName, "String", prefix, arrayLevel, imports);
                    break;
                case ARRAY:
                    handleArray(writer, prefix, nestedTypes, jsonValue, jsonField, fieldName, arrayLevel + 1, imports);
                    break;
                case NULL:
                default:
                    throw new UnsupportedOperationException("Unsupported " + value + ".");
            }
        } else {
            getLog().warn("Empty arrays are ignored");
        }
    }

    private void fieldGetSetMethods(final Writer writer,
                                    final String jsonField, final String field,
                                    final String type, final String prefix, final int arrayLevel,
                                    final Collection<String> imports) throws IOException {
        final String actualType = buildArrayType(arrayLevel, type);
        final String actualField = buildValidFieldName(jsonField);
        final String methodName = StringUtils.capitalize(actualField);

        if (!jsonField.equals(field)) { // TODO: add it to imports in eager visitor
            imports.add("org.apache.johnzon.mapper.JohnzonProperty");
            writer.append(prefix).append("@JohnzonProperty(\"").append(jsonField).append("\")\n");
        }

        writer.append(prefix).append("private ").append(actualType).append(" ").append(actualField).append(";\n");
        writer.append(prefix).append("public ").append(actualType).append(" get").append(methodName).append("() {\n");
        writer.append(prefix).append("    return ").append(actualField).append(";\n");
        writer.append(prefix).append("}\n");
        writer.append(prefix).append("public void set").append(methodName).append("(final ").append(actualType).append(" newValue) {\n");
        writer.append(prefix).append("    this.").append(actualField).append(" = newValue;\n");
        writer.append(prefix).append("}\n");
    }

    private String buildArrayType(final int arrayLevel, final String type) {
        if (arrayLevel == 0) { // quick exit
            return type;
        }

        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < arrayLevel; i++) {
            builder.append("List<");
        }
        builder.append(type);
        for (int i = 0; i < arrayLevel; i++) {
            builder.append(">");
        }
        return builder.toString();
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
        final String jsonToClass = packageBase + '.' + javaName;
        final File outputFile = new File(target, jsonToClass.replace('.', '/') + ".java");

        outputFile.getParentFile().mkdirs();
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputFile);
            generate(readerFactory, source, writer, javaName);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (final IOException e) {
                // no-op
            }
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
