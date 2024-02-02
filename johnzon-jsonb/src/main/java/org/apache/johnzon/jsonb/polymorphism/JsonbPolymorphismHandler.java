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
package org.apache.johnzon.jsonb.polymorphism;

import org.apache.johnzon.mapper.access.Meta;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbSubtype;
import jakarta.json.bind.annotation.JsonbTypeInfo;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonbPolymorphismHandler {
    private final Map<Class<?>, JsonbPolymorphismTypeInfo> typeInfoCache = new HashMap<>();

    public boolean hasPolymorphism(Class<?> clazz) {
        return getOrCreatePolymorphismTypeInfo(clazz) != null;
    }

    public Map.Entry<String, String>[] getPolymorphismPropertiesToSerialize(Class<?> clazz, Collection<String> otherProperties) {
        List<Map.Entry<String, String>> result = new ArrayList<>();

        JsonbPolymorphismTypeInfo polymorphismTypeInfo = getOrCreatePolymorphismTypeInfo(clazz);
        while (polymorphismTypeInfo != null) {
            if (polymorphismTypeInfo.hasSubtypeInformation()) {
                if (otherProperties.contains(polymorphismTypeInfo.getTypeKey())) {
                    throw new JsonbException("JsonbTypeInfo key '" + polymorphismTypeInfo.getTypeKey() + "' collides with other properties in json");
                }

                String bestMatchingAlias = null;
                for (Map.Entry<String, Class<?>> aliasToType : polymorphismTypeInfo.getAliases().entrySet()) {
                    String alias = aliasToType.getKey();
                    Class<?> type = aliasToType.getValue();

                    if (type.isAssignableFrom(clazz)) {
                        bestMatchingAlias = alias;

                        if (clazz == type) { // Exact match found, no need to continue further
                            break;
                        }
                    }
                }

                if (bestMatchingAlias != null) {
                    result.add(0, Map.entry(polymorphismTypeInfo.getTypeKey(), bestMatchingAlias));
                }
            }

            polymorphismTypeInfo = polymorphismTypeInfo.getFirstParent();
        }

        return result.toArray(Map.Entry[]::new);
    }

    public Class<?> getTypeToDeserialize(JsonObject jsonObject, Class<?> clazz) {
        JsonbPolymorphismTypeInfo typeInfo = getOrCreatePolymorphismTypeInfo(clazz);
        if (typeInfo == null) {
            return clazz;
        }

        JsonValue typeValue = jsonObject.get(typeInfo.getTypeKey());
        if (typeValue == null) {
            return clazz;
        }

        if (typeValue.getValueType() != JsonValue.ValueType.STRING) {
            throw new JsonbException("Property '" + typeInfo.getTypeKey() + "' isn't a String, resolving JsonbSubtype is impossible");
        }

        String typeValueString = ((JsonString) typeValue).getString();
        final Class<?> result = typeInfo.getAliases().get(typeValueString);
        if (result == null) {
            throw new JsonbException("No JsonbSubtype found for alias '" + typeValueString + "' on " + clazz.getName());
        }

        return result;
    }

    /**
     * Looks up a {@link JsonbPolymorphismTypeInfo} from the cache or creates it for the given <code>clazz</code> if it supports polymorphism.
     * This is the case if either one of these conditions is truthy:
     * <ul>
     *     <li><code>clazz</code> has an {@link JsonbTypeInfo} annotation</li>
     *     <li>any class in the type hierarchy of <code>clazz</code> has an {@link JsonbTypeInfo} annotation</li>
     * </ul>
     * @param clazz Class to inspect
     * @return {@link JsonbPolymorphismTypeInfo} if the class supports polymorphism, <code>null</code> otherwise
     */
    public JsonbPolymorphismTypeInfo getOrCreatePolymorphismTypeInfo(Class<?> clazz) {
        if (typeInfoCache.containsKey(clazz)) {
            return typeInfoCache.get(clazz);
        }

        JsonbPolymorphismTypeInfo result = null;
        JsonbTypeInfo directAnnotation = Meta.getAnnotation((AnnotatedElement) clazz, JsonbTypeInfo.class);
        if (directAnnotation != null) {
            result = new JsonbPolymorphismTypeInfo(clazz, directAnnotation);
        }

        List<JsonbPolymorphismTypeInfo> parents = new ArrayList<>();
        List<Class<?>> candidates = List.of(clazz);
        while (!candidates.isEmpty()) {
            // Parents have been found on previous level -> don't walk inheritance tree further to avoid processing the same classes twice
            if (!parents.isEmpty()) {
                break;
            }

            List<Class<?>> candidatesNextLevel = new ArrayList<>();
            for (Class<?> current : candidates) {
                if (current.getSuperclass() != null) {
                    candidatesNextLevel.add(current.getSuperclass());

                    if (Meta.getAnnotation((AnnotatedElement) current.getSuperclass(), JsonbTypeInfo.class) != null) {
                        parents.add(getOrCreatePolymorphismTypeInfo(current.getSuperclass()));
                    }
                }

                for (Class<?> iface : current.getInterfaces()) {
                    candidatesNextLevel.add(iface);

                    if (Meta.getAnnotation((AnnotatedElement) iface, JsonbTypeInfo.class) != null) {
                        parents.add(getOrCreatePolymorphismTypeInfo(iface));
                    }
                }
            }

            candidates = candidatesNextLevel;
        }

        if (!parents.isEmpty()) {
            if (result == null) {
                result = new JsonbPolymorphismTypeInfo(clazz, null);
            }

            result.getParents().addAll(parents);
        }

        typeInfoCache.put(clazz, result);
        return result;
    }

    /**
     * Validates {@link JsonbTypeInfo} annotation on clazz and its parents (superclass/interfaces),
     * see {@link JsonbPolymorphismHandler#validateSubtypeCompatibility(Class)}, {@link JsonbPolymorphismHandler#validateOnlyOneParentWithTypeInfo(Class)}
     * and {@link JsonbPolymorphismHandler#validateNoTypeInfoKeyCollision(Class)}
     *
     * @param classToValidate Class to validate
     * @throws JsonbException validation failed
     */
    public void validateJsonbPolymorphismAnnotations(Class<?> classToValidate) {
        validateSubtypeCompatibility(classToValidate);
        validateOnlyOneParentWithTypeInfo(classToValidate);
        validateNoTypeInfoKeyCollision(classToValidate);
    }

    /**
     * Validation fails if any clazz and {@link JsonbSubtype#type()} aren't compatible.
     *
     * @param classToValidate Class to validate
     * @throws JsonbException validation failed
     */
    protected void validateSubtypeCompatibility(Class<?> classToValidate) {
        JsonbPolymorphismTypeInfo polymorphismTypeInfo = getOrCreatePolymorphismTypeInfo(classToValidate);
        if (polymorphismTypeInfo == null || !polymorphismTypeInfo.hasSubtypeInformation()) {
            return;
        }

        for (Map.Entry<String, Class<?>> aliasToType : polymorphismTypeInfo.getAliases().entrySet()) {
            String alias = aliasToType.getKey();
            Class<?> type = aliasToType.getValue();

            if (!classToValidate.isAssignableFrom(type)) {
                throw new JsonbException("JsonbSubtype '" + alias + "'" +
                        " (" + type.getName() + ") is not a subclass of " + classToValidate);
            }
        }
    }

    /**
     * Validates that only one parent class (superclass + interfaces) has {@link JsonbTypeInfo} annotation
     *
     * @param classToValidate class to validate
     * @throws JsonbException validation failed
     */
    protected void validateOnlyOneParentWithTypeInfo(Class<?> classToValidate) {
        JsonbPolymorphismTypeInfo polymorphismTypeInfo = getOrCreatePolymorphismTypeInfo(classToValidate);
        if (polymorphismTypeInfo != null && polymorphismTypeInfo.getParents().size() > 1) {
            throw new JsonbException("More than one interface/superclass of " + classToValidate.getName() +
                    " has JsonbTypeInfo Annotation");
        }
    }

    /**
     * Validates that {@link JsonbTypeInfo#key()} is only defined once in type hierarchy.
     * Assumes {@link JsonbPolymorphismHandler#validateOnlyOneParentWithTypeInfo(Class)} already passed.
     *
     * @param classToValidate class to validate
     * @throws JsonbException validation failed
     */
    protected void validateNoTypeInfoKeyCollision(Class<?> classToValidate) {
        Map<String, Class<?>> keyToDefiningClass = new HashMap<>();
        JsonbPolymorphismTypeInfo current = getOrCreatePolymorphismTypeInfo(classToValidate);
        while (current != null) {
            final Class<?> existing = keyToDefiningClass.put(current.getTypeKey(), current.getClazz());
            if (existing != null) {
                throw new JsonbException("JsonbTypeInfo key '" + current.getTypeKey() + "' found more than once in type hierarchy of " + classToValidate
                        + " (first defined in " + existing.getName() + ", then defined again in " + current.getClazz().getName() + ")");
            }

            current = current.getFirstParent();
        }
    }
}
