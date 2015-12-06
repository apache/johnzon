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
package javax.json.bind;

import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

public interface JsonbBuilder {
    JsonbBuilder withConfig(final JsonbConfig config);

    JsonbBuilder withProvider(final JsonProvider jsonpProvider);

    Jsonb build();

    static Jsonb create() {
        return JsonbProvider.provider().create().build();
    }

    static Jsonb create(final JsonbConfig config) {
        return JsonbProvider.provider().create().withConfig(config).build();
    }

    static JsonbBuilder newBuilder() {
        return JsonbProvider.provider().create();
    }

    static JsonbBuilder newBuilder(final String providerName) {
        return JsonbProvider.provider(providerName).create();
    }

    static JsonbBuilder newBuilder(final JsonbProvider provider) {
        return provider.create();
    }
}
