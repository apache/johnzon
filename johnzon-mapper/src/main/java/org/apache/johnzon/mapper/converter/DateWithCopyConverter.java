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

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.internal.ConverterAdapter;

import java.util.Date;

// needed for openjpa for instance which proxies dates
public class DateWithCopyConverter implements Adapter<Date, String> {
    private final Adapter<Date, String> delegate;

    public DateWithCopyConverter(final Adapter<Date, String> delegate) {
        this.delegate = delegate == null ? new ConverterAdapter<>(new DateConverter("yyyyMMddHHmmssZ"), Date.class) : delegate;
    }

    @Override
    public Date to(final String s) {
        return delegate.to(s);
    }

    @Override
    public String from(final Date date) {
        return delegate.from(new Date(date.getTime()));
    }
}
