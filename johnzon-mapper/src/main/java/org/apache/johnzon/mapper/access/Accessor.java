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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;

class Accessor {
    private static final MethodHandle TRY_SET_ACCESSIBLE = findTrySetAccessible();
    
    private Accessor() {
    }
    
    private static MethodHandle findTrySetAccessible() {
        try {
            // The AccessibleObject::trySetAccessible is available from Java 9 and above
            return MethodHandles
                .lookup()
                .findVirtual(AccessibleObject.class, "trySetAccessible", MethodType.methodType(boolean.class));
        } catch (final IllegalAccessException | NoSuchMethodException ex) {
            // The method is not available
            return null;
        }
    }
    
    public static boolean trySetAccessible(final AccessibleObject obj) {
        if (!obj.isAccessible()) {
            if (TRY_SET_ACCESSIBLE != null) {
                try {
                    return (boolean)TRY_SET_ACCESSIBLE.invoke(obj);
                } catch (Throwable ex) {
                    // Unsuccessful invocation, let's fallback to AccessibleObject::setAccessible()
                }
            } 
            
            // It may potentially throw InaccessibleObjectException on JDK16+ 
            obj.setAccessible(true);
        }
        
        return true;
    }
}
