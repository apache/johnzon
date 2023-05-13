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

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

class JsonBuilderFactoryImpl implements JsonBuilderFactory, Serializable {
    private final Map<String, Object> internalConfig = new HashMap<String, Object>();
    private RejectDuplicateKeysMode rejectDuplicateKeysMode;
    private JsonProviderImpl provider;
    private BufferStrategy.BufferProvider<char[]> bufferProvider;
    private static final List<String> SUPPORTED_CONFIG_KEYS = RejectDuplicateKeysMode.CONFIG_KEYS;

    protected JsonBuilderFactoryImpl() {
        // no-op: serialization
    }

    JsonBuilderFactoryImpl(final Map<String, ?> config, final BufferStrategy.BufferProvider<char[]> bufferProvider,
                           final RejectDuplicateKeysMode rejectDuplicateKeysMode, final JsonProviderImpl provider) {
        this.bufferProvider = bufferProvider;
        this.rejectDuplicateKeysMode = rejectDuplicateKeysMode;
        this.provider = provider;
        if (config != null && !config.isEmpty()) {
            for (String configKey : config.keySet()) {
                if(SUPPORTED_CONFIG_KEYS.contains(configKey)) {
                    internalConfig.put(configKey, config.get(configKey));
                } else {
                    Logger.getLogger(this.getClass().getName())
                            .warning(configKey + " is not supported by " + getClass().getName());
                }
            }
        }
    }

    @Override
    public JsonObjectBuilder createObjectBuilder() {
        return new JsonObjectBuilderImpl(emptyMap(), bufferProvider, rejectDuplicateKeysMode, provider);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder(JsonObject initialData) {
        return new JsonObjectBuilderImpl(initialData, bufferProvider, rejectDuplicateKeysMode, provider);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder() {
        return new JsonArrayBuilderImpl(emptyList(), bufferProvider, rejectDuplicateKeysMode, provider);
    }


    @Override
    public JsonArrayBuilder createArrayBuilder(JsonArray initialData) {
        return new JsonArrayBuilderImpl(initialData, bufferProvider, rejectDuplicateKeysMode, provider);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder(Collection<?> initialData) {
        return new JsonArrayBuilderImpl(initialData, bufferProvider, rejectDuplicateKeysMode, provider);
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(internalConfig);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder(Map<String, Object> initialValues) {
        return new JsonObjectBuilderImpl(initialValues, bufferProvider, rejectDuplicateKeysMode, provider);
    }

}
