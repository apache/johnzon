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
package org.apache.johnzon.jsonb;

import org.apache.johnzon.mapper.Mapper;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbException;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.CharBuffer;
import java.util.Collection;

public class JohnsonJsonb implements Jsonb {
    private final Mapper delegate;

    public JohnsonJsonb(final Mapper build) {
        this.delegate = build;
    }

    @Override
    public <T> T fromJson(final String str, final Class<T> type) throws JsonbException {
        return delegate.readObject(str, type);
    }

    @Override
    public <T> T fromJson(final String str, final Type runtimeType) throws JsonbException {
        if (isArray(runtimeType)) {
            return (T) delegate.readArray(new StringReader(str), Class.class.cast(runtimeType).getComponentType());
        } else if (isCollection(runtimeType)) {
            return (T) delegate.readCollection(new StringReader(str), ParameterizedType.class.cast(runtimeType));
        }
        return delegate.readObject(str, runtimeType);
    }

    @Override
    public <T> T fromJson(final Readable readable, final Class<T> type) throws JsonbException {
        return delegate.readObject(toReader(readable), type);
    }

    @Override
    public <T> T fromJson(final Readable readable, final Type runtimeType) throws JsonbException {
        if (isArray(runtimeType)) {
            return (T) delegate.readArray(toReader(readable), Class.class.cast(runtimeType).getComponentType());
        } else if (isCollection(runtimeType)) {
            return (T) delegate.readCollection(toReader(readable), ParameterizedType.class.cast(runtimeType));
        }
        return delegate.readObject(toReader(readable), runtimeType);
    }

    @Override
    public <T> T fromJson(final InputStream stream, final Class<T> type) throws JsonbException {
        return delegate.readObject(stream, type);
    }

    @Override
    public <T> T fromJson(final InputStream stream, final Type runtimeType) throws JsonbException {
        if (isArray(runtimeType)) {
            return (T) delegate.readArray(stream, Class.class.cast(runtimeType).getComponentType());
        } else if (isCollection(runtimeType)) {
            return (T) delegate.readCollection(stream, ParameterizedType.class.cast(runtimeType));
        }
        return delegate.readObject(stream, runtimeType);
    }

    @Override
    public String toJson(final Object object) throws JsonbException {
        if (isArray(object.getClass())) {
            return delegate.writeArrayAsString((Object[]) object);
        } else if (Collection.class.isInstance(object)) {
            return delegate.writeArrayAsString(Collection.class.cast(object));
        }
        return delegate.writeObjectAsString(object);
    }

    @Override
    public String toJson(final Object object, final Type runtimeType) throws JsonbException {
        if (isArray(runtimeType)) {
            return delegate.writeArrayAsString((Object[]) object);
        } else if (isCollection(runtimeType)) {
            return delegate.writeArrayAsString(Collection.class.cast(object));
        }
        return delegate.writeObjectAsString(object);
    }

    @Override
    public void toJson(final Object object, final Appendable appendable) throws JsonbException {
        if (isArray(object.getClass())) {
            delegate.writeArray((Object[]) object, toWriter(appendable));
        } else if (Collection.class.isInstance(object)) {
            delegate.writeArray(Collection.class.cast(object), toWriter(appendable));
        } else {
            delegate.writeObject(object, toWriter(appendable));
        }
    }

    @Override
    public void toJson(final Object object, final Type runtimeType, final Appendable appendable) throws JsonbException {
        if (isArray(runtimeType)) {
            delegate.writeArray((Object[]) object, toWriter(appendable));
        } else if (isCollection(runtimeType)) {
            delegate.writeArray(Collection.class.cast(object), toWriter(appendable));
        } else {
            delegate.writeObject(object, toWriter(appendable));
        }
    }

    @Override
    public void toJson(final Object object, final OutputStream stream) throws JsonbException {
        if (isArray(object.getClass())) {
            delegate.writeArray((Object[]) object, stream);
        } else if (Collection.class.isInstance(object)) {
            delegate.writeArray(Collection.class.cast(object), stream);
        } else {
            delegate.writeObject(object, stream);
        }
    }

    @Override
    public void toJson(final Object object, final Type runtimeType, final OutputStream stream) throws JsonbException {
        if (isArray(runtimeType)) {
            delegate.writeArray((Object[]) object, stream);
        } else if (isCollection(runtimeType)) {
            delegate.writeArray(Collection.class.cast(object), stream);
        } else {
            delegate.writeObject(object, stream);
        }
    }

    private boolean isArray(final Type runtimeType) {
        return Class.class.isInstance(runtimeType) && Class.class.cast(runtimeType).isArray();
    }

    private boolean isCollection(final Type runtimeType) {
        if (!ParameterizedType.class.isInstance(runtimeType)) {
            return false;
        }
        final Type rawType = ParameterizedType.class.cast(runtimeType).getRawType();
        return Class.class.isInstance(rawType) && Collection.class.isAssignableFrom(Class.class.cast(rawType));
    }

    private Writer toWriter(final Appendable appendable) {
        return Writer.class.isInstance(appendable) ? Writer.class.cast(appendable) :
            new Writer() {
                @Override
                public void write(final char[] cbuf, final int off, final int len) throws IOException {
                    appendable.append(new String(cbuf, off, len));
                }

                @Override
                public void flush() throws IOException {
                    if (Flushable.class.isInstance(appendable)) {
                        Flushable.class.cast(appendable);
                    }
                }

                @Override
                public void close() throws IOException {
                    if (Closeable.class.isInstance(appendable)) {
                        Closeable.class.cast(appendable);
                    }
                }
            };
    }

    private Reader toReader(final Readable readable) {
        return Reader.class.isInstance(readable) ? Reader.class.cast(readable) :
            new Reader() {
                @Override
                public int read(final char[] cbuf, final int off, final int len) throws IOException {
                    int r;
                    final CharBuffer cb = CharBuffer.allocate(len);
                    while ((r = readable.read(cb)) >= 0) {
                        System.arraycopy(cb.array(), 0, cbuf, off, r);
                    }
                    return readable.read(CharBuffer.allocate(len));
                }

                @Override
                public void close() throws IOException {
                    if (Closeable.class.isInstance(readable)) {
                        Closeable.class.cast(readable);
                    }
                }
            };
    }
}
