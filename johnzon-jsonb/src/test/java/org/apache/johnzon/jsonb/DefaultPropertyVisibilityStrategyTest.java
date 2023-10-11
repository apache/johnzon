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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.json.bind.annotation.JsonbVisibility;

import org.junit.Test;

public class DefaultPropertyVisibilityStrategyTest {
    @Test // note it is not a valid case since our default impl is internal but it guarantees we dont wrongly impl equals
    public void subclassing() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final TheClass theClass = jsonb.fromJson("{\"foo\":true}", TheClass.class);
            assertTrue(theClass.isFoo());
        }
    }

    @Test
    public void annotated() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            final VisibleCauseAnnotated theClass = jsonb.fromJson("{\"foo\":true}", VisibleCauseAnnotated.class);
            assertTrue(theClass.isFoo());
        }
    }

    @Test
    public void hiddenGetter() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            assertFalse(jsonb.fromJson("{\"foo\":true}", HideAllModel.class).isFoo());
            assertFalse(jsonb.fromJson("{\"foo\":true}", HideAllDefaultModel.class).isFoo());
        }
    }

    @Test
    public void matchingPrivateAccessors() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            assertEquals("{}", jsonb.toJson(new PublicFieldWithPrivateAccessors()));
            assertEquals("{}", jsonb.toJson(new PublicBooleanFieldWithPrivateAccessors()));

            {
                final PublicFieldWithPrivateAccessors unmarshalledObject =
                    jsonb.fromJson("{\"foo\":\"bad\"}", PublicFieldWithPrivateAccessors.class);
                assertEquals("bar", unmarshalledObject.foo);
            }
            {
                final PublicBooleanFieldWithPrivateAccessors unmarshalledObject =
                    jsonb.fromJson("{\"foo\":\"false\"}", PublicBooleanFieldWithPrivateAccessors.class);
                assertEquals(true, unmarshalledObject.foo);
            }
        }
    }

    @Test
    public void noMatchingPrivateAccessors() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            assertEquals("{\"foo\":\"bar\"}", jsonb.toJson(new PublicFieldWithoutMatchingPrivateAccessors()));

            final PublicFieldWithoutMatchingPrivateAccessors unmarshalledObject =
                jsonb.fromJson("{\"foo\":\"good\"}", PublicFieldWithoutMatchingPrivateAccessors.class);
            assertEquals("good", unmarshalledObject.foo);
        }
    }

    @Test
    public void oneAccessorOnly() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create()) {
            assertEquals("{}", jsonb.toJson(new PublicFieldWithMatchingPrivateGetterAccessors()));
            assertEquals("{\"foo\":\"bar\"}", jsonb.toJson(new PublicFieldWithMatchingPrivateSetterAccessors()));
            assertEquals("{\"foo\":\"bar\"}", jsonb.toJson(new PublicFieldWithPublicAccessors()));

            {
                final PublicFieldWithMatchingPrivateGetterAccessors unmarshalledObject =
                    jsonb.fromJson("{\"foo\":\"good\"}", PublicFieldWithMatchingPrivateGetterAccessors.class);
                assertEquals("good", unmarshalledObject.foo);
            }
            {
                final PublicFieldWithMatchingPrivateSetterAccessors unmarshalledObject =
                    jsonb.fromJson("{\"foo\":\"bad\"}", PublicFieldWithMatchingPrivateSetterAccessors.class);
                assertEquals("bar", unmarshalledObject.foo);
            }
            {
                final PublicFieldWithPublicAccessors unmarshalledObject =
                    jsonb.fromJson("{\"foo\":\"good\"}", PublicFieldWithPublicAccessors.class);
                assertEquals("good", unmarshalledObject.foo);
            }
        }
    }

    public static class HideAll extends DefaultPropertyVisibilityStrategy {
        @Override
        public boolean isVisible(final Field field) {
            return false;
        }

        @Override
        public boolean isVisible(final Method method) {
            return false;
        }
    }

    public static class MyVisibility extends DefaultPropertyVisibilityStrategy {
        @Override
        public boolean isVisible(final Field field) {
            return true;
        }

        @Override
        public boolean isVisible(final Method method) {
            return true;
        }
    }

    // if there is a matching private getter/setter, the field even if it's public must be ignored
    public static final class PublicFieldWithPrivateAccessors {
        public String foo = "bar";

        private String getFoo() {
            return foo;
        }

        private void setFoo(final String foo) {
            this.foo = foo;
        }
    }

    public static final class PublicFieldWithPublicAccessors {
        public String foo = "bar";

        public String getFoo() {
            return foo;
        }

        public void setFoo(final String foo) {
            this.foo = foo;
        }
    }

    public static final class PublicBooleanFieldWithPrivateAccessors {
        public boolean foo = true;

        private boolean isFoo() {
            return foo;
        }

        private void setFoo(final boolean foo) {
            this.foo = foo;
        }
    }

    // if there is not matching getter/setter for a public field, then the field is used directly
    public static final class PublicFieldWithoutMatchingPrivateAccessors {
        public String foo = "bar";
    }

    public static final class PublicFieldWithMatchingPrivateGetterAccessors {
        public String foo = "bar";

        private String getFoo() {
            return foo;
        }
    }

    public static final class PublicFieldWithMatchingPrivateSetterAccessors {
        public String foo = "bar";

        private void setFoo(final String foo) {
            this.foo = foo;
        }
    }

    @JsonbVisibility(MyVisibility.class)
    public static final class TheClass {
        @JsonbProperty
        private boolean foo;

        public boolean isFoo() {
            return foo;
        }
    }

    public static final class VisibleCauseAnnotated {
        @JsonbProperty
        private boolean foo;

        public boolean isFoo() {
            return foo;
        }
    }

    @JsonbVisibility(HideAll.class)
    public static final class HideAllModel {
        protected boolean foo;

        @JsonbTransient
        public boolean isFoo() {
            return foo;
        }
    }

    public static final class HideAllDefaultModel {
        protected boolean foo;

        @JsonbTransient
        public boolean isFoo() {
            return foo;
        }
    }
}
