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
package org.apache.johnzon.mapper.converter;

import org.apache.johnzon.mapper.Converter;

import java.util.Date;

// needed for openjpa for instance which proxies dates
public class DateWithCopyConverter implements Converter<Date> {
    private final Converter<Date> delegate;

    public DateWithCopyConverter(final Converter<Date> delegate) {
        this.delegate = delegate == null ? new DateConverter("yyyyMMddHHmmssZ") : delegate;
    }

    @Override
    public String toString(final Date instance) {
        return delegate.toString(new Date(instance.getTime()));
    }

    @Override
    public Date fromString(final String text) {
        return delegate.fromString(text);
    }
}
