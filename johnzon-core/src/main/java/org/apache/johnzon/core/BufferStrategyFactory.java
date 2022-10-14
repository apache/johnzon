/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.johnzon.core.util.ClassUtil;

public class BufferStrategyFactory {
    private static final Map<String, BufferStrategy> DEFAULT_STRATEGIES;
    static {
        DEFAULT_STRATEGIES = new HashMap<>();

        DEFAULT_STRATEGIES.put("BY_INSTANCE", CharBufferByInstanceProvider::new);
        DEFAULT_STRATEGIES.put("THREAD_LOCAL", CharBufferThreadLocalProvider::new);
        DEFAULT_STRATEGIES.put("QUEUE", CharBufferQueueProvider::new);
        DEFAULT_STRATEGIES.put("SINGLETON", CharBufferSingletonProvider::new);
    }

    private BufferStrategyFactory() {
        // utility class ct
    }

    /**
     * creates a BufferStrategy based on the name.
     *
     * The following BufferStrategies are supported out of the box:
     * <ul>
     *     <li>BY_INSTANCE</li>
     *     <li>THREAD_LOCAL</li>
     *     <li>QUEUE</li>
     *     <li>SINGLETON</li>
     * </ul>
     *
     * You can also pass in a fully qualified class name of a custom {@link BufferStrategy}.
     *
     * @param strategyName one of the supported BufferStrategies as per above
     * @return an instance of the chosen BufferStrategy
     * @throws IllegalArgumentException if the given strategyName does not resolve to a BufferStrategy.
     */
    public static BufferStrategy valueOf(String strategyName) {
        BufferStrategy bufferStrategy = DEFAULT_STRATEGIES.get(strategyName.toUpperCase(Locale.ENGLISH));
        if (bufferStrategy == null) {
            // try to load the BufferStrategy via reflection
            Class<?> bsClass = ClassUtil.loadClassOptional(strategyName, false);
            if (bsClass == null || bsClass.isAssignableFrom(BufferStrategy.class)) {
                throw new IllegalArgumentException("Could not load Johnzon BufferStrategy " + strategyName +
                        ". Valid BufferStrategies are " + DEFAULT_STRATEGIES.keySet().toString() +
                        " or a fully qualified class name of an implementation of " + BufferStrategy.class.getName());
            }

            try {
                bufferStrategy = (BufferStrategy) bsClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return bufferStrategy;
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

    private static abstract class SingletonProvider<T> implements BufferStrategy.BufferProvider<T> {
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

    private static abstract class ThreadLocalProvider<T> implements BufferStrategy.BufferProvider<T> {
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

    private static class CharBufferByInstanceProvider implements BufferStrategy.BufferProvider<char[]> {
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

    private static abstract class QueueProvider<T> implements BufferStrategy.BufferProvider<T> {
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
}
