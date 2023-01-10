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
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.json.Json;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class NoWarningTest {
    public ByteArrayOutputStream out;
    public ByteArrayOutputStream err;
    private PrintStream oldOut;
    private PrintStream oldErr;
    private Handler handler;

    @Before
    public void capture() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        oldOut = System.out;
        oldErr = System.err;
        System.setOut(new PrintStream(out));
        final PrintStream stderr = new PrintStream(err);
        System.setErr(stderr);
        handler = new Handler() {
            @Override
            public void publish(final LogRecord record) {
                stderr.println(record.getMessage());
                oldErr.println(record.getMessage());
            }

            @Override
            public void flush() {
                // no-op
            }

            @Override
            public void close() throws SecurityException {
                flush();
            }
        };
        Logger.getLogger("").addHandler(handler);
    }

    @After
    public void reset() {
        System.setOut(oldOut);
        System.setErr(oldErr);
        Logger.getLogger("").removeHandler(handler);
    }

    @Test
    public void noWarn() throws UnsupportedEncodingException {
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
                .build()
                .close();
        // no warn log
        assertTrue(out.toString("UTF-8").isEmpty());
        assertTrue(err.toString("UTF-8").isEmpty());
    }
    
    @Test
    public void warn() throws UnsupportedEncodingException {
        Map<String, Object> unsupportedConfig = new HashMap<String, Object>();
        unsupportedConfig.put("xxx.yyy.zzz", "");
        Json.createGeneratorFactory(unsupportedConfig)
                .createGenerator(new ByteArrayOutputStream())
                .write(0)
                .close();
        //warn log
        String log = out.toString("UTF-8") + err.toString("UTF-8");
        assertFalse(log.isEmpty());
        assertTrue(log.contains("xxx.yyy.zzz"));
    }
}
