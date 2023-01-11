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
package org.test.apache.johnzon.mojo;

import jakarta.json.bind.annotation.JsonbProperty;
import java.util.List;

public record SomeValue(
    String benchmark,
    String mode,
    Double threads,
    Double forks,
    Double warmupIterations,
    String warmupTime,
    Double measurementIterations,
    String measurementTime,
    PrimaryMetric primaryMetric,
    SecondaryMetrics secondaryMetrics) {

    public static record PrimaryMetric(
        Double score,
        Double scoreError,
        List<Double> scoreConfidence,
        ScorePercentiles scorePercentiles,
        String scoreUnit,
        List<List<Double>> rawData) {

        public static record ScorePercentiles(
            @JsonbProperty("0.0") Double _00,
            @JsonbProperty("50.0") Double _500,
            @JsonbProperty("90.0") Double _900,
            @JsonbProperty("95.0") Double _950,
            @JsonbProperty("99.0") Double _990,
            @JsonbProperty("99.9") Double _999,
            @JsonbProperty("99.99") Double _9999,
            @JsonbProperty("99.999") Double _99999,
            @JsonbProperty("99.9999") Double _999999,
            @JsonbProperty("100.0") Double _1000) {
        }
    }

    public static record SecondaryMetrics() {
    }
}
