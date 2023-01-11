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

import org.apache.johnzon.core.spi.JsonPointerFactory;

import jakarta.json.JsonPointer;
import jakarta.json.spi.JsonProvider;

/**
 * This is not a standard factory but allows Johnzon to support an extended version of JSon Pointer.
 * By default, we support an extended usage of /- when used with replace/remove/get. But in the johnzon-jsonp-strict
 * module, it's overridden so we can pass the JSONP TCK in standalone environments or TomEE or else.
 */
public class DefaultJsonPointerFactory implements JsonPointerFactory {

    @Override
    public JsonPointer createPointer(final JsonProvider provider, final String path) {
        return new JsonPointerImpl(provider, path);
    }

}
