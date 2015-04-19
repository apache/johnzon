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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.StandardErrorStreamLog;
import org.junit.contrib.java.lang.system.StandardOutputStreamLog;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class NoWarningTest {
    
    @Rule
    public final StandardOutputStreamLog out = new StandardOutputStreamLog();

    @Rule
    public final StandardErrorStreamLog err = new StandardErrorStreamLog();

    @Test
    public void noWarn() {
        new MapperBuilder()
                .setEncoding("UTF-8")
                .setSupportConstructors(true)
                .setAccessModeName("field")
                .setBufferStrategy("queue")
                .setDoCloseOnStreams(true)
                .setBufferSize(45678)
                .setMaxSize(789465)
                .setSkipNull(true)
                .setSupportsComments(true)
                .build();
        // no warn log
        assertTrue(out.getLog().isEmpty());
        assertTrue(err.getLog().isEmpty());
    }
    
    @Test
    public void warn() {
        Map<String, Object> unsupportedConfig = new HashMap<String, Object>();
        unsupportedConfig.put("xxx.yyy.zzz", "");
        Json.createGeneratorFactory(unsupportedConfig).createGenerator(new ByteArrayOutputStream());
        //warn log
        String log = out.getLog()+err.getLog();
        assertFalse(log.isEmpty());
        assertTrue(log.contains("xxx.yyy.zzz"));
    }
}
