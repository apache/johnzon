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

/**
 * An Adapter is similar to a {@link Converter}.
 * The main difference is that a Converter always converts from/to a String.
 * An adapter might e.g. convert to a Date or any other Object which will
 * then be json-ified.
 *
 * A small example which has a special Java type to internally represent dates.
 * Let's call it {@code DateHolder}.
 * Our {@code Mapper} should treat it as a {@code java.util.Date}.
 * For doing so we create a {@code DateHolderAdapter} like the following example shows:
 * <pre>
 * {@code public static class DateHolderAdapter implements Adapter<DateHolder, Date>} {
 *     {@code @Override}
 *     public DateHolder to(Date date) {
 *         DateHolder dh = new DateHolder(date.getTime());
 *         return dh;
 *     }
 *
 *     {@code @Override}
 *     public Date from(DateHolder dateHolder) {
 *        return new Date(dateHolder.getDate());
 *     }
 * }
 * </pre>
 *
 * Consider a POJO has a DateHolder.
 * When serialising the {@code Mapper} will first use the {@code DateHolderAdapter#from(DateHolder)} and from there to JSON.
 * When reading JSON the {@code to(Date)} method will be used.
 *
 * @param <POJO_TYPE> The Java type in the POJO (Plain Old Java Object)
 * @param <JSON_TYPE> The Java Type which will be used to transform to JSON.
 */
public interface Adapter<POJO_TYPE, JSON_TYPE> extends MapperConverter {
    /**
     * Transfer JSONTYPE_TYPE from JSON to POJO as POJO_TYPE.
     * @param b the JSON type
     * @return the equivalent Java type
     */
    POJO_TYPE to(JSON_TYPE b);

    /**
     * Take the POJO_TYPE object A from a POJO an convert it to JSON_TYPE which will be inserted into the JSON output.
     * @param a the Java type
     * @return the equivalent JSON type
     */
    JSON_TYPE from(POJO_TYPE a);
}
