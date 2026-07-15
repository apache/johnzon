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
package org.apache.johnzon.mapper.access;

import org.apache.johnzon.mapper.JohnzonIgnore;
import org.apache.johnzon.mapper.JohnzonRecord;
import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperBuilder;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import org.apache.johnzon.mapper.access.AccessMode.Reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MethodAccessModeTest {
    @Test
    public void defaultExcludedMethods() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        assertEquals(Set.of("toString", "hashCode"), mode.getExcludedMethods());
    }

    @Test
    public void customExcludedMethods() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        mode.setExcludedMethods(Set.of("foo", "bar"));
        assertEquals(Set.of("foo", "bar"), mode.getExcludedMethods());
    }

    @Test
    public void excludedMethodsNullResetsToDefault() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        mode.setExcludedMethods(null);
        assertEquals(Set.of("toString", "hashCode"), mode.getExcludedMethods());
    }

    @Test
    public void excludedMethodsEmptyResetsToDefault() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        mode.setExcludedMethods(Set.of());
        assertEquals(Set.of("toString", "hashCode"), mode.getExcludedMethods());
    }

    @Test
    public void excludedMethodsAreNotInReadersForRecords() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        final Map<String, MethodAccessMode.Reader> readers = mode.findReaders(JohnzonRecordSimple.class);
        assertFalse(readers.containsKey("toString"));
        assertFalse(readers.containsKey("hashCode"));
        assertTrue(readers.containsKey("name"));
    }

    @Test
    public void customExcludedMethodIsNotInReadersForRecords() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        mode.setExcludedMethods(Set.of("name"));
        final Map<String, MethodAccessMode.Reader> readers = mode.findReaders(JohnzonRecordSimple.class);
        assertFalse(readers.containsKey("name"));
        assertTrue(readers.containsKey("extra"));
    }

    @Test
    public void excludedMethodsWithMapperBuilderMethodMode() {
        final Mapper mapper = new MapperBuilder()
                .setAccessModeName("method")
                .setAccessModeExcludedMethods(Set.of("extra"))
                .build();
        final StringWriter writer = new StringWriter();
        final JohnzonRecordSimple obj = new JohnzonRecordSimple();
        obj.name = "test";
        obj.extra = "data";
        mapper.writeObject(obj, writer);
        assertTrue(writer.toString().contains("\"name\""));
        assertFalse(writer.toString().contains("\"extra\""));
    }

    @Test
    public void excludedMethodsHaveNoEffectOnNonRecordPropertyDescriptors() {
        final FieldAndMethodAccessMode mode = new FieldAndMethodAccessMode(true, true, false, true, false);
        mode.setExcludedMethods(Set.of("foo"));
        final Map<String, Reader> readers = mode.findReaders(Pojo.class);
        // "foo" is a standard bean property, excludedMethods only applies in the record branch
        assertTrue(readers.containsKey("foo"));
    }

    @Test
    public void johnzonIgnoreOnNonRecordPojoMethod() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        final Map<String, MethodAccessMode.Reader> readers = mode.findReaders(PojoWithIgnore.class);
        assertTrue(readers.containsKey("foo"));
        assertFalse(readers.containsKey("bar"));
    }

    @Test
    public void johnzonIgnoreVersionedOnNonRecordPojoMethod() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        mode.setVersion(1);
        final Map<String, MethodAccessMode.Reader> readers = mode.findReaders(PojoWithVersionedIgnore.class);
        assertTrue(readers.containsKey("foo"));
        assertFalse(readers.containsKey("bar"));

        final MethodAccessMode mode2 = new MethodAccessMode(true, true, true);
        mode2.setVersion(3);
        final Map<String, MethodAccessMode.Reader> readers2 = mode2.findReaders(PojoWithVersionedIgnore.class);
        assertTrue(readers2.containsKey("foo"));
        assertTrue(readers2.containsKey("bar"));
    }

    @Test
    public void johnzonIgnoreOnBooleanIsMethod() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        final Map<String, MethodAccessMode.Reader> readers = mode.findReaders(PojoWithIgnoredIsMethod.class);
        assertTrue(readers.containsKey("active"));
        assertFalse(readers.containsKey("deleted"));
    }

    @Test
    public void johnzonIgnoreVersionedOnBooleanIsMethod() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        mode.setVersion(1);
        final Map<String, MethodAccessMode.Reader> readers = mode.findReaders(PojoWithVersionedIgnoredIsMethod.class);
        assertTrue(readers.containsKey("active"));
        assertFalse(readers.containsKey("deleted"));

        final MethodAccessMode mode2 = new MethodAccessMode(true, true, true);
        mode2.setVersion(3);
        final Map<String, MethodAccessMode.Reader> readers2 = mode2.findReaders(PojoWithVersionedIgnoredIsMethod.class);
        assertTrue(readers2.containsKey("active"));
        assertTrue(readers2.containsKey("deleted"));
    }

    @Test
    public void johnzonIgnoreDefaultMinVersionAlwaysIgnored() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        final Map<String, MethodAccessMode.Reader> readers = mode.findReaders(WithJohnzonIgnoreAlways.class);
        assertFalse(readers.containsKey("hidden"));
        assertTrue(readers.containsKey("visible"));
    }

    @Test
    public void johnzonIgnoreMinVersionBelowMapperVersion() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        mode.setVersion(2);
        final Map<String, MethodAccessMode.Reader> readers = mode.findReaders(WithJohnzonIgnoreVersioned.class);
        assertTrue(readers.containsKey("hidden"));
        assertTrue(readers.containsKey("visible"));
    }

    @Test
    public void johnzonIgnoreMinVersionAboveMapperVersion() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        mode.setVersion(1);
        final Map<String, MethodAccessMode.Reader> readers = mode.findReaders(WithJohnzonIgnoreVersioned.class);
        assertFalse(readers.containsKey("hidden"));
        assertTrue(readers.containsKey("visible"));
    }

    @Test
    public void johnzonIgnoreMinVersionWithMethodAccessModeDirectly() {
        final Mapper mapper = new MapperBuilder()
                .setAccessModeName("method")
                .setVersion(0)
                .build();
        final StringWriter writer = new StringWriter();
        final WithJohnzonIgnoreVersioned obj = new WithJohnzonIgnoreVersioned();
        obj.hidden = "secret";
        obj.visible = "public";
        mapper.writeObject(obj, writer);
        assertTrue(writer.toString().contains("\"visible\""));
        assertFalse(writer.toString().contains("\"hidden\""));
    }

    @Test
    public void johnzonIgnoreMinVersionMetWithMethodAccessMode() {
        final Mapper mapper = new MapperBuilder()
                .setAccessModeName("method")
                .setVersion(2)
                .build();
        final StringWriter writer = new StringWriter();
        final WithJohnzonIgnoreVersioned obj = new WithJohnzonIgnoreVersioned();
        obj.hidden = "secret";
        obj.visible = "public";
        mapper.writeObject(obj, writer);
        assertTrue(writer.toString().contains("\"visible\""));
        assertTrue(writer.toString().contains("\"hidden\""));
    }

    @Test
    public void johnzonIgnoreVersionedEndToEndDefaultAccessMode() {
        final Mapper mapper = new MapperBuilder().setVersion(0).build();
        final StringWriter writer = new StringWriter();
        final WithJohnzonIgnoreVersioned obj = new WithJohnzonIgnoreVersioned();
        obj.hidden = "secret";
        obj.visible = "public";
        mapper.writeObject(obj, writer);
        assertTrue(writer.toString().contains("\"visible\""));
        assertFalse(writer.toString().contains("\"hidden\""));
    }

    @Test
    public void johnzonIgnoreVersionedMetEndToEndDefaultAccessMode() {
        final Mapper mapper = new MapperBuilder().setVersion(2).build();
        final StringWriter writer = new StringWriter();
        final WithJohnzonIgnoreVersioned obj = new WithJohnzonIgnoreVersioned();
        obj.hidden = "secret";
        obj.visible = "public";
        mapper.writeObject(obj, writer);
        assertTrue(writer.toString().contains("\"visible\""));
        assertTrue(writer.toString().contains("\"hidden\""));
    }

    @Test
    public void fieldAndMethodAccessModeAlwaysIgnoredMethodSuppressesFieldReader() {
        final FieldAndMethodAccessMode mode = new FieldAndMethodAccessMode(true, true, false, true, false);
        final Map<String, Reader> readers = mode.findReaders(PojoWithIgnore.class);
        assertTrue(readers.containsKey("foo"));
        assertFalse(readers.containsKey("bar"));
    }

    @Test
    public void fieldAndMethodAccessModeVersionedIgnoreSuppressesFieldReader() {
        final FieldAndMethodAccessMode mode = new FieldAndMethodAccessMode(true, true, false, true, false);
        mode.setVersion(1);
        final Map<String, Reader> readers = mode.findReaders(PojoWithVersionedIgnore.class);
        assertTrue(readers.containsKey("foo"));
        assertFalse(readers.containsKey("bar"));

        final FieldAndMethodAccessMode mode2 = new FieldAndMethodAccessMode(true, true, false, true, false);
        mode2.setVersion(3);
        final Map<String, Reader> readers2 = mode2.findReaders(PojoWithVersionedIgnore.class);
        assertTrue(readers2.containsKey("foo"));
        assertTrue(readers2.containsKey("bar"));
    }

    @Test
    public void fieldAndMethodAccessModeVersionPassThrough() {
        final FieldAndMethodAccessMode mode = new FieldAndMethodAccessMode(true, true, false, true, false);
        mode.setVersion(5);
        assertEquals(5, mode.getVersion());
    }

    @Test
    public void fieldAndMethodAccessModeExcludedMethodsPassThrough() {
        final FieldAndMethodAccessMode mode = new FieldAndMethodAccessMode(true, true, false, true, false);
        mode.setExcludedMethods(Set.of("custom"));
        assertEquals(Set.of("custom"), mode.getExcludedMethods());
    }

    @Test
    public void fieldAndMethodAccessModeSupportAllGettersPassThrough() {
        final FieldAndMethodAccessMode mode = new FieldAndMethodAccessMode(true, true, false, true, false);
        mode.setSupportAllRecordAttributes(true);
        assertTrue(mode.isSupportAllGetters());
    }

    @Test
    public void supportAllRecordAttributesNoEffectOnJohnzonRecord() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        mode.setSupportAllRecordAttributes(false);
        final Map<String, MethodAccessMode.Reader> readers = mode.findReaders(JohnzonRecordSimple.class);
        // @JohnzonRecord returns null componentNames, so supportAllRecordAttributes has no effect
        assertTrue(readers.containsKey("name"));
        assertTrue(readers.containsKey("extra"));
    }

    @Test
    public void supportAllRecordAttributesTrueAlsoIncludesAllForJohnzonRecord() {
        final MethodAccessMode mode = new MethodAccessMode(true, true, true);
        mode.setSupportAllRecordAttributes(true);
        final Map<String, MethodAccessMode.Reader> readers = mode.findReaders(JohnzonRecordSimple.class);
        assertTrue(readers.containsKey("name"));
        assertTrue(readers.containsKey("extra"));
    }

    @JohnzonRecord
    public static class JohnzonRecordSimple {
        private String name;
        private String extra;

        public String name() {
            return name;
        }

        public String extra() {
            return extra;
        }
    }

    public static class Pojo {
        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(final String foo) {
            this.foo = foo;
        }
    }

    public static class PojoWithIgnore {
        private String foo;
        private String bar;

        public String getFoo() {
            return foo;
        }

        public void setFoo(final String foo) {
            this.foo = foo;
        }

        @JohnzonIgnore
        public String getBar() {
            return bar;
        }

        public void setBar(final String bar) {
            this.bar = bar;
        }
    }

    public static class PojoWithVersionedIgnore {
        private String foo;
        private String bar;

        public String getFoo() {
            return foo;
        }

        public void setFoo(final String foo) {
            this.foo = foo;
        }

        @JohnzonIgnore(minVersion = 2)
        public String getBar() {
            return bar;
        }

        public void setBar(final String bar) {
            this.bar = bar;
        }
    }

    public static class PojoWithIgnoredIsMethod {
        private boolean active;
        private boolean deleted;

        public boolean isActive() {
            return active;
        }

        public void setActive(final boolean active) {
            this.active = active;
        }

        @JohnzonIgnore
        public boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(final boolean deleted) {
            this.deleted = deleted;
        }
    }

    public static class PojoWithVersionedIgnoredIsMethod {
        private boolean active;
        private boolean deleted;

        public boolean isActive() {
            return active;
        }

        public void setActive(final boolean active) {
            this.active = active;
        }

        @JohnzonIgnore(minVersion = 2)
        public boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(final boolean deleted) {
            this.deleted = deleted;
        }
    }

    public static class WithJohnzonIgnoreAlways {
        String visible;
        String hidden;

        public String getVisible() {
            return visible;
        }

        @JohnzonIgnore
        public String getHidden() {
            return hidden;
        }
    }

    public static class WithJohnzonIgnoreVersioned {
        String visible;
        String hidden;

        public String getVisible() {
            return visible;
        }

        @JohnzonIgnore(minVersion = 2)
        public String getHidden() {
            return hidden;
        }
    }
}
