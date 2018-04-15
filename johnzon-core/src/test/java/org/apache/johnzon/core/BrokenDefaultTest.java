/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.core;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Queue;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;

import org.junit.Ignore;
import org.junit.Test;

public class BrokenDefaultTest {

    @Test
    @Ignore("buggy but pushing to share the use case")
    public void run() throws NoSuchFieldException, IllegalAccessException, UnsupportedEncodingException { // shouldnt fail by default
        final JsonParserFactory factory = Json.createParserFactory(Collections.EMPTY_MAP);
        final int length = 1024 * 1024;
        assertEquals(0, get(Queue.class, get(
                BufferStrategy.BufferProvider.class, factory, "bufferProvider"), "queue").size());

        final JsonParser parser = factory.createParser(newDynamicInput(length));

        try {
            int eventCount = 0;
            while (parser.hasNext()) {
                eventCount++;
                final JsonParser.Event next = parser.next();
                if (eventCount == 2 && next == JsonParser.Event.VALUE_STRING) {
                    assertEquals(length, parser.getString().length());
                }
            }
        } finally {
            parser.close();
        }

        assertEquals(1, get(Queue.class, get(
                BufferStrategy.BufferProvider.class, factory, "bufferProvider"), "queue").size());
    }

    private <T> T get(final Class<T> returnType, final Object instance, final String field)
            throws NoSuchFieldException, IllegalAccessException {
        Class<?> current = instance.getClass();
        while (current != Object.class) {
            try {
                final Field declaredField = current.getDeclaredField(field);
                if (!declaredField.isAccessible()) {
                    declaredField.setAccessible(true);
                }
                return returnType.cast(declaredField.get(instance));
            } catch (final NoSuchFieldException nsfe) {
                current = current.getSuperclass();
            }
        }
        throw new IllegalAccessError(instance + " field: " + field);
    }

    private InputStream newDynamicInput(final int size) throws UnsupportedEncodingException {
        return new InputStream() {

            private InputStream before = new ByteArrayInputStream("{\"key\":\"".getBytes("UTF-8"));

            private InputStream after = new ByteArrayInputStream("\"}".getBytes("UTF-8"));

            private int remaining = size;

            @Override
            public int read() throws IOException {
                {
                    final int val = before.read();
                    if (val >= 0) {
                        return val;
                    }
                }
                if (remaining < 0) {
                    return after.read();
                }
                remaining--;
                return 'a';
            }
        };
    }
}
