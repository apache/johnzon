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
package org.apache.fleece.core;

import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonCharBufferStreamParser extends JsonBaseStreamParser {
    private static final Logger LOGGER = Logger.getLogger(JsonCharBufferStreamParser.class.getName());
    private static final boolean LOG = LOGGER.isLoggable(Level.FINE);

    private final char[] buffer;
    private final Reader in;
    private final BufferStrategy.BufferProvider<char[]> bufferProvider;
    private final BufferStrategy.BufferProvider<char[]> valueProvider;
    private int pointer = -1;
    private int avail;
    private char mark;
    private boolean reset;

    public JsonCharBufferStreamParser(final Reader reader, final int maxStringLength,
                                      final BufferStrategy.BufferProvider<char[]> bufferProvider,
                                      final BufferStrategy.BufferProvider<char[]> valueBuffer) {
        super(maxStringLength, valueBuffer.newBuffer());
        this.in = reader;
        this.buffer = bufferProvider.newBuffer();
        this.bufferProvider = bufferProvider;
        this.valueProvider = valueBuffer;
    }

    @Override
    protected char readNextChar() throws IOException {
        if (reset) {
            reset = false;
            return mark;
        }

        if (avail <= 0) {
            avail = in.read(buffer, 0, buffer.length);

            pointer = -1;

            if (LOG) {
                LOGGER.fine("******* Fill buffer with " + avail
                        + " chars");
            }

            if (avail <= 0) {
                throw new IOException("EOF");
            }
        }

        pointer++;
        avail--;
        return buffer[pointer];

    }

    @Override
    protected void mark() {
        mark = buffer[pointer];
    }

    @Override
    protected void reset() {
        reset = true;
    }

    @Override
    protected void closeUnderlyingSource() throws IOException {
        bufferProvider.release(buffer);
        valueProvider.release(currentValue);
        in.close();
    }
}
