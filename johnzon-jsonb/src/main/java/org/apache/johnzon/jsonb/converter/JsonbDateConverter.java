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
import java.util.Date;

public class JsonbDateConverter extends JsonbDateConverterBase<Date> {
    public JsonbDateConverter(final JsonbDateFormat dateFormat) {
        super(dateFormat);
    }

    @Override
    public String toString(final Date instance) {
        return formatter == null ? Long.toString(instance.getTime()) : formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(instance.getTime()), ZoneOffset.UTC));
    }

    @Override
    public Date fromString(final String text) {
        return formatter == null ? new Date(Long.parseLong(text)) : Date.from(LocalDateTime.parse(text, formatter).toInstant(ZoneOffset.UTC));
    }
}
