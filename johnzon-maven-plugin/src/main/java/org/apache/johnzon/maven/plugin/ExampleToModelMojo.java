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

import static java.nio.charset.StandardCharsets.UTF_8;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
    private static final List<Character> FORBIDDEN_JAVA_NAMES = asList('-', '_', '.', '{', '}');

    @Parameter(property = "johnzon.source", defaultValue = "${project.basedir}/src/main/johnzon")
    protected File source;

    @Parameter(property = "johnzon.target", defaultValue = "${project.build.directory}/generated-sources/johnzon")
    protected File target;

    @Parameter(property = "johnzon.package", defaultValue = "com.johnzon.generated")
    protected String packageBase;

    @Parameter
    protected String header;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "johnzon.attach", defaultValue = "true")
    protected boolean attach;

    @Parameter(property = "johnzon.useRecord", defaultValue = "false")
    protected boolean useRecord;

    @Parameter(property = "johnzon.useJsonb", defaultValue = "false")
    protected boolean useJsonb;

    @Parameter(property = "johnzon.ignoreNull", defaultValue = "false")
    protected boolean ignoreNull;

    @Override
    public void execute() throws MojoExecutionException {
        final JsonReaderFactory readerFactory = Json.createReaderFactory(Collections.emptyMap());
        if (source.isFile()) {
            generateFile(readerFactory, source);
        } else {
            final File[] children = source.listFiles((dir, name) -> name.endsWith(".json"));
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
        try (final JsonReader reader = readerFactory.createReader(new FileReader(source))) {
            final JsonStructure structure = reader.read();
            if (JsonArray.class.isInstance(structure) || !JsonObject.class.isInstance(structure)) { // quite redundant for now but to avoid surprises in future
                throw new MojoExecutionException("This plugin doesn't support array generation, generate the model (generic) and handle arrays in your code");
            }

            final JsonObject object = JsonObject.class.cast(structure);
            final Collection<String> imports = new TreeSet<>();

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

            if (useRecord) {
                writer.write("public record " + javaName + "(");
                writer.write(memBuffer.toString());
            } else {
                writer.write("public class " + javaName + " {");
                writer.write(memBuffer.toString());
            }
            writer.write("}\n");
        } catch (final IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void generateFieldsAndMethods(final StringWriter writer, final JsonObject object, final String prefix,
                                          final Collection<String> imports) throws IOException {
        final Map<String, JsonObject> nestedTypes = new TreeMap<>();
        {
            final Iterator<Map.Entry<String, JsonValue>> iterator = object.entrySet().iterator();
            if (!object.isEmpty()) {
                writer.write("\n");
            }
            while (iterator.hasNext()) {
                final Map.Entry<String, JsonValue> entry = iterator.next();
                final String key = entry.getKey();
                final String fieldName = toJavaFieldName(key);
                final boolean hasNext = iterator.hasNext();
                switch (entry.getValue().getValueType()) {
                    case ARRAY:
                        imports.add("java.util.List");
                        handleArray(writer, prefix, nestedTypes, entry.getValue(), key, fieldName, 1, imports, !hasNext);
                        break;
                    case OBJECT:
                        final String type = toJavaName(fieldName);
                        nestedTypes.put(type, JsonObject.class.cast(entry.getValue()));
                        fieldGetSetMethods(writer, key, fieldName, type, prefix, 0, imports, !hasNext);
                        break;
                    case TRUE:
                    case FALSE:
                        fieldGetSetMethods(writer, key, fieldName, "Boolean", prefix, 0, imports, !hasNext);
                        break;
                    case NUMBER:
                        fieldGetSetMethods(writer, key, fieldName, "Double", prefix, 0, imports, !hasNext);
                        break;
                    case STRING:
                        fieldGetSetMethods(writer, key, fieldName, "String", prefix, 0, imports, !hasNext);
                        break;
                    case NULL:
                    default:
                        if (ignoreNull) {
                            if (useRecord && writer.getBuffer().length() > 0) {
                                writer.getBuffer().setLength(writer.getBuffer().length() - 2);
                                writer.write("\n");
                            }
                        } else {
                            throw new UnsupportedOperationException("Unsupported " + entry.getValue() + ".");
                        }
                }
                if (hasNext) {
                    writer.write("\n");
                } else if (useRecord) {
                    writer.write(") {\n");
                }
            }
        }

        if (object.isEmpty()) {
            if (useRecord) {
                writer.write(") {\n");
            } else {
                writer.write("\n");
            }
        }

        if (!object.isEmpty() && !nestedTypes.isEmpty()) {
            writer.write("\n");
        }

        final Iterator<Map.Entry<String, JsonObject>> entries = nestedTypes.entrySet().iterator();
        while (entries.hasNext()) {
            final Map.Entry<String, JsonObject> entry = entries.next();
            if (useRecord) {
                writer.write(prefix + "public static record " + entry.getKey() + "(");
            } else {
                writer.write(prefix + "public static class " + entry.getKey() + " {");
            }
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
                             final String jsonField, final String fieldName,
                             final int arrayLevel,
                             final Collection<String> imports,
                             final boolean last) throws IOException {
        final JsonArray array = JsonArray.class.cast(value);
        if (array.size() > 0) { // keep it simple for now - 1 level, we can have an awesome recursive algo later if needed
            final JsonValue jsonValue = array.get(0);
            switch (jsonValue.getValueType()) {
                case OBJECT:
                    final String javaName = toJavaName(fieldName);
                    nestedTypes.put(javaName, JsonObject.class.cast(jsonValue));
                    fieldGetSetMethods(writer, jsonField, fieldName, javaName, prefix, arrayLevel, imports, last);
                    break;
                case TRUE:
                case FALSE:
                    fieldGetSetMethods(writer, jsonField, fieldName, "Boolean", prefix, arrayLevel, imports, last);
                    break;
                case NUMBER:
                    fieldGetSetMethods(writer, jsonField, fieldName, "Double", prefix, arrayLevel, imports, last);
                    break;
                case STRING:
                    fieldGetSetMethods(writer, jsonField, fieldName, "String", prefix, arrayLevel, imports, last);
                    break;
                case ARRAY:
                    handleArray(writer, prefix, nestedTypes, jsonValue, jsonField, fieldName, arrayLevel + 1, imports, last);
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
                                    final Collection<String> imports, final boolean last) throws IOException {
        final String actualType = buildArrayType(arrayLevel, type);
        final String actualField = buildValidFieldName(jsonField);
        final String methodName = capitalize(actualField);

        if (!jsonField.equals(field)) {
            if (useJsonb) {
                imports.add("jakarta.json.bind.annotation.JsonbProperty");
                writer.append(prefix).append("@JsonbProperty(\"").append(jsonField).append("\")");
            } else {
                imports.add("org.apache.johnzon.mapper.JohnzonProperty");
                writer.append(prefix).append("@JohnzonProperty(\"").append(jsonField).append("\")");
            }
            if (useRecord) {
                writer.append(" ");
            } else {
                writer.append("\n").append(prefix);
            }
        } else {
            writer.append(prefix);
        }
        if (!useRecord) {
            writer.append("private ");
        }
        writer.append(actualType).append(" ").append(actualField);
        if (!useRecord) {
            writer.append(";\n");
            writer.append(prefix).append("public ").append(actualType).append(" get").append(methodName).append("() {\n");
            writer.append(prefix).append("    return ").append(actualField).append(";\n");
            writer.append(prefix).append("}\n");
            writer.append(prefix).append("public void set").append(methodName).append("(final ").append(actualType).append(" newValue) {\n");
            writer.append(prefix).append("    this.").append(actualField).append(" = newValue;\n");
            writer.append(prefix).append("}\n");
        } else if (!last) {
            writer.append(",");
        }
    }

    private String capitalize(final String str) {
        if (str != null && !str.isEmpty()) {
            final char firstChar = str.charAt(0);
            return Character.isTitleCase(firstChar) ?
                    str :
                    (Character.toTitleCase(firstChar) + str.substring(1));
        }
        return str;
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
        final String javaName = capitalize(toJavaName(source.getName()));
        final String jsonToClass = packageBase + '.' + javaName;
        final File outputFile = new File(target, jsonToClass.replace('.', '/') + ".java");

        outputFile.getParentFile().mkdirs();
        try (final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), UTF_8.name())) {
            generate(readerFactory, source, writer, javaName);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        // no-op
    }

    private String buildValidFieldName(final String jsonField) {
        String val = toJavaFieldName(jsonField);
        if (Character.isDigit(jsonField.charAt(0))) {
            val = "_" + jsonField;
        }
        return val.replace(".", "");
    }

    private String toJavaFieldName(final String key) {
        final String javaName = toJavaName(key);
        return Character.toLowerCase(javaName.charAt(0)) + javaName.substring(1);
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
        return capitalize(builder.toString());
    }

    private interface Visitor {
        void visitObject(JsonObject structure);

        void visitArray(JsonArray structure);
    }
}
