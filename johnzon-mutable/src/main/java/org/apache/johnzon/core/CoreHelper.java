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

package org.apache.johnzon.core;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.apache.johnzon.mutable.MutableJsonStructure;
import org.apache.johnzon.mutable.MutableJsonStructure.Ancestor;

@Experimental
public class CoreHelper {

    protected CoreHelper() {
        
    }
    
    static JsonString createJsonString(final String value) {
        return new JsonStringImpl(value);
    }

    static JsonNumber createJsonNumber(final Number number) {

        if (number instanceof BigDecimal) {
            return new JsonNumberImpl((BigDecimal) number);
        } else if (number instanceof BigInteger) {
            return new JsonNumberImpl(new BigDecimal(((BigInteger) number)));
        } else if (number instanceof Double) {
            return new JsonDoubleImpl((Double) number);
        } else if (number instanceof Float) {
            return new JsonDoubleImpl((Float) number);
        } else if (number instanceof Long) {
            return new JsonLongImpl((Long) number);
        } else {
            return new JsonLongImpl(number.intValue());
        }

    }

    protected static MutableJsonStructure toMutableJsonStructure0(final JsonStructure structure) {
        if (structure instanceof JsonArray) {
            return new MutableJsonArray((JsonArray) structure, null);
        }

        return new MutableJsonObject((JsonObject) structure, null);
    }

    static MutableJsonStructure toMutableJsonStructure(final JsonStructure structure, final Ancestor ac) {
        if (structure instanceof JsonArray) {
            return new MutableJsonArray((JsonArray) structure, ac);
        }

        return new MutableJsonObject((JsonObject) structure, ac);
    }

    /**
     * Create new empty mutable JSON object
     * 
     * @return new empty mutable JSON object
     */
    protected static MutableJsonStructure createNewMutableObject0() {
        // implementation is not performant, could be done better
        return toMutableJsonStructure0(JsonValue.EMPTY_JSON_OBJECT).copy();
    }

    /**
     * Create new empty mutable JSON array
     * 
     * @return new empty mutable JSON array
     */
    protected static MutableJsonStructure createNewMutableArray0() {
        // implementation is not performant, could be done better
        return toMutableJsonStructure0(JsonValue.EMPTY_JSON_ARRAY).copy();
    }

}
