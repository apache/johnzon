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

import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedQueue;

public enum BufferStrategy {
    BY_INSTANCE {
        @Override
        public BufferProvider<char[]> newCharProvider(final int size) {
            return new CharBufferByInstanceProvider(size);
        }

        @Override
        public BufferProvider<StringBuilder> newStringBuilderProvider(final int size) {
            return new StringBuilderByInstanceProvider(size);
        }
    },
    THREAD_LOCAL {
        @Override
        public BufferProvider<char[]> newCharProvider(final int size) {
            return new CharBufferThreadLocalProvider(size);
        }

        @Override
        public BufferProvider<StringBuilder> newStringBuilderProvider(final int size) {
            return new StringBuilderThreadLocalProvider(size);
        }
    },
    QUEUE {
        @Override
        public BufferProvider<char[]> newCharProvider(final int size) {
            return new CharBufferQueueProvider(size);
        }

        @Override
        public BufferProvider<StringBuilder> newStringBuilderProvider(final int size) {
            return new StringBuilderQueueProvider(size);
        }
    },
    SINGLETON {
        @Override
        public BufferProvider<char[]> newCharProvider(final int size) {
            return new CharBufferSingletonProvider(size);
        }

        @Override
        public BufferProvider<StringBuilder> newStringBuilderProvider(final int size) {
            return new StringBuilderSingletonProvider(size);
        }
    };

    public abstract BufferProvider<char[]> newCharProvider(int size);
    public abstract BufferProvider<StringBuilder> newStringBuilderProvider(int size);

    public static interface BufferProvider<T> extends Serializable {
        T newBuffer();

        void release(T value);
    }

    private static class CharBufferSingletonProvider extends SingletonProvider<char[]> {
        public CharBufferSingletonProvider(final int size) {
            super(size);
        }

        @Override
        protected char[] newInstance(int size) {
            return new char[size];
        }

        @Override
        public void release(final char[] value) {
            // no-op
        }
    }

    private static class StringBuilderSingletonProvider extends SingletonProvider<StringBuilder> {
        public StringBuilderSingletonProvider(final int size) {
            super(size);
        }

        @Override
        protected StringBuilder newInstance(final int size) {
            return new StringBuilder(size);
        }

        @Override
        public void release(final StringBuilder value) {
            value.setLength(0);
        }
    }

    private static abstract class SingletonProvider<T> implements BufferProvider<T> {
        protected final T buffer;

        public SingletonProvider(final int size) {
            buffer = newInstance(size);
        }

        protected abstract T newInstance(int size);

        @Override
        public T newBuffer() {
            return buffer;
        }

        @Override
        public void release(final T value) {
            // no-op
        }
    }

    private static abstract class ThreadLocalProvider<T> implements BufferProvider<T> {
        private final ThreadLocalBufferCache<T> cache;

        public ThreadLocalProvider(final int size) {
            cache = new ThreadLocalBufferCache<T>(size) {
                @Override
                protected T newValue(int defaultSize) {
                    return newInstance(size);
                }
            };
        }

        protected abstract T newInstance(int size);

        @Override
        public T newBuffer() {
            return cache.getCache();
        }

        @Override
        public void release(final T value) {
            cache.release(value);
        }
    }

    private static class CharBufferThreadLocalProvider extends ThreadLocalProvider<char[]> {
        public CharBufferThreadLocalProvider(int size) {
            super(size);
        }

        @Override
        protected char[] newInstance(final int size) {
            return new char[size];
        }
    }

    private static class StringBuilderThreadLocalProvider extends ThreadLocalProvider<StringBuilder> {
        public StringBuilderThreadLocalProvider(int size) {
            super(size);
        }

        @Override
        protected StringBuilder newInstance(final int size) {
            return new StringBuilder(size);
        }

        @Override
        public void release(final StringBuilder value) {
            value.setLength(0);
            super.release(value);
        }
    }

    private static class CharBufferByInstanceProvider implements BufferProvider<char[]> {
        private final int size;

        public CharBufferByInstanceProvider(final int size) {
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

    private static class StringBuilderByInstanceProvider implements BufferProvider<StringBuilder> {
        private final int size;

        public StringBuilderByInstanceProvider(final int size) {
            this.size = size;
        }

        @Override
        public StringBuilder newBuffer() {
            return new StringBuilder(size);
        }

        @Override
        public void release(final StringBuilder value) {
            // no-op
        }
    }

    private static abstract class QueueProvider<T> implements BufferProvider<T> {
        private final int size;
        private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<T>();

        public QueueProvider(final int size) {
            this.size = size;
        }

        protected abstract T newInstance(int size);

        @Override
        public T newBuffer() {
            final T buffer = queue.poll();
            if (buffer == null) {
                return newInstance(size);
            }
            return buffer;
        }

        @Override
        public void release(final T value) {
            queue.offer(value);
        }
    }

    private static class CharBufferQueueProvider extends QueueProvider<char[]> {
        public CharBufferQueueProvider(final int size) {
            super(size);
        }

        @Override
        protected char[] newInstance(int size) {
            return new char[size];
        }
    }

    private static class StringBuilderQueueProvider extends QueueProvider<StringBuilder> {
        public StringBuilderQueueProvider(final int size) {
            super(size);
        }

        @Override
        protected StringBuilder newInstance(int size) {
            return new StringBuilder(size);
        }

        @Override
        public void release(final StringBuilder value) {
            value.setLength(0);
            super.release(value);
        }
    }
}
