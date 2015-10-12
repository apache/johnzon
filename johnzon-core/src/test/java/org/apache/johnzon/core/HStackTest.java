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

import org.junit.Assert;
import org.junit.Test;

public class HStackTest {

    @Test
    public void normalUse() {

        final HStack<String> stack = new HStack<String>();
        Assert.assertEquals(0, stack.size());
        Assert.assertNull(stack.pop());
        Assert.assertNull(stack.peek());

        stack.push("1");
        Assert.assertEquals(1, stack.size());
        Assert.assertEquals("1", stack.peek());
        Assert.assertEquals(1, stack.size());
        Assert.assertEquals("1", stack.pop());
        Assert.assertEquals(0, stack.size());

        stack.push("1");
        stack.push("2");
        stack.push("3");
        stack.push("4");
        stack.push("5");
        stack.push("6");
        stack.push("7");

        Assert.assertEquals(7, stack.size());
        Assert.assertEquals("7", stack.peek());
        Assert.assertEquals("7", stack.peek());
        Assert.assertEquals("7", stack.peek());
        Assert.assertEquals("7", stack.pop());
        Assert.assertEquals("6", stack.pop());
        Assert.assertEquals("5", stack.peek());
        Assert.assertEquals(5, stack.size());
        Assert.assertEquals("5", stack.pop());
        Assert.assertEquals("4", stack.pop());
        Assert.assertEquals("3", stack.pop());
        Assert.assertEquals("2", stack.pop());
        Assert.assertEquals("1", stack.pop());
        Assert.assertNull(stack.peek());
        Assert.assertNull(stack.peek());
        Assert.assertNull(stack.peek());
        Assert.assertNull(stack.pop());
        Assert.assertNull(stack.pop());
        Assert.assertEquals(0, stack.size());
    }
}
