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
package org.apache.johnzon.jsonb.converter;

import org.apache.johnzon.mapper.TypeAwareAdapter;

import javax.json.bind.JsonbException;
import javax.json.bind.adapter.JsonbAdapter;
import java.lang.reflect.Type;

public class JohnzonJsonbAdapter<JsonType, OriginalType> implements TypeAwareAdapter<OriginalType, JsonType> {
    private final JsonbAdapter<OriginalType, JsonType> delegate;
    private final Type from;
    private final Type to;

    public JohnzonJsonbAdapter(final JsonbAdapter<OriginalType, JsonType> delegate, final Type from, final Type to) {
        this.delegate = delegate;
        this.from = from;
        this.to = to;
    }

    @Override
    public OriginalType to(final JsonType obj) {
        if (obj == null) {
            return null;
        }
        try {
            return delegate.adaptFromJson(obj);
        } catch (final Exception e) {
            throw new JsonbException(e.getMessage(), e);
        }
    }

    @Override
    public JsonType from(final OriginalType obj) {
        if (obj == null) {
            return null;
        }
        try {
            return delegate.adaptToJson(obj);
        } catch (final Exception e) {
            throw new JsonbException(e.getMessage(), e);
        }
    }

    @Override
    public Type getTo() {
        return to;
    }

    @Override
    public Type getFrom() {
        return from;
    }
}
