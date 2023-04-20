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
package org.apache.johnzon.jsonb.polymorphism;

import jakarta.json.bind.annotation.JsonbSubtype;
import jakarta.json.bind.annotation.JsonbTypeInfo;
import java.util.HashMap;
import java.util.Map;

public class JsonbPolymorphismTypeInfo {
    private final String typeKey;
    private final Map<String, Class<?>> aliases;

    protected JsonbPolymorphismTypeInfo(JsonbTypeInfo annotation) {
        this.typeKey = annotation.key();

        aliases = new HashMap<>();
        for (JsonbSubtype subtype : annotation.value()) {
            aliases.put(subtype.alias(), subtype.type());
        }
    }

    public String getTypeKey() {
        return typeKey;
    }

    public Map<String, Class<?>> getAliases() {
        return aliases;
    }
}
