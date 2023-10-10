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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;

public enum RejectDuplicateKeysMode {
    DEFAULT(Map::put),
    TRUE((map, k, v) -> {
        if (map.put(k, v) != null) {
            throw new JsonException("Rejected key: '" + k + "', already present");
        }
    }),
    FIRST(Map::putIfAbsent);

    static final List<String> CONFIG_KEYS = asList(
            JsonConfig.KEY_STRATEGY, // jsonp 2.1 spec
            "johnzon.rejectDuplicateKeys", // our specific one
            "org.glassfish.json.rejectDuplicateKeys" // the spec includes it (yes :facepalm:)
    );

    public static RejectDuplicateKeysMode from(final Map<String, ?> config) {
        if (config == null) {
            return DEFAULT;
        }

        return CONFIG_KEYS.stream()
                .map(config::get)
                .filter(Objects::nonNull)
                .findFirst()
                .map(String::valueOf)
                .map(it -> "false".equalsIgnoreCase(it) || "LAST".equalsIgnoreCase(it) ? "DEFAULT" : it) // aliases to avoid to add an enum value for nothing
                .map(it -> "NONE".equalsIgnoreCase(it) ? "true" : it)
                .map(it -> valueOf(it.toUpperCase(Locale.ROOT).trim()))
                .orElse(DEFAULT);
    }

    private final Put put;

    RejectDuplicateKeysMode(final Put put) {
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
