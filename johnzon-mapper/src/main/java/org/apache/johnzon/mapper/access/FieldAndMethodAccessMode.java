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
package org.apache.johnzon.mapper.access;

import java.util.HashMap;
import java.util.Map;

// methods override fields
public class FieldAndMethodAccessMode extends BaseAccessMode {
    private final FieldAccessMode fields = new FieldAccessMode();
    private final MethodAccessMode methods = new MethodAccessMode(false);

    @Override
    public Map<String, Reader> doFindReaders(final Class<?> clazz) {
        final Map<String, Reader> readers = new HashMap<String, Reader>(fields.findReaders(clazz));
        readers.putAll(methods.findReaders(clazz));
        return readers;
    }

    @Override
    public Map<String, Writer> doFindWriters(final Class<?> clazz) {
        final Map<String, Writer> writers = new HashMap<String, Writer>(fields.findWriters(clazz));
        writers.putAll(methods.findWriters(clazz));
        return writers;
    }
}
