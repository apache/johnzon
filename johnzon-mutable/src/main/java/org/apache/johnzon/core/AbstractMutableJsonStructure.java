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

import java.util.Set;

import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.johnzon.mutable.MutableJsonStructure;

/**
 * 
 *
 */
abstract class AbstractMutableJsonStructure implements MutableJsonStructure {

    private final ValueType valueType;
    private final Ancestor ancestor;

    AbstractMutableJsonStructure(final ValueType valueType, final Ancestor ancestor) {
        if (valueType == null) {
            throw new IllegalArgumentException();
        }

        this.valueType = valueType;
        this.ancestor = ancestor;
        throwIfNotStructure();

    }

    @Override
    public final Ancestor getAncestor() {
        return ancestor;
    }

    @Override
    public JsonStructure toJsonStructure() {
        throwIfNotStructure();
        return null;
    }

    @Override
    public int size() {
        throwIfNotStructure();
        return -1;
    }

    @Override
    public Set<String> getKeys() {
        throwIfNotObject();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure get(final String key) {
        throwIfNotObject();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure set(final String key, final MutableJsonStructure value) {
        throwIfNotObject();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure remove(final String key) {
        throwIfNotObject();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure add(final String key, final MutableJsonStructure value) {
        throwIfNotObject();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure get(final int index) {
        throwIfNotArray();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure set(final int index, final MutableJsonStructure value) {
        throwIfNotArray();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure remove(final int index) {
        throwIfNotArray();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure add(final int index, final MutableJsonStructure value) {
        throwIfNotArray();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure add(final MutableJsonStructure value) {
        throwIfNotArray();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure set(final int index, final JsonValue value) {
        throwIfNotArray();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure add(final int index, final JsonValue value) {
        throwIfNotArray();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure set(final String key, final JsonValue value) {
        throwIfNotObject();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure add(final String key, final JsonValue value) {
        throwIfNotObject();
        return null;
    }

    @SuppressWarnings("unused")
    @Override
    public MutableJsonStructure add(final JsonValue value) {
        throwIfNotArray();
        return null;
    }

    @Override
    public final MutableJsonStructure set(final String key, final String value) {
        throwIfNotObject();
        return set(key, CoreHelper.createJsonString(value));
    }

    @Override
    public final MutableJsonStructure set(final String key, final Number value) {
        throwIfNotObject();
        return set(key, CoreHelper.createJsonNumber(value));
    }

    @Override
    public final MutableJsonStructure add(final String key, final String value) {
        throwIfNotObject();
        return add(key, CoreHelper.createJsonString(value));
    }

    @Override
    public final MutableJsonStructure add(final String key, final Number value) {
        throwIfNotObject();
        return add(key, CoreHelper.createJsonNumber(value));
    }

    @Override
    public final MutableJsonStructure set(final int index, final String value) {
        throwIfNotArray();
        return set(index, CoreHelper.createJsonString(value));
    }

    @Override
    public final MutableJsonStructure set(final int index, final Number value) {
        throwIfNotArray();
        return set(index, CoreHelper.createJsonNumber(value));
    }

    @Override
    public final MutableJsonStructure add(final int index, final String value) {
        throwIfNotArray();
        return add(index, CoreHelper.createJsonString(value));
    }

    @Override
    public final MutableJsonStructure add(final int index, final Number value) {
        throwIfNotArray();
        return add(index, CoreHelper.createJsonNumber(value));
    }

    @Override
    public final MutableJsonStructure add(final String value) {
        throwIfNotArray();
        return add(CoreHelper.createJsonString(value));
    }

    @Override
    public final MutableJsonStructure add(final Number value) {
        throwIfNotArray();
        return add(CoreHelper.createJsonNumber(value));
    }

    @Override
    public boolean isJsonArray() {
        throwIfNotStructure();
        return isArray();
    }

    @Override
    public String getLeafAsString(final String key) {
        return JsonString.class.cast(getLeaf(key)).getString();
    }

    @Override
    public int getLeafAsInt(final String key) {
        return JsonNumber.class.cast(getLeaf(key)).intValueExact();
    }

    @Override
    public boolean getLeafAsBoolean(final String key) {
        final JsonValue val = getLeaf(key);
        switch (val.getValueType()) {
            case TRUE:
                return true;
            case FALSE:
                return false;
            default:
                throw new ClassCastException();
        }
    }

    @Override
    public boolean isLeafNull(final String key) {
        final JsonValue val = getLeaf(key);
        switch (val.getValueType()) {
            case NULL:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String getLeafAsString(final int index) {
        return JsonString.class.cast(getLeaf(index)).getString();
    }

    @Override
    public int getLeafAsInt(final int index) {
        return JsonNumber.class.cast(getLeaf(index)).intValueExact();
    }

    @Override
    public boolean getLeafAsBoolean(final int index) {
        final JsonValue val = getLeaf(index);
        switch (val.getValueType()) {
            case TRUE:
                return true;
            case FALSE:
                return false;
            default:
                throw new ClassCastException();
        }
    }

    @Override
    public boolean isLeafNull(final int index) {
        final JsonValue val = getLeaf(index);
        switch (val.getValueType()) {
            case NULL:
                return true;
            default:
                return false;
        }
    }

    protected final boolean isObject() {
        return valueType.equals(ValueType.OBJECT);
    }

    protected final boolean isArray() {
        return valueType.equals(ValueType.ARRAY);
    }

    protected final boolean isStructure() {
        return isArray() || isObject();
    }

    protected final void throwIfNotStructure() {
        if (!isStructure()) {
            throw new JsonException("Only valid for structure");
        }
    }

    protected final void throwIfNotArray() {
        if (!isArray()) {
            throw new JsonException("Only valid for array");
        }
    }

    protected final void throwIfNotObject() {
        if (!isObject()) {
            throw new JsonException("Only valid for object");
        }
    }

    @Override
    public MutableJsonStructure getParent() {
        if (ancestor == null) {
            return null;
        }

        return ancestor.getMutableJsonStructure();
    }

    @Override
    public boolean exists(final String key) {
        return getKeys().contains(key);
    }

    @Override
    public boolean exists(final int index) {
        return index < size();
    }

    @Override
    public MutableJsonStructure copy() {
        //can be done better, not very performant here
        return CoreHelper.toMutableJsonStructure0(toJsonStructure());
    }

}
