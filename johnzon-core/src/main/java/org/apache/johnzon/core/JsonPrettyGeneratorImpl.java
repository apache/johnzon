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

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentMap;

import org.apache.johnzon.core.BufferStrategy.BufferProvider;

class JsonPrettyGeneratorImpl extends JsonGeneratorImpl {

    private static final String INDENT = "  ";
    private int depth;

    JsonPrettyGeneratorImpl(final OutputStream out, final BufferProvider<char[]> bufferProvider, final ConcurrentMap<String, String> cache) {
        super(out, bufferProvider, cache);
    }

    JsonPrettyGeneratorImpl(final OutputStream out, final Charset encoding, final BufferProvider<char[]> bufferProvider,
            final ConcurrentMap<String, String> cache) {
        super(out, encoding, bufferProvider, cache);
    }

    JsonPrettyGeneratorImpl(final Writer writer, final BufferProvider<char[]> bufferProvider, final ConcurrentMap<String, String> cache) {
        super(writer, bufferProvider, cache);
    }

    @Override
    protected void incrementDepth() {
        depth++;
    }

    @Override
    protected void decrementDepth() {
        depth--;
    }

    @Override
    protected void writeEol() {
        justWrite(EOL);
    }

    @Override
    protected void writeIndent() {
        if (depth > 0) {
            for (int i = 0; i < depth; i++) {
                justWrite(INDENT);
            }
        }
    }
}
