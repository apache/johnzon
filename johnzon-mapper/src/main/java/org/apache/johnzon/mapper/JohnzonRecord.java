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

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Forces method named as properties to be used as getters (String foo() will match the attribute foo).
 * Also enables a constructor with all properties even if not marked as @ConstructorProperties or equivalent.
 * It simulates Java 14+ records.
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface JohnzonRecord {
    /**
     * When not using -parameters compiler argument, enables to customize parameter names.
     * It is only real in @JohnzonRecord classes.
     */
    @Target(PARAMETER)
    @Retention(RUNTIME)
    @interface Name {
        String value();
    }
}
