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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.apache.johnzon.mutable.MutableJsonStructure;

final class MutableJsonObject extends AbstractMutableJsonStructure {

    private final Map<String, GenericJsonValue> mutableMap;

    MutableJsonObject(final JsonObject map, final Ancestor ancestor) {
        super(JsonValue.ValueType.OBJECT, ancestor);

        this.mutableMap = new LinkedHashMap<String, GenericJsonValue>();

        for (final Iterator<Entry<String, JsonValue>> iterator = map.entrySet().iterator(); iterator.hasNext();) {
            final Entry<String, JsonValue> entry = iterator.next();
            final JsonValue value = entry.getValue();
            final Ancestor ca = new AncestorImpl(this, entry.getKey());

            if (value.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                mutableMap.put(entry.getKey(), new GenericJsonValue(CoreHelper.toMutableJsonStructure((JsonStructure) value, ca)));
            } else if (value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
                mutableMap.put(entry.getKey(), new GenericJsonValue(CoreHelper.toMutableJsonStructure((JsonStructure) value, ca)));
            } else {
                mutableMap.put(entry.getKey(), new GenericJsonValue(value, ca));
            }

        }

    }

    @Override
    public JsonValue getLeaf(final String key) {
        throwIfNotObject();

        if (!mutableMap.containsKey(key)) {
            throw new JsonException("no such key: '" + key + "'");
        }

        final GenericJsonValue genericJsonValue = mutableMap.get(key);
        if (genericJsonValue.isJsonValue()) {
            return genericJsonValue.getJsonValue();
        }

        throw new JsonException("not a value");

    }

    @Override
    public final MutableJsonStructure set(final String key, final JsonValue value) {
        throwIfNotObject();

        if (!mutableMap.containsKey(key)) {
            throw new JsonException("no such key: '" + key + "'");
        }

        mutableMap.put(key, new GenericJsonValue(value, getAncestor()));
        return this;
    }

    @Override
    public final MutableJsonStructure add(final String key, final JsonValue value) {
        throwIfNotObject();
        mutableMap.put(key, new GenericJsonValue(value, getAncestor()));
        return this;
    }

    @Override
    public MutableJsonStructure get(final String key) {
        final GenericJsonValue genericJsonValue = mutableMap.get(key);

        if (genericJsonValue == null) {
            throw new JsonException("no such key: '" + key + "'");
        }

        if (!genericJsonValue.isJsonValue()) {
            return genericJsonValue.getMutableStructure();
        }

        throw new JsonException("not a mutable structure, " + genericJsonValue);
    }

    @Override
    public MutableJsonStructure set(final String key, final MutableJsonStructure value) {

        if (!mutableMap.containsKey(key)) {
            throw new JsonException("no such key: '" + key + "'");
        }

        mutableMap.put(key, new GenericJsonValue(value));
        return this;
    }

    @Override
    public MutableJsonStructure remove(final String key) {

        if (!mutableMap.containsKey(key)) {
            throw new JsonException("no such key: '" + key + "'");
        }

        mutableMap.remove(key);
        return this;
    }

    @Override
    public MutableJsonStructure add(final String key, final MutableJsonStructure value) {
        mutableMap.put(key, new GenericJsonValue(value));
        return this;
    }

    @Override
    public JsonStructure toJsonStructure() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();

        for (final Iterator<Entry<String, GenericJsonValue>> iterator = mutableMap.entrySet().iterator(); iterator.hasNext();) {
            final Entry<String, GenericJsonValue> entry = iterator.next();

            if (entry.getValue().isJsonValue()) {
                builder.add(entry.getKey(), entry.getValue().getJsonValue());
            } else {
                builder.add(entry.getKey(), entry.getValue().getMutableStructure().toJsonStructure());
            }
        }

        return builder.build();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mutableMap == null) ? 0 : mutableMap.hashCode());
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
        final MutableJsonObject other = (MutableJsonObject) obj;
        if (mutableMap == null) {
            if (other.mutableMap != null) {
                return false;
            }
        } else if (!mutableMap.equals(other.mutableMap)) {
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
        return mutableMap.size();
    }

    @Override
    public Set<String> getKeys() {
        return mutableMap.keySet();
    }

    @Override
    public JsonValue getLeaf(@SuppressWarnings("unused") final int index) {
        throwIfNotArray();
        return null;
    }

    @Override
    public boolean isLeaf(@SuppressWarnings("unused") final int index) {
        throwIfNotArray();
        throw new RuntimeException("cannot happen");
    }

    @Override
    public boolean isLeaf(final String key) {

        if (!mutableMap.containsKey(key)) {
            throw new JsonException("no such key: '" + key + "'");
        }

        final GenericJsonValue genericJsonValue = mutableMap.get(key);
        return genericJsonValue.isJsonValue();
    }

    @Override
    public MutableJsonStructure set(final MutableJsonStructure value) {
        mutableMap.clear();
        mutableMap.putAll(((MutableJsonObject) value).mutableMap);
        return this;
    }
}
