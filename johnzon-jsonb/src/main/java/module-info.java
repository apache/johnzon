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
module org.apache.johnzon.jsonb {

    requires org.apache.johnzon.core;
    requires org.apache.johnzon.mapper;

    requires java.logging;

    requires transitive jakarta.cdi;
    requires transitive jakarta.inject;
    requires transitive jakarta.interceptor;
    requires transitive jakarta.json;
    requires transitive jakarta.json.bind;
    requires transitive jakarta.ws.rs;

    exports org.apache.johnzon.jsonb;
    exports org.apache.johnzon.jsonb.adapter;
    exports org.apache.johnzon.jsonb.api.experimental;
    exports org.apache.johnzon.jsonb.cdi;
    exports org.apache.johnzon.jsonb.converter;
    exports org.apache.johnzon.jsonb.extension;
    exports org.apache.johnzon.jsonb.factory;
    exports org.apache.johnzon.jsonb.order;
    exports org.apache.johnzon.jsonb.polymorphism;
    exports org.apache.johnzon.jsonb.reflect;
    exports org.apache.johnzon.jsonb.serializer;
    exports org.apache.johnzon.jsonb.spi;

    provides jakarta.enterprise.inject.spi.Extension with org.apache.johnzon.jsonb.cdi.JohnzonCdiExtension;
    provides jakarta.json.bind.spi.JsonbProvider with org.apache.johnzon.jsonb.JohnzonProvider;
}