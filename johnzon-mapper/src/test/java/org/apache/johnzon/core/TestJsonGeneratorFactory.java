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
package org.apache.johnzon.core;

import jakarta.json.stream.JsonGenerator;
import java.io.Writer;
import java.util.Collections;

/**
 * This class is only used for {@link org.apache.johnzon.mapper.JsonGeneratorCloseTest}.
 *
 * Since {@link JsonGeneratorImpl} is package private we need to add this
 * class in {@code org.apache.johnzon.core} to use {@link JsonGeneratorImpl} and wrap our
 * own {@link TestBufferProvider}.
 *
 * It's a little bit dirty and will be removed if anyone has a better
 * solution.
 */
public class TestJsonGeneratorFactory extends JsonGeneratorFactoryImpl {

    public TestJsonGeneratorFactory() {
        super(Collections.emptyMap());
    }


    @Override
    public JsonGenerator createGenerator(Writer writer) {
        return new JsonGeneratorImpl(writer, TestBufferProvider.INSTANCE, false);
    }

}
