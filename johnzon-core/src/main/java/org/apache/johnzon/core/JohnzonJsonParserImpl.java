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

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * Base parser which handles higher level operations which are
 * mixtures of Reader and Parsers like {@code getObject(), getValue(), getArray()}
 */
public abstract class JohnzonJsonParserImpl implements JohnzonJsonParser {

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
        if (current != Event.START_ARRAY && current != Event.START_OBJECT) {
            throw new IllegalStateException(current + " doesn't support getArray()");
        }

        JsonReaderImpl jsonReader = new JsonReaderImpl(this, true);
        return jsonReader.readValue();
    }

    @Override
    public void skipObject() {
        // could surely get improved.
        // But no need for now as this method is not used that often.
        getObject();
    }

    @Override
    public void skipArray() {
        // could surely get improved.
        // But no need for now as this method is not used that often.
        getArray();
    }
}
