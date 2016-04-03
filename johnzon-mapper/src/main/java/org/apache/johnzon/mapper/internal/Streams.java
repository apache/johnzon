/*
 *     Licensed to the Apache Software Foundation (ASF) under one or more
 *     contributor license agreements.  See the NOTICE file distributed with
 *     this work for additional information regarding copyright ownership.
 *     The ASF licenses this file to You under the Apache License, Version 2.0
 *     (the "License"); you may not use this file except in compliance with
 *     the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.apache.johnzon.mapper.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;

public final class Streams {
    private Streams() {
        // no-op
    }

    public static Writer noClose(final Writer from) {
        return new Writer() {
            @Override
            public void write(final int c) throws IOException {
                from.write(c);
            }

            @Override
            public void write(final char[] cbuf) throws IOException {
                from.write(cbuf);
            }

            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                from.write(cbuf, off, len);
            }

            @Override
            public void write(final String str) throws IOException {
                from.write(str);
            }

            @Override
            public void write(final String str, final int off, final int len) throws IOException {
                from.write(str, off, len);
            }

            @Override
            public Writer append(final CharSequence csq) throws IOException {
                return from.append(csq);
            }

            @Override
            public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
                return from.append(csq, start, end);
            }

            @Override
            public Writer append(final char c) throws IOException {
                return from.append(c);
            }

            @Override
            public void flush() throws IOException {
                from.flush();
            }

            @Override
            public void close() throws IOException {
                from.flush();
            }
        };
    }

    public static Reader noClose(final Reader from) {
        return new Reader() {
            @Override
            public void close() throws IOException {
                // no-op
            }

            @Override
            public int read(final CharBuffer target) throws IOException {
                return from.read(target);
            }

            @Override
            public int read() throws IOException {
                return from.read();
            }

            @Override
            public int read(final char[] cbuf) throws IOException {
                return from.read(cbuf);
            }

            @Override
            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                return from.read(cbuf, off, len);
            }

            @Override
            public long skip(final long n) throws IOException {
                return from.skip(n);
            }

            @Override
            public boolean ready() throws IOException {
                return from.ready();
            }

            @Override
            public boolean markSupported() {
                return from.markSupported();
            }

            @Override
            public void mark(final int readAheadLimit) throws IOException {
                from.mark(readAheadLimit);
            }

            @Override
            public void reset() throws IOException {
                from.reset();
            }
        };
    }
    public static OutputStream noClose(final OutputStream from) {
        return new OutputStream() {
            @Override
            public void close() throws IOException {
                from.flush();
            }

            @Override
            public void write(final int b) throws IOException {
                from.write(b);
            }

            @Override
            public void write(final byte[] b) throws IOException {
                from.write(b);
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException {
                from.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                from.flush();
            }
        };
    }

    public static InputStream noClose(final InputStream from) {
        return new InputStream() {
            @Override
            public void close() throws IOException {
                // no-op
            }

            @Override
            public int read(final byte[] b) throws IOException {
                return from.read(b);
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                return from.read(b, off, len);
            }

            @Override
            public long skip(final long n) throws IOException {
                return from.skip(n);
            }

            @Override
            public int available() throws IOException {
                return from.available();
            }

            @Override
            public void mark(final int readlimit) {
                from.mark(readlimit);
            }

            @Override
            public void reset() throws IOException {
                from.reset();
            }

            @Override
            public boolean markSupported() {
                return from.markSupported();
            }

            @Override
            public int read() throws IOException {
                return from.read();
            }
        };
    }
}
