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
package org.apache.johnzon.mapper.number;

public final class Validator {
    private Validator() {
        // no-op
    }

    public static void validateByte(final int value) {
        // bytes have a special handling as they are often used
        // to transport binary. So we have to pass on the full 8 bit.
        // TODO: ATTENTION: this is only an intermediate solution until JOHNZON-177
        // resp https://github.com/eclipse-ee4j/jsonb-api/issues/82 is properly specced
        if (value < -128 || value > 255) {
            throw new java.lang.ArithmeticException("Overflow");
        }
    }
}
