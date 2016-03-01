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

import org.apache.johnzon.mapper.access.FieldAccessMode;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LocaleTest {
    @Test
    public void roundTrip() {
        final String expected = "{\"locale\":\"fr_FR\"}";
        final Mapper mapper = new MapperBuilder().setAccessMode(new FieldAccessMode(false, false)).build();
        {
            final Locale locale = Locale.FRANCE;
            final LocaleHolder holder = new LocaleHolder();
            holder.locale = locale;
            assertEquals(expected, mapper.writeObjectAsString(holder));
        }
        {
            final LocaleHolder holder = mapper.readObject(expected, LocaleHolder.class);
            assertNotNull(holder.locale);
            assertEquals("fr_FR", holder.locale.toString());
        }
    }

    public static class LocaleHolder {
        private Locale locale;
    }
}
