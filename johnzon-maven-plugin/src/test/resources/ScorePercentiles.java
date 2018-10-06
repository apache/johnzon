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

import org.apache.johnzon.mapper.JohnzonProperty;

public class ScorePercentiles {
    @JohnzonProperty("0.0")
    private Double _00;
    public Double get_00() {
        return _00;
    }
    public void set_00(final Double newValue) {
        this._00 = newValue;
    }

    @JohnzonProperty("50.0")
    private Double _500;
    public Double get_500() {
        return _500;
    }
    public void set_500(final Double newValue) {
        this._500 = newValue;
    }

    @JohnzonProperty("90.0")
    private Double _900;
    public Double get_900() {
        return _900;
    }
    public void set_900(final Double newValue) {
        this._900 = newValue;
    }

    @JohnzonProperty("95.0")
    private Double _950;
    public Double get_950() {
        return _950;
    }
    public void set_950(final Double newValue) {
        this._950 = newValue;
    }

    @JohnzonProperty("99.0")
    private Double _990;
    public Double get_990() {
        return _990;
    }
    public void set_990(final Double newValue) {
        this._990 = newValue;
    }

    @JohnzonProperty("99.9")
    private Double _999;
    public Double get_999() {
        return _999;
    }
    public void set_999(final Double newValue) {
        this._999 = newValue;
    }

    @JohnzonProperty("99.99")
    private Double _9999;
    public Double get_9999() {
        return _9999;
    }
    public void set_9999(final Double newValue) {
        this._9999 = newValue;
    }

    @JohnzonProperty("99.999")
    private Double _99999;
    public Double get_99999() {
        return _99999;
    }
    public void set_99999(final Double newValue) {
        this._99999 = newValue;
    }

    @JohnzonProperty("99.9999")
    private Double _999999;
    public Double get_999999() {
        return _999999;
    }
    public void set_999999(final Double newValue) {
        this._999999 = newValue;
    }

    @JohnzonProperty("100.0")
    private Double _1000;
    public Double get_1000() {
        return _1000;
    }
    public void set_1000(final Double newValue) {
        this._1000 = newValue;
    }
}