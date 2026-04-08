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

import static org.junit.Assert.assertEquals;

/**
 * JSON-B 3.0 Section 3.4 / RFC 8259 Section 6 — Double maps to JSON Number.
 *
 * @see ee.jakarta.tck.json.bind.defaultmapping.basictypes.BasicJavaTypesMappingTest
 */
public class DoubleSimpleTest extends BuiltInSymmetryTest {

    public static class Widget {

        private Double value;

        public Double getValue() {
            return value;
        }

        public void setValue(final Double value) {
            this.value = value;
        }
    }

    @Override
    public void assertWrite(final Jsonb jsonb) {
        final Widget widget = new Widget();
        widget.setValue(1.5);
        assertEquals("{\"value\":1.5}", jsonb.toJson(widget));
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        final Widget widget = jsonb.fromJson("{\"value\":1.5}", Widget.class);
        assertEquals(Double.valueOf(1.5), widget.getValue());
    }
}
