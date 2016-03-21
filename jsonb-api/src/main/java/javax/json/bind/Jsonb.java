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
package javax.json.bind;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

public interface Jsonb extends AutoCloseable {
    <T> T fromJson(String str, Class<T> type) throws JsonbException;

    <T> T fromJson(String str, Type runtimeType) throws JsonbException;

    <T> T fromJson(Reader reader, Class<T> type) throws JsonbException;

    <T> T fromJson(Reader reader, Type runtimeType) throws JsonbException;

    <T> T fromJson(InputStream stream, Class<T> type) throws JsonbException;

    <T> T fromJson(InputStream stream, Type runtimeType) throws JsonbException;

    String toJson(Object object) throws JsonbException;

    String toJson(Object object, Type runtimeType) throws JsonbException;

    void toJson(Object object, Writer writer) throws JsonbException;

    void toJson(Object object, Type runtimeType, Writer writer) throws JsonbException;

    void toJson(Object object, OutputStream stream) throws JsonbException;

    void toJson(Object object, Type runtimeType, OutputStream stream) throws JsonbException;
}
