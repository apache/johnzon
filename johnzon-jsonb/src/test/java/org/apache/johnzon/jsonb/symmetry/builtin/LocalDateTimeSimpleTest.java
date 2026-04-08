/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.jsonb.symmetry.builtin;

import jakarta.json.bind.Jsonb;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

/**
 * JSON-B 3.0 Section 3.5 — LocalDate uses ISO_LOCAL_DATE format: {@code yyyy-MM-dd}.
 *
 * @see ee.jakarta.tck.json.bind.defaultmapping.dates.DatesMappingTest#testLocalDateMapping()
 */
public class LocalDateTimeSimpleTest extends BuiltInSymmetryTest {

    public static class Widget {

        private LocalDateTime value;

        public LocalDateTime getValue() {
            return value;
        }

        public void setValue(final LocalDateTime value) {
            this.value = value;
        }
    }

    @Override
    public void assertWrite(final Jsonb jsonb) {
        final Widget widget = new Widget();
        widget.setValue(LocalDateTime.of(2024, 1, 15, 10, 30, 45));
        assertEquals("{\"value\":\"2024-01-15T10:30:45\"}", jsonb.toJson(widget));
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        final Widget widget = jsonb.fromJson("{\"value\":\"2024-01-15T10:30:45\"}", Widget.class);
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 45), widget.getValue());
    }
}
