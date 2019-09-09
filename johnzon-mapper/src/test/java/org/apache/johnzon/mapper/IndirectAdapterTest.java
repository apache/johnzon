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
package org.apache.johnzon.mapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

import org.junit.Assert;
import org.junit.Test;

public class IndirectAdapterTest {
    protected abstract static class GenericTimeAdapter<T extends TemporalAccessor> implements Adapter<T, String> {
        private final DateTimeFormatter formatter;
        private final TemporalQuery<T> query;

        private GenericTimeAdapter(final DateTimeFormatter formatter, final TemporalQuery<T> query) {
            this.formatter = formatter;
            this.query = query;
        }

        @Override
        public T to(final String b) {
            return formatter.parse(b, query);
        }

        @Override
        public String from(final T a) {
            return formatter.format(a);
        }
    }

    public static final class LocalDateAdapter extends GenericTimeAdapter<LocalDate> {
        public LocalDateAdapter() {
            super(DateTimeFormatter.ISO_DATE, LocalDate::from);
        }
    }


    public static class BeanType {
        @JohnzonConverter(LocalDateAdapter.class)
        public LocalDate date;
    }

    @Test
    public void testIndirectAdapter() {
        try (final Mapper mapper = new MapperBuilder().build()) {
            final BeanType content = mapper.readObject("{\"date\":\"2019-09-09\"}", BeanType.class);
            Assert.assertEquals(LocalDate.of(2019, 9, 9), content.date);
        }
    }
}
