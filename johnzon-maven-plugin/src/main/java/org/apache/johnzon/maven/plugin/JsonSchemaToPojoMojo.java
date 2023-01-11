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

import org.apache.johnzon.jsonschema.generator.PojoGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

/**
 * Generates mojo bindings from json schema specification(s).
 */
@Mojo(name = "jsonschema2pojo", defaultPhase = GENERATE_SOURCES)
public class JsonSchemaToPojoMojo extends AbstractMojo {
    /**
     * Generation configuration.
     * Note that if source is a directory, class name is ignored and auto set from schema name.
     */
    @Parameter
    private PojoGenerator.PojoConfiguration generator;

    /**
     * Extensions to consider if source is a directory.
     */
    @Parameter(property = "johnzon.jsonschema.extensions", defaultValue = ".jsonschema.json")
    private List<String> jsonSchemaExtensions;

    /**
     * Source jsonschema or directory containing json schemas.
     */
    @Parameter(property = "johnzon.source", defaultValue = "${project.basedir}/src/main/johnzon/jsonschema")
    private File source;

    /**
     * Where to dump generated classes.
     */
    @Parameter(property = "johnzon.target", defaultValue = "${project.build.directory}/generated-sources/johnzon-pojo")
    private File target;

    @Override
    public void execute() {
        final JsonReaderFactory readerFactory = Json.createReaderFactory(emptyMap());
        if (source.isDirectory()) {
            try {
                Files.walkFileTree(source.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        final String name = file.getFileName().toString();
                        final List<String> matchingExt = jsonSchemaExtensions.stream()
                                .filter(name::endsWith)
                                .sorted(comparing(String::length).reversed())
                                .collect(toList());
                        if (matchingExt.size() >= 1) {
                            final PojoGenerator.PojoConfiguration conf = generator == null ? new PojoGenerator.PojoConfiguration() : generator;
                            conf.setClassName(name.substring(0, name.length() - matchingExt.get(0).length()));
                            dump(new PojoGenerator(conf)
                                    .visitSchema(read(readerFactory, source.toPath()))
                                    .generate());
                        }
                        return super.visitFile(file, attrs);
                    }
                });
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            dump(new PojoGenerator(generator == null ? new PojoGenerator.PojoConfiguration() : generator)
                    .visitSchema(read(readerFactory, source.toPath()))
                    .generate());
        }
    }

    private JsonObject read(final JsonReaderFactory readerFactory, final Path path) {
        try (final JsonReader reader = readerFactory.createReader(Files.newBufferedReader(path))) {
            return reader.readObject();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void dump(final Map<String, String> generate) {
        final Path root = target.toPath();
        try {
            for (final Map.Entry<String, String> entry : generate.entrySet()) {
                final Path out = root.resolve(entry.getKey());
                Files.createDirectories(out.getParent());
                Files.write(out, entry.getValue().getBytes(StandardCharsets.UTF_8));
            }
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }
}
