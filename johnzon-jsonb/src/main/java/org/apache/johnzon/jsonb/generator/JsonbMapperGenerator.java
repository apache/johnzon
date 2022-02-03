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

import org.apache.johnzon.jsonb.JohnzonBuilder;
import org.apache.johnzon.jsonb.JohnzonJsonb;
import org.apache.johnzon.mapper.Mappings;
import org.apache.johnzon.mapper.access.AccessMode;
import org.apache.johnzon.mapper.access.FieldAndMethodAccessMode;
import org.apache.johnzon.mapper.access.MethodAccessMode;

import javax.json.bind.JsonbConfig;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.joining;

public class JsonbMapperGenerator implements Runnable {
    private final Configuration configuration;

    public JsonbMapperGenerator(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void run() {
        requireNonNull(configuration.output, "no output set");
        requireNonNull(configuration.classes, "no classes set");
        try (final JohnzonJsonb jsonb = JohnzonJsonb.class.cast(new JohnzonBuilder()
                .withConfig(configuration.config == null ? new JsonbConfig() : configuration.config)
                .build())) {
            final Mappings mappings = jsonb.getDelegate().getMappings();
            configuration.classes.forEach(clazz -> {
                final Mappings.ClassMapping mapping = mappings.findOrCreateClassMapping(clazz);

                final String suffix = "$$JohnzonJsonb"; // todo: make it configurable?
                final Path target = configuration.output.resolve(clazz.getName().replace('.', '/') + suffix + ".class");
                info(() -> "Generating JSON-B for '" + clazz.getName() + "' to '" + target + "'");

                final StringBuilder out = new StringBuilder();
                if (configuration.header != null) {
                    out.append(configuration.header);
                }
                if (clazz.getPackage() != null) {
                    out.append("package ").append(clazz.getPackage().getName()).append(";\n\n");
                }

                out.append("import org.apache.johnzon.jsonb.generator.GeneratedJohnzonJsonb;\n");
                out.append("import org.apache.johnzon.jsonb.JohnzonJsonb;\n");
                out.append("import javax.json.JsonGenerator;\n");
                out.append("import javax.json.JsonReader;\n");
                out.append("import javax.json.JsonValue;\n");
                out.append("\n");
                out.append("public class ").append(clazz.getSimpleName()).append(suffix).append(" implements GeneratedJohnzonJsonb {\n");
                out.append("    public ").append(clazz.getSimpleName()).append(suffix).append("(final JohnzonJsonb root) {\n");
                out.append("        super(root);\n");
                out.append("    }\n");
                out.append("\n");
                out.append("    @Override\n");
                out.append("    public <T> T fromJson(final Reader reader) {\n");
                if (mapping.setters.isEmpty()) { // will always be empty
                    out.append("        return JsonValue.EMPTY_JSON_OBJECT;\n");
                } else {
                    // todo: use mappings.getters and expose with getters jsonb.getMapper().getJsonReaderFactory()
                    out.append("        try (final JsonReader reader = root.getMapper().getReaderFactory().createReader(reader)) {\n");
                    out.append("            final JsonValue value = reader.readValue();\n");
                    out.append("            switch (value.getValueType()) {\n");
                    out.append("                case OBJECT: {\n");
                    out.append("                    final ").append(clazz.getSimpleName()).append(suffix).append(" instance = new ")
                            .append(clazz.getSimpleName()).append(suffix).append("();\n");
                    out.append(mapping.setters.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(setter -> toSetter(setter.getValue(), setter.getKey()))
                            .collect(joining("\n", "", "\n")));
                    out.append("                    return instance;\n");
                    out.append("                }\n");
                    out.append("                case NULL:\n");
                    out.append("                    return null;\n");
                    out.append("                default:\n");
                    out.append("                    throw new IllegalStateException(\"invalid value type: '\" + value.getValueType() + \"'\");\n");
                    out.append("            }\n");
                    out.append("        }\n");
                }
                out.append("    }\n");
                out.append("\n");
                out.append("    @Override\n");
                out.append("    public void toJson(final Object object, final Writer writer) {\n");
                if (mapping.getters.isEmpty()) { // will always be empty
                    out.append("        writer.write(\"{}\");\n");
                } else {
                    out.append("        try (final JsonGenerator generator = root.getDelegate().getGeneratorFactory().createGenerator(writer)) {\n");
                    out.append(mapping.getters.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(setter -> toGetter(setter.getValue(), setter.getKey()))
                            .collect(joining("\n", "", "\n")));
                    out.append("        }\n");
                }
                // root.getDelegate().getGeneratorFactory().createGenerator()
                out.append("    }\n");
                out.append("}\n\n");

                try {
                    Files.createDirectories(target.getParent());
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }

                String content = out.toString();
                boolean preferJakarta;
                if (configuration.preferJakarta != null) {
                    preferJakarta = configuration.preferJakarta;
                } else {
                    try {
                        Thread.currentThread().getContextClassLoader().loadClass("jakarta.json.spi.JsonProvider");
                        preferJakarta = true;
                    } catch (final NoClassDefFoundError | ClassNotFoundException e) {
                        preferJakarta = false;
                    }
                }
                if (preferJakarta) {
                    content = content.replace(" javax.json.", " jakarta.json.");
                }
                try (final Writer writer = Files.newBufferedWriter(target, UTF_8)) {
                    writer.append(content);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String toGetter(final Mappings.Getter value, final String name) {
        try {
            final Field reader = value.getClass().getDeclaredField("reader");
            if (!reader.isAccessible()) {
                reader.setAccessible(true);
            }
            final Object wrapped = reader.get(value);
            final Field finalReader = Stream.of(wrapped.getClass().getDeclaredFields())
                    .filter(it -> it.getName().contains("finalReader") && AccessMode.Reader.class == it.getType())
                    .peek(it -> {
                        if (!it.isAccessible()) {
                            it.setAccessible(true);
                        }
                    })
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No finalReader field in " + wrapped));
            return toGetter(AccessMode.Reader.class.cast(finalReader.get(wrapped)), name);
        } catch (final IllegalAccessException | NoSuchFieldException nsfe) {
            throw new IllegalArgumentException("Unsupported getter: " + value, nsfe);
        }
    }

    private String toGetter(final MethodAccessMode.MethodReader reader, final String name) {
        final Type type = reader.getType();
        if (type == String.class || type == int.class || type == long.class || type == boolean.class || type == double.class
                || type == BigDecimal.class || type == BigInteger.class) {
            return "" +
                    "            {\n" +
                    "                final " + Class.class.cast(type).getSimpleName() + " value = instance." + reader.getMethod().getName() + "();\n" +
                    "                if (value != null) {\n" +
                    "                    generator.write(\"" + name + "\", value);\n" +
                    "                }\n" +
                    "            }" +
                    "";
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private String toGetter(final AccessMode.Reader reader, final String name) {
        if (FieldAndMethodAccessMode.CompositeReader.class.isInstance(reader)) {
            final MethodAccessMode.MethodReader mr = MethodAccessMode.MethodReader.class.cast(
                    FieldAndMethodAccessMode.CompositeReader.class.cast(reader).getType1());
            return toGetter(mr, name);
        } else if (MethodAccessMode.MethodReader.class.isInstance(reader)) {
            return toGetter(MethodAccessMode.MethodReader.class.cast(reader), name);
        }
        throw new IllegalArgumentException("Unsupported reader: " + reader);
    }


    private String toSetter(final MethodAccessMode.MethodWriter reader, final String name) {
        return "" +
                "                    {\n" +
                "                        final JsonValue value = instance.get(\"" + name + "\");\n" +
                "                        if (value != null) {\n" +
                "                            instance." + reader.getMethod().getName() + "(" +
                coerceFunction(reader.getMethod().getGenericParameterTypes()[0]) + "(value));\n" +
                "                        }\n" +
                "                    }" +
                "";
    }

    private String coerceFunction(final Type type) {
        if (type == String.class) {
            return "json2String";
        }
        if (type == int.class) {
            return "json2Int";
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private String toSetter(final AccessMode.Writer writer, final String setter) {
        if (FieldAndMethodAccessMode.CompositeWriter.class.isInstance(writer)) {
            final MethodAccessMode.MethodWriter mr = MethodAccessMode.MethodWriter.class.cast(
                    FieldAndMethodAccessMode.CompositeWriter.class.cast(writer).getType1());
            return toSetter(mr, setter);
        } else if (MethodAccessMode.MethodWriter.class.isInstance(writer)) {
            return toSetter(MethodAccessMode.MethodWriter.class.cast(writer), setter);
        }
        throw new IllegalArgumentException("Unsupported writer: " + writer);
    }

    private String toSetter(final Mappings.Setter value, final String name) {
        try {
            final Field writer = value.getClass().getDeclaredField("writer");
            if (!writer.isAccessible()) {
                writer.setAccessible(true);
            }
            final Object wrapped = writer.get(value);
            final Field finalWriter = Stream.of(wrapped.getClass().getDeclaredFields())
                    .filter(it -> it.getName().contains("initialWriter") && AccessMode.Writer.class == it.getType())
                    .peek(it -> {
                        if (!it.isAccessible()) {
                            it.setAccessible(true);
                        }
                    })
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No initialWriter field in " + wrapped));
            return toSetter(AccessMode.Writer.class.cast(finalWriter.get(wrapped)), name);
        } catch (final IllegalAccessException | NoSuchFieldException nsfe) {
            throw new IllegalArgumentException("Unsupported getter: " + value, nsfe);
        }
    }

    protected void info(final Supplier<String> message) {
        logger().info(message);
    }

    protected void error(final Supplier<String> message, final Throwable throwable) {
        logger().log(SEVERE, throwable, message);
    }

    private Logger logger() {
        return Logger.getLogger(getClass().getName());
    }

    public static class Configuration {
        private Boolean preferJakarta;
        private String header;
        private Collection<Class<?>> classes;
        private Path output;
        private JsonbConfig config;

        public Configuration setUseJakarta(final Boolean preferJakarta) {
            this.preferJakarta = preferJakarta;
            return this;
        }

        public Configuration setHeader(final String header) {
            this.header = header;
            return this;
        }

        public Configuration setConfig(final JsonbConfig config) {
            this.config = config;
            return this;
        }

        public Configuration setClasses(final Collection<Class<?>> classes) {
            this.classes = classes;
            return this;
        }

        public Configuration setOutput(final Path output) {
            this.output = output;
            return this;
        }
    }
}
