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
package org.apache.johnzon.jaxrs;

import jakarta.json.JsonStructure;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;

// @Provider // don't let it be scanned, it would conflict with JsrProvider
@Produces({
    "*/json",
    "*/*+json", "*/x-json",
    "*/javascript", "*/x-javascript"
})
@Consumes({
    "*/json",
    "*/*+json", "*/x-json",
    "*/javascript", "*/x-javascript"
})
public class WildcardJsrProvider extends DelegateProvider<JsonStructure> {
    public WildcardJsrProvider() {
        super(new JsrMessageBodyReader(), new JsrMessageBodyWriter());
    }

    protected boolean shouldThrowNoContentExceptionOnEmptyStreams() {
        return Boolean.getBoolean("johnzon.jaxrs.jsr.wildcard.throwNoContentExceptionOnEmptyStreams");
    }
}
