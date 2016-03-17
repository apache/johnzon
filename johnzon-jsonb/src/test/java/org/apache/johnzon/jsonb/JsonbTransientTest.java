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
package org.apache.johnzon.jsonb;

import org.junit.Test;

import javax.json.bind.Jsonb;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.spi.JsonbProvider;

import static org.junit.Assert.assertEquals;

public class JsonbTransientTest {
    @Test
    public void roundtrip() {
        final Jsonb jsonb = JsonbProvider.provider().create().build();
        final Book book = jsonb.fromJson("{\"_name\": \"test\", \"id\":123}", Book.class);
        assertEquals("test", book.getName());
        assertEquals(0, book.getId());

        book.id = 123;
        assertEquals("{\"_name\":\"test\"}", jsonb.toJson(book));
    }

    public static class Book {
        @JsonbTransient
        private long id;

        @JsonbProperty("_name")
        private String name;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
