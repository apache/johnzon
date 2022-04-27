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
package org.apache.johnzon.core.io;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class BoundedOutputStreamWriterTest {
    // sanity check
    @Test
    public void write() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (final BoundedOutputStreamWriter writer = new BoundedOutputStreamWriter(outputStream, UTF_8, 10)) {
            writer.write("ok");
            writer.write('1');
        }
        assertEquals("ok1", outputStream.toString("UTF-8"));
    }

    // enables to check buffer size respects
    @Test
    public void sizeLimit() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (final BoundedOutputStreamWriter writer = new BoundedOutputStreamWriter(outputStream, UTF_8, 10)) {
            writer.write("1234567890");
            assertEquals(0, outputStream.size()); // was not yet written since it matches buffer size
            writer.write('1');
            assertEquals(10, outputStream.size()); // was written
        }
        assertEquals("12345678901", outputStream.toString("UTF-8"));
    }

    // enables to check a small buffer size enables to have a faster outputstream feedback
    @Test
    public void sizeLimit2() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (final BoundedOutputStreamWriter writer = new BoundedOutputStreamWriter(outputStream, UTF_8, 2)) {
            writer.write("1234567890");
            writer.write('1');
        }
        assertEquals("12345678901", outputStream.toString("UTF-8"));
    }
}
