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

package org.apache.johnzon.osgi.cdi;

import static org.apache.aries.component.dsl.OSGi.NOOP;
import static org.apache.aries.component.dsl.OSGi.effects;
import static org.apache.aries.component.dsl.OSGi.register;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Priority;

import org.apache.aries.component.dsl.OSGi;
import org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider;
import org.osgi.framework.Constants;
import org.osgi.service.cdi.CDIConstants;

public class RegisterCdiExtension {

    private RegisterCdiExtension() {
    }

    public static OSGi<?> ifPossible() {
        if (tryLoadingCdi()) {
            return register(
                jakarta.enterprise.inject.spi.Extension.class,
                new org.apache.johnzon.osgi.cdi.CdiExtensionFactory(),
                getCdiExtensionProperties()
            );
        } else {
           return effects(NOOP, NOOP);
        }
    }

    private static boolean tryLoadingCdi() {
        try {
            Class.forName("jakarta.enterprise.inject.spi.Extension");
            return true;
        } catch (ClassNotFoundException cfne) {
            return false;
        }
    }

    @SuppressWarnings("serial")
    private static Map<String, Object> getCdiExtensionProperties() {
        return new HashMap<String, Object>() {{
            put(CDIConstants.CDI_EXTENSION_PROPERTY, "JavaJSONB");
            put("aries.cdi.extension.mode", "implicit");

            putIfAbsent(
                Constants.SERVICE_RANKING,
                JsonbJaxrsProvider.class.getAnnotation(Priority.class).value());
        }};
    }

}
