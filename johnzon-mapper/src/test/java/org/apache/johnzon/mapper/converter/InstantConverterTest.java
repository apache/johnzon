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
package org.apache.johnzon.mapper.converter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.johnzon.mapper.MapperBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InstantConverterTest {

    @Test
    public void convert() {
        final Model model = new Model();
        Instant testInstant = Instant.parse("2007-12-03T14:15:00.00Z");
        model.setDate(testInstant);
        String actual = new MapperBuilder().build().writeObjectAsString(model);
        assertEquals(testInstant.getEpochSecond(), Model.class.cast(new MapperBuilder().build().readObject(actual, Model.class)).getDate().getEpochSecond());

        InstantConverter ic = new InstantConverter();

        testInstant = Instant.parse("2007-12-03T14:15:00.00Z");
        DateTimeFormatter formatterOut = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC"));
        String expectedJsonDateOut = formatterOut.format(testInstant);

        testParse("2007-12-03T11:15:00-03:00", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T14:15:00Z", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T17:15:00+0300", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T17:15:00.0+03:00", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T17:15:00.0+0300", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T17:15:00.00+03:00", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T11:15:00.00-03:00", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T17:15:00.00+0300", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T17:15:00.000+03:00", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T17:15:00.000+0300", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T14:15:00.000+0000", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T14:15:00.000Z", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T17:15+03:00", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T17:15+0300", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T14:15:00", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T14:15Z", testInstant, expectedJsonDateOut);
        testParse("2007-12-03T14:15", testInstant, expectedJsonDateOut);
        testParse("20071203141500Z", testInstant, expectedJsonDateOut);
        testParse("200712031415Z", testInstant, expectedJsonDateOut);
        testParse("20071203111500-0300", testInstant, expectedJsonDateOut);
        testParse("20071203171500+0300", testInstant, expectedJsonDateOut);
        testParse("20071203111500-03:00", testInstant, expectedJsonDateOut);
        testParse("20071203171500+03:00", testInstant, expectedJsonDateOut);
        testParse("200712031115-0300", testInstant, expectedJsonDateOut);
        testParse("200712031115-03:00", testInstant, expectedJsonDateOut);
        testParse("200712031415", testInstant, expectedJsonDateOut);
        testParse("20071203141500", testInstant, expectedJsonDateOut);

    }
    
    private void testParse(String jsonDateIn, Instant testInstant, String expectedJsonDateOut) {
        InstantConverter ic = new InstantConverter();
        Instant convertedToInstant = ic.fromString(jsonDateIn);
        assertEquals(testInstant, convertedToInstant);
        String backToJsonDate = ic.toString(convertedToInstant);
        // System.out.println(backToJsonDate);
        assertEquals(expectedJsonDateOut, backToJsonDate);
    }

    public static class Model {

        private Instant date;

        public Instant getDate() {
            return date;
        }

        public void setDate(Instant date) {
            this.date = date;
        }
    }
}
