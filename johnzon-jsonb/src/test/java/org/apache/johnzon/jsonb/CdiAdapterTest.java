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
package org.apache.johnzon.jsonb;

import org.apache.johnzon.jsonb.cdi.JohnzonCdiExtension;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.corespi.scanner.xbean.CdiArchive;
import org.apache.webbeans.corespi.se.DefaultScannerService;
import org.apache.webbeans.lifecycle.StandaloneLifeCycle;
import org.apache.webbeans.proxy.OwbNormalScopeProxy;
import org.apache.webbeans.service.ClassLoaderProxyService;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.DefiningClassService;
import org.apache.webbeans.spi.LoaderService;
import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.api.ResourceReference;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.webbeans.xml.DefaultBeanArchiveInformation;
import org.junit.Test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CdiAdapterTest {
    @Test
    public void run() throws Exception {
        final ClassLoader currentClassLoader = WebBeansUtil.getCurrentClassLoader();
        WebBeansFinder.clearInstances(currentClassLoader);
        final Map<Class<?>, Object> services = new HashMap<>();
        services.put(ScannerService.class, new DefaultScannerService() {
            @Override
            protected void configure() {
                initFinder();
                final DefaultBeanArchiveInformation information = new DefaultBeanArchiveInformation("file://foo");
                information.setBeanDiscoveryMode(BeanArchiveService.BeanDiscoveryMode.ALL);
                try {
                    archive.classesByUrl().put(information.getBdaUrl(), new CdiArchive.FoundClasses(
                            URI.create(information.getBdaUrl()).toURL(), asList(
                            Service.class.getName(), ModelAdapter.class.getName()),
                            information));
                } catch (final MalformedURLException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        services.put(ResourceInjectionService.class, new ResourceInjectionService() {
            @Override
            public void injectJavaEEResources(final Object managedBeanInstance) {
                System.out.println();
            }

            @Override
            public <X, T extends Annotation> X getResourceReference(final ResourceReference<X, T> resourceReference) {
                return null;
            }

            @Override
            public void clear() {
                // no-op
            }
        });
        services.put(LoaderService.class, new LoaderService() {
            @Override
            public <T> List<T> load(final Class<T> serviceType, final ClassLoader loader) {
                if (Extension.class == serviceType) {
                    return singletonList(serviceType.cast(new JohnzonCdiExtension()));
                }
                return Collections.emptyList();
            }

            @Override
            public <T> List<T> load(final Class<T> serviceType) {
                return emptyList();
            }
        });
        final Properties properties = new Properties();
        properties.setProperty(DefiningClassService.class.getName(), ClassLoaderProxyService.class.getName());
        final WebBeansContext webBeansContext = new WebBeansContext(services, properties);
        DefaultSingletonService.class.cast(WebBeansFinder.getSingletonService()).register(
                currentClassLoader, webBeansContext);
        final ContainerLifecycle testLifecycle = new StandaloneLifeCycle();
        testLifecycle.startApplication(null);
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            assertEquals("{\"model\":\"5\"}", jsonb.toJson(new Root(new Model(5))));
        } finally {
            testLifecycle.stopApplication(null);
            WebBeansFinder.clearInstances(currentClassLoader);
        }
    }

    public static class Root {
        @JsonbTypeAdapter(ModelAdapter.class)
        public final Model model;

        public Root(final Model model) {
            this.model = model;
        }
    }

    public static class Model {
        private final int val;

        public Model(final int i) {
            val = i;
        }
    }

    @ApplicationScoped
    public static class Service {
        public String toString(final Model model) {
            return Integer.toString(model.val);
        }
    }

    @ApplicationScoped
    public static class ModelAdapter implements JsonbAdapter<Model, String> {
        @Inject
        private Service service;

        @Override
        public Model adaptFromJson(final String obj) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public String adaptToJson(final Model obj) throws Exception {
            assertTrue(OwbNormalScopeProxy.class.isInstance(service)); // additional test
            return service.toString(obj);
        }
    }
}
