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

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbSubtype;
import jakarta.json.bind.annotation.JsonbTypeInfo;
import org.apache.johnzon.mapper.polymorphism.PolymorphismHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JsonbPolymorphismHandler implements PolymorphismHandler {
    @Override
    public boolean hasPolymorphism(Class<?> clazz) {
        return clazz.isAnnotationPresent(JsonbTypeInfo.class) || !getParentClassesWithTypeInfo(clazz).isEmpty();
    }

    @Override
    public List<Map.Entry<String, String>> getPolymorphismPropertiesToSerialize(Class<?> clazz, Collection<String> otherProperties) {
        List<Map.Entry<String, String>> entries = new ArrayList<>();

        Class<?> current = clazz;
        while (current != null) {
            validateJsonbTypeInfo(current);
            validateOnlyOneParentWithTypeInfo(current);

            JsonbTypeInfo typeInfo = current.getAnnotation(JsonbTypeInfo.class);
            if (typeInfo != null) {
                if (otherProperties.contains(typeInfo.key())) {
                    throw new JsonbException("JsonbTypeInfo key '" + typeInfo.key() + "' collides with other properties in json");
                }

                if (entries.stream().anyMatch(entry -> Objects.equals(entry.getKey(), typeInfo.key()))) {
                    throw new JsonbException("JsonbTypeInfo key '" + typeInfo.key() + "' found more than once in type hierarchy of " + clazz.getName());
                }

                String bestMatchingAlias = null;
                for (JsonbSubtype subtype : typeInfo.value()) {
                    if (subtype.type().isAssignableFrom(clazz)) {
                        bestMatchingAlias = subtype.alias();

                        if (clazz == subtype.type()) { // Exact match found, no need to continue further
                            break;
                        }
                    }
                }

                if (bestMatchingAlias != null) {
                    entries.add(0, Map.entry(typeInfo.key(), bestMatchingAlias));
                }
            }

            List<Class<?>> parentClassesWithTypeInfo = getParentClassesWithTypeInfo(current);
            current = parentClassesWithTypeInfo.isEmpty() ? null : parentClassesWithTypeInfo.get(0);
        }

        return entries;
    }

    @Override
    public Class<?> getTypeToDeserialize(JsonObject jsonObject, Class<?> clazz) {
        validateJsonbTypeInfo(clazz);
        validateOnlyOneParentWithTypeInfo(clazz);

        JsonbTypeInfo typeInfo = clazz.getAnnotation(JsonbTypeInfo.class);
        if (typeInfo == null || !jsonObject.containsKey(typeInfo.key())) {
            return clazz;
        }

        JsonValue typeValue = jsonObject.get(typeInfo.key());
        if (!(typeValue instanceof JsonString)) {
            throw new JsonbException("Property '" + typeInfo.key() + "' isn't a String, resolving "
                    + "JsonbSubtype is impossible");
        }

        String typeValueString = ((JsonString) typeValue).getString();
        for (JsonbSubtype subtype : typeInfo.value()) {
            if (subtype.alias().equals(typeValueString)) {
                return subtype.type();
            }
        }

        throw new JsonbException("No JsonbSubtype found for alias '" + typeValueString + "' on " + clazz.getName());
    }

    /**
     * Validates that only one parent class (superclass + interfaces) has {@link JsonbTypeInfo} annotation
     *
     * @param classToValidate class to validate
     * @throws JsonbException validation failed
     */
    private void validateOnlyOneParentWithTypeInfo(Class<?> classToValidate) {
        if (getParentClassesWithTypeInfo(classToValidate).size() > 1) {
            throw new JsonbException("More than one interface/superclass of " + classToValidate.getName() +
                    " has JsonbTypeInfo Annotation");
        }
    }

    /**
     * Validates {@link JsonbTypeInfo} on clazz.
     * Validation fails either if any {@link JsonbSubtype#type()} is the same as clazz
     * or if any clazz and {@link JsonbSubtype#type()} aren't compatible.
     *
     * @param classToValidate Class to validate
     * @throws JsonbException validation failed
     */
    private void validateJsonbTypeInfo(Class<?> classToValidate) {
        if (!classToValidate.isAnnotationPresent(JsonbTypeInfo.class)) {
            return;
        }

        JsonbTypeInfo typeInfo = classToValidate.getAnnotation(JsonbTypeInfo.class);
        for (JsonbSubtype subtype : typeInfo.value()) {
            if (!classToValidate.isAssignableFrom(subtype.type())) {
                throw new JsonbException("JsonbSubtype '" + subtype.alias() + "'" +
                        " (" + subtype.type().getName() + ") is not a subclass of " + classToValidate);
            }
        }
    }

    /**
     * Collects all parent classes (superclass + interfaces) that have the JsonbTypeInfo annotation
     * @param clazz base class
     * @return List of classes with JsonbTypeInfo annotation
     */
    private List<Class<?>> getParentClassesWithTypeInfo(Class<?> clazz) {
        List<Class<?>> result = new ArrayList<>();

        if (clazz.getSuperclass() != null && clazz.getSuperclass().isAnnotationPresent(JsonbTypeInfo.class)) {
            result.add(clazz.getSuperclass());
        }

        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.isAnnotationPresent(JsonbTypeInfo.class)) {
                result.add(iface);
            }
        }

        return result;
    }
}
