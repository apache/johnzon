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

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import jakarta.json.bind.config.PropertyNamingStrategy;

import static org.junit.Assert.assertEquals;

@RunWith(Theories.class)
public class PropertyNamingStrategyFactoryTest {
    @DataPoints()
    public static String[][] points() {
        return new String[][] {
            new String[] { PropertyNamingStrategy.IDENTITY, "a", "a" },
            new String[] { PropertyNamingStrategy.IDENTITY, "aBEOCBDJ4397dkabqWLCd", "aBEOCBDJ4397dkabqWLCd" },
            new String[] { PropertyNamingStrategy.CASE_INSENSITIVE, "aBEOCBDJ4397dkabqWLCd", "aBEOCBDJ4397dkabqWLCd" }, // not really testable there
            new String[] { PropertyNamingStrategy.LOWER_CASE_WITH_DASHES, "lower-dash", "lower-dash" },
            new String[] { PropertyNamingStrategy.LOWER_CASE_WITH_DASHES, "lower_dash", "lower_dash" },
            new String[] { PropertyNamingStrategy.LOWER_CASE_WITH_DASHES, "lowerDash", "lower-dash" },
            new String[] { PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES, "lower_under", "lower_under" },
            new String[] { PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES, "lowerUnder", "lower_under" },
            new String[] { PropertyNamingStrategy.UPPER_CAMEL_CASE, "fooBar", "FooBar" },
            new String[] { PropertyNamingStrategy.UPPER_CAMEL_CASE_WITH_SPACES, "fooBar", "Foo Bar" },
        };
    }

    @Theory
    public void valid(final String[] config) {
        assertEquals(config[2], new PropertyNamingStrategyFactory(config[0]).create().translateName(config[1]));
    }
}
