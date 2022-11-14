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
package org.apache.johnzon.mapper.internal;

/**
 * Internal class to easily collect information about the 'depth' of a json object
 * without having to eagerly construct it.
 * <p>
 * For use in recursive generator and parser method calls to defer string operations.
 */
public class JsonPointerTracker {
    public static final JsonPointerTracker ROOT = new JsonPointerTracker(null, null);

    private final JsonPointerTracker parent;
    private final String currentNode;

    private String jsonPointer;


    /**
     * @param parent      or {@code null} if this is the root object
     * @param currentNode the name of the attribute or "/" for the root object
     */
    public JsonPointerTracker(JsonPointerTracker parent, String currentNode) {
        this.parent = parent;
        this.currentNode = currentNode;
    }

    /**
     * For Arrays and Lists.
     *
     * @param jsonPointer the json node
     * @param i           current counter number
     */
    public JsonPointerTracker(JsonPointerTracker jsonPointer, int i) {
        this(jsonPointer, Integer.toString(i));
    }

    @Override
    public String toString() {
        if (jsonPointer != null) {
            return jsonPointer;
        }
        if (parent != null) {
            return jsonPointer = (parent != ROOT ? parent + "/" : "/") + encode(currentNode);
        }
        if (currentNode != null) {
            return jsonPointer = "/" + encode(currentNode);
        }
        return jsonPointer = "/";
    }

    // from org.apache.johnzon.mapper.internal.JsonPointerTracker to not depend on johnzon-core
    private static String encode(final String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return replace(replace(s, "~", "~0"), "/", "~1");
    }

    private static String replace(final String src, final String from, final String to) {
        if (src.isEmpty()) {
            return src;
        }
        final int start = src.indexOf(from);
        if (start >= 0) {
            return src.substring(0, start) + to + replace(src.substring(start + from.length()), from, to);
        }
        return src;
    }
}
