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
package org.apache.johnzon.jsonschema.generator;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

// todo: support ref resolution for schema and avoid to generate as much classes as attributes
public class PojoGenerator {
    private final PojoConfiguration configuration;

    protected final Set<String> imports = new TreeSet<>(String::compareTo);
    protected final List<Attribute> attributes = new ArrayList<>();
    protected final Map<String, String> nested = new TreeMap<>(String::compareTo);
    private boolean isEnum;

    public PojoGenerator(final PojoConfiguration configuration) {
        this.configuration = configuration;
    }

    public PojoGenerator setNested(final Map<String, String> nested) {
        this.nested.putAll(nested);
        return this;
    }

    public Map<String, String> generate() {
        if (isEnum) {
            return nested;
        }

        final String name = configuration.getPackageName() + '.' + configuration.getClassName();
        final String path = name.replace('.', '/') + ".java";
        attributes.sort(comparing(a -> a.javaName));
        if (!attributes.isEmpty()) {
            imports.add(Objects.class.getName());
        }
        final String content = "" +
                "package " + configuration.getPackageName() + ";\n" +
                "\n" +
                (imports.isEmpty() ? "" : imports.stream()
                        .sorted()
                        .map(it -> "import " + it + ";")
                        .collect(joining("\n", "", "\n\n"))) +
                beforeClassDeclaration() +
                "public class " + configuration.getClassName() + afterClassName() + " {\n" +
                (attributes.isEmpty() ?
                        ("" +
                                "    @Override\n" +
                                "    public int hashCode() {\n" +
                                "        return 0;\n" +
                                "    }\n" +
                                "\n" +
                                "    @Override\n" +
                                "    public boolean equals(final Object other) {\n" +
                                "        return other instanceof " + configuration.getClassName() + ";\n" +
                                "    }\n") :
                        (attributes.stream()
                                .map(a -> "" +
                                        (configuration.isAddJsonbProperty() && !Objects.equals(a.javaName, a.jsonName) ?
                                                "    @JsonbProperty(\"" + a.jsonName.replace("\"", "\\\"") + "\")\n" :
                                                "") +
                                        "    private " + a.type + " " + a.javaName + ";")
                                .collect(joining("\n", "", "\n\n")) +
                                (configuration.isAddAllArgsConstructor() ?
                                        "    public " + configuration.getClassName() + "() {\n" +
                                                "        // no-op\n" +
                                                "    }\n" +
                                                "\n" +
                                                "    public " + configuration.getClassName() + "(" +
                                                attributes.stream()
                                                        .map(a -> "final " + a.type + " " + a.javaName)
                                                        .collect(joining(
                                                                ",\n" + IntStream.range(
                                                                                0,
                                                                                "    public (".length() +
                                                                                        configuration.getClassName().length())
                                                                        .mapToObj(i -> " ")
                                                                        .collect(joining()),
                                                                "",
                                                                ") {\n" +
                                                                        attributes.stream()
                                                                                .map(a -> "        this." + a.getJavaName() + " = " + a.javaName + ";\n")
                                                                                .collect(joining()) +
                                                                        "    }\n\n")) :
                                        "") +
                                attributes.stream()
                                        .map(a -> {
                                            final String marker = Character.toUpperCase(a.javaName.charAt(0)) + a.javaName.substring(1);
                                            return "" +
                                                    "    public " + a.type + " get" + Character.toUpperCase(a.javaName.charAt(0)) + a.javaName.substring(1) + "() {\n" +
                                                    "        return " + a.javaName + ";\n" +
                                                    "    }\n" +
                                                    "\n" +
                                                    "    public " +
                                                    (configuration.isFluentSetters() ? configuration.getClassName() : "void") +
                                                    " set" + marker + "(final " + a.type + " " + a.javaName + ") {\n" +
                                                    "        this." + a.javaName + " = " + a.javaName + ";\n" +
                                                    (configuration.isFluentSetters() ? "        return this;\n" : "") +
                                                    "    }\n" +
                                                    "";
                                        })
                                        .collect(joining("\n", "", "\n")) +
                                "    @Override\n" +
                                "    public int hashCode() {\n" +
                                "        return Objects.hash(\n" +
                                attributes.stream()
                                        .map(a -> a.javaName)
                                        .collect(joining(",\n                ", "                ", ");\n")) +
                                "    }\n" +
                                "\n" +
                                "    @Override\n" +
                                "    public boolean equals(final Object __other) {\n" +
                                "        if (!(__other instanceof " + configuration.getClassName() + ")) {\n" +
                                "            return false;\n" +
                                "        }\n" +
                                "        final " + configuration.getClassName() + " __otherCasted = (" + configuration.getClassName() + ") __other;\n" +
                                "        return " + attributes.stream()
                                .map(a -> a.javaName)
                                .map(it -> "Objects.equals(" + it + ", __otherCasted." + it + ")")
                                .collect(joining(" &&\n            ")) + ";\n" +
                                "    }\n"
                        )) +
                beforeClassEnd() +
                "}\n";
        if (nested.isEmpty()) {
            return singletonMap(path, content);
        }
        return Stream.concat(
                        nested.entrySet().stream(),
                        Stream.of(new AbstractMap.SimpleImmutableEntry<>(path, content)))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, () -> new TreeMap<>(String::compareTo)));
    }

    public PojoGenerator visitSchema(final JsonObject schema) {
        if (!schema.containsKey("properties")) {
            if (schema.containsKey("enum")) {
                isEnum = true;
                doEnum(schema.getJsonArray("enum"), configuration.getClassName());
                return this;
            }

            throw new IllegalArgumentException("Unsupported schema since it does not contain any properties: " + schema);
        }

        final JsonObject properties = getValueAs(schema, "properties", JsonObject.class);
        final JsonValue required = schema.get("required");
        final List<String> requiredAttrs = required != null && required.getValueType() == JsonValue.ValueType.ARRAY ?
                required.asJsonArray().stream().map(JsonString.class::cast).map(JsonString::getString).collect(toList()) :
                null;
        attributes.addAll(properties.entrySet().stream()
                .map(e -> {
                    final String javaName = toJavaName(e.getKey());
                    return new Attribute(
                            javaName, e.getKey(),
                            asType(javaName, e.getValue().asJsonObject(), requiredAttrs != null && requiredAttrs.contains(e.getKey())));
                })
                .collect(toList()));

        return this;
    }

    /**
     * @param ref the reference to resolve.
     * @return the reference class name if resolved else null.
     */
    protected String onRef(final Ref ref) {
        if (configuration.getOnRef() != null) {
            return configuration.getOnRef().apply(ref);
        }
        return null; // todo: check if already in nested for ex
    }

    protected String beforeEnumEnd() {
        return "";
    }

    protected String beforeClassEnd() {
        return "";
    }

    protected String afterClassName() {
        return "";
    }

    /**
     * @param values            the enum values (key is json name, value is java name).
     * @param valuesAreInjected is there a toString() with json name or is the enum anemic (only values, no method)
     * @return the data to add after enum name and before the opening brace in enum declaration.
     */
    protected String afterEnumName(final Map<String, String> values, final boolean valuesAreInjected) {
        return "";
    }

    protected String enumImports() {
        return "";
    }

    protected String beforeEnumDeclaration() {
        return "";
    }

    protected String beforeClassDeclaration() {
        return "";
    }

    protected String asType(final String javaName, final JsonObject schema, final boolean required) {
        final JsonValue ref = schema.get("$ref");
        if (ref != null && ref.getValueType() == JsonValue.ValueType.STRING) {
            final String name = onRef(new Ref(
                    JsonString.class.cast(ref).getString(), configuration.getClassName(), imports, attributes, nested));
            if (name != null) {
                return name;
            }
        }

        final JsonValue value = schema.get("type");
        String type;
        if (value == null) {
            if (schema.containsKey("properties") || schema.containsKey("additionalProperties")) {
                type = "object";
            } else if (schema.containsKey("items")) {
                type = "array";
            } else { // unknown, don't fail for wrongly written schema
                imports.add(JsonValue.class.getName());
                return JsonValue.class.getSimpleName();
            }
        } else {
            type = JsonString.class.cast(value).getString();
        }
        final JsonValue formatValue = schema.get("date-time");
        if (formatValue != null && formatValue.getValueType() == JsonValue.ValueType.STRING) {
            type = JsonString.class.cast(formatValue).getString();
        }

        switch (type) {
            case "array":
                final JsonObject items = getValueAs(schema, "items", JsonObject.class);
                final String itemType = onItemSchema(javaName, items);
                imports.add(List.class.getName());
                return List.class.getSimpleName() + "<" + itemType + ">";
            case "object":
                return onObjectAttribute(javaName, schema);
            case "null":
                imports.add(JsonValue.class.getName());
                return JsonValue.class.getSimpleName();
            case "boolean":
                return required ? "boolean" : "Boolean";
            case "string":
                final JsonValue enumList = schema.get("enum");
                if (enumList != null && enumList.getValueType() == JsonValue.ValueType.ARRAY) {
                    return onEnum(javaName, enumList, schema);
                }
                return "String";
            case "number":
            case "double": // openapi
                return required ? "double" : "Double";
            // openapi types
            case "int":
            case "int32":
            case "integer":
                return required ? "int" : "Integer";
            case "int64":
            case "long":
                return required ? "long" : "Long";
            case "float":
                return required ? "float" : "Float";
            case "date":
                imports.add(LocalDate.class.getName());
                return LocalDate.class.getSimpleName();
            case "duration":
                imports.add(Duration.class.getName());
                return Duration.class.getSimpleName();
            case "date-time":
            case "dateTime":
                imports.add(OffsetDateTime.class.getName());
                return OffsetDateTime.class.getSimpleName();
            case "time":
                imports.add(LocalTime.class.getName());
                return LocalTime.class.getSimpleName();
            case "byte":
                return "byte[]";
            case "uuid":
            case "hostname":
            case "idn-hostname":
            case "email":
            case "idn-email":
            case "ipv4":
            case "ipv6":
            case "uri":
            case "uri-reference":
            case "iri":
            case "iri-reference":
            case "uri-template":
            case "json-pointer":
            case "relative-json-pointer":
            case "regex":
            case "binary": // base64
            case "password":
                return "String";
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    protected String onEnum(final String javaName, final JsonValue enumList, final JsonObject schema) {
        final String className = enumName(javaName, schema);
        doEnum(enumList, className);
        return className;
    }

    private void doEnum(final JsonValue enumList, final String className) {
        final Map<String, String> values = enumList.asJsonArray().stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .collect(toMap(identity(), this::toJavaName));
        final boolean injectValues = !values.keySet().equals(new HashSet<>(values.values())); // java != json
        nested.put(
                configuration.getPackageName().replace('.', '/') + '/' + className + ".java", "" +
                        "package " + configuration.getPackageName() + ";\n" +
                        "\n" +
                        enumImports() +
                        (injectValues ? "import " + JsonbProperty.class.getName() + ";\n\n" : "") +
                        beforeEnumDeclaration() +
                        "public enum " + className + afterEnumName(values, injectValues) + " {\n" +
                        values.entrySet().stream()
                                .map(it -> "" +
                                        (injectValues && configuration.isAddJsonbProperty() ?
                                                "    @JsonbProperty(\"" + it.getKey().replace("\"", "\\\"") + "\")\n" :
                                                "") +
                                        "    " + it.getValue() +
                                        (injectValues ? "(\"" + it.getKey().replace("\"", "\\\"") + "\")" : ""))
                                .collect(joining(",\n", "", injectValues ? ";\n\n" : "\n")) +
                        (injectValues ?
                                "" +
                                        "    private String value;\n" +
                                        "\n" +
                                        "    " + className + "(final String value) {\n" +
                                        "        this.value = value;\n" +
                                        "    }\n" +
                                        "\n" +
                                        "    public String toString() {\n" +
                                        "        return value;\n" +
                                        "    }\n" +
                                        "" :
                                "") +
                        beforeEnumEnd() +
                        "}\n");
    }

    protected String enumName(final String javaName, final JsonObject schema) {
        return configuration.getClassName() + Character.toUpperCase(javaName.charAt(0)) + javaName.substring(1);
    }

    protected String onObjectAttribute(final String javaName, final JsonObject schema) {
        final JsonValue additionalProperties = schema.get("additionalProperties");
        final JsonValue properties = schema.get("properties");
        final boolean hasProperties = properties != null && properties.getValueType() == JsonValue.ValueType.OBJECT;
        if (!hasProperties &&
                additionalProperties != null &&
                additionalProperties.getValueType() == JsonValue.ValueType.OBJECT) {
            final JsonObject propSchema = additionalProperties.asJsonObject();
            final JsonValue propTypeValue = propSchema.get("type");
            if (propTypeValue != null && propTypeValue.getValueType() == JsonValue.ValueType.STRING) {
                String propType = JsonString.class.cast(propTypeValue).getString();
                final JsonValue formatValue = schema.get("date-time");
                if (formatValue != null && formatValue.getValueType() == JsonValue.ValueType.STRING) {
                    propType = JsonString.class.cast(formatValue).getString();
                }
                switch (propType) {
                    case "uuid":
                    case "hostname":
                    case "idn-hostname":
                    case "email":
                    case "idn-email":
                    case "ipv4":
                    case "ipv6":
                    case "uri":
                    case "uri-reference":
                    case "iri":
                    case "iri-reference":
                    case "uri-template":
                    case "json-pointer":
                    case "relative-json-pointer":
                    case "regex":
                    case "string":
                    case "binary":
                    case "password":
                        imports.add(Map.class.getName());
                        return "Map<String, String>";
                    case "boolean":
                        imports.add(Map.class.getName());
                        return "Map<String, Boolean>";
                    case "number":
                    case "double":
                        imports.add(Map.class.getName());
                        return "Map<String, Double>";
                    case "int":
                    case "int32":
                    case "integer":
                        imports.add(Map.class.getName());
                        return "Map<String, Integer>";
                    case "int64":
                    case "long":
                        imports.add(Map.class.getName());
                        return "Map<String, Long>";
                    case "float":
                        imports.add(Map.class.getName());
                        return "Map<String, Float>";
                    case "date":
                        imports.add(Map.class.getName());
                        imports.add(LocalDate.class.getName());
                        return "Map<String, LocalDate>";
                    case "dateTime":
                    case "date-time":
                        imports.add(Map.class.getName());
                        imports.add(OffsetDateTime.class.getName());
                        return "Map<String, OffsetDateTime>";
                    case "duration":
                        imports.add(Map.class.getName());
                        imports.add(Duration.class.getName());
                        return "Map<String, Duration>";
                    case "time":
                        imports.add(Map.class.getName());
                        imports.add(LocalTime.class.getName());
                        return "Map<String, LocalTime>";
                    default:
                        // todo: case array, object
                }
            }
        } else if (hasProperties) {
            final String className = configuration.getClassName() + Character.toUpperCase(javaName.charAt(0)) + javaName.substring(1);
            nested.putAll(newSubPojoGenerator(new PojoConfiguration()
                    .setPackageName(configuration.getPackageName())
                    .setClassName(className)
                    .setAddJsonbProperty(configuration.isAddJsonbProperty())
                    .setAddAllArgsConstructor(configuration.isAddAllArgsConstructor())
                    .setOnRef(configuration.getOnRef()), schema)
                    .visitSchema(schema)
                    .generate());
            return className;
        }

        imports.add(JsonObject.class.getName());
        return JsonObject.class.getSimpleName();
    }

    protected PojoGenerator newSubPojoGenerator(final PojoConfiguration pojoConfiguration, final JsonObject schema) {
        return new PojoGenerator(pojoConfiguration);
    }

    protected String onItemSchema(final String javaName, final JsonObject schema) {
        final JsonValue ref = schema.get("$ref");
        if (ref != null && ref.getValueType() == JsonValue.ValueType.STRING) {
            final String name = onRef(new Ref(
                    JsonString.class.cast(ref).getString(), configuration.getClassName(),
                    imports, attributes, nested));
            if (name != null) {
                return name;
            }
        }

        final JsonValue propTypeValue = schema.get("type");
        if (propTypeValue != null && propTypeValue.getValueType() == JsonValue.ValueType.STRING) {
            String type = JsonString.class.cast(propTypeValue).getString();
            final JsonValue formatValue = schema.get("date-time");
            if (formatValue != null && formatValue.getValueType() == JsonValue.ValueType.STRING) {
                type = JsonString.class.cast(formatValue).getString();
            }
            switch (type) {
                case "array":
                    throw new IllegalStateException("Array of array unsupported");
                case "object":
                    final String className = configuration.getClassName() + Character.toUpperCase(javaName.charAt(0)) + javaName.substring(1);
                    nested.putAll(newSubPojoGenerator(new PojoConfiguration()
                            .setPackageName(configuration.getPackageName())
                            .setClassName(className)
                            .setAddJsonbProperty(configuration.isAddJsonbProperty())
                            .setAddAllArgsConstructor(configuration.isAddAllArgsConstructor())
                            .setOnRef(configuration.getOnRef()), schema)
                            .visitSchema(schema)
                            .generate());
                    return className;
                case "null":
                    imports.add(JsonValue.class.getName());
                    return JsonValue.class.getSimpleName();
                case "boolean":
                    return "Boolean";
                case "uuid":
                case "hostname":
                case "idn-hostname":
                case "email":
                case "idn-email":
                case "ipv4":
                case "ipv6":
                case "uri":
                case "uri-reference":
                case "iri":
                case "iri-reference":
                case "uri-template":
                case "json-pointer":
                case "relative-json-pointer":
                case "regex":
                case "string":
                case "binary":
                case "password":
                    return "String";
                case "number":
                case "double":
                    return "Double";
                case "int":
                case "int32":
                case "integer":
                    return "Integer";
                case "int64":
                case "long":
                    return "Long";
                case "float":
                    return "Float";
                case "date":
                    imports.add(LocalDate.class.getName());
                    return LocalDate.class.getSimpleName();
                case "dateTime":
                    imports.add(OffsetDateTime.class.getName());
                    return OffsetDateTime.class.getSimpleName();
                case "duration":
                    imports.add(Duration.class.getName());
                    return Duration.class.getSimpleName();
                case "time":
                    imports.add(LocalTime.class.getName());
                    return LocalTime.class.getSimpleName();
                case "byte":
                    return "byte[]";
                default:
                    throw new IllegalArgumentException("Unsupported type: " + type);
            }
        }

        imports.add(JsonValue.class.getName());
        return JsonValue.class.getSimpleName();
    }

    protected String toJavaName(final String key) {
        String name = key.chars()
                .mapToObj(i -> Character.toString(!Character.isJavaIdentifierPart(i) ? '_' : (char) i))
                .collect(joining());
        if (Character.isDigit(name.charAt(0))) {
            name = "a" + name;
        }
        while (!name.isEmpty() && (!Character.isJavaIdentifierStart(name.charAt(0)) || name.charAt(0) == '_' || name.charAt(0) == '$')) {
            name = name.substring(1);
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Can't find a name for '" + key + "'");
        }

        if (isReserved(name)) {
            name += "Value";
        }

        if (name.contains("_")) {
            name = toCamelCase(name);
        }

        if (!Objects.equals(key, name) && configuration.isAddJsonbProperty()) {
            imports.add(JsonbProperty.class.getName());
        }
        return name;
    }

    protected String toCamelCase(final String name) {
        final StringBuilder out = new StringBuilder(name.length());
        boolean up = false;
        for (final char c : name.toCharArray()) {
            if (up) {
                out.append(Character.toUpperCase(c));
                up = false;
            } else if (c == '_') {
                up = true;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    protected boolean isReserved(final String name) {
        return "continue".equals(name) || "break".equals(name) || "default".equals(name) ||
                "do".equals(name) || "while".equals(name) ||
                "for".equals(name) ||
                "if".equals(name) || "else".equals(name) ||
                "enum".equals(name) ||
                "int".equals(name) ||
                "long".equals(name) ||
                "float".equals(name) ||
                "double".equals(name) ||
                "boolean".equals(name) ||
                "byte".equals(name) ||
                "char".equals(name) ||
                "short".equals(name) ||
                "String".equals(name);
    }

    private static <T> T getValueAs(final JsonObject schema, final String attribute, final Class<T> type) {
        final JsonValue value = schema.get(attribute);
        if (value == null) {
            throw new IllegalArgumentException("No \"" + attribute + "\" value in " + schema);
        }
        return valueAs(schema, type, value);
    }

    private static <T> T valueAs(final JsonObject schema, final Class<T> type, final JsonValue value) {
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("\"items\" not an object: " + schema);
        }
        return type.cast(value);
    }

    public static class PojoConfiguration {
        private String packageName = "org.apache.johnzon.generated.pojo";
        private String className;
        private boolean addJsonbProperty = true;
        private boolean addAllArgsConstructor = true;
        private boolean fluentSetters = false;
        private Function<Ref, String> onRef;

        public Function<Ref, String> getOnRef() {
            return onRef;
        }

        public PojoConfiguration setOnRef(final Function<Ref, String> onRef) {
            this.onRef = onRef;
            return this;
        }

        public boolean isFluentSetters() {
            return fluentSetters;
        }

        public PojoConfiguration setFluentSetters(final boolean fluentSetters) {
            this.fluentSetters = fluentSetters;
            return this;
        }

        public boolean isAddAllArgsConstructor() {
            return addAllArgsConstructor;
        }

        public PojoConfiguration setAddAllArgsConstructor(final boolean addAllArgsConstructor) {
            this.addAllArgsConstructor = addAllArgsConstructor;
            return this;
        }

        public boolean isAddJsonbProperty() {
            return addJsonbProperty;
        }

        public PojoConfiguration setAddJsonbProperty(final boolean addJsonbProperty) {
            this.addJsonbProperty = addJsonbProperty;
            return this;
        }

        public String getClassName() {
            return className;
        }

        public PojoConfiguration setClassName(final String className) {
            this.className = className;
            return this;
        }

        public String getPackageName() {
            return packageName;
        }

        public PojoConfiguration setPackageName(final String packageName) {
            this.packageName = packageName;
            return this;
        }
    }

    protected static class Attribute {
        protected final String javaName;
        protected final String jsonName;
        protected final String type;

        protected Attribute(final String javaName, final String jsonName, final String type) {
            this.javaName = javaName;
            this.jsonName = jsonName;
            this.type = type;
        }

        public String getJavaName() {
            return javaName;
        }

        public String getJsonName() {
            return jsonName;
        }

        public String getType() {
            return type;
        }
    }

    public static class Ref {
        private final String ref;
        private final String enclosingClass;
        private final Set<String> imports;
        private final List<Attribute> attributes;
        private final Map<String, String> nested;

        private Ref(final String ref, final String enclosingClass, final Set<String> imports,
                    final List<Attribute> attributes, final Map<String, String> nested) {
            this.ref = ref;
            this.enclosingClass = enclosingClass;
            this.imports = imports;
            this.attributes = attributes;
            this.nested = nested;
        }

        public String getEnclosingClass() {
            return enclosingClass;
        }

        public String getRef() {
            return ref;
        }

        public Set<String> getImports() {
            return imports;
        }

        public List<Attribute> getAttributes() {
            return attributes;
        }

        public Map<String, String> getNested() {
            return nested;
        }
    }
}
