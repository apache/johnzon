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
package org.apache.johnzon.jsonb.extras.polymorphism;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public final class Polymorphic {
    private Polymorphic() {
        // no-op
    }

    private static String getId(final Class<?> type) {
        final JsonId mapping = type.getAnnotation(JsonId.class);
        if (mapping == null) {
            throw new IllegalArgumentException("No @Id on " + type);
        }
        final String id = mapping.value();
        return id.isEmpty() ? type.getSimpleName() : id;
    }

    public static class Serializer<T> implements JsonbSerializer<T> {
        private transient volatile ConcurrentMap<Class<?>, String> idMapping = new ConcurrentHashMap<>();

        @Override
        public void serialize(final T obj, final JsonGenerator generator, final SerializationContext ctx) {
            ensureInit();
            ctx.serialize(new Wrapper<>(getOrLoadId(obj), obj), generator);
        }

        private String getOrLoadId(final T obj) {
            final Class<?> type = obj.getClass();
            String id = idMapping.get(type);
            if (id == null) {
                id = getId(type);
                idMapping.putIfAbsent(type, id);
            }
            return id;
        }

        private void ensureInit() {
            if (idMapping == null) {
                synchronized (this) {
                    if (idMapping == null) {
                        idMapping = new ConcurrentHashMap<>();
                    }
                }
            }
        }
    }

    public static class DeSerializer<T> implements JsonbDeserializer<T> {
        private transient volatile ConcurrentMap<String, Type> classMapping = new ConcurrentHashMap<>();

        @Override
        public T deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
            ensureInit();
            if (classMapping == null || classMapping.isEmpty()) {
                synchronized (this) {
                    if (classMapping == null || classMapping.isEmpty()) {
                        loadMapping(rtType);
                    }
                }
            }
            if (!parser.hasNext()) {
                return null;
            }
            eatStartObject(parser);
            eatTypeKey(parser);
            final String typeId = getTypeValue(parser);
            eatValueStart(parser);
            final Type type = requireNonNull(classMapping.get(typeId), "No mapping for " + typeId);
            parser.next();
            return (T) ctx.deserialize(type, parser);
        }

        private void loadMapping(final Type rtType) {
            final Class<?> from;
            if (ParameterizedType.class.isInstance(rtType)) {
                final Type rawType = ParameterizedType.class.cast(rtType).getRawType();
                if (!Class.class.isInstance(rawType)) {
                    throw new IllegalStateException("Unsupported type: " + rawType);
                }
                from = Class.class.cast(rawType);
            } else if (Class.class.isInstance(rtType)) {
                from = Class.class.cast(rtType);
            } else {
                throw new IllegalStateException("Unsupported type: " + rtType);
            }

            final JsonChildren classes = from.getAnnotation(JsonChildren.class);
            if (classes == null) {
                throw new IllegalArgumentException("No @Classes on " + from);
            }

            classMapping.putAll(Stream.of(classes.value())
                    .collect(toMap(Polymorphic::getId, identity())));
        }

        private void eatStartObject(final JsonParser parser) {
            if (parser.next() != JsonParser.Event.START_OBJECT) {
                throw new IllegalArgumentException("Invalid JSON, expected START_OBJECT");
            }
        }

        private void eatTypeKey(final JsonParser parser) {
            if (!parser.hasNext() || parser.next() != JsonParser.Event.KEY_NAME) {
                throw new IllegalArgumentException("Invalid JSON, expected KEY_NAME");
            }
            if (!"_type".equals(parser.getString())) {
                throw new IllegalArgumentException("Expected key _type");
            }
        }

        private void eatValueStart(final JsonParser parser) {
            if (!parser.hasNext() || parser.next() != JsonParser.Event.KEY_NAME) {
                throw new IllegalArgumentException("Invalid JSON, expected KEY_NAME");
            }
            if (!parser.hasNext() || !"_value".equals(parser.getString())) {
                throw new IllegalArgumentException("Expected key _value");
            }
        }

        private String getTypeValue(final JsonParser parser) {
            final JsonParser.Event next = parser.next();
            if (!parser.hasNext() || next != JsonParser.Event.VALUE_STRING) {
                throw new IllegalArgumentException("Unexpected event " + next);
            }
            return parser.getString();
        }

        private void ensureInit() {
            if (classMapping == null) {
                synchronized (this) {
                    if (classMapping == null) {
                        classMapping = new ConcurrentHashMap<>();
                    }
                }
            }
        }
    }

    public static class Wrapper<T> {
        @JsonbProperty("_type")
        public String id;

        @JsonbProperty("_value")
        public T value;

        private Wrapper(final String id, final T obj) {
            this.id = id;
            this.value = obj;
        }
    }

    @Inherited
    @Retention(RUNTIME)
    public @interface JsonChildren {
        /**
         * @return the list of leaf classes which can be instantiated by the children.
         */
        Class<?>[] value();
    }

    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface JsonId {
        String value() default "";
    }
}
