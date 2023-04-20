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
        return Meta.getAnnotation((AnnotatedElement) clazz, JsonbTypeInfo.class) != null || getParentWithTypeInfo(clazz) != null;
    }

    public Map.Entry<String, String>[] getPolymorphismPropertiesToSerialize(Class<?> clazz, Collection<String> otherProperties) {
        List<Map.Entry<String, String>> result = new ArrayList<>();

        Class<?> current = clazz;
        while (current != null) {
            // Only try to resolve types when there's a JsonbTypeInfo Annotation present on the current type, Meta.getAnnotation tries to
            // walk up parents by itself until it finds the given Annotation and could incorrectly cause JsonbExceptions to be thrown
            // (multiple JsonbTypeInfos with same key found even if thats not actually the case)
            JsonbTypeInfo typeInfo = Meta.getAnnotation((AnnotatedElement) current, JsonbTypeInfo.class);
            if (typeInfo != null) {
                if (otherProperties.contains(typeInfo.key())) {
                    throw new JsonbException("JsonbTypeInfo key '" + typeInfo.key() + "' collides with other properties in json");
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
                    result.add(0, Map.entry(typeInfo.key(), bestMatchingAlias));
                }
            }

            current = getParentWithTypeInfo(current);
        }

        return result.toArray(Map.Entry[]::new);
    }

    public Class<?> getTypeToDeserialize(JsonObject jsonObject, Class<?> clazz) {
        JsonbPolymorphismTypeInfo typeInfo = typeInfoCache.get(clazz);
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

    public void populateTypeInfoCache(Class<?> clazz) {
        if (typeInfoCache.containsKey(clazz)) {
            return;
        }

        final JsonbTypeInfo annotation = Meta.getAnnotation((AnnotatedElement) clazz, JsonbTypeInfo.class);
        if (annotation != null) {
            typeInfoCache.put(clazz, new JsonbPolymorphismTypeInfo(annotation));
        }
    }

    /**
     * Validates {@link JsonbTypeInfo} annotation on clazz and its parents (superclass/interfaces),
     * see {@link JsonbPolymorphismHandler#validateSubtypeCompatibility(Class)}, {@link JsonbPolymorphismHandler#validateOnlyOneParentWithTypeInfo(Class)}
     * and {@link JsonbPolymorphismHandler#validateNoTypeInfoKeyCollision(Class)}
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
        JsonbTypeInfo typeInfo = Meta.getAnnotation((AnnotatedElement) classToValidate, JsonbTypeInfo.class);
        if (typeInfo == null) {
            return;
        }

        for (JsonbSubtype subtype : typeInfo.value()) {
            if (!classToValidate.isAssignableFrom(subtype.type())) {
                throw new JsonbException("JsonbSubtype '" + subtype.alias() + "'" +
                        " (" + subtype.type().getName() + ") is not a subclass of " + classToValidate);
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
        boolean found = classToValidate.getSuperclass() != null && Meta.getAnnotation((AnnotatedElement) classToValidate.getSuperclass(), JsonbTypeInfo.class) != null;

        for (Class<?> iface : classToValidate.getInterfaces()) {
            if (iface != null && Meta.getAnnotation((AnnotatedElement) iface, JsonbTypeInfo.class) != null) {
                if (found) {
                    throw new JsonbException("More than one interface/superclass of " + classToValidate.getName() +
                            " has JsonbTypeInfo Annotation");
                }

                found = true;
            }
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

        Class<?> current = classToValidate;
        while (current != null) {
            final JsonbTypeInfo annotation = Meta.getAnnotation((AnnotatedElement) current, JsonbTypeInfo.class);
            if (annotation != null) {
                String key = annotation.key();
                final Class<?> existing = keyToDefiningClass.put(key, current);
                if (existing != null) {
                    throw new JsonbException("JsonbTypeInfo key '" + key + "' found more than once in type hierarchy of " + classToValidate
                            + " (first defined in " + existing.getName() + ", then defined again in " + current.getName() + ")");
                }
            }

            current = getParentWithTypeInfo(current);
        }
    }

    protected Class<?> getParentWithTypeInfo(Class<?> clazz) {
        if (clazz.getSuperclass() != null && Meta.getAnnotation((AnnotatedElement) clazz.getSuperclass(), JsonbTypeInfo.class) != null) {
            return clazz.getSuperclass();
        }

        for (Class<?> iface : clazz.getInterfaces()) {
            if (Meta.getAnnotation((AnnotatedElement) iface, JsonbTypeInfo.class) != null) {
                return iface;
            }
        }

        return null;
    }
}
