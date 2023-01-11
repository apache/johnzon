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

import jakarta.json.bind.annotation.JsonbDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class JsonbOffsetDateTimeConverter extends JsonbDateConverterBase<OffsetDateTime> {
    private static final ZoneId UTC = ZoneId.of("UTC");

    public JsonbOffsetDateTimeConverter(final JsonbDateFormat dateFormat) {
        super(dateFormat);
    }

    @Override
    public String toString(final OffsetDateTime instance) {
        return formatter == null ? instance.toString() : instance.format(formatter);
    }

    @Override
    public OffsetDateTime fromString(final String text) {
        return formatter == null ? OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(text)), UTC) : OffsetDateTime.parse(text, formatter);
    }
}
