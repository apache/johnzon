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

import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.spi.JsonProvider;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class SerializableValue implements Serializable {
    private static final AtomicReference<JsonReaderFactory> FACTORY_ATOMIC_REFERENCE = new AtomicReference<JsonReaderFactory>();

    private final String value;

    SerializableValue(final String value) {
        this.value = value;
    }

    private Object readResolve() throws ObjectStreamException {
        final JsonReader parser = factory().createReader(new StringReader(value));
        try {
            return parser.read();
        } finally {
            parser.close();
        }
    }

    private static JsonReaderFactory factory() { // avoid to create too much instances of provider or factories, not needed
        JsonReaderFactory factory = FACTORY_ATOMIC_REFERENCE.get();
        if (factory == null) {
            FACTORY_ATOMIC_REFERENCE.compareAndSet(null, JsonProvider.provider().createReaderFactory(Collections.<String, Object>emptyMap()));
            factory = FACTORY_ATOMIC_REFERENCE.get();
        }
        return factory;
    }
}
