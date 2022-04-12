/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.mapper.internal;

import org.apache.johnzon.mapper.util.BeanUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BeanUtilTest {

    @Test
    public void testGetterNames() {
        assertEquals("getMyName", BeanUtil.getterName("myName", Integer.class));
        assertEquals("isEnabled", BeanUtil.getterName("enabled", Boolean.class));
        assertEquals("isEnabled", BeanUtil.getterName("enabled", boolean.class));
    }

    @Test
    public void testSetterNames() {
        assertEquals("setMyName", BeanUtil.setterName("myName"));
        assertEquals("setEnabled", BeanUtil.setterName("enabled"));
    }

    @Test
    public void testCapitalize() {
        assertEquals("Enabled", BeanUtil.capitalize("enabled"));
        assertEquals("URL", BeanUtil.capitalize("URL"));
        assertEquals("Url", BeanUtil.capitalize("url"));
    }

    @Test
    public void testDecapitalize() {
        assertEquals("enabled", BeanUtil.decapitalize("Enabled"));
        assertEquals("URL", BeanUtil.decapitalize("URL"));
    }
}
