/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.johnzon.jsonb;

import org.apache.johnzon.mapper.MapperException;
import org.junit.Test;

public class JsonbBeanGetterUserExceptionsTest {

    private static final RuntimeException USER_EXCEPTION = new RuntimeException("I am user, hear me roar");

    @Test
    public void object() {
        ExceptionAsserts.toJson(new Widget())
                // TODO Review: shouldn't this be JsonbException?
                .assertInstanceOf(MapperException.class)
                .assertMessage("Error calling public java.lang.String org.apache.johnzon.jsonb.JsonbBean" +
                        "GetterUserExceptionsTest$Widget.getString()")
                .assertCauseChain(USER_EXCEPTION);
    }

    public static class Widget {
        private String string;

        public String getString() {
            throw USER_EXCEPTION;
        }

        public void setString(final String string) {
            this.string = string;
        }
    }
}
