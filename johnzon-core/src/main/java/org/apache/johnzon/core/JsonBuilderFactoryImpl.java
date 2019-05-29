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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

class JsonBuilderFactoryImpl implements JsonBuilderFactory, Serializable {
    private final Map<String, Object> internalConfig = new HashMap<String, Object>();
    private BufferStrategy.BufferProvider<char[]> bufferProvider;
    private static final String[] SUPPORTED_CONFIG_KEYS = new String[] {
        // nothing yet
    };

    protected JsonBuilderFactoryImpl() {
        // no-op: serialization
    }

    JsonBuilderFactoryImpl(final Map<String, ?> config, final BufferStrategy.BufferProvider<char[]> bufferProvider) {
        this.bufferProvider = bufferProvider;
        if (config != null && config.size() > 0) {
            final List<String> supportedConfigKeys = Arrays.asList(SUPPORTED_CONFIG_KEYS);
            for (String configKey : config.keySet()) {
                if(supportedConfigKeys.contains(configKey)) {
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
        return new JsonObjectBuilderImpl(emptyMap(), bufferProvider);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder(JsonObject initialData) {
        return new JsonObjectBuilderImpl(initialData, bufferProvider);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder() {
        return new JsonArrayBuilderImpl(emptyList(), bufferProvider);
    }


    @Override
    public JsonArrayBuilder createArrayBuilder(JsonArray initialData) {
        return new JsonArrayBuilderImpl(initialData, bufferProvider);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder(Collection<?> initialData) {
        return new JsonArrayBuilderImpl(initialData, bufferProvider);
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(internalConfig);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder(Map<String, Object> initialValues) {
        return new JsonObjectBuilderImpl(initialValues, bufferProvider);
    }

}
