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

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;

/**
 * <p>This class is an immutable representation of a JSON Pointer as specified in
 * <a href="http://tools.ietf.org/html/rfc6901">RFC 6901</a>.
 * </p>
 * <p> A JSON Pointer, when applied to a target {@link JsonValue},
 * defines a reference location in the target.</p>
 * <p> An empty JSON Pointer string defines a reference to the target itself.</p>
 * <p> If the JSON Pointer string is non-empty, it must be a sequence
 * of '/' prefixed tokens, and the target must either be a {@link JsonArray}
 * or {@link JsonObject}. If the target is a {@code JsonArray}, the pointer
 * defines a reference to an array element, and the last token specifies the index.
 * If the target is a {@link JsonObject}, the pointer defines a reference to a
 * name/value pair, and the last token specifies the name.
 * </p>
 * <p> The method {@link JsonPointer#getValue getValue()} returns the referenced value.
 * The methods {@link JsonPointer#add add()}, {@link JsonPointer#replace replace()},
 * and {@link JsonPointer#remove remove()} executes the operations specified in
 * <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>. </p>
 *
 * @since 1.1
 */

public class JsonPointer {

    private final String jsonPointer;
    private final String[] referenceTokens;

    /**
     * Constructs and initializes a JsonPointer.
     *
     * @param jsonPointer the JSON Pointer string
     * @throws NullPointerException if {@code jsonPointer} is {@code null}
     * @throws JsonException        if {@code jsonPointer} is not a valid JSON Pointer
     */
    public JsonPointer(String jsonPointer) {
        if (jsonPointer == null) {
            throw new NullPointerException("jsonPointer must not be null");
        }
        if (!jsonPointer.equals("") && !jsonPointer.startsWith("/")) {
            throw new JsonException("A non-empty JSON pointer must begin with a '/'");
        }

        this.jsonPointer = jsonPointer;
        referenceTokens = jsonPointer.split("/", -1);
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

        JsonPointer that = (JsonPointer) obj;
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

        if (jsonPointer.equals("")) {
            return target;
        }

        JsonValue jsonValue = target;

        for (int i = 1; i < referenceTokens.length; i++) {
            String decodedReferenceToken = JsonPointerUtil.decode(referenceTokens[i]);

            if (jsonValue instanceof JsonObject) {
                JsonObject jsonObject = (JsonObject) jsonValue;
                jsonValue = jsonObject.get(decodedReferenceToken);

                if (jsonValue == null) {
                    throw new JsonException("The JsonObject " + jsonObject + " contains no value for token " + decodedReferenceToken);
                }
            } else if (jsonValue instanceof JsonArray) {
                JsonArray jsonArray = (JsonArray) jsonValue;

                try {
                    int index = Integer.parseInt(decodedReferenceToken);
                    if (index >= jsonArray.size()) {
                        throw new JsonException("The JsonArray " + jsonArray + " contains no element for index " + index);
                    }
                    if (decodedReferenceToken.startsWith("0") && decodedReferenceToken.length() > 1) {
                        throw new JsonException("The token " + decodedReferenceToken + " with leading zeros is not allowed to reference an element of a JsonArray");
                    }

                    jsonValue = jsonArray.get(index);
                } catch (NumberFormatException e) {
                    throw new JsonException("The token " + decodedReferenceToken + " for the JsonArray " + jsonArray + " is not a number", e);
                }
            }

        }

        return jsonValue;
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
        return null;
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
        return null;
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
        return null;
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
        return (JsonObject) this.add((JsonStructure) target, value);
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
        return (JsonArray) this.add((JsonStructure) target, value);
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
     *                              {@code target} with the specified {@code value}.
     * @see #replace(JsonStructure, JsonValue)
     */
    public JsonObject replace(JsonObject target, JsonValue value) {
        return (JsonObject) this.replace((JsonStructure) target, value);
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
     *                              {@code target} with the specified {@code value}.
     * @see #replace(JsonStructure, JsonValue)
     */
    public JsonArray replace(JsonArray target, JsonValue value) {
        return (JsonArray) this.replace((JsonStructure) target, value);
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
        return (JsonObject) this.remove((JsonStructure) target);
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
        return (JsonArray) this.remove((JsonStructure) target);
    }


}
