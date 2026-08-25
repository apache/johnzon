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
package org.apache.johnzon.mapper.jsonp;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonLocation;
import jakarta.json.stream.JsonParser;

public class RewindableJsonParser implements JsonParser {
    private final JsonParser delegate;
    private Event last;
    private boolean pendingRewind;

    public RewindableJsonParser(final JsonParser delegate) {
        this.delegate = delegate;
    }

    public Event getLast() {
        return last;
    }

    @Override
    public boolean hasNext() {
        return pendingRewind || delegate.hasNext();
    }

    @Override
    public Event next() {
        if (pendingRewind) {
            pendingRewind = false;
            return last;
        }
        return last = delegate.next();
    }

    @Override
    public Event currentEvent() {
        if (last == null) {
            if (!delegate.hasNext()) { // value adapters without event stream
                return delegate.currentEvent();
            }
            last = delegate.next();
            pendingRewind = true;
        }
        return last;
    }

    @Override
    public String getString() {
        return delegate.getString();
    }

    @Override
    public boolean isIntegralNumber() {
        return delegate.isIntegralNumber();
    }

    @Override
    public int getInt() {
        return delegate.getInt();
    }

    @Override
    public long getLong() {
        return delegate.getLong();
    }

    @Override
    public BigDecimal getBigDecimal() {
        return delegate.getBigDecimal();
    }

    @Override
    public JsonLocation getLocation() {
        return delegate.getLocation();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public JsonObject getObject() {
        return trackResult(delegate::getObject);
    }

    @Override
    public JsonValue getValue() {
        return trackResult(delegate::getValue);
    }

    @Override
    public JsonArray getArray() {
        return trackResult(delegate::getArray);
    }

    @Override
    public Stream<JsonValue> getArrayStream() {
        return trackStream(delegate.getArrayStream());
    }

    @Override
    public Stream<Map.Entry<String, JsonValue>> getObjectStream() {
        return trackStream(delegate.getObjectStream());
    }

    @Override
    public Stream<JsonValue> getValueStream() {
        return trackStream(delegate.getValueStream());
    }

    @Override
    public void skipArray() {
        trackAdvance(delegate::skipArray);
    }

    @Override
    public void skipObject() {
        trackAdvance(delegate::skipObject);
    }

    private <T> T trackResult(final Supplier<T> operation) {
        pendingRewind = false;
        final T result = operation.get();
        last = delegate.currentEvent();
        return result;
    }

    private void trackAdvance(final Runnable operation) {
        pendingRewind = false;
        operation.run();
        last = delegate.currentEvent();
    }

    private <T> Stream<T> trackStream(final Stream<T> stream) {
        pendingRewind = false;
        last = delegate.currentEvent();
        final boolean parallel = stream.isParallel();
        final Spliterator<T> spliterator = stream.spliterator();
        final Spliterator<T> tracking = new Spliterators.AbstractSpliterator<T>(
                spliterator.estimateSize(), spliterator.characteristics()) {
            @Override
            public boolean tryAdvance(final Consumer<? super T> action) {
                try {
                    return spliterator.tryAdvance(action);
                } finally {
                    last = delegate.currentEvent();
                }
            }
        };
        return StreamSupport.stream(tracking, parallel).onClose(stream::close);
    }
}
