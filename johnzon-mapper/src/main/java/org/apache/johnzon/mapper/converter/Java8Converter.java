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
package org.apache.johnzon.mapper.converter;


import java.time.ZoneId;
import java.util.TimeZone;
import org.apache.johnzon.mapper.Converter;

public abstract class Java8Converter<T> implements Converter<T> {

    final static TimeZone TIME_ZONE_UTC = TimeZone.getTimeZone("UTC");
    final static ZoneId ZONE_ID_UTC = ZoneId.of("UTC");

    static void logIfDeprecatedTimeZone(final String text) {
        /* TODO: get the list, UTC is clearly not deprecated but uses 3 letters
        if (text.length() == 3) { // don't fail but log it
            Logger.getLogger(JohnzonBuilder.class.getName()).severe("Deprecated timezone: " + text);
        }
        */
    }
}
