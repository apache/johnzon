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

package org.apache.johnzon.osgi;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.aries.component.dsl.OSGi.all;
import static org.apache.aries.component.dsl.OSGi.coalesce;
import static org.apache.aries.component.dsl.OSGi.configuration;
import static org.apache.aries.component.dsl.OSGi.configurations;
import static org.apache.aries.component.dsl.OSGi.ignore;
import static org.apache.aries.component.dsl.OSGi.just;
import static org.apache.aries.component.dsl.OSGi.register;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider;
import org.apache.johnzon.osgi.cdi.RegisterCdiExtension;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {

    private static final Converter CONVERTER = Converters.standardConverter();

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
            ignore(
                CONFIGURATION.flatMap(
                    entry -> register(
                        new String[]{
                            MessageBodyReader.class.getName(),
                            MessageBodyWriter.class.getName()
                        },
                        new JsonbJaxrsProviderFactory(entry.getValue()),
                        getJaxrsExtensionProperties(entry.getKey(), entry.getValue())
                    )
                )
            ),
            ignore(RegisterCdiExtension.ifPossible())
        ).run(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        _result.close();
    }

    @SuppressWarnings("serial")
    private Map<String, ?> getJaxrsExtensionProperties(
        Dictionary<String, ?> properties, Config config) {

        Enumeration<String> keys = properties.keys();

        return new HashMap<String, Object>() {{
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();

                if(!key.startsWith(".")) {
                    put(key, properties.get(key));
                }
            }

            put(JaxrsWhiteboardConstants.JAX_RS_EXTENSION, true);
            put(JaxrsWhiteboardConstants.JAX_RS_NAME, "johnzon.jsonb");

            putIfAbsent("ignores", config.ignores());
            putIfAbsent(
                JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT,
                config.osgi_jaxrs_application_select());
            putIfAbsent(
                JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE,
                config.osgi_jaxrs_media_type());
            putIfAbsent(
                Constants.SERVICE_RANKING,
                JsonbJaxrsProvider.class.getAnnotation(Priority.class).value());
        }};
    }

    private static Optional<String> notEmpty(String value) {
        return ofNullable(value).filter(s -> !s.isEmpty());
    }

    private static Optional<Boolean> whenTrue(boolean value) {
        return of(Boolean.valueOf(value)).filter(b -> b == Boolean.TRUE);
    }

    private static class JsonbJaxrsProviderFactory implements PrototypeServiceFactory<JsonbJaxrsProvider<?>> {

        private final Config config;

        public JsonbJaxrsProviderFactory(Config config) {
            this.config = config;
        }

        @Override
        public JsonbJaxrsProvider<?> getService(
            Bundle bundle, ServiceRegistration<JsonbJaxrsProvider<?>> registration) {

            return new ExtendedJsonbJaxrsProvider(config);
        }

        @Override
        public void ungetService(
            Bundle bundle, ServiceRegistration<JsonbJaxrsProvider<?>> registration, JsonbJaxrsProvider<?> service) {
        }

    }

    private static class ExtendedJsonbJaxrsProvider extends JsonbJaxrsProvider<Object> {
        public ExtendedJsonbJaxrsProvider(final Config config) {
            super(Arrays.asList(config.ignores()));
            this.config.setProperty("johnzon.skip-cdi", true); // by default disable it since to work it requires some effort

            whenTrue(config.throw_no_content_exception_on_empty_streams()).ifPresent(this::setThrowNoContentExceptionOnEmptyStreams);
            whenTrue(config.fail_on_unknown_properties()).ifPresent(this::setFailOnUnknownProperties);
            whenTrue(config.use_js_range()).ifPresent(this::setUseJsRange);
            notEmpty(config.other_properties()).ifPresent(this::setOtherProperties);
            whenTrue(config.ijson()).ifPresent(this::setIJson);
            notEmpty(config.encoding()).ifPresent(this::setEncoding);
            notEmpty(config.binary_datastrategy()).ifPresent(this::setBinaryDataStrategy);
            notEmpty(config.property_naming_strategy()).ifPresent(this::setPropertyNamingStrategy);
            notEmpty(config.property_order_strategy()).ifPresent(this::setPropertyOrderStrategy);
            whenTrue(config.null_values()).ifPresent(this::setNullValues);
            whenTrue(config.pretty()).ifPresent(this::setPretty);
            whenTrue(config.fail_on_missing_creator_values()).ifPresent(this::setFailOnMissingCreatorValues);
            notEmpty(config.polymorphic_serialization_predicate()).ifPresent(this::setPolymorphicSerializationPredicate);
            notEmpty(config.polymorphic_deserialization_predicate()).ifPresent(this::setPolymorphicDeserializationPredicate);
            notEmpty(config.polymorphic_discriminator()).ifPresent(this::setPolymorphicDiscriminator);
        }
    }

}
