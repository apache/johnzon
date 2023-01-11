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
package org.apache.johnzon.mapper;

import jakarta.json.stream.JsonGenerator;

/**
 * Handles writing Json for Objects.
 * Internally it uses a {@link JsonGenerator} to write JSON
 *
 * To write JSON-P structure elements you can use the {@link #getJsonGenerator()} method.
 *
 */
public interface MappingGenerator {

    /**
     * @return the {@link JsonGenerator} used internally to write the JSON output.
     */
    JsonGenerator getJsonGenerator();

    /**
     * Write the given Object o into the current JSON layer.
     * This will <em>not</em> open a new json layer ('{', '}')
     * but really just write the attributes of o to the currently opened layer.
     *
     * Consider you have a class
     * <pre>
     *     public class Customer {
     *         private String firstName;
     *         private String lastName;
     *         private Address address;
     *         ...
     *     }
     * </pre>
     * then the resulting JSON String will e.g. look like
     * <pre>
     *     "firstName":"Karl", "lastName":"SomeName", "address":{"street":"mystreet"}
     * </pre>
     * @param o the object to write
     * @param generator the jsonp generator to use
     * @return itself, for easier chaining of commands
     */
    MappingGenerator writeObject(Object o, JsonGenerator generator);

    default MappingGenerator writeObject(final String key, final Object o, final JsonGenerator generator) {
        return writeObject(o, generator);
    }
}
