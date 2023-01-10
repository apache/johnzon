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

import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperBuilder;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.Provider;
import java.util.Collection;

@Provider // this is scanned cause does not overlap with JohnzonProvider in terms of mime types
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
public class WildcardJohnzonProvider<T> extends DelegateProvider<T> {
    public WildcardJohnzonProvider(final Mapper mapper, final Collection<String> ignores) {
        super(new JohnzonMessageBodyReader<T>(mapper, ignores), new JohnzonMessageBodyWriter<T>(mapper, ignores));
    }

    public WildcardJohnzonProvider() {
        this(new MapperBuilder().setDoCloseOnStreams(false).build(), null);
    }

    protected boolean shouldThrowNoContentExceptionOnEmptyStreams() {
        return Boolean.getBoolean("johnzon.jaxrs.johnzon.wildcard.throwNoContentExceptionOnEmptyStreams");
    }
}
