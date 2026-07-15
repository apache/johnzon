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

import org.apache.johnzon.mapper.JohnzonIgnore;
import org.apache.johnzon.mapper.JohnzonRecord;
import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

import jakarta.json.bind.annotation.JsonbTransient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JohnzonIgnoreJsonbTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    @Rule
    public final JsonbRule jsonbWithExcludedMethods = new JsonbRule()
            .withProperty("johnzon.accessMode.excludedMethods", "extra")
            .withProperty("johnzon.accessMode.supportAllRecordAttributes", Boolean.TRUE);

    @Rule
    public final JsonbRule jsonbWithVersion0 = new JsonbRule()
            .withProperty("johnzon.version", 0);

    @Rule
    public final JsonbRule jsonbWithVersion2 = new JsonbRule()
            .withProperty("johnzon.version", 2);

    @Test
    public void johnzonIgnoreDefaultMinVersionIsTransient() {
        final AlwaysIgnored obj = new AlwaysIgnored();
        obj.visible = "yes";
        obj.hidden = "no";
        final String json = jsonb.toJson(obj);
        assertTrue(json.contains("\"visible\""));
        assertFalse(json.contains("\"hidden\""));
    }

    @Test
    public void johnzonIgnoreWithJsonbTransientBothAreTransient() {
        final BothTransient obj = new BothTransient();
        obj.field1 = "a";
        obj.field2 = "b";
        final String json = jsonb.toJson(obj);
        assertFalse(json.contains("\"field1\""));
        assertFalse(json.contains("\"field2\""));
    }

    @Test
    public void johnzonIgnoreVersionedRespectedViaJsonbConfig() {
        final VersionedRecord obj = new VersionedRecord();
        obj.name = "test";
        obj.extra = "data";
        final String json = jsonbWithExcludedMethods.toJson(obj);
        assertTrue(json.contains("\"name\""));
        assertFalse(json.contains("\"extra\""));
    }

    @Test
    public void johnzonIgnoreMinVersionBelowConfiguredVersionIsNotTransient() {
        final VersionedPojo obj = new VersionedPojo();
        obj.visible = "yes";
        obj.hidden = "no";
        final String json = jsonbWithVersion2.toJson(obj);
        assertTrue(json.contains("\"visible\""));
        assertTrue(json.contains("\"hidden\""));
    }

    @Test
    public void johnzonIgnoreMinVersionAboveConfiguredVersionIsTransient() {
        final VersionedPojo obj = new VersionedPojo();
        obj.visible = "yes";
        obj.hidden = "no";
        final String json = jsonbWithVersion0.toJson(obj);
        assertTrue(json.contains("\"visible\""));
        assertFalse(json.contains("\"hidden\""));
    }

    public static class AlwaysIgnored {
        public String visible;

        @JohnzonIgnore
        public String hidden;

        public String getVisible() {
            return visible;
        }

        public void setVisible(final String visible) {
            this.visible = visible;
        }

        public String getHidden() {
            return hidden;
        }

        public void setHidden(final String hidden) {
            this.hidden = hidden;
        }
    }

    public static class BothTransient {
        @JsonbTransient
        public String field1;

        @JohnzonIgnore
        public String field2;

        public String getField1() {
            return field1;
        }

        public void setField1(final String field1) {
            this.field1 = field1;
        }

        public String getField2() {
            return field2;
        }

        public void setField2(final String field2) {
            this.field2 = field2;
        }
    }

    @JohnzonRecord
    public static class VersionedRecord {
        private String name;
        private String extra;

        public String name() {
            return name;
        }

        public String extra() {
            return extra;
        }
    }

    public static class VersionedPojo {
        private String visible;
        private String hidden;

        public String getVisible() {
            return visible;
        }

        public void setVisible(final String visible) {
            this.visible = visible;
        }

        @JohnzonIgnore(minVersion = 2)
        public String getHidden() {
            return hidden;
        }

        public void setHidden(final String hidden) {
            this.hidden = hidden;
        }
    }
}
