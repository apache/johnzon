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

/**
 * An Adapter is similar to a {@link Converter}.
 * The main difference is that a Converter always converts from/to a String.
 * An adapter might e.g. convert to a Date or any other Object which will
 * then be json-ified.
 *
 * @param <A>
 * @param <B>
 */
public interface Adapter<A, B> extends MapperConverter {
    /**
     * Transfer B to JSON as A.
     * A will be inserted into the JSON output
     */
    A to(B b);

    /**
     * Take the object A from JSON an convert it to B to store in the POJO
     */
    B from(A a);
}
