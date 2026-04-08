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
import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * JSON-B 3.0 Section 3.4 — URI maps to JSON String via {@code URI.toString()}.
 *
 * @see ee.jakarta.tck.json.bind.defaultmapping.specifictypes.SpecificTypesMappingTest
 */
public class URISimpleTest extends BuiltInSymmetryTest {

    public static class Widget {

        private URI value;

        public URI getValue() {
            return value;
        }

        public void setValue(final URI value) {
            this.value = value;
        }
    }

    @Override
    public void assertWrite(final Jsonb jsonb) {
        final Widget widget = new Widget();
        widget.setValue(URI.create("https://example.com"));
        assertEquals("{\"value\":\"https://example.com\"}", jsonb.toJson(widget));
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        final Widget widget = jsonb.fromJson("{\"value\":\"https://example.com\"}", Widget.class);
        assertEquals(URI.create("https://example.com"), widget.getValue());
    }
}
