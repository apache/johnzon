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
package org.apache.johnzon.core;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

public class CommentsJsonStreamParserImpl extends JsonStreamParserImpl {
    public CommentsJsonStreamParserImpl(final InputStream inputStream,
                                        final int maxStringLength,
                                        final BufferStrategy.BufferProvider<char[]> bufferProvider,
                                        final BufferStrategy.BufferProvider<char[]> valueBuffer,
                                        final boolean autoAdjust) {
        super(inputStream, maxStringLength, bufferProvider, valueBuffer, autoAdjust);
    }

    public CommentsJsonStreamParserImpl(final InputStream inputStream,
                                        final Charset encoding,
                                        final int maxStringLength,
                                        final BufferStrategy.BufferProvider<char[]> bufferProvider,
                                        final BufferStrategy.BufferProvider<char[]> valueBuffer,
                                        final boolean autoAdjust) {
        super(inputStream, encoding, maxStringLength, bufferProvider, valueBuffer, autoAdjust);
    }

    public CommentsJsonStreamParserImpl(final Reader reader,
                                        final int maxStringLength,
                                        final BufferStrategy.BufferProvider<char[]> bufferProvider,
                                        final BufferStrategy.BufferProvider<char[]> valueBuffer,
                                        final boolean autoAdjust) {
        super(reader, maxStringLength, bufferProvider, valueBuffer, autoAdjust);
    }

    @Override
    protected Event defaultHandling(final char c) {
        if (c == '/') {
            char next = readNextChar();
            if (next == '/') { // fail
                do {
                    next = readNextChar();
                } while (next != EOL);
                return next();
            } else if (next == '*') {
                next = 0;
                char previous;
                do {
                    previous = next;
                    next = readNextNonWhitespaceChar(readNextChar());
                } while (next != '/' || previous != '*');
                readNextNonWhitespaceChar(next);
                return next();
            }
        }
        return super.defaultHandling(c);
    }
}
