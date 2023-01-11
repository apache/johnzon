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

@Capability(namespace = SERVICE_NAMESPACE,
    attribute = {
        CDI_EXTENSION_PROPERTY + "=JavaJSONB",
        "objectClass:List<String>='jakarta.enterprise.inject.spi.Extension'"})
@Capability(namespace = CDI_EXTENSION_PROPERTY,
    attribute = CDI_EXTENSION_PROPERTY + "=JavaJSONB")
@Requirement(
    namespace = EXTENDER_NAMESPACE,
    name = CDI_CAPABILITY_NAME,
    version = CDI_SPECIFICATION_VERSION,
    effective = EFFECTIVE_ACTIVE)
package org.apache.johnzon.osgi.cdi;

import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.resource.Namespace.EFFECTIVE_ACTIVE;
import static org.osgi.service.cdi.CDIConstants.CDI_CAPABILITY_NAME;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;
import static org.osgi.service.cdi.CDIConstants.CDI_SPECIFICATION_VERSION;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Requirement;
