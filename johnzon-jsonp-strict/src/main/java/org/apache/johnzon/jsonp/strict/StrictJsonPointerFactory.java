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
package org.apache.johnzon.jsonp.strict;

import org.apache.johnzon.core.JsonPointerImpl;
import org.apache.johnzon.core.spi.JsonPointerFactory;

import javax.json.JsonPointer;
import javax.json.spi.JsonProvider;

/**
 * This aims at replacing the {@link org.apache.johnzon.core.DefaultJsonPointerFactory} in order to force Johnzon
 * to comply with the specification and pass the TCK.
 */
public class StrictJsonPointerFactory implements JsonPointerFactory {

    @Override
    public JsonPointer createPointer(final JsonProvider provider, final String path) {
        return new StrictJsonPointerImpl(provider, path);
    }

    /**
     * This overrides the default shift and puts Johnzon into a standard behavior to pass the TCK
     */
    private static class StrictJsonPointerImpl extends JsonPointerImpl {
        public StrictJsonPointerImpl(final JsonProvider provider, final String path) {
            super(provider, path);
        }

        protected int minusShift() {
            return 0;
        }
    }

}
