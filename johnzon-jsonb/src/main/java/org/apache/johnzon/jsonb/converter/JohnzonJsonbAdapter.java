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

import org.apache.johnzon.mapper.Adapter;

import javax.json.bind.JsonbException;
import javax.json.bind.adapter.JsonbAdapter;

public class JohnzonJsonbAdapter<A, B> implements Adapter<A, B> {
    private final JsonbAdapter<A, B> delegate;

    public JohnzonJsonbAdapter(final JsonbAdapter<A, B> delegate) {
        this.delegate = delegate;
    }

    @Override
    public A to(final B obj) {
        if (obj == null && !delegate.handlesNullValue()) {
            return null;
        }
        try {
            return delegate.adaptTo(obj);
        } catch (final Exception e) {
            throw new JsonbException(e.getMessage(), e);
        }
    }

    @Override
    public B from(final A obj) {
        if (obj == null && !delegate.handlesNullValue()) {
            return null;
        }
        try {
            return delegate.adaptFrom(obj);
        } catch (final Exception e) {
            throw new JsonbException(e.getMessage(), e);
        }
    }
}
