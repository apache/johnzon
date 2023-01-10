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
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;

/**
 * {@link java.io.OutputStreamWriter} delegating directly to a sun.nio.cs.StreamEncoder with a controlled underlying buffer size.
 * It enables to wrap an {@link OutputStream} as a {@link Writer} but with a faster feedback than a default
 * {@link java.io.OutputStreamWriter} which uses a 8k buffer by default (encapsulated).
 * <p>
 * Note: the "flush error" can be of 2 characters (lcb in StreamEncoder) but we can't do much better when encoding.
 */
public class BoundedOutputStreamWriter extends Writer {
    private final Writer delegate;

    public BoundedOutputStreamWriter(final OutputStream outputStream,
                                     final Charset charset,
                                     final int maxSize) {
        delegate = Channels.newWriter(
                Channels.newChannel(outputStream),
                charset.newEncoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE),
                maxSize);
    }

    @Override
    public void write(final int c) throws IOException {
        delegate.write(c);
    }

    @Override
    public void write(final char[] chars, final int off, final int len) throws IOException {
        delegate.write(chars, off, len);
    }

    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        delegate.write(str, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        delegate.write(cbuf);
    }

    @Override
    public void write(final String str) throws IOException {
        delegate.write(str);
    }

    @Override
    public Writer append(final CharSequence csq) throws IOException {
        return delegate.append(csq);
    }

    @Override
    public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
        return delegate.append(csq, start, end);
    }

    @Override
    public Writer append(final char c) throws IOException {
        return delegate.append(c);
    }
}
