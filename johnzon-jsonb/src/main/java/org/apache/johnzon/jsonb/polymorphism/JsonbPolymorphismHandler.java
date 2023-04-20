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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonbPolymorphismHandler {
    private final Map<Class<?>, JsonbPolymorphismTypeInfo> typeInfoCache = new HashMap<>();

    public boolean hasPolymorphism(Class<?> clazz) {
        return clazz.isAnnotationPresent(JsonbTypeInfo.class) || getParentWithTypeInfo(clazz) != null;
    }

    public Map.Entry<String, String>[] getPolymorphismPropertiesToSerialize(Class<?> clazz, Collection<String> otherProperties) {
        List<Map.Entry<String, String>> result = new ArrayList<>();

        Class<?> current = clazz;
        while (current != null) {
            // Only try to resolve types when there's a JsonbTypeInfo Annotation present on the current type, Meta.getAnnotation tries to
            // walk up parents by itself until it finds the given Annotation and could incorrectly cause JsonbExceptions to be thrown
            // (multiple JsonbTypeInfos with same key found even if thats not actually the case)
            if (current.isAnnotationPresent(JsonbTypeInfo.class)) {
                JsonbTypeInfo typeInfo = Meta.getAnnotation(current, JsonbTypeInfo.class);
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
        if (!typeInfoCache.containsKey(clazz)) {
            return clazz;
        }

        JsonbPolymorphismTypeInfo typeInfo = typeInfoCache.get(clazz);
        if (!jsonObject.containsKey(typeInfo.getTypeKey())) {
            return clazz;
        }

        JsonValue typeValue = jsonObject.get(typeInfo.getTypeKey());
        if (typeValue.getValueType() != JsonValue.ValueType.STRING) {
            throw new JsonbException("Property '" + typeInfo.getTypeKey() + "' isn't a String, resolving JsonbSubtype is impossible");
        }

        String typeValueString = ((JsonString) typeValue).getString();
        if (!typeInfo.getAliases().containsKey(typeValueString)) {
            throw new JsonbException("No JsonbSubtype found for alias '" + typeValueString + "' on " + clazz.getName());
        }

        return typeInfo.getAliases().get(typeValueString);
    }

    public void populateTypeInfoCache(Class<?> clazz) {
        if (typeInfoCache.containsKey(clazz) || !clazz.isAnnotationPresent(JsonbTypeInfo.class)) {
            return;
        }

        typeInfoCache.put(clazz, new JsonbPolymorphismTypeInfo(Meta.getAnnotation(clazz, JsonbTypeInfo.class)));
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
        if (!classToValidate.isAnnotationPresent(JsonbTypeInfo.class)) {
            return;
        }

        JsonbTypeInfo typeInfo = Meta.getAnnotation(classToValidate, JsonbTypeInfo.class);
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
        boolean found = classToValidate.getSuperclass() != null && Meta.getAnnotation(classToValidate.getSuperclass(), JsonbTypeInfo.class) != null;

        for (Class<?> iface : classToValidate.getInterfaces()) {
            if (iface != null && Meta.getAnnotation(iface, JsonbTypeInfo.class) != null) {
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
            if (current.isAnnotationPresent(JsonbTypeInfo.class)) {
                String key = Meta.getAnnotation(current, JsonbTypeInfo.class).key();

                if (keyToDefiningClass.containsKey(key)) {
                    throw new JsonbException("JsonbTypeInfo key '" + key + "' found more than once in type hierarchy of " + classToValidate
                    + " (first defined in " + keyToDefiningClass.get(key).getName() + ", then defined again in " + current.getName() + ")");
                }

                keyToDefiningClass.put(key, current);
            }

            current = getParentWithTypeInfo(current);
        }
    }

    protected Class<?> getParentWithTypeInfo(Class<?> clazz) {
        if (clazz.getSuperclass() != null && Meta.getAnnotation(clazz.getSuperclass(), JsonbTypeInfo.class) != null) {
            return clazz.getSuperclass();
        }

        for (Class<?> iface : clazz.getInterfaces()) {
            if (Meta.getAnnotation(iface, JsonbTypeInfo.class) != null) {
                return iface;
            }
        }

        return null;
    }
}
