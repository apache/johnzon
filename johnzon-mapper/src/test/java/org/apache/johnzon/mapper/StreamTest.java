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
package org.apache.johnzon.mapper;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;

public class StreamTest {
    private final Mapper mapper = new MapperBuilder().setAttributeOrder(String::compareTo).build();

    @Test
    public void roundTrip() {
        final String json = "{\"ints\":[0,1,2,3,4],\"strings\":[\"a\",\"b\",\"c\"]}";
        final ILoveStreams instance = new ILoveStreams();
        assertEquals(json, mapper.writeObjectAsString(instance));
        instance.ints = null;
        instance.strings = null;
        assertEquals("{}", mapper.writeObjectAsString(instance));
        final ILoveStreams deserialized = mapper.readObject(json, ILoveStreams.class);
        assertEquals(asList("a", "b", "c"), deserialized.strings.collect(toList()));
        assertArrayEquals(IntStream.range(0, 5).toArray(), deserialized.ints.toArray());
    }

    public static class ILoveStreams {
        public Stream<String> strings = Stream.of("a", "b", "c");
        public IntStream ints = IntStream.range(0, 5);
    }
}
