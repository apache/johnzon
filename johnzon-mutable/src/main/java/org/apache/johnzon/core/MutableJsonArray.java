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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.apache.johnzon.mutable.MutableJsonStructure;

final class MutableJsonArray extends AbstractMutableJsonStructure {

    private final List<GenericJsonValue> mutableList;

    MutableJsonArray(final JsonArray list, final Ancestor ancestor) {
        super(JsonValue.ValueType.ARRAY, ancestor);

        mutableList = new ArrayList<GenericJsonValue>();

        int i = 0;
        for (final Iterator iterator = list.iterator(); iterator.hasNext();) {
            final JsonValue value = (JsonValue) iterator.next();
            final Ancestor ca = new AncestorImpl(this, i);

            if (value.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                mutableList.add(new GenericJsonValue(CoreHelper.toMutableJsonStructure((JsonStructure) value, ca)));
            } else if (value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
                mutableList.add(new GenericJsonValue(CoreHelper.toMutableJsonStructure((JsonStructure) value, ca)));
            } else {
                mutableList.add(new GenericJsonValue(value, ca));
            }
            i++;
        }
    }

    @Override
    public JsonValue getLeaf(final int index) {
        throwIfNotArray();
        try {
            final GenericJsonValue genericJsonValue = mutableList.get(index);
            if (genericJsonValue.isJsonValue()) {
                return genericJsonValue.getJsonValue();
            }
        } catch (final IndexOutOfBoundsException e) {
            throw new JsonException("invalid index " + index);
        }

        throw new JsonException("not a value");
    }

    @Override
    public final MutableJsonStructure set(final int index, final JsonValue value) {
        throwIfNotArray();
        try {
            mutableList.set(index, new GenericJsonValue(value, getAncestor()));
        } catch (final IndexOutOfBoundsException e) {
            throw new JsonException("invalid index " + index);
        }
        return this;
    }

    @Override
    public final MutableJsonStructure add(final int index, final JsonValue value) {
        throwIfNotArray();
        try {
            mutableList.add(index, new GenericJsonValue(value, getAncestor()));
        } catch (final IndexOutOfBoundsException e) {
            throw new JsonException("invalid index " + index);
        }
        return this;
    }

    @Override
    public final MutableJsonStructure add(final JsonValue value) {
        throwIfNotArray();
        mutableList.add(new GenericJsonValue(value, getAncestor()));
        return this;
    }

    @Override
    public MutableJsonStructure get(final int index) {
        try {
            final GenericJsonValue genericJsonValue = mutableList.get(index);
            if (!genericJsonValue.isJsonValue()) {
                return genericJsonValue.getMutableStructure();
            }
        } catch (final IndexOutOfBoundsException e) {
            throw new JsonException("invalid index " + index);
        }

        throw new JsonException("not a mutable structure");
    }

    @Override
    public MutableJsonStructure set(final int index, final MutableJsonStructure value) {
        try {
            mutableList.set(index, new GenericJsonValue(value));
        } catch (final IndexOutOfBoundsException e) {
            throw new JsonException("invalid index " + index);
        }
        return this;
    }

    @Override
    public MutableJsonStructure remove(final int index) {
        try {
            mutableList.remove(index);
        } catch (final IndexOutOfBoundsException e) {
            throw new JsonException("invalid index " + index);
        }
        return this;
    }

    @Override
    public MutableJsonStructure add(final int index, final MutableJsonStructure value) {
        try {
            mutableList.add(index, new GenericJsonValue(value));
        } catch (final IndexOutOfBoundsException e) {
            throw new JsonException("invalid index " + index);
        }
        return this;
    }

    @Override
    public MutableJsonStructure add(final MutableJsonStructure value) {
        mutableList.add(new GenericJsonValue(value));
        return this;
    }

    @Override
    public JsonStructure toJsonStructure() {

        final JsonArrayBuilder builder = Json.createArrayBuilder();

        for (final Iterator<GenericJsonValue> iterator = mutableList.iterator(); iterator.hasNext();) {
            final GenericJsonValue genericJsonValue = iterator.next();

            if (genericJsonValue.isJsonValue()) {
                builder.add(genericJsonValue.getJsonValue());
            } else {
                builder.add(genericJsonValue.getMutableStructure().toJsonStructure());
            }

        }

        return builder.build();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mutableList == null) ? 0 : mutableList.hashCode());
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
        final MutableJsonArray other = (MutableJsonArray) obj;
        if (mutableList == null) {
            if (other.mutableList != null) {
                return false;
            }
        } else if (!mutableList.equals(other.mutableList)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.toJsonStructure().toString();
    }

    @Override
    public int size() {
        return mutableList.size();
    }

    @Override
    public JsonValue getLeaf(@SuppressWarnings("unused") final String key) {
        throwIfNotObject();
        return null;
    }

    @Override
    public boolean isLeaf(final int index) {
        GenericJsonValue genericJsonValue = null;
        try {
            genericJsonValue = mutableList.get(index);
        } catch (final IndexOutOfBoundsException e) {
            throw new JsonException("invalid index " + index);
        }

        return genericJsonValue.isJsonValue();
    }

    @Override
    public boolean isLeaf(final String key) {
        throwIfNotObject();
        throw new RuntimeException("cannot happen");
    }

    @Override
    public MutableJsonStructure set(final MutableJsonStructure value) {
        mutableList.clear();
        mutableList.addAll(((MutableJsonArray) value).mutableList);
        return this;
    }
}