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
package org.apache.johnzon.jsonb.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

public class PerHierarchyAndLexicographicalOrderFieldComparatorTest {

    @Test
    public void inheritedFieldsComeFirstThenLexicographicalWithinLevel() {
        final Comparator<String> comparator = new PerHierarchyAndLexicographicalOrderFieldComparator(Sub.class);
        final List<String> order = Arrays.asList("subField", "aField", "superField", "zField");
        Collections.sort(order, comparator);
        assertEquals(Arrays.asList("superField", "zField", "aField", "subField"), order);
    }

    @Test
    public void equalNamesCompareZero() {
        final Comparator<String> comparator = new PerHierarchyAndLexicographicalOrderFieldComparator(Sub.class);
        assertEquals(0, comparator.compare("aField", "aField"));
    }

    @Test
    public void onlyOneInCacheComparesStrings() {
        final Comparator<String> comparator = new PerHierarchyAndLexicographicalOrderFieldComparator(Sub.class);
        final List<String> order = Arrays.asList("notAMember", "aField");
        Collections.sort(order, comparator);
        assertEquals(Arrays.asList("aField", "notAMember"), order);
    }

    @Test
    public void bothUnknownCompareStrings() {
        final Comparator<String> comparator = new PerHierarchyAndLexicographicalOrderFieldComparator(Sub.class);
        final List<String> order = Arrays.asList("zeta", "alpha");
        Collections.sort(order, comparator);
        assertEquals(Arrays.asList("alpha", "zeta"), order);
    }

    @Test
    public void nullHandling() {
        final Comparator<String> comparator = new PerHierarchyAndLexicographicalOrderFieldComparator(Sub.class);
        assertTrue(comparator.compare(null, "aField") > 0);
        assertTrue(comparator.compare("aField", null) < 0);
        assertEquals(0, comparator.compare(null, null));
        assertEquals(0, comparator.compare("aField", "aField"));
    }

    @Test
    public void booleanIsAccessorIsIdentified() {
        final Comparator<String> comparator = new PerHierarchyAndLexicographicalOrderFieldComparator(BooleanHolder.class);
        assertEquals(0, comparator.compare("flag", "flag"));
        final List<String> order = Arrays.asList("other", "flag");
        Collections.sort(order, comparator);
        assertEquals(Arrays.asList("flag", "other"), order);
    }

    public static class Super {
        private String superField;
        private String zField;

        public String getSuperField() {
            return superField;
        }

        public void setSuperField(final String superField) {
            this.superField = superField;
        }

        public String getzField() {
            return zField;
        }

        public void setzField(final String zField) {
            this.zField = zField;
        }
    }

    public static class Sub extends Super {
        private String subField;
        private String aField;

        public String getSubField() {
            return subField;
        }

        public void setSubField(final String subField) {
            this.subField = subField;
        }

        public String getaField() {
            return aField;
        }

        public void setaField(final String aField) {
            this.aField = aField;
        }
    }

    public static class BooleanHolder {
        private boolean flag;

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(final boolean flag) {
            this.flag = flag;
        }
    }
}