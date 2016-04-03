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

import javax.json.JsonNumber;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonValue;

public class ReaderHandler {
    private final boolean johnzon;

    private ReaderHandler(final boolean johnzon) {
        this.johnzon = johnzon;
    }

    public static ReaderHandler create(final JsonReaderFactory readerFactory) {
        if (readerFactory.getClass().getName().equals("org.apache.johnzon.core.JsonReaderFactoryImpl")) {
            return new ReaderHandler(true);
        }
        return new ReaderHandler(false);
    }

    public JsonValue read(final JsonReader reader) {
        if (johnzon) {
            return JohnzonReaderHandler.read(reader);
        }
        return reader.read();
    }

    public boolean isJsonLong(final JsonNumber number) {
        if (johnzon) {
            return JohnzonReaderHandler.isLong(number);
        }
        return false; // will be slower but not a big deal
    }
}
