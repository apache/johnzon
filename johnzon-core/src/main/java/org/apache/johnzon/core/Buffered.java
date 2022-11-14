/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.johnzon.core;

/**
 * A <code>Buffered</code> is a source or destination of data that is buffered
 * before writing or reading.  The bufferSize method allows all participants
 * in the underlying stream to align on this buffer size for optimization.
 *
 * This interface is designed in the spirit of {@code java.io.Flushable} and
 * {@code java.io.Closeable}
 *
 * @since 1.2.17
 */
public interface Buffered { // https://github.com/apache/johnzon/pull/84#discussion_r860563179 for the naming ;)

    /**
     * The buffer size used by this stream while reading input or before writing
     * output to the underlying stream.
     * @return the size of the buffer
     */
    int bufferSize();
}
