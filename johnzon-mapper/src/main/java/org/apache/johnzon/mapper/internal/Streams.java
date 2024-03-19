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

public final class Streams {
    private Streams() {
        // no-op
    }

    public static Writer noClose(final Writer writer) {
        return new DelegatingWriter(writer) {
            @Override
            public void close() throws IOException {
                flush();
            }
        };
    }

    public static Reader noClose(final Reader reader) {
        return new DelegatingReader(reader) {
            @Override
            public void close() throws IOException {
                // no-op
            }
        };
    }
    public static OutputStream noClose(final OutputStream outputStream) {
        return new DelegatingOutputStream(outputStream) {
            @Override
            public void close() throws IOException {
                flush();
            }
        };
    }

    public static InputStream noClose(final InputStream inputStream) {
        return new DelegatingInputStream(inputStream) {
            @Override
            public void close() throws IOException {
                // no-op
            }
        };
    }
}
