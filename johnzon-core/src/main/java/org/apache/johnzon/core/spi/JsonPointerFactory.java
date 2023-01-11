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
package org.apache.johnzon.core.spi;

import jakarta.json.JsonPointer;
import jakarta.json.spi.JsonProvider;

/**
 * Factory to create JsonPointer instances. We have a default one in Johnzon, but the aim is tom being able to
 * override it in some edge cases. It uses a usual service loader mechanism to load and sort the factories using
 * the ordinal.
 */
public interface JsonPointerFactory {

    JsonPointer createPointer(JsonProvider provider, String path);

    default int ordinal() {
        return 0;
    }
}
