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
package org.apache.johnzon.jaxrs.jsonb.jaxrs;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NoContentException;
import javax.ws.rs.ext.MessageBodyReader;

import org.junit.Test;

public final class JsonbJaxrsProviderTest {

    @Test(expected = NoContentException.class)
    public final void shouldThrowNoContentException() throws IOException {
        // given
        final MessageBodyReader<Foo> mbr = new JsonbJaxrsProvider<>();

        // when
        mbr.readFrom(Foo.class, Foo.class, new Annotation[0], APPLICATION_JSON_TYPE,
                new EmptyMultivaluedMap<String, String>(), new ByteArrayInputStream(new byte[0]));

        // then
        // should throw NoContentException
    }

    private static final class Foo {
        // no members
    }

    private static final class EmptyMultivaluedMap<K, V> implements MultivaluedMap<K, V> {

        @Override
        public final int size() {
            return 0;
        }

        @Override
        public final boolean isEmpty() {
            return true;
        }

        @Override
        public final boolean containsKey(final Object key) {
            return false;
        }

        @Override
        public final boolean containsValue(final Object value) {
            return false;
        }

        @Override
        public final List<V> get(final Object key) {
            return null;
        }

        @Override
        public final List<V> put(final K key, final List<V> value) {
            return null;
        }

        @Override
        public final List<V> remove(final Object key) {
            return null;
        }

        @Override
        public final void putAll(final Map<? extends K, ? extends List<V>> m) {
        }

        @Override
        public final void clear() {
        }

        @Override
        public final Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public final Collection<List<V>> values() {
            return Collections.emptySet();
        }

        @Override
        public final Set<Entry<K, List<V>>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public final void putSingle(final K key, final V value) {
        }

        @Override
        public final void add(final K key, final V value) {
        }

        @Override
        public final V getFirst(final K key) {
            return null;
        }

        @Override
        public final void addAll(final K key, final V... newValues) {
        }

        @Override
        public final void addAll(final K key, final List<V> valueList) {
        }

        @Override
        public final void addFirst(final K key, final V value) {
        }

        @Override
        public final boolean equalsIgnoreValueOrder(final MultivaluedMap<K, V> otherMap) {
            return false;
        }

    }

}
