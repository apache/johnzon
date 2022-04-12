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
package org.apache.johnzon.core.util;

/**
 * ClassLoader related utils
 */
public final class ClassUtil {

    private ClassUtil() {
        // private utility class ct
    }

    /**
     * @return either the ThreadContextClassLoader or the CL of this very class if no TCCL exists
     */
    public static ClassLoader getClassLoader() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null) {
            return tccl;
        }

        return ClassUtil.class.getClassLoader();
    }

    /**
     * @param className           to be loaded
     * @param ignoreBrokenClasses if {@link NoClassDefFoundError} should be ignored
     * @return Class or {@code null} if the class could not be found
     */
    public static Class<?> loadClassOptional(String className, boolean ignoreBrokenClasses) {
        ClassLoader cl = getClassLoader();

        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            // all fine, that class is optional!
            return null;
        } catch (NoClassDefFoundError ncdfe) {
            if (ignoreBrokenClasses) {
                return null;
            }
            throw ncdfe;
        }
    }

    /**
     * Calculate the name of a getter based on the name of it's field and the type
     *
     * @param fieldName
     * @param type      of the field
     * @return "get" or "is" method name for the field
     */
    public static String getterName(String fieldName, Class<?> type) {
        StringBuilder sb = new StringBuilder(50);
        sb.append(type == Boolean.class || type == boolean.class ? "is" : "get");
        sb.append(Character.toUpperCase(fieldName.charAt(0))).append(fieldName.substring(1));
        return sb.toString();
    }

    /**
     * Calculate the name of a setter based on the name of it's field
     *
     * @param fieldName
     * @return "set" method name for the field
     */
    public static String setterName(String fieldName) {
        StringBuilder sb = new StringBuilder(50);
        sb.append("set");
        sb.append(Character.toUpperCase(fieldName.charAt(0))).append(fieldName.substring(1));
        return sb.toString();
    }

    public static String capitalizeName(String fieldName) {
        StringBuilder sb = new StringBuilder(50);
        sb.append(Character.toUpperCase(fieldName.charAt(0))).append(fieldName.substring(1));
        return sb.toString();
    }
}