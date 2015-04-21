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

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.johnzon.core.Experimental;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
  * Example: @JohnzonVirtualObject(path = {"nested", "nested-again"}, field = { "a", "b" })
  * will generate {"nested":{"nested-again":{"a":"xxx", "b": "yyy"}}}
  */
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
@Experimental
public @interface JohnzonVirtualObject {
    /**
     * @return the virtual object(s) path.
     */
    String[] path();

    /**
     * @return the list of fields to consider.
     */
    Field[] fields();

    @interface Field {
        String value();
        boolean read() default true;
        boolean write() default true;
    }
}
