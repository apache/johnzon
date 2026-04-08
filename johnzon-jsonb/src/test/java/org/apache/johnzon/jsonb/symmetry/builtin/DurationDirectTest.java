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
import java.time.Duration;

import static org.junit.Assert.assertEquals;

/**
 * JSON-B 3.0 Section 3.5 — Duration uses ISO 8601 duration format: {@code PTnHnMnS}.
 *
 * @see ee.jakarta.tck.json.bind.defaultmapping.dates.DatesMappingTest#testDurationMapping()
 */
public class DurationDirectTest extends BuiltInSymmetryTest {

    @Override
    public void assertWrite(final Jsonb jsonb) {
        assertEquals("\"PT1H30M\"", jsonb.toJson(Duration.parse("PT1H30M")));
    }

    @Override
    public void assertRead(final Jsonb jsonb) {
        assertEquals(Duration.parse("PT1H30M"), jsonb.fromJson("\"PT1H30M\"", Duration.class));
    }
}
