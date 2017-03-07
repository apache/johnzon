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

import javax.json.JsonValue;

/**
 * Creates a JsonPatchBuilder which will create {@link javax.json.JsonMergePatch} as defined in
 * https://tools.ietf.org/html/rfc7396
 */
public class JsonMergePatchBuilder {

    /**
     * Create a merged patch by comparing the source to the target.
     * Applying this JsonPatch to the source will give you the target.
     * A mergePatch is a JsonValue as defined in http://tools.ietf.org/html/rfc7396
     *
     * If you have a JSON like
     * <pre>
     * {
     *   "a": "b",
     *   "c": {
     *     "d": "e",
     *     "f": "g"
     *   }
     * }
     * </pre>
     *
     * Then you can change the value of "a" and removing "f" by sending:
     * <pre>
     * {
     *   "a":"z",
     *   "c": {
     *     "f": null
     *   }
     * }
     * </pre>
     *
     * @see #mergePatch(JsonValue, JsonValue)
     *
     * @since 1.1
     */
    public JsonValue createMergePatch(JsonValue source , JsonValue target) {
        return null;
    }

    /**
     * Merge the given patch to the existing source
     * A mergePatch is a JsonValue as defined in http://tools.ietf.org/html/rfc7396
     *
     * @return the result of applying the patch to the source
     *
     * @see #createMergePatch(JsonValue, JsonValue)
     * @since 1.1
     */
    public JsonValue mergePatch(JsonValue source, JsonValue patch) {
        return null;
    }

}
