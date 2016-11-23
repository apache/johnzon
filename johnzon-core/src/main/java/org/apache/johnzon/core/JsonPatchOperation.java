/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.apache.johnzon.core;

/**
 * <p>
 * available operations for {@link javax.json.JsonPatch}.
 * </p>
 * <p>
 * NOTICE: the behavoir of some operations depends on which {@link javax.json.JsonValue} they are performed.
 * for details please check the documentation of the very operation.
 * </p>
 *
 * @since 1.1
 */
public enum JsonPatchOperation {

    /**
     * <p>
     * required members are:
     * <ul>
     * <li>"op": "add"</li>
     * <li>"path": "path/to/add"</li>
     * <li>"value": "{@link javax.json.JsonValue}ToAdd"</li>
     * </ul>
     * </p>
     * <p>
     * if the "path/to" does not exist, this operation will result in an error.<br>
     * if the "path/to/add" already exists, the value will be <strong>replaced</strong>
     * </p>
     * <p>
     * for {@link javax.json.JsonArray}s the new value will be inserted at the specified index
     * and the element(s) at/after are shifted to the right. the '-' character is used to append the value
     * at the and of the {@link javax.json.JsonArray}.
     * </p>
     */
    ADD,

    /**
     * <p>
     * required members are:
     * <ul>
     * <li>"op": "remove"</li>
     * <li>"path": "path/to/remove"</li>
     * </ul>
     * </p>
     * <p>
     * if the "path/to/remove" does not exist, the operation will fail.
     * </p>
     * <p>
     * for {@link javax.json.JsonArray}s the values after the removed value are shifted to the left
     * </p>
     */
    REMOVE,

    /**
     * <p>
     * required members are:
     * <ul>
     * <li>"op": "replace"</li>
     * <li>"path": "path/to/replace"</li>
     * <li>"value": "the new {@link javax.json.JsonValue}"</li>
     * </ul>
     * </p>
     * <p>
     * this operation is identical to {@link #REMOVE} followed by {@link #ADD}
     * </p>
     */
    REPLACE,

    /**
     * <p>
     * required members are:
     * <ul>
     * <li>"op": "move"</li>
     * <li>"from": "path/to/move/from"</li>
     * <li>"path": "path/to/move/to"</li>
     * </ul>
     * </p>
     * <p>
     * the operation will fail it the "path/to/move/from" does not exist
     * </p>
     * <p>
     * NOTICE: a location can not be moved into one of it's children. (from /a/b/c to /a/b/c/d)
     * </p>
     * <p>
     * this operation is identical to {@link #REMOVE} from "from" and {@link #ADD} to the "path"
     * </p>
     */
    MOVE,

    /**
     * <p>
     * required members are:
     * <ul>
     * <li>"op": "copy"</li>
     * <li>"from": "path/to/copy/from"</li>
     * <li>"path": "path/to/add"</li>
     * </ul>
     * </p>
     * <p>
     * the operation will result in an error if the "from" location does not exist
     * </p>
     * <p>
     * this operation is identical to {@link #ADD} with the "from" value
     * </p>
     */
    COPY,

    /**
     * <p>
     * required members are:
     * <ul>
     * <li>"op": "test"</li>
     * <li>"path": "/path/to/test"</li>
     * <li>"value": "{@link javax.json.JsonValue} to test"</li>
     * </ul>
     * </p>
     * <p>
     * this operation fails, if the value is NOT equal with the /path/to/test
     * </p>
     * <p>
     * ordering of the elements in a {@link javax.json.JsonObject} is NOT significant however
     * the position of an element in a {@link javax.json.JsonArray} is significant for equality.
     * </p>
     */
    TEST

}
