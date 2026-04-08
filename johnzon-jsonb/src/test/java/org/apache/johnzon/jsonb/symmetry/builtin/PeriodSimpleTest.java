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
import java.time.Period;

import static org.junit.Assert.assertEquals;

/**
 * JSON-B 3.0 Section 3.5 — Period uses ISO 8601 period format: {@code PnYnMnD}.
 *
 * @see ee.jakarta.tck.json.bind.defaultmapping.dates.DatesMappingTest#testPeriodMapping()
 */
public class PeriodSimpleTest extends BuiltInSymmetryTest {

    public static class Widget {

        private Period value;

        public Period getValue() {
            return value;
        }

        public void setValue(final Period value) {
            this.value = value;
        }
    }

    @Override
    public void assertWrite(final Jsonb jsonb) {
        final Widget widget = new Widget();
        widget.setValue(Period.of(1, 2, 3));
        assertEquals("{\"value\":\"P1Y2M3D\"}", jsonb.toJson(widget));
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        final Widget widget = jsonb.fromJson("{\"value\":\"P1Y2M3D\"}", Widget.class);
        assertEquals(Period.of(1, 2, 3), widget.getValue());
    }
}
