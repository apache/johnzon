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

import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ConcurrentMap;

public class JsonPrettyGeneratorImpl extends JsonGeneratorImpl<JsonPrettyGeneratorImpl> {
    private static final String DEFAULT_INDENTATION = "  ";

    private String indent; // should be final but writeEnd needs it not final, we could change write() to support indent

    public JsonPrettyGeneratorImpl(final Writer writer, final ConcurrentMap<String, String> cache) {
        this(writer, null, false, "", cache);
    }

    public JsonPrettyGeneratorImpl(final Writer writer, final JsonPrettyGeneratorImpl parent,
                                   final boolean array, final String prefix,
                                   final ConcurrentMap<String, String> cache) {
        super(writer, parent, array, cache);
        this.indent = prefix;
    }

    private void addCommaIfNeeded() {
        if (needComma) {
            try {
                writer.write(',');
                ln();
            } catch (final IOException e) {
                throw new JsonGenerationException(e.getMessage(), e);
            }
            needComma = false;
        }
    }

    private void ln() {
        try {
            writer.write('\n');
        } catch (final IOException e) {
            throw new JsonGenerationException(e.getMessage(), e);
        }
    }

    private String nextIndent() {
        return indent != null ? indent + DEFAULT_INDENTATION : null;
    }

    @Override
    protected JsonGenerator newJsonGenerator(final Writer writer, final JsonPrettyGeneratorImpl parent, final boolean array) {
        return new JsonPrettyGeneratorImpl(writer, parent, array, nextIndent(), cache);
    }

    @Override
    public JsonGenerator writeStartObject() {
        final JsonGenerator generator = super.writeStartObject();
        ln();
        return generator;
    }

    @Override
    public JsonGenerator writeStartObject(final String name) {
        final JsonGenerator generator = super.writeStartObject(name);
        ln();
        return generator;
    }

    @Override
    public JsonGenerator writeStartArray() {
        final JsonGenerator generator = super.writeStartArray();
        ln();
        return generator;
    }

    @Override
    public JsonGenerator writeStartArray(final String name) {
        final JsonGenerator generator = super.writeStartArray(name);
        ln();
        return generator;
    }

    @Override
    public JsonGenerator writeEnd() {
        ln();
        needComma = false;
        final String thisIndent = indent;
        indent = parent != null ? parent.indent : indent;
        noCheckWriteAndForceComma(array ? "]" : "}");
        indent = thisIndent;
        return parent != null ? parent : this;
    }

    @Override
    protected JsonGenerator noCheckWriteAndForceComma(final String value) {
        addCommaIfNeeded();
        try {
            if (indent != null) {
                writer.write(indent);
            }
            writer.write(value);
        } catch (final IOException e) {
            throw new JsonGenerationException(e.getMessage(), e);
        }
        needComma = true;
        return this;
    }
}
