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
package org.apache.johnzon.websocket.internal;

import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpointConfig;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public abstract class TypeAwareDecoder {
    protected Type type;

    public TypeAwareDecoder() {
        // no-op
    }

    public TypeAwareDecoder(final Type type) {
        this.type = type;
    }

    protected void init(final EndpointConfig endpointConfig) {
        if (type != null) {
            return;
        }

        if (ServerEndpointConfig.class.isInstance(endpointConfig)) {
            final Class<?> endpointClass = ServerEndpointConfig.class.cast(endpointConfig).getEndpointClass();
            for (final Method m : endpointClass.getMethods()) {
                if (Object.class == m.getDeclaringClass()) {
                    continue;
                }
                if (m.getAnnotation(OnMessage.class) != null) {
                    final Type[] genericParameterTypes = m.getGenericParameterTypes();
                    for (int i = 0; i < genericParameterTypes.length; i++) {
                        if (genericParameterTypes[i] == Session.class) {
                            continue;
                        }
                        boolean param = false;
                        for (final Annotation a : m.getParameterAnnotations()[i]) {
                            if (PathParam.class == a.annotationType()) {
                                param = true;
                                break;
                            }
                        }
                        if (!param) {
                            this.type = genericParameterTypes[i];
                            break;
                        }
                    }
                    break;
                }
            }
            if (type == null) {
                throw new IllegalArgumentException("didn't find @OnMessage in " + endpointClass);
            }
        } else {
            type = Type.class.cast(endpointConfig.getUserProperties().get("johnzon.websocket.message.type"));
            if (type == null) {
                throw new IllegalArgumentException("didn't find johnzon.websocket.message.type");
            }
        }
    }
}
