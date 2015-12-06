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
package org.apache.johnzon.jsonb.converter;

import javax.json.bind.annotation.JsonbDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class JsonbLocalDateTimeConverter extends JsonbDateConverterBase<LocalDateTime> {
    public JsonbLocalDateTimeConverter(final JsonbDateFormat dateFormat) {
        super(dateFormat);
    }

    @Override
    public String toString(final LocalDateTime instance) {
        return formatter == null ? Long.toString(instance.toInstant(ZoneOffset.UTC).toEpochMilli()) : instance.format(formatter);
    }

    @Override
    public LocalDateTime fromString(final String text) {
        return formatter == null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(text)), ZoneOffset.UTC) : LocalDateTime.parse(text, formatter);
    }
}
