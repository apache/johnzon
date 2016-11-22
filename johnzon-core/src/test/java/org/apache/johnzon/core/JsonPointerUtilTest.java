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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class JsonPointerUtilTest {

    @Test
    public void testEncodeNull() {
        assertNull(JsonPointerUtil.encode(null));
    }

    @Test
    public void testEncodeEmptyString() {
        String encodedString = JsonPointerUtil.encode("");
        assertEquals("", encodedString);
    }

    @Test
    public void testEncodeNoTransformation() {
        String encodedString = JsonPointerUtil.encode("TestString");
        assertEquals("TestString", encodedString);
    }

    @Test
    public void testEncodeFirstTransformation() {
        String encodedString = JsonPointerUtil.encode("~");
        assertEquals("~0", encodedString);
    }

    @Test
    public void testEncodeSecondTransformation() {
        String encodedString = JsonPointerUtil.encode("/");
        assertEquals("~1", encodedString);
    }

    @Test
    public void testEncodeWholeTransformation() {
        String decodedString = JsonPointerUtil.encode("~/");
        assertEquals("~0~1", decodedString);
    }

    @Test
    public void testDecodeNull() {
        assertNull(JsonPointerUtil.decode(null));
    }

    @Test
    public void testDecodeEmptyString() {
        String decodedString = JsonPointerUtil.decode("");
        assertEquals("", decodedString);
    }

    @Test
    public void testDecodeNoTransformation() {
        String decodedString = JsonPointerUtil.decode("TestString");
        assertEquals("TestString", decodedString);
    }

    @Test
    public void testDecodeFirstTransformation() {
        String decodedString = JsonPointerUtil.decode("~1");
        assertEquals("/", decodedString);
    }

    @Test
    public void testDecodeSecondTransformation() {
        String decodedString = JsonPointerUtil.decode("~0");
        assertEquals("~", decodedString);
    }

    @Test
    public void testDecodeWholeTransformation() {
        String decodedString = JsonPointerUtil.decode("~01");
        assertEquals("~1", decodedString);
    }

}
