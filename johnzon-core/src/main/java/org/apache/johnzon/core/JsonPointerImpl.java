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
package org.apache.johnzon.core;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonPointer;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class JsonPointerImpl implements JsonPointer {

    private final String jsonPointer;
    private final List<String> referenceTokens = new ArrayList<>();
    private final String lastReferenceToken;

    /**
     * Constructs and initializes a JsonPointer.
     *
     * @param jsonPointer the JSON Pointer string
     * @throws NullPointerException if {@code jsonPointer} is {@code null}
     * @throws JsonException        if {@code jsonPointer} is not a valid JSON Pointer
     */
    public JsonPointerImpl(String jsonPointer) {
        if (jsonPointer == null) {
            throw new NullPointerException("jsonPointer must not be null");
        }
        if (!jsonPointer.equals("") && !jsonPointer.startsWith("/")) {
            throw new JsonException("A non-empty JsonPointer string must begin with a '/'");
        }

        this.jsonPointer = jsonPointer;
        String[] encodedReferenceTokens = jsonPointer.split("/", -1);

        for (String encodedReferenceToken : encodedReferenceTokens) {
            referenceTokens.add(JsonPointerUtil.decode(encodedReferenceToken));
        }
        lastReferenceToken = referenceTokens.get(referenceTokens.size() - 1);
    }

    /**
     * Compares this {@code JsonPointer} with another object.
     *
     * @param obj the object to compare this {@code JsonPointer} against
     * @return true if the given object is a {@code JsonPointer} with the same
     * reference tokens as this one, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        JsonPointerImpl that = (JsonPointerImpl) obj;
        return jsonPointer.equals(that.jsonPointer);
    }

    /**
     * Returns the hash code value for this {@code JsonPointer} object.
     * The hash code of this object is defined by the hash codes of it's reference tokens.
     *
     * @return the hash code value for this {@code JsonPointer} object
     */
    @Override
    public int hashCode() {
        return jsonPointer.hashCode();
    }

    /**
     * Returns the value at the referenced location in the specified {@code target}
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @return the referenced value in the target.
     * @throws NullPointerException if {@code target} is null
     * @throws JsonException        if the referenced value does not exist
     */
    public JsonValue getValue(JsonStructure target) {
        if (target == null) {
            throw new NullPointerException("target must not be null");
        }
        if (isEmptyJsonPointer()) {
            return target;
        }

        JsonValue jsonValue = target;
        for (int i = 1; i < referenceTokens.size(); i++) {
            jsonValue = getValue(jsonValue, referenceTokens.get(i), i, referenceTokens.size() - 1);
        }
        return jsonValue;
    }

    @Override
    public boolean containsValue(JsonStructure target) {
        try {
            getValue(target);
            return true;
        } catch (JsonException je) {
            return false;
        }
    }

    /**
     * Adds or replaces a value at the referenced location in the specified
     * {@code target} with the specified {@code value}.
     * <ol>
     * <li>If the reference is the target (empty JSON Pointer string),
     * the specified {@code value}, which must be the same type as
     * specified {@code target}, is returned.</li>
     * <li>If the reference is an array element, the specified {@code value} is inserted
     * into the array, at the referenced index. The value currently at that location, and
     * any subsequent values, are shifted to the right (adds one to the indices).
     * Index starts with 0. If the reference is specified with a "-", or if the
     * index is equal to the size of the array, the value is appended to the array.</li>
     * <li>If the reference is a name/value pair of a {@code JsonObject}, and the
     * referenced value exists, the value is replaced by the specified {@code value}.
     * If the value does not exist, a new name/value pair is added to the object.</li>
     * </ol>
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @param value  the value to be added
     * @return the transformed {@code target} after the value is added.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException        if the reference is an array element and
     *                              the index is out of range ({@code index < 0 || index > array size}),
     *                              or if the pointer contains references to non-existing objects or arrays.
     */
    public JsonStructure add(JsonStructure target, JsonValue value) {
        validateAdd(target);
        if (isEmptyJsonPointer()) {
            if (value.getClass() != target.getClass()) {
                throw new JsonException("The value must have the same type as the target");
            }
            return (JsonStructure) value;
        }

        return addInternal(target, value);
    }

    /**
     * Adds or replaces a value at the referenced location in the specified
     * {@code target} with the specified {@code value}.
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @param value  the value to be added
     * @return the transformed {@code target} after the value is added.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException        if the reference is an array element and
     *                              the index is out of range ({@code index < 0 || index > array size}),
     *                              or if the pointer contains references to non-existing objects or arrays.
     * @see #add(JsonStructure, JsonValue)
     */
    public JsonObject add(JsonObject target, JsonValue value) {
        validateAdd(target);

        return addInternal(target, value);
    }

    /**
     * Adds or replaces a value at the referenced location in the specified
     * {@code target} with the specified {@code value}.
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @param value  the value to be added
     * @return the transformed {@code target} after the value is added.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException        if the reference is an array element and
     *                              the index is out of range ({@code index < 0 || index > array size}),
     *                              or if the pointer contains references to non-existing objects or arrays.
     * @see #add(JsonStructure, JsonValue)
     */
    public JsonArray add(JsonArray target, JsonValue value) {
        validateAdd(target);

        return addInternal(target, value);
    }

    /**
     * Replaces the value at the referenced location in the specified
     * {@code target} with the specified {@code value}.
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @param value  the value to be stored at the referenced location
     * @return the transformed {@code target} after the value is replaced.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException        if the referenced value does not exist,
     *                              or if the reference is the target.
     */
    public JsonStructure replace(JsonStructure target, JsonValue value) {
        if (target instanceof JsonObject) {
            return replace((JsonObject) target, value);
        } else {
            return replace((JsonArray) target, value);
        }
    }

    /**
     * Replaces the value at the referenced location in the specified
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @param value  the value to be stored at the referenced location
     * @return the transformed {@code target} after the value is replaced.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException        if the referenced value does not exist,
     *                              or if the reference is the target.
     * @see #replace(JsonStructure, JsonValue)
     */
    public JsonObject replace(JsonObject target, JsonValue value) {
        return add(remove(target), value);
    }

    /**
     * Replaces the value at the referenced location in the specified
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @param value  the value to be stored at the referenced location
     * @return the transformed {@code target} after the value is replaced.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException        if the referenced value does not exist,
     *                              or if the reference is the target.
     * @see #replace(JsonStructure, JsonValue)
     */
    public JsonArray replace(JsonArray target, JsonValue value) {
        return add(remove(target), value);
    }

    /**
     * Removes the value at the reference location in the specified {@code target}
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @return the transformed {@code target} after the value is removed.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException        if the referenced value does not exist,
     *                              or if the reference is the target.
     */
    public JsonStructure remove(JsonStructure target) {
        if (target instanceof JsonObject) {
            return remove((JsonObject) target);
        } else {
            return remove((JsonArray) target);
        }
    }

    /**
     * Removes the value at the reference location in the specified {@code target}
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @return the transformed {@code target} after the value is removed.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException        if the referenced value does not exist,
     *                              or if the reference is the target.
     * @see #remove(JsonStructure)
     */
    public JsonObject remove(JsonObject target) {
        validateRemove(target);

        return (JsonObject) remove(target, 1, referenceTokens.size() - 1);
    }

    /**
     * Removes the value at the reference location in the specified {@code target}
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @return the transformed {@code target} after the value is removed.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException        if the referenced value does not exist,
     *                              or if the reference is the target.
     * @see #remove(JsonStructure)
     */
    public JsonArray remove(JsonArray target) {
        validateRemove(target);

        return (JsonArray) remove(target, 1, referenceTokens.size() - 1);
    }

    String getJsonPointer() {
        return jsonPointer;
    }

    private void validateAdd(JsonValue target) {
        validateJsonPointer(target, referenceTokens.size() - 1);
    }

    private void validateRemove(JsonValue target) {
        validateJsonPointer(target, referenceTokens.size());
        if (isEmptyJsonPointer()) {
            throw new JsonException("The reference must not be the target");
        }
    }

    private boolean isEmptyJsonPointer() {
        return jsonPointer.equals("");
    }

    private JsonValue getValue(JsonValue jsonValue, String referenceToken, int currentPosition, int referencePosition) {
        if (jsonValue instanceof JsonObject) {
            JsonObject jsonObject = (JsonObject) jsonValue;
            jsonValue = jsonObject.get(referenceToken);

            if (jsonValue != null) {
                return jsonValue;
            }
            throw new JsonException("'" + jsonObject + "' contains no value for name '" + referenceToken + "'");
        } else if (jsonValue instanceof JsonArray) {
            validateArrayIndex(referenceToken);

            try {
                JsonArray jsonArray = (JsonArray) jsonValue;
                int arrayIndex = Integer.parseInt(referenceToken);
                validateArraySize(jsonArray, arrayIndex, jsonArray.size());
                return jsonArray.get(arrayIndex);
            } catch (NumberFormatException e) {
                throw new JsonException("'" + referenceToken + "' is no valid array index", e);
            }
        } else {
            if (currentPosition != referencePosition) {
                return jsonValue;
            }
            throw new JsonException("'" + jsonValue + "' contains no element for '" + referenceToken + "'");
        }
    }

    private <T extends JsonStructure> T addInternal(T jsonValue, JsonValue newValue) {
        List<String> currentPath = new ArrayList<>();
        currentPath.add("");

        return (T) addInternal(jsonValue, newValue, currentPath);
    }

    private JsonValue addInternal(JsonValue jsonValue, JsonValue newValue, List<String> currentPath) {
        if (jsonValue instanceof JsonObject) {
            JsonObject jsonObject = (JsonObject) jsonValue;
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

            if (jsonObject.isEmpty()) {
                objectBuilder.add(lastReferenceToken, newValue);
            } else {
                for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {

                    currentPath.add(entry.getKey());
                    objectBuilder.add(entry.getKey(), addInternal(entry.getValue(), newValue, currentPath));
                    currentPath.remove(entry.getKey());

                    if (currentPath.size() == referenceTokens.size() - 1 &&
                        currentPath.get(currentPath.size() - 1).equals(referenceTokens.get(referenceTokens.size() - 2))) {
                        objectBuilder.add(lastReferenceToken, newValue);
                    }
                }
            }
            return objectBuilder.build();
        } else if (jsonValue instanceof JsonArray) {
            JsonArray jsonArray = (JsonArray) jsonValue;
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

            int arrayIndex = -1;
            if (currentPath.size() == referenceTokens.size() - 1 &&
                currentPath.get(currentPath.size() - 1).equals(referenceTokens.get(referenceTokens.size() - 2))) {

                arrayIndex = getArrayIndex(lastReferenceToken, jsonArray, true);
            }

            int jsonArraySize = jsonArray.size();
            for (int i = 0; i <= jsonArraySize; i++) {
                if (i == arrayIndex) {
                    arrayBuilder.add(newValue);
                }
                if (i == jsonArraySize) {
                    break;
                }

                String path = String.valueOf(i);
                currentPath.add(path);
                arrayBuilder.add(addInternal(jsonArray.get(i), newValue, currentPath));
                currentPath.remove(path);
            }
            return arrayBuilder.build();
        }
        return jsonValue;
    }

    private JsonValue remove(JsonValue jsonValue, int currentPosition, int referencePosition) {
        if (jsonValue instanceof JsonObject) {
            JsonObject jsonObject = (JsonObject) jsonValue;
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();

            for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                if (currentPosition == referencePosition
                        && lastReferenceToken.equals(entry.getKey())) {
                    continue;
                }
                objectBuilder.add(entry.getKey(), remove(entry.getValue(), currentPosition + 1, referencePosition));
            }
            return objectBuilder.build();
        } else if (jsonValue instanceof JsonArray) {
            JsonArray jsonArray = (JsonArray) jsonValue;
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

            int arrayIndex = -1;
            if (currentPosition == referencePosition) {
                arrayIndex = getArrayIndex(lastReferenceToken, jsonArray, false);
            }

            int jsonArraySize = jsonArray.size();
            for (int i = 0; i < jsonArraySize; i++) {
                if (i == arrayIndex) {
                    continue;
                }
                arrayBuilder.add(remove(jsonArray.get(i), currentPosition + 1, referencePosition));
            }
            return arrayBuilder.build();
        }
        return jsonValue;
    }

    private int getArrayIndex(String referenceToken, JsonArray jsonArray, boolean addOperation) {
        if (addOperation && referenceToken.equals("-")) {
            return jsonArray.size();
        }

        validateArrayIndex(referenceToken);

        try {
            int arrayIndex = Integer.parseInt(referenceToken);
            int arraySize = addOperation ? jsonArray.size() + 1 : jsonArray.size();
            validateArraySize(jsonArray, arrayIndex, arraySize);
            return arrayIndex;
        } catch (NumberFormatException e) {
            throw new JsonException("'" + referenceToken + "' is no valid array index", e);
        }
    }

    private void validateJsonPointer(JsonValue target, int size) throws NullPointerException, JsonException {
        if (target == null) {
            throw new NullPointerException("target must not be null");
        }

        JsonValue jsonValue = target;
        for (int i = 1; i < size; i++) {
            jsonValue = getValue(jsonValue, referenceTokens.get(i), i, referenceTokens.size() - 1);
        }
    }

    private void validateArrayIndex(String referenceToken) throws JsonException {
        if (referenceToken.startsWith("+") || referenceToken.startsWith("-")) {
            throw new JsonException("An array index must not start with '" + referenceToken.charAt(0) + "'");
        }
        if (referenceToken.startsWith("0") && referenceToken.length() > 1) {
            throw new JsonException("An array index must not start with a leading '0'");
        }
    }

    private void validateArraySize(JsonArray jsonArray, int arrayIndex, int arraySize) throws JsonException {
        if (arrayIndex >= arraySize) {
            throw new JsonException("'" + jsonArray + "' contains no element for index " + arrayIndex);
        }
    }

}
