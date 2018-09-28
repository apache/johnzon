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

package org.apache.johnzon.jaxrs.osgi;

import static org.apache.aries.component.dsl.OSGi.all;
import static org.apache.aries.component.dsl.OSGi.coalesce;
import static org.apache.aries.component.dsl.OSGi.configuration;
import static org.apache.aries.component.dsl.OSGi.configurations;
import static org.apache.aries.component.dsl.OSGi.just;
import static org.apache.aries.component.dsl.OSGi.register;
import static org.apache.aries.component.dsl.OSGi.service;
import static org.apache.aries.component.dsl.OSGi.serviceReferences;
import static org.apache.aries.component.dsl.Utils.highest;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.johnzon.jaxrs.JohnzonProvider;
import org.apache.johnzon.jaxrs.JsrProvider;
import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

public class Activator implements BundleActivator {
    private static final Converter CONVERTER = Converters.standardConverter();

    private static final OSGi<Entry<Dictionary<String, ?>, JsrConfig>> JSR_CONFIGURATION = coalesce(
        all(
            configurations(JsrConfig.CONFIG_PID),
            configuration(JsrConfig.CONFIG_PID)
        ),
        just(Hashtable::new)
    ).map(
        properties -> new AbstractMap.SimpleImmutableEntry<>(
            properties,
            CONVERTER.convert(properties).to(JsrConfig.class)
        )
    );

    private static final OSGi<Entry<Dictionary<String, ?>, Config>> CONFIGURATION = coalesce(
        all(
            configurations(Config.CONFIG_PID),
            configuration(Config.CONFIG_PID)
        ),
        just(Hashtable::new)
    ).map(
        properties -> new AbstractMap.SimpleImmutableEntry<>(
            properties,
            CONVERTER.convert(properties).to(Config.class)
        )
    );

    private OSGiResult _result;

    @Override
    public void start(BundleContext context) throws Exception {
        _result = all(
            JSR_CONFIGURATION.flatMap(
                entry -> register(
                    new String[]{
                        MessageBodyReader.class.getName(),
                        MessageBodyWriter.class.getName()
                    },
                    new JsrProviderFactory(),
                    getJsrRegistrationProperties(entry.getKey(), entry.getValue())
                )
            ),
            CONFIGURATION.flatMap(
                entry -> coalesce(
                    service(highest(serviceReferences(Mapper.class, entry.getValue().mapper()))),
                    just(() -> new MapperBuilder().setDoCloseOnStreams(false).build())
                ).flatMap(
                    mapper -> register(
                        new String[]{
                            MessageBodyReader.class.getName(),
                            MessageBodyWriter.class.getName()
                        },
                        new JohnzonProviderFactory(mapper, entry.getValue().ignores()),
                        getRegistrationProperties(entry.getKey(), entry.getValue())
                    )
                )
            )
        ).run(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        _result.close();
    }

    @SuppressWarnings("serial")
    private Map<String, ?> getJsrRegistrationProperties(
        Dictionary<String, ?> properties, JsrConfig config) {

        Enumeration<String> keys = properties.keys();

        return new Hashtable<String, Object>() {{
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();

                if(!key.startsWith(".")) {
                    put(key, properties.get(key));
                }
            }

            put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, true);
            put(JaxrsWhiteboardConstants.JAX_RS_NAME, "johnzon.jsonp");

            putIfAbsent(
                JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
                config.osgi_jaxrs_application_select());
            putIfAbsent(
                JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE,
                config.osgi_jaxrs_media_type());
        }};
    }

    @SuppressWarnings("serial")
    private Map<String, ?> getRegistrationProperties(
        Dictionary<String, ?> properties, Config config) {

        Enumeration<String> keys = properties.keys();

        return new Hashtable<String, Object>() {{
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();

                if(!key.startsWith(".")) {
                    put(key, properties.get(key));
                }
            }

            put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, true);
            put(JaxrsWhiteboardConstants.JAX_RS_NAME, "johnzon.json");

            putIfAbsent("ignores", config.ignores());
            putIfAbsent(
                JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
                config.osgi_jaxrs_application_select());
            putIfAbsent(
                JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE,
                config.osgi_jaxrs_media_type());
        }};
    }

    private class JohnzonProviderFactory implements PrototypeServiceFactory<JohnzonProvider<?>> {

        private Mapper mapper;
        private Collection<String> ignores;

        public JohnzonProviderFactory(Mapper mapper, String[] ignores) {
            this.mapper = mapper;
            this.ignores = Arrays.asList(ignores);
        }

        @Override
        public JohnzonProvider<?> getService(
            Bundle bundle, ServiceRegistration<JohnzonProvider<?>> registration) {

            return new JohnzonProvider<>(mapper, ignores);
        }

        @Override
        public void ungetService(
            Bundle bundle, ServiceRegistration<JohnzonProvider<?>> registration, JohnzonProvider<?> service) {
        }

    }

    private class JsrProviderFactory implements PrototypeServiceFactory<JsrProvider> {

        @Override
        public JsrProvider getService(
            Bundle bundle, ServiceRegistration<JsrProvider> registration) {

            return new JsrProvider();
        }

        @Override
        public void ungetService(
            Bundle bundle, ServiceRegistration<JsrProvider> registration, JsrProvider service) {
        }

    }

}
