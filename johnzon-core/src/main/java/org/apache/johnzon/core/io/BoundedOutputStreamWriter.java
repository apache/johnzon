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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CoderResult;

/**
 * Custom implementation of {@link Writer} that allows for wrapping an {@link OutputStream} with a controlled underlying buffer size.
 *
 * It enables to wrap an {@link OutputStream} as a {@link Writer} but with a faster feedback than a default
 * {@link java.io.OutputStreamWriter} which uses a 8k buffer by default (encapsulated).
 */
public class BoundedOutputStreamWriter extends Writer {
    private final OutputStream outputStream;
    private final CharsetEncoder encoder;
    private final ByteBuffer buffer;

    public BoundedOutputStreamWriter(final OutputStream outputStream,
                                     final Charset charset,
                                     final int maxSize) {
        this.outputStream = outputStream;
        this.encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        this.buffer = ByteBuffer.allocate(maxSize);
    }

    @Override
    public void write(final char[] chars, final int off, final int len) throws IOException {
        appendToBuffer(CharBuffer.wrap(chars, off, len));
    }

    @Override
    public void flush() throws IOException {
        flushInternal();
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        flushInternal();
        outputStream.close();
    }

    private void appendToBuffer(CharBuffer charBuffer) throws IOException {
        while (charBuffer.hasRemaining()) {
            CoderResult coderResult = encoder.encode(charBuffer, buffer, false);

            if (coderResult.isError()) {
                coderResult.throwException();
            }

            // Input is not fully writable to buffer? -> time to flush
            if (coderResult.isOverflow()) {
                flushInternal();
            }
        }
    }

    private void flushInternal() throws IOException {
        buffer.flip();
        while (buffer.hasRemaining()) {
            outputStream.write(buffer.get());
        }

        buffer.clear();
    }
}
