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

public class SomeValue {
    private String benchmark;
    public String getBenchmark() {
        return benchmark;
    }
    public void setBenchmark(final String newValue) {
        this.benchmark = newValue;
    }

    private String mode;
    public String getMode() {
        return mode;
    }
    public void setMode(final String newValue) {
        this.mode = newValue;
    }

    private Double threads;
    public Double getThreads() {
        return threads;
    }
    public void setThreads(final Double newValue) {
        this.threads = newValue;
    }

    private Double forks;
    public Double getForks() {
        return forks;
    }
    public void setForks(final Double newValue) {
        this.forks = newValue;
    }

    private Double warmupIterations;
    public Double getWarmupIterations() {
        return warmupIterations;
    }
    public void setWarmupIterations(final Double newValue) {
        this.warmupIterations = newValue;
    }

    private String warmupTime;
    public String getWarmupTime() {
        return warmupTime;
    }
    public void setWarmupTime(final String newValue) {
        this.warmupTime = newValue;
    }

    private Double measurementIterations;
    public Double getMeasurementIterations() {
        return measurementIterations;
    }
    public void setMeasurementIterations(final Double newValue) {
        this.measurementIterations = newValue;
    }

    private String measurementTime;
    public String getMeasurementTime() {
        return measurementTime;
    }
    public void setMeasurementTime(final String newValue) {
        this.measurementTime = newValue;
    }

    private PrimaryMetric primaryMetric;
    public PrimaryMetric getPrimaryMetric() {
        return primaryMetric;
    }
    public void setPrimaryMetric(final PrimaryMetric newValue) {
        this.primaryMetric = newValue;
    }

    private SecondaryMetrics secondaryMetrics;
    public SecondaryMetrics getSecondaryMetrics() {
        return secondaryMetrics;
    }
    public void setSecondaryMetrics(final SecondaryMetrics newValue) {
        this.secondaryMetrics = newValue;
    }

}
