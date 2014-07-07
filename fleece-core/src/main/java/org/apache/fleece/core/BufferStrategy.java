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

import java.io.Serializable;

public enum BufferStrategy {
    BY_INSTANCE {
        @Override
        public BufferProvider newProvider(final int size) {
            return new ByInstanceProvider(size);
        }
    },
    THREAD_LOCAL {
        @Override
        public BufferProvider newProvider(final int size) {
            return new ThreadLocalProvider(size);
        }
    },
    SINGLETON {
        @Override
        public BufferProvider newProvider(final int size) {
            return new SingletonProvider(size);
        }
    };

    public abstract BufferProvider newProvider(int size);

    public static interface BufferProvider extends Serializable {
        char[] newBuffer();

        void release(char[] value);
    }

    private static class SingletonProvider implements BufferProvider, Serializable {
        private final char[] buffer;

        public SingletonProvider(final int size) {
            buffer = new char[size];
        }

        @Override
        public char[] newBuffer() {
            return buffer;
        }

        @Override
        public void release(char[] value) {
            // no-op
        }
    }

    private static class ThreadLocalProvider implements BufferProvider {
        private final BufferCache<char[]> cache;

        public ThreadLocalProvider(final int size) {
            cache = new BufferCache<char[]>(size) {
                @Override
                protected char[] newValue(int defaultSize) {
                    return new char[size];
                }
            };
        }

        @Override
        public char[] newBuffer() {
            return cache.newValue(0);
        }

        @Override
        public void release(final char[] value) {
            cache.release(value);
        }
    }

    private static class ByInstanceProvider implements BufferProvider {
        private final int size;

        public ByInstanceProvider(final int size) {
            this.size = size;
        }

        @Override
        public char[] newBuffer() {
            return new char[size];
        }

        @Override
        public void release(final char[] value) {
            // no-op
        }
    }
}
