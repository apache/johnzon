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

import jakarta.json.JsonConfig;
import jakarta.json.JsonException;
import jakarta.json.JsonValue;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public enum DuplicateKeysMode {
    NONE((map, k, v) -> {
        if (map.put(k, v) != null) {
            throw new JsonException("Rejected key: '" + k + "', already present");
        }
    }),
    FIRST(Map::putIfAbsent),
    LAST(Map::put);

    static final List<String> CONFIG_KEYS = asList(
            JsonConfig.KEY_STRATEGY, // jsonp 2.1 spec
            "johnzon.rejectDuplicateKeys", // our specific one
            "org.glassfish.json.rejectDuplicateKeys" // the spec includes it (yes :facepalm:)
    );

    public static DuplicateKeysMode from(final Map<String, ?> config) {
        if (config == null) {
            return LAST;
        }

        for (String configKey : CONFIG_KEYS) {
            Object value = config.get(configKey);
            if (value == null) {
                continue;
            }

            if (configKey.equals(JsonConfig.KEY_STRATEGY)) {
                JsonConfig.KeyStrategy specKeyStrategy = (JsonConfig.KeyStrategy) value;

                switch (specKeyStrategy) {
                    case NONE:
                        return NONE;

                    case FIRST:
                        return FIRST;

                    default:
                    case LAST:
                        return LAST;
                }
            }

            String valueAsString = String.valueOf(value);
            if ("true".equals(valueAsString)) {
                return NONE;
            }
        }

        return LAST;
    }

    private final Put put;

    DuplicateKeysMode(final Put put) {
        this.put = put;
    }

    public Put put() {
        return put;
    }

    @FunctionalInterface
    public interface Put {
        void put(final Map<String, JsonValue> receiptor, final String key, final JsonValue value);
    }
}
