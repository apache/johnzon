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

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.se.DefaultScannerService;
import org.apache.webbeans.lifecycle.StandaloneLifeCycle;
import org.apache.webbeans.proxy.OwbNormalScopeProxy;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.adapter.JsonbAdapter;
import javax.json.bind.annotation.JsonbTypeAdapter;
import java.util.Properties;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CdiAdapterTest {
    @Test
    public void run() {
        WebBeansFinder.clearInstances(WebBeansUtil.getCurrentClassLoader());
        final ContainerLifecycle testLifecycle = new StandaloneLifeCycle();
        new WebBeansContext(singletonMap(
                ScannerService.class, new DefaultScannerService() {
                    @Override
                    protected AnnotationFinder initFinder() {
                        return new AnnotationFinder(new ClassesArchive(Service.class, ModelAdapter.class));
                    }
                }), new Properties());
        testLifecycle.startApplication(null);
        try {
            Jsonb jsonb = JsonbBuilder.create();
            assertEquals("{\"model\":\"5\"}", jsonb.toJson(new Root(new Model(5))));
            try {
                AutoCloseable.class.cast(jsonb).close();
            } catch (final Exception e) {
                fail(e.getMessage());
            }
        } finally {
            testLifecycle.stopApplication(null);
            WebBeansFinder.clearInstances(WebBeansUtil.getCurrentClassLoader());
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
