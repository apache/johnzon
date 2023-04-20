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
package org.apache.johnzon.jsonb;

import org.apache.johnzon.jsonb.polymorphism.JsonbPolymorphismHandler;
import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.MapperConfig;
import org.apache.johnzon.mapper.Mappings;
import org.apache.johnzon.mapper.ObjectConverter;
import org.apache.johnzon.mapper.access.AccessMode;

import jakarta.json.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.BiFunction;

public class JsonbMappings extends Mappings {
    private final JsonbPolymorphismHandler polymorphismHandler;

    public JsonbMappings(MapperConfig config) {
        super(config);

        this.polymorphismHandler = new JsonbPolymorphismHandler();
    }

    @Override
    protected Mappings.ClassMapping createClassMapping(Class<?> inClazz, Map<Type, Type> resolvedTypes) {
        Mappings.ClassMapping original = super.createClassMapping(inClazz, resolvedTypes);
        if (!polymorphismHandler.hasPolymorphism(inClazz) || original.polymorphicDeserializedTypeResolver != null || original.serializedPolymorphicProperties != null) {
            return original;
        }

        polymorphismHandler.validateJsonbPolymorphismAnnotations(original.clazz);
        polymorphismHandler.populateTypeInfoCache(original.clazz);
        return new ClassMapping(
                original.clazz, original.factory, original.getters, original.setters,
                original.adapter, original.reader, original.writer, original.anyGetter,
                original.anySetter, original.anyField, original.mapAdder,
                polymorphismHandler.getPolymorphismPropertiesToSerialize(original.clazz, original.getters.keySet()),
                polymorphismHandler::getTypeToDeserialize);
    }

    public static class ClassMapping extends Mappings.ClassMapping {
        protected ClassMapping(final Class<?> clazz, final AccessMode.Factory factory,
                               final Map<String, Getter> getters, final Map<String, Setter> setters,
                               final Adapter<?, ?> adapter,
                               final ObjectConverter.Reader<?> reader, final ObjectConverter.Writer<?> writer,
                               final Getter anyGetter, final Method anySetter, final Field anyField,
                               final Method mapAdder,
                               final Map.Entry<String, String>[] serializedPolymorphicProperties,
                               final BiFunction<JsonObject, Class<?>, Class<?>> polymorphicDeserializedTypeResolver) {
            super(clazz, factory, getters, setters, adapter, reader, writer, anyGetter, anySetter, anyField, mapAdder,
                    serializedPolymorphicProperties, polymorphicDeserializedTypeResolver);
        }
    }
}
