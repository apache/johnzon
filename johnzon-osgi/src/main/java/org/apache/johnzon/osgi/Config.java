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

import jakarta.ws.rs.core.MediaType;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    description = "Configuration for Johnzon JSONB JAX-RS Whiteboard Message Body Reader & Writer",
    name = "Johnzon JAX-RS JSONB",
    pid = Config.CONFIG_PID
)
public @interface Config {
    public static final String CONFIG_PID = "org.apache.johnzon.jaxrs.jsonb";

    @AttributeDefinition(description = "List of fully qualified class names to ignore")
    String[] ignores() default {};

    @AttributeDefinition(description = "Filter expression used to match the extension to JAX-RS Whiteboard Applications")
    String osgi_jaxrs_application_select() default "(!(johnzon.jsonb=false))";

    @AttributeDefinition(description = "List of media types handled by the extension")
    String[] osgi_jaxrs_media_type() default MediaType.APPLICATION_JSON;

    boolean throw_no_content_exception_on_empty_streams() default false;

    boolean fail_on_unknown_properties() default false;

    boolean use_js_range() default false;

    String other_properties() default "";

    boolean ijson() default false;

    String encoding() default "";

    String binary_datastrategy() default "";

    String property_naming_strategy() default "";

    String property_order_strategy() default "";

    boolean null_values() default false;

    boolean pretty() default false;

    boolean fail_on_missing_creator_values() default false;

    String polymorphic_serialization_predicate() default "";

    String polymorphic_deserialization_predicate() default "";

    String polymorphic_discriminator() default "";

}