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

import java.io.Serializable;

import javax.json.stream.JsonLocation;

final class JsonLocationImpl implements JsonLocation, Serializable {
    
    public static final JsonLocation UNKNOWN_LOCATION = new JsonLocationImpl(-1, -1, -1);
    
    private final long lineNumber;
    private final long columnNumber;
    private final long streamOffset;

    JsonLocationImpl(final long lineNumber, final long columnNumber, final long streamOffset) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.streamOffset = streamOffset;
    }

    @Override
    public long getLineNumber() {
        return lineNumber;
    }

    @Override
    public long getColumnNumber() {
        return columnNumber;
    }

    @Override
    public long getStreamOffset() {
        return streamOffset;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JsonLocationImpl that = JsonLocationImpl.class.cast(o);
        return columnNumber == that.columnNumber && lineNumber == that.lineNumber && streamOffset == that.streamOffset;

    }

    @Override
    public int hashCode() {
        int result = (int) (lineNumber ^ (lineNumber >>> 32));
        result = 31 * result + (int) (columnNumber ^ (columnNumber >>> 32));
        result = 31 * result + (int) (streamOffset ^ (streamOffset >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "[lineNumber=" + lineNumber + ", columnNumber=" + columnNumber + ", streamOffset=" + streamOffset + "]";
    }
}
