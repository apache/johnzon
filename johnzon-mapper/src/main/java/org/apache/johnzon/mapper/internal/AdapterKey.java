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
package org.apache.johnzon.mapper.internal;

import java.lang.reflect.Type;

public class AdapterKey {
    private final Type from;
    private final Type to;
    private final int hash;

    public AdapterKey(final Type from, final Type to) {
        this.from = from;
        this.to = to;

        int result = from.hashCode();
        result = 31 * result + to.hashCode();
        this.hash = result;
    }

    public Type getFrom() {
        return from;
    }

    public Type getTo() {
        return to;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AdapterKey that = AdapterKey.class.cast(o);
        return from.equals(that.from) && to.equals(that.to);

    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "AdapterKey{" +
            "from=" + from +
            ", to=" + to +
            '}';
    }
}
