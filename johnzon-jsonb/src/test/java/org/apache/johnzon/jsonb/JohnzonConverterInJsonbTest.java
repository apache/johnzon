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

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.spi.JsonbProvider;

import org.apache.johnzon.mapper.Converter;
import org.apache.johnzon.mapper.JohnzonConverter;
import org.apache.johnzon.mapper.MappingGenerator;
import org.apache.johnzon.mapper.MappingParser;
import org.apache.johnzon.mapper.ObjectConverter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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

        TestDTO deserialized = jsonb.fromJson(json, TestDTO.class);
        assertEquals(dto.instant.toEpochMilli(), deserialized.instant.toEpochMilli());
    }

    @Test
    public void testObjectConverter() {
        TestDTOWithOC dto = new TestDTOWithOC();
        dto.dto = new TestDTO();
        dto.dto.instant = Instant.now();

        Jsonb jsonb = JsonbProvider.provider().create().build();
        String json = jsonb.toJson(dto);
        assertNotNull(json);

        TestDTOWithOC deserialized = jsonb.fromJson(json, TestDTOWithOC.class);
        assertEquals(deserialized.dto.instant.toEpochMilli(), dto.dto.instant.toEpochMilli());
    }

    public static class TestDTOWithOC {
        @JohnzonConverter(TestDTOConverter.class)
        private TestDTO dto;

        public TestDTO getDto() {
            return dto;
        }

        public void setDto(TestDTO dto) {
            this.dto = dto;
        }
    }

    public static class TestDTOConverter implements ObjectConverter.Codec<TestDTO> {

        private static final String TIMESTAMP_JSON_KEY = "timestamp";

        @Override
        public void writeJson(TestDTO instance, MappingGenerator jsonbGenerator) {
            jsonbGenerator.getJsonGenerator().write(TIMESTAMP_JSON_KEY, instance.instant.atZone(ZoneId.of("UTC")).toString());
        }

        @Override
        public TestDTO fromJson(JsonValue jsonValue, Type targetType, MappingParser parser) {
            TestDTO dto = new TestDTO();
            dto.instant = ZonedDateTime.parse(((JsonObject) jsonValue).getString(TIMESTAMP_JSON_KEY)).toInstant();
            return dto;
        }
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
