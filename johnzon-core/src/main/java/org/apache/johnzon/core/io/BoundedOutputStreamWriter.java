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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * A {@link BufferedWriter} that wraps an {@link OutputStreamWriter} and automatically flushes it when flushing its internal buffer.
 * It enables to wrap an {@link OutputStream} as a {@link Writer} but with a faster feedback than a default
 * {@link OutputStreamWriter} which uses a 8k buffer by default (encapsulated).
 */
public class BoundedOutputStreamWriter extends BufferedWriter {
    private final int bufferSize;

    private int writtenSinceLastFlush = 0;

    public BoundedOutputStreamWriter(OutputStream outputStream, Charset charset, int maxSize) {
        super(new OutputStreamWriter(outputStream, charset), maxSize);

        this.bufferSize = maxSize;
    }

    // Only methods that are directly modifying the internal buffer in BufferedWriter should be overwritten here,
    // otherwise we might track the same char being written twice

    @Override
    public void write(String s, int off, int len) throws IOException {
        autoFlush();
        super.write(s, off, len);

        writtenSinceLastFlush += len;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        autoFlush();
        super.write(cbuf, off, len);

        writtenSinceLastFlush += len;
    }

    @Override
    public void write(int c) throws IOException {
        autoFlush();
        super.write(c);

        writtenSinceLastFlush += 1;
    }

    private void autoFlush() throws IOException {
        if (writtenSinceLastFlush >= bufferSize) {
            flush();

            writtenSinceLastFlush = 0;
        }
    }
}
