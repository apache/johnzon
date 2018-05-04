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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This {@link BufferStrategy.BufferProvider} is only used for
 * {@link org.apache.johnzon.mapper.JsonGeneratorCloseTest}.
 *
 * It's a simple buffer which exposes methods to test how often {@link #newBuffer()}
 * and {@link #release(Object)} is called.
 */
public class TestBufferProvider implements BufferStrategy.BufferProvider<char[]> {

    public static final TestBufferProvider INSTANCE = new TestBufferProvider();

    private static final ConcurrentLinkedQueue<char[]> QUEUE = new ConcurrentLinkedQueue<char[]>();

    private AtomicInteger releaseCalls = new AtomicInteger(0);
    private AtomicInteger newBufferCalls = new AtomicInteger(0);


    private TestBufferProvider() {
        // no instantiation
    }


    @Override
    public char[] newBuffer() {
        newBufferCalls.incrementAndGet();

        char[] buffer = QUEUE.poll();
        if (buffer == null) {
            buffer = new char[4096];
        }

        return buffer;
    }

    @Override
    public void release(char[] value) {

        releaseCalls.incrementAndGet();

        QUEUE.offer(value);
    }


    public int releaseCalls() {
        return releaseCalls.get();
    }

    public int newBufferCalls() {
        return newBufferCalls.get();
    }

    public void clear() {
        QUEUE.clear();
        releaseCalls.set(0);
        newBufferCalls.set(0);
    }
}
