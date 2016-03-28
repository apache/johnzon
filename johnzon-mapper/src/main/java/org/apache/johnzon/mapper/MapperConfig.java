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

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.johnzon.mapper.access.AccessMode;

/**
 * Contains internal configuration for all the mapper stuff
 */
class MapperConfig implements Cloneable {
    private boolean close = false;
    private boolean skipNull = true;
    private boolean skipEmptyArray = false;
    private boolean supportsComments = false;
    private boolean treatByteArrayAsBase64;
    private boolean treatByteArrayAsBase64URL;
    private boolean readAttributeBeforeWrite;
    private boolean prettyPrint;
    private AccessMode accessMode;
    private Charset encoding = Charset.forName(System.getProperty("johnzon.mapper.encoding", "UTF-8"));

    //X TODO we need a more elaborated approache at the end, but for now it's fine
    private Map<Class<?>, ObjectConverter<?>> objectConverters = new HashMap<Class<?>, ObjectConverter<?>>();


    MapperConfig() {
    }

    void setClose(boolean close) {
        this.close = close;
    }

    public boolean isClose() {
        return close;
    }

    public boolean isSkipNull() {
        return skipNull;
    }

    void setSkipNull(boolean skipNull) {
        this.skipNull = skipNull;
    }

    public boolean isSkipEmptyArray() {
        return skipEmptyArray;
    }

    void setSkipEmptyArray(boolean skipEmptyArray) {
        this.skipEmptyArray = skipEmptyArray;
    }

    public boolean isSupportsComments() {
        return supportsComments;
    }

    void setSupportsComments(boolean supportsComments) {
        this.supportsComments = supportsComments;
    }

    public boolean isTreatByteArrayAsBase64() {
        return treatByteArrayAsBase64;
    }

    void setTreatByteArrayAsBase64(boolean treatByteArrayAsBase64) {
        this.treatByteArrayAsBase64 = treatByteArrayAsBase64;
    }

    public boolean isTreatByteArrayAsBase64URL() {
        return treatByteArrayAsBase64URL;
    }

    void setTreatByteArrayAsBase64URL(boolean treatByteArrayAsBase64URL) {
        this.treatByteArrayAsBase64URL = treatByteArrayAsBase64URL;
    }

    public boolean isReadAttributeBeforeWrite() {
        return readAttributeBeforeWrite;
    }

    void setReadAttributeBeforeWrite(boolean readAttributeBeforeWrite) {
        this.readAttributeBeforeWrite = readAttributeBeforeWrite;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public AccessMode getAccessMode() {
        return accessMode;
    }

    void setAccessMode(AccessMode accessMode) {
        this.accessMode = accessMode;
    }

    public Charset getEncoding() {
        return encoding;
    }

    void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    <T> void addObjectConverter(Class<T> targetType, ObjectConverter<T> objectConverter) {
        objectConverters.put(targetType, objectConverter);
    }

    public Map<Class<?>, ObjectConverter<?>> getObjectConverters() {
        return objectConverters;
    }

    @Override
    public MapperConfig clone() {
        try {
            return (MapperConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }


}
