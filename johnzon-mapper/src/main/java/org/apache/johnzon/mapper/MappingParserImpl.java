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
package org.apache.johnzon.mapper;

import java.lang.reflect.Type;

import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;

import org.apache.johnzon.core.JsonReaderImpl;

/**
 * This class is not concurrently usable as it contains state.
 */
public class MappingParserImpl implements MappingParser {
    private final JsonParser jsonParser;

    private JsonReader jsonReader = null;

    public MappingParserImpl(JsonParser jsonParser) {
        this.jsonParser = jsonParser;
    }

    @Override
    public JsonParser getJsonParser() {
        return jsonParser;
    }

    @Override
    public JsonReader getJsonReader() {
        if (jsonReader == null) {
            jsonReader = new JsonReaderImpl(jsonParser);
        }
        return jsonReader;
    }

    @Override
    public <T> T readObject(Type targetType) {
        return null;
    }

    @Override
    public <T> T readObject(JsonValue jsonValue, Type targetType) {
        return null;
    }
}
