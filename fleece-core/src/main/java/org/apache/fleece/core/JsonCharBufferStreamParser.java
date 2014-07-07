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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonCharBufferStreamParser extends JsonBaseStreamParser {
    private static final Logger LOGGER = Logger.getLogger(JsonCharBufferStreamParser.class.getName());
    private static final boolean LOG = LOGGER.isLoggable(Level.FINE);

    /*
     * private static final BufferCache<char[]> BUFFER_CACHE = new
     * BufferCache<char[]>(
     * Integer.getInteger("org.apache.fleece.default-char-buffer", 8192) ) {
     * 
     * @Override protected char[] newValue(final int defaultSize) { return new
     * char[defaultSize]; } };
     */

    private final char[] buffer0;// BUFFER_CACHE.getCache();
    private final Reader in;
    private int pointer = -1;
    private int avail;
    private char mark;
    private boolean reset;

    // private int availOnMark;

    // Test increment buffer sizes

    public JsonCharBufferStreamParser(final Reader reader,
            final int maxStringLength, final int bufferSize) {
        super(maxStringLength);
        in = reader;
        buffer0 = new char[bufferSize];
    }

    public JsonCharBufferStreamParser(final InputStream stream,
            final int maxStringLength, final int bufferSize) {
        this(new InputStreamReader(stream), maxStringLength, bufferSize);
    }

    public JsonCharBufferStreamParser(final InputStream in,
            final Charset charset, final int maxStringLength, final int bufferSize) {
        this(new InputStreamReader(in, charset), maxStringLength, bufferSize);
    }

    @Override
    protected char readNextChar() throws IOException {
        if (reset) {
            reset = false;
            return mark;
        }

        if (avail <= 0) {
            if (LOG) {
                LOGGER.fine("avail:" + avail + "/pointer:" + pointer);
            }

            avail = in.read(buffer0, 0, buffer0.length);

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
        return buffer0[pointer];

    }

    @Override
    protected void mark() {
        if (LOG) {
            LOGGER.fine("    MARK " + buffer0[pointer] + " ("
                    + (int) buffer0[pointer] + ")");
        }
        mark = buffer0[pointer];
    }

    @Override
    protected void reset() {
        if (LOG) {
            LOGGER.fine("    RESET ");
        }
        reset = true;
    }

    @Override
    protected void closeUnderlyingSource() throws IOException {
        // BUFFER_CACHE.release(buffer0);
        if (in != null) {
            in.close();
        }
    }

}
