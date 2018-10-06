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

import java.util.List;

public class PrimaryMetric {
    private Double score;
    public Double getScore() {
        return score;
    }
    public void setScore(final Double newValue) {
        this.score = newValue;
    }

    private Double scoreError;
    public Double getScoreError() {
        return scoreError;
    }
    public void setScoreError(final Double newValue) {
        this.scoreError = newValue;
    }

    private List<Double> scoreConfidence;
    public List<Double> getScoreConfidence() {
        return scoreConfidence;
    }
    public void setScoreConfidence(final List<Double> newValue) {
        this.scoreConfidence = newValue;
    }

    private ScorePercentiles scorePercentiles;
    public ScorePercentiles getScorePercentiles() {
        return scorePercentiles;
    }
    public void setScorePercentiles(final ScorePercentiles newValue) {
        this.scorePercentiles = newValue;
    }

    private String scoreUnit;
    public String getScoreUnit() {
        return scoreUnit;
    }
    public void setScoreUnit(final String newValue) {
        this.scoreUnit = newValue;
    }

    private List<List<Double>> rawData;
    public List<List<Double>> getRawData() {
        return rawData;
    }
    public void setRawData(final List<List<Double>> newValue) {
        this.rawData = newValue;
    }
}