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
import java.io.OutputStream;
import java.util.Objects;

public class DelegatingOutputStream extends OutputStream {

    private OutputStream outputStream;

    public DelegatingOutputStream(OutputStream outputStream) {
        this.outputStream = Objects.requireNonNull(outputStream, "delegate outputStream must not be null");
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    @Override
    public void write(final int b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        outputStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }
}