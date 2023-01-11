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
package org.apache.johnzon.jsonb.serializer;

import java.lang.reflect.Type;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParsingException;

import org.apache.johnzon.mapper.MappingParser;

public class JohnzonDeserializationContext implements DeserializationContext {
    private final MappingParser runtime;
    private final JsonBuilderFactory builderFactory;
    private final JsonProvider jsonp;

    public JohnzonDeserializationContext(final MappingParser runtime,
                                         final JsonBuilderFactory builderFactory,
                                         final JsonProvider jsonp) {
        this.runtime = runtime;
        this.builderFactory = builderFactory;
        this.jsonp = jsonp;
    }

    @Override
    public <T> T deserialize(final Class<T> clazz, final JsonParser parser) {
        return runtime.readObject(read(parser), clazz);
    }

    @Override
    public <T> T deserialize(final Type type, final JsonParser parser) {
        return runtime.readObject(read(parser), type);
    }

    private JsonValue read(final JsonParser parser) {
        final JsonParser.Event next = /*RewindableJsonParser.class.isInstance(parser) ?
                RewindableJsonParser.class.cast(parser).getLast() : */ parser.next();
        switch (next) {
            case START_OBJECT: {
                final JsonObjectBuilder objectBuilder = builderFactory.createObjectBuilder();
                parseObject(null, parser, objectBuilder);
                return objectBuilder.build();
            }
            case END_OBJECT:
                return JsonValue.EMPTY_JSON_OBJECT;
            case END_ARRAY:
                return JsonValue.EMPTY_JSON_ARRAY;
            case START_ARRAY:
                final JsonArrayBuilder arrayBuilder = builderFactory.createArrayBuilder();
                parseArray(parser, arrayBuilder);
                return arrayBuilder.build();
            case KEY_NAME: { // object
                final JsonObjectBuilder objectBuilder = builderFactory.createObjectBuilder();
                parseObject(parser.getString(), parser, objectBuilder);
                return objectBuilder.build();
            }
            case VALUE_STRING:
                return jsonp.createValue(parser.getString());
            case VALUE_FALSE:
                return JsonValue.FALSE;
            case VALUE_TRUE:
                return JsonValue.TRUE;
            case VALUE_NULL:
                return JsonValue.NULL;
            case VALUE_NUMBER:
                return jsonp.createValue(parser.getBigDecimal());
            default:
                throw new JsonParsingException("Unknown structure: " + next, parser.getLocation());
        }
    }

    private void parseObject(final String originalKey, final JsonParser parser, final JsonObjectBuilder builder) {
        String key = originalKey;
        while (parser.hasNext()) {
            final JsonParser.Event next = parser.next();
            switch (next) {
                case KEY_NAME:
                    key = parser.getString();
                    break;

                case VALUE_STRING:
                    builder.add(key, jsonp.createValue(parser.getString()));
                    break;

                case START_OBJECT:
                    final JsonObjectBuilder subObject = builderFactory.createObjectBuilder();
                    parseObject(null, parser, subObject);
                    builder.add(key, subObject);
                    break;

                case START_ARRAY:
                    final JsonArrayBuilder subArray = builderFactory.createArrayBuilder();
                    parseArray(parser, subArray);
                    builder.add(key, subArray);
                    break;

                case VALUE_NUMBER:
                    if (parser.isIntegralNumber()) {
                        builder.add(key, jsonp.createValue(parser.getLong()));
                    } else {
                        builder.add(key, jsonp.createValue(parser.getBigDecimal()));
                    }
                    break;

                case VALUE_NULL:
                    builder.addNull(key);
                    break;

                case VALUE_TRUE:
                    builder.add(key, true);
                    break;

                case VALUE_FALSE:
                    builder.add(key, false);
                    break;

                case END_OBJECT:
                    return;

                case END_ARRAY:
                    throw new JsonParsingException("']', shouldn't occur", parser.getLocation());

                default:
                    throw new JsonParsingException(next.name() + ", shouldn't occur", parser.getLocation());
            }
        }
    }

    private void parseArray(final JsonParser parser, final JsonArrayBuilder builder) {
        while (parser.hasNext()) {
            final JsonParser.Event next = parser.next();
            switch (next) {
                case VALUE_STRING:
                    builder.add(jsonp.createValue(parser.getString()));
                    break;

                case VALUE_NUMBER:
                    if (parser.isIntegralNumber()) {
                        builder.add(jsonp.createValue(parser.getLong()));
                    } else {
                        builder.add(jsonp.createValue(parser.getBigDecimal()));
                    }
                    break;

                case START_OBJECT:
                    final JsonObjectBuilder subObject = builderFactory.createObjectBuilder();
                    parseObject(null, parser, subObject);
                    builder.add(subObject);
                    break;

                case START_ARRAY:
                    final JsonArrayBuilder subArray = builderFactory.createArrayBuilder();
                    parseArray(parser, subArray);
                    builder.add(subArray);
                    break;

                case END_ARRAY:
                    return;

                case VALUE_NULL:
                    builder.addNull();
                    break;

                case VALUE_TRUE:
                    builder.add(true);
                    break;

                case VALUE_FALSE:
                    builder.add(false);
                    break;

                case KEY_NAME:
                    throw new JsonParsingException("array doesn't have keys", parser.getLocation());

                case END_OBJECT:
                    throw new JsonParsingException("'}', shouldn't occur", parser.getLocation());

                default:
                    throw new JsonParsingException(next.name() + ", shouldn't occur", parser.getLocation());
            }
        }
    }
}
