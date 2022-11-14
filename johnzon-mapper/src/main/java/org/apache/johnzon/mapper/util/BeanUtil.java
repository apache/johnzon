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
package org.apache.johnzon.mapper.util;


/**
 * Some simple bean introspection methods.
 * To avoid a dependency on the awt java.beans.introspector which is a desktop level class.
 */
public final class BeanUtil {

    private BeanUtil() {
        // private utility class ct
    }

    /**
     * Calculate the name of a getter based on the name of it's field and the type
     *
     * @param fieldName of the field
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
     * @param fieldName of the field
     * @return "set" method name for the field
     */
    public static String setterName(String fieldName) {
        StringBuilder sb = new StringBuilder(50);
        sb.append("set");
        sb.append(Character.toUpperCase(fieldName.charAt(0))).append(fieldName.substring(1));
        return sb.toString();
    }

    /**
     * capitalize according to java beans specification
     */
    public static String capitalize(String fieldName) {
        StringBuilder sb = new StringBuilder(50);
        sb.append(Character.toUpperCase(fieldName.charAt(0))).append(fieldName.substring(1));
        return sb.toString();
    }

    /**
     * decapitalize according to java beans specification.
     * That is start the given field with a lower case, but only if the 2nd char is not also an uppercase character.
     * eg; "Enabled" will become "enabled", but "URL" will remain "URL".
     */
    public static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }

        if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
}
