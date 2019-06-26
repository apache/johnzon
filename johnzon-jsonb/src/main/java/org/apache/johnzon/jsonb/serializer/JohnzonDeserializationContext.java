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

import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;

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
        final JsonParser.Event next = parser.next();
        switch (next) {
            case START_OBJECT:
                final JsonObjectBuilder objectBuilder = builderFactory.createObjectBuilder();
                parseObject(parser, objectBuilder);
                if (parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return objectBuilder.build();
            case START_ARRAY:
                final JsonArrayBuilder arrayBuilder = builderFactory.createArrayBuilder();
                parseArray(parser, arrayBuilder);
                if (parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return arrayBuilder.build();
            case VALUE_STRING:
                final JsonString string = jsonp.createValue(parser.getString());
                if (parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return string;
            case VALUE_FALSE:
                if (parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return JsonValue.FALSE;
            case VALUE_TRUE:
                if (parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return JsonValue.TRUE;
            case VALUE_NULL:
                if (parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return JsonValue.NULL;
            case VALUE_NUMBER:
                final JsonNumber number = jsonp.createValue(parser.getBigDecimal());
                if (parser.hasNext()) {
                    throw new JsonParsingException("Expected end of file", parser.getLocation());
                }
                return number;
            default:
                throw new JsonParsingException("Unknown structure: " + next, parser.getLocation());
        }
    }

    private void parseObject(final JsonParser parser, final JsonObjectBuilder builder) {
        String key = null;
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
                    parseObject(parser, subObject);
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
                    parseObject(parser, subObject);
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
