/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.jsonb;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;

import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.JohnzonConverter;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

/**
 * Test that a Johnzon Converter works even in JsonB mode
 */
public class JohnzonConverterInJsonbTest {

    @Test
    public void testJohnzonConverter() {
        TestDTO dto = new TestDTO();
        dto.setInstant(Instant.now());

        Jsonb jsonb = JsonbProvider.provider().create().build();
        String json = jsonb.toJson(dto);
        assertNotNull(json);
    }

    public static class TestDTO {
        @JohnzonConverter(TestInstantConverter.class)
        private Instant instant;

        public Instant getInstant() {
            return instant;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }
    }


    public static class TestInstantConverter implements Converter<Instant> {
        public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern(
                "yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public String toString(Instant date) {
            return FORMAT.withZone(ZoneId.of("UTC")).format(date);
        }

        @Override
        public Instant fromString(String dateStr) {
            return LocalDateTime.parse(dateStr, FORMAT)
                    .atZone(ZoneId.of("UTC"))
                    .toInstant();
        }

    }

}
