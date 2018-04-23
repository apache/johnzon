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
package org.apache.johnzon.jsonb.serializer;

import org.apache.johnzon.core.JsonReaderImpl;
import org.apache.johnzon.mapper.MappingParser;

import javax.json.JsonValue;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.stream.JsonParser;
import java.lang.reflect.Type;

// TODO: test it
public class JohnzonDeserializationContext implements DeserializationContext {
    private final MappingParser runtime;

    public JohnzonDeserializationContext(final MappingParser runtime) {
        this.runtime = runtime;
    }

    @Override
    public <T> T deserialize(final Class<T> clazz, final JsonParser parser) {
        return runtime.readObject(read(parser), clazz);
    }

    @Override
    public <T> T deserialize(final Type type, final JsonParser parser) {
        return runtime.readObject(read(parser), type);
    }

    private JsonValue read(final JsonParser parser) { // TODO: use jsonp 1.1 and not johnzon internals
        return new JsonReaderImpl(parser, true).readValue();
    }
}
