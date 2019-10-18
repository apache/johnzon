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
package org.apache.johnzon.jsonb.api.experimental;

import java.util.function.Function;
import java.util.function.Predicate;

public class PolymorphicConfig {
    private Function<String, Class<?>> typeLoader = a -> {
        throw new IllegalArgumentException("Unknown alias: '" + a + "'");
    };
    private Function<Class<?>, String> discriminatorMapper = type -> {
        throw new IllegalArgumentException("Unknown class '" + type.getName() + "'");
    };
    private Predicate<Class<?>> serializationPredicate = c -> false;
    private Predicate<Class<?>> deserializationPredicate = c -> false;
    private String discriminator = "@type";

    public PolymorphicConfig withDeserializationPredicate(final Predicate<Class<?>> deserializationPredicate) {
        this.deserializationPredicate = deserializationPredicate;
        return this;
    }

    public PolymorphicConfig withSerializationPredicate(final Predicate<Class<?>> serializationPredicate) {
        this.serializationPredicate = serializationPredicate;
        return this;
    }

    public PolymorphicConfig withDiscriminatorMapper(final Function<Class<?>, String> discriminatorMapper) {
        this.discriminatorMapper = discriminatorMapper;
        return this;
    }

    // note this prevents @JsonbCreator usage but otherwise the user will do its own mapping with a deserializer
    public PolymorphicConfig withTypeLoader(final Function<String, Class<?>> typeLoader) {
        this.typeLoader = typeLoader;
        return this;
    }

    public PolymorphicConfig withDiscriminator(final String value) {
        this.discriminator = value;
        return this;
    }

    public Predicate<Class<?>> getDeserializationPredicate() {
        return deserializationPredicate;
    }

    public Function<String, Class<?>> getTypeLoader() {
        return typeLoader;
    }

    public Function<Class<?>, String> getDiscriminatorMapper() {
        return discriminatorMapper;
    }

    public Predicate<Class<?>> getSerializationPredicate() {
        return serializationPredicate;
    }

    public String getDiscriminator() {
        return discriminator;
    }
}
