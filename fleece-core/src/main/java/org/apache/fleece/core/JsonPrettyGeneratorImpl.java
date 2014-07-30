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
package org.apache.fleece.core;

import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentMap;

import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

final class JsonPrettyGeneratorImpl extends JsonGeneratorImpl {
    private static final String DEFAULT_INDENTATION = "  ";
    private final String indent;

    public JsonPrettyGeneratorImpl(final Writer writer, final BufferStrategy.BufferProvider<char[]> bufferProvider,
            final ConcurrentMap<String, String> cache) {
        super(writer, bufferProvider, cache);
        indent = DEFAULT_INDENTATION;
    }

    public JsonPrettyGeneratorImpl(final OutputStream out, final Charset encoding,
            final BufferStrategy.BufferProvider<char[]> bufferProvider, final ConcurrentMap<String, String> cache) {
        super(out, encoding, bufferProvider, cache);
        indent = DEFAULT_INDENTATION;
    }

    public JsonPrettyGeneratorImpl(final OutputStream out, final BufferStrategy.BufferProvider<char[]> bufferProvider,
            final ConcurrentMap<String, String> cache) {
        super(out, bufferProvider, cache);
        indent = DEFAULT_INDENTATION;
    }

    private void writeEOL() {
        justWrite(EOL);
    }

    private void writeIndent(final int correctionOffset) {
        for (int i = 0; i < depth + correctionOffset; i++) {
            justWrite(indent);
        }
    }

    @Override
    protected void addCommaIfNeeded() {
        if (needComma) {
            justWrite(COMMA_CHAR);
            writeEOL();
            writeIndent(0);
            needComma = false;
        }

    }

    @Override
    public JsonGenerator writeStartObject() {
        if (depth > 0 && !needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.writeStartObject();

    }

    @Override
    public JsonGenerator writeStartObject(final String name) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.writeStartObject(name);

    }

    @Override
    public JsonGenerator writeStartArray() {
        if (depth > 0 && !needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.writeStartArray();

    }

    @Override
    public JsonGenerator writeStartArray(final String name) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.writeStartArray(name);

    }

    //end

    @Override
    public JsonGenerator writeEnd() {
        writeEOL();
        writeIndent(-1);
        return super.writeEnd();

    }

    //normal

    @Override
    public JsonGenerator write(final String name, final JsonValue value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }

        return super.write(name, value);

    }

    @Override
    public JsonGenerator write(final String name, final String value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(name, value);
    }

    @Override
    public JsonGenerator write(final String name, final BigInteger value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(name, value);

    }

    @Override
    public JsonGenerator write(final String name, final BigDecimal value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(name, value);

    }

    @Override
    public JsonGenerator write(final String name, final int value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(name, value);

    }

    @Override
    public JsonGenerator write(final String name, final long value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(name, value);

    }

    @Override
    public JsonGenerator write(final String name, final double value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(name, value);

    }

    @Override
    public JsonGenerator write(final String name, final boolean value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(name, value);

    }

    @Override
    public JsonGenerator writeNull(final String name) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.writeNull(name);

    }

    @Override
    public JsonGenerator write(final JsonValue value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(value);

    }

    @Override
    public JsonGenerator write(final String value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(value);

    }

    @Override
    public JsonGenerator write(final BigDecimal value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(value);

    }

    @Override
    public JsonGenerator write(final BigInteger value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(value);

    }

    @Override
    public JsonGenerator write(final int value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(value);

    }

    @Override
    public JsonGenerator write(final long value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(value);

    }

    @Override
    public JsonGenerator write(final double value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(value);

    }

    @Override
    public JsonGenerator write(final boolean value) {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.write(value);

    }

    @Override
    public JsonGenerator writeNull() {
        if (!needComma) {
            writeEOL();
            writeIndent(0);
        }
        return super.writeNull();
    }

}
