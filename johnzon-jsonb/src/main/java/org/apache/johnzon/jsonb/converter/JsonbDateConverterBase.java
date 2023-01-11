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
import java.time.format.DateTimeFormatter;

public abstract class JsonbDateConverterBase<T> extends JsonbLocaleParserConverterBase<T> {
    protected final DateTimeFormatter formatter;

    protected JsonbDateConverterBase(final JsonbDateFormat dateFormat) {
        final String value = dateFormat.value();
        final String locale = dateFormat.locale();
        final boolean ms = value.equals(JsonbDateFormat.TIME_IN_MILLIS);
        formatter = ms ? null :
            (!locale.equals(JsonbDateFormat.DEFAULT_LOCALE) ?
                DateTimeFormatter.ofPattern(value).withLocale(newLocale(locale)) : DateTimeFormatter.ofPattern(value));
    }
}
