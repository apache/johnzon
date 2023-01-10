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

import org.apache.johnzon.jsonschema.generator.Schema;
import org.apache.johnzon.jsonschema.generator.SchemaProcessor;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.config.PropertyOrderStrategy;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Stream;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;
import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME_PLUS_SYSTEM;

@Mojo(name = "jsonschema", defaultPhase = PROCESS_CLASSES, requiresDependencyResolution = RUNTIME_PLUS_SYSTEM)
public class PojoToJsonSchemaMojo extends AbstractMojo {
    @Parameter(property = "johnzon.jsonschema.schemaClass")
    protected String schemaClass;

    @Parameter(property = "johnzon.jsonschema.target", defaultValue = "${project.build.outputDirectory}/jsonschema/schema.json")
    protected File target;

    @Parameter(property = "johnzon.jsonschema.classesDir", defaultValue = "${project.build.outputDirectory}")
    protected File classesDir;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Component
    protected MavenProjectHelper projectHelper;

    @Parameter(property = "johnzon.attach", defaultValue = "true")
    protected boolean attach;

    @Parameter(property = "johnzon.jsonschema.classifier", defaultValue = "jsonschema")
    protected String classifier;

    @Parameter(property = "johnzon.jsonschema.title")
    protected String title;

    @Parameter(property = "johnzon.jsonschema.description")
    protected String description;

    @Override
    public void execute() throws MojoExecutionException {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        try (final URLClassLoader loader = newLoader(old);
             final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                     .withFormatting(true)
                     .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            thread.setContextClassLoader(loader);
            final SchemaProcessor.InMemoryCache cache = new SchemaProcessor.InMemoryCache();
            final Schema schema = new SchemaProcessor()
                    .mapSchemaFromClass(loader.loadClass(schemaClass.trim()), cache);
            schema.setTitle(title);
            schema.setDescription(description);
            if (!cache.getDefinitions().isEmpty()) {
                schema.setDefinitions(cache.getDefinitions());
            }
            if (target.getParent() != null) {
                Files.createDirectories(target.toPath().getParent());
            }
            Files.write(target.toPath(), jsonb.toJson(schema).getBytes(StandardCharsets.UTF_8));
        } catch (final Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            thread.setContextClassLoader(old);
        }
        if (attach && project != null) {
            projectHelper.attachArtifact(project, "json", classifier, target);
        }
    }

    private URLClassLoader newLoader(final ClassLoader parent) {
        return new URLClassLoader(
                Stream.concat(project.getArtifacts().stream()
                                .map(Artifact::getFile)
                                .filter(Objects::nonNull),
                        Stream.of(classesDir))
                        .filter(File::exists)
                        .map(it -> {
                            try {
                                return it.toURI().toURL();
                            } catch (final MalformedURLException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .toArray(URL[]::new),
                parent);
    }
}
