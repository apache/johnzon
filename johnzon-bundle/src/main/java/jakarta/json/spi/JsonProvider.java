/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package jakarta.json.spi;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonPatch;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonPointer;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public abstract class JsonProvider {
    private static final class Holder {
        private static final JsonProvider DEFAULT = new org.apache.johnzon.core.JsonProviderImpl();
    }
    
    protected JsonProvider() {
        // no-op
    }

    public static JsonProvider provider() {
        return Holder.DEFAULT;
    }

    public abstract JsonParser createParser(Reader reader);

    public abstract JsonParser createParser(InputStream in);

    public abstract JsonParserFactory createParserFactory(Map<String, ?> config);

    public abstract JsonGenerator createGenerator(Writer writer);

    public abstract JsonGenerator createGenerator(OutputStream out);

    public abstract JsonGeneratorFactory createGeneratorFactory(Map<String, ?> config);

    public abstract JsonReader createReader(Reader reader);

    public abstract JsonReader createReader(InputStream in);

    public abstract JsonWriter createWriter(Writer writer);

    public abstract JsonWriter createWriter(OutputStream out);

    public abstract JsonWriterFactory createWriterFactory(Map<String,?> config);

    public abstract JsonReaderFactory createReaderFactory(Map<String,?> config);

    public abstract JsonObjectBuilder createObjectBuilder();

    public JsonObjectBuilder createObjectBuilder(JsonObject object) {
        throw new UnsupportedOperationException();
    }

    public JsonObjectBuilder createObjectBuilder(Map<String, ?> map) {
        throw new UnsupportedOperationException();
    }

    public abstract JsonArrayBuilder createArrayBuilder();

    public JsonArrayBuilder createArrayBuilder(JsonArray array) {
        throw new UnsupportedOperationException();
    }

    public JsonPointer createPointer(String jsonPointer) {
        throw new UnsupportedOperationException();
    }

    public JsonPatchBuilder createPatchBuilder() {
        throw new UnsupportedOperationException();
    }

    public JsonPatchBuilder createPatchBuilder(JsonArray array) {
        throw new UnsupportedOperationException();
    }

    public JsonPatch createPatch(JsonArray array) {
        throw new UnsupportedOperationException();
    }

    public JsonPatch createDiff(JsonStructure source, JsonStructure target) {
        throw new UnsupportedOperationException();
    }

    public JsonMergePatch createMergePatch(JsonValue patch) {
        throw new UnsupportedOperationException();
    }

    public JsonMergePatch createMergeDiff(JsonValue source, JsonValue target) {
        throw new UnsupportedOperationException();
    }

    public JsonArrayBuilder createArrayBuilder(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    public abstract JsonBuilderFactory createBuilderFactory(Map<String,?> config);

    public JsonString createValue(String value) {
        throw new UnsupportedOperationException();
    }

    public JsonNumber createValue(int value) {
        throw new UnsupportedOperationException();
    }

    public JsonNumber createValue(long value) {
        throw new UnsupportedOperationException();
    }

    public JsonNumber createValue(double value) {
        throw new UnsupportedOperationException();
    }

    public JsonNumber createValue(BigDecimal value) {
        throw new UnsupportedOperationException();
    }

    public JsonNumber createValue(BigInteger value) {
        throw new UnsupportedOperationException();
    }

    public JsonNumber createValue(Number number) {
        if (number instanceof Integer) {
            return createValue(number.intValue());
        } else if (number instanceof Long) {
            return createValue(number.longValue());
        } else if (number instanceof Double) {
            return createValue(number.doubleValue());
        } else if (number instanceof BigInteger) {
            return createValue((BigInteger) number);
        } else if (number instanceof BigDecimal) {
            return createValue((BigDecimal) number);
        } else {
            throw new UnsupportedOperationException(number + " type is not known");
        }
    }
}

