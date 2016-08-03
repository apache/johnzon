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

import org.apache.johnzon.mapper.JohnzonConverter;
import org.apache.johnzon.mapper.MapperBuilder;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class TimestampAdapterTest {
    @Test
    public void convert() {
        final Model model = new Model();
        model.setDate(new Date(0));
        final String actual = new MapperBuilder().build().writeObjectAsString(model);
        assertEquals("{\"date\":0}", actual);
        assertEquals(0L, Model.class.cast(new MapperBuilder().build().readObject(actual, Model.class)).getDate().getTime());
    }

    public static class Model {
        @JohnzonConverter(TimestampAdapter.class)
        private Date date;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }
}
