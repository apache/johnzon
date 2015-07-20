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

import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.apache.johnzon.mutable.MutableJsonStructure;
import org.apache.johnzon.mutable.MutableJsonStructure.Ancestor;

/**
 * 
 *
 */
final class GenericJsonValue {

    private final JsonValue jsonValue;
    private final MutableJsonStructure mutableStructure;
    private final Ancestor ancestor;

    GenericJsonValue(final MutableJsonStructure mutableStructure) {
        super();

        if (mutableStructure == null) {
            throw new IllegalArgumentException();
        }

        this.mutableStructure = mutableStructure;
        this.jsonValue = null;
        this.ancestor = null;
    }

    GenericJsonValue(final JsonValue jsonValue, final Ancestor ancestor) {
        super();

        if (jsonValue == null) {
            throw new IllegalArgumentException("jsonValue must not be null");
        }

        if (jsonValue.getValueType().equals(JsonValue.ValueType.ARRAY) || jsonValue.getValueType().equals(JsonValue.ValueType.OBJECT)) {
            //convert to mutable;
            this.ancestor = null;
            this.jsonValue = null;
            this.mutableStructure = CoreHelper.toMutableJsonStructure0((JsonStructure) jsonValue);
        } else {

            this.ancestor = ancestor;
            this.jsonValue = jsonValue;
            this.mutableStructure = null;
        }
    }

    public boolean isJsonValue() {
        return jsonValue != null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((jsonValue == null) ? 0 : jsonValue.hashCode());
        result = prime * result + ((mutableStructure == null) ? 0 : mutableStructure.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GenericJsonValue other = (GenericJsonValue) obj;
        if (jsonValue == null) {
            if (other.jsonValue != null) {
                return false;
            }
        } else if (!jsonValue.equals(other.jsonValue)) {
            return false;
        }
        if (mutableStructure == null) {
            if (other.mutableStructure != null) {
                return false;
            }
        } else if (!mutableStructure.equals(other.mutableStructure)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return isJsonValue() ? jsonValue.toString() : mutableStructure.toString();
    }

    public JsonValue getJsonValue() {
        return jsonValue;
    }

    public MutableJsonStructure getMutableStructure() {
        return mutableStructure;
    }

    public Ancestor getAncestor() {
        return ancestor;
    }
}
