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
package org.apache.johnzon.jsonb.symmetry;

import jakarta.json.bind.Jsonb;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class SymmetryTest {

    public abstract Jsonb jsonb();

    public abstract void assertWrite(final Jsonb jsonb);

    public abstract void assertRead(final Jsonb jsonb);

    /**
     * Assert a simple write operation
     */
    @Test
    public void write() throws Exception {
        try (final Jsonb jsonb = jsonb()) {
            assertWrite(jsonb);
        }
    }

    /**
     * Assert a simple read operation
     */
    @Test
    public void read() throws Exception {
        try (final Jsonb jsonb = jsonb()) {
            assertRead(jsonb);
        }
    }

    /**
     * Validate any caching done from a write operation
     * leads to a consistent result on any future
     * write operations
     */
    @Test
    public void writeAfterWrite() throws Exception {
        try (final Jsonb jsonb = jsonb()) {
            assertWrite(jsonb);
            assertWrite(jsonb);
            assertWrite(jsonb);
            assertWrite(jsonb);
        }
    }

    /**
     * Validate any caching done from a read operation
     * leads to a consistent result on any future
     * read operations
     */
    @Test
    public void readAfterRead() throws Exception {
        try (final Jsonb jsonb = jsonb()) {
            assertRead(jsonb);
            assertRead(jsonb);
            assertRead(jsonb);
            assertRead(jsonb);
        }
    }

    /**
     * Validate any caching done from a read operation
     * does not alter the expected behavior of a write
     * operation
     */
    @Test
    public void writeAfterRead() throws Exception {
        try (final Jsonb jsonb = jsonb()) {
            assertRead(jsonb);
            assertWrite(jsonb);
            assertRead(jsonb);
            assertWrite(jsonb);
        }
    }

    /**
     * Validate any caching done from a write operation
     * does not alter the expected behavior of a read
     * operation
     */
    @Test
    public void readAfterWrite() throws Exception {
        try (final Jsonb jsonb = jsonb()) {
            assertWrite(jsonb);
            assertRead(jsonb);
            assertWrite(jsonb);
            assertRead(jsonb);
        }
    }
}
