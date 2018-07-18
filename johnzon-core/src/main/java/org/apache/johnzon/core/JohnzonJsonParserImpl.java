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


import java.util.Map;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * Base parser which handles higher level operations which are
 * mixtures of Reader and Parsers like {@code getObject(), getValue(), getArray()}
 */
public abstract class JohnzonJsonParserImpl implements JohnzonJsonParser {

    /**
     * @return {@code true} if we are currently inside an array
     */
    protected abstract boolean isInArray();

    @Override
    public JsonObject getObject() {
        Event current = current();
        if (current != Event.START_OBJECT) {
            throw new IllegalStateException(current + " doesn't support getObject()");
        }

        JsonReaderImpl jsonReader = new JsonReaderImpl(this, true);
        return jsonReader.readObject();
    }


    @Override
    public JsonArray getArray() {
        Event current = current();
        if (current != Event.START_ARRAY) {
            throw new IllegalStateException(current + " doesn't support getArray()");
        }

        JsonReaderImpl jsonReader = new JsonReaderImpl(this, true);
        return jsonReader.readArray();
    }

    @Override
    public JsonValue getValue() {
        Event current = current();
        switch (current) {
            case START_ARRAY:
            case START_OBJECT:
                JsonReaderImpl jsonReader = new JsonReaderImpl(this, true);
                return jsonReader.readValue();
            case VALUE_TRUE:
                return JsonValue.TRUE;
            case VALUE_FALSE:
                return JsonValue.FALSE;
            case VALUE_NULL:
                return JsonValue.NULL;
            case VALUE_STRING:
            case KEY_NAME:
                return new JsonStringImpl(getString());
            case VALUE_NUMBER:
                if (isIntegralNumber()) {
                    return new JsonLongImpl(getLong());
                } else {
                    return new JsonNumberImpl(getBigDecimal());
                }
            default:
                throw new IllegalStateException(current + " doesn't support getValue()");
        }
    }

    @Override
    public void skipObject() {
        int level = 1;
        do {
            Event event = next();
            if (event == Event.START_OBJECT) {
                level++;
            } else if (event == Event.END_OBJECT) {
                level --;
            }
        } while (level > 0 && hasNext());
    }

    @Override
    public void skipArray() {
        if (isInArray()) {
            int level = 1;
            do {
                Event event = next();
                if (event == Event.START_ARRAY) {
                    level++;
                } else if (event == Event.END_ARRAY) {
                    level--;
                }
            } while (level > 0 && hasNext());
        }
    }

    @Override
    public Stream<JsonValue> getArrayStream() {
        //X TODO this implementation is very simplistic
        //X I find it unintuitive what the spec intends here
        //X we probably need to improve this
        return getArray().stream();
    }

    @Override
    public Stream<Map.Entry<String, JsonValue>> getObjectStream() {
        //X TODO this implementation is very simplistic
        //X I find it unintuitive what the spec intends here
        //X we probably need to improve this
        return getObject().entrySet().stream();
    }

    @Override
    public Stream<JsonValue> getValueStream() {
        //X TODO this implementation is very simplistic
        //X I find it unintuitive what the spec intends here
        //X we probably need to improve this
        Event current = current();
        if (current  == Event.START_ARRAY) {
            return getArrayStream();
        }
        if (current  == Event.START_OBJECT) {
            return getObject().values().stream();
        }
        throw new IllegalStateException(current + " doesn't support getValueStream");
    }
}
