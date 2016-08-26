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

import javax.json.JsonValue;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;

public final class SerializablePrimitives {
    private SerializablePrimitives() {
        // no-op
    }

    public static final JsonValue NULL = new SerializableJsonValue(JsonValue.NULL);
    public static final JsonValue TRUE = new SerializableJsonValue(JsonValue.TRUE);
    public static final JsonValue FALSE = new SerializableJsonValue(JsonValue.FALSE);

    private static final class SerializableJsonValue implements JsonValue, Serializable {
        private final JsonValue delegate;

        SerializableJsonValue(final JsonValue value) {
            delegate = value;
        }

        @Override
        public ValueType getValueType() {
            return delegate.getValueType();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o || o == delegate) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (SerializableJsonValue.class.isInstance(o)) {
                return SerializableJsonValue.class.cast(o).delegate.equals(delegate);
            }
            return JsonValue.class.isInstance(o) && o.equals(delegate);

        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        private Object writeReplace() throws ObjectStreamException {
            return new SerializationReplacement(delegate.toString());
        }
    }

    private static final class SerializationReplacement implements Serializable {
        private final String value;

        private SerializationReplacement(final String value) {
            this.value = value;
        }

        private Object readResolve() throws ObjectStreamException {
            if ("null".equals(value)) {
                return NULL;
            }
            if ("true".equals(value)) {
                return TRUE;
            }
            if ("false".equals(value)) {
                return FALSE;
            }
            throw new InvalidObjectException("Unknown " + value);
        }
    }
}
