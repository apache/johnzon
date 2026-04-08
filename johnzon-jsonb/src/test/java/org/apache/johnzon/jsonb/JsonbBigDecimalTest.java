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

import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import jakarta.json.bind.annotation.JsonbProperty;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonbBigDecimalTest {
    @Rule
    public final JsonbRule rule = new JsonbRule()
            .withProperty("johnzon.useBigIntegerStringAdapter", "false")
            .withProperty("johnzon.useBigDecimalStringAdapter", "false")
            ;

    private static class BigNumberWrapper {
        @JsonbProperty("bd")
        private BigDecimal bd;

        @JsonbProperty("bi")
        private BigInteger bi;

        public BigDecimal getBd() {
            return bd;
        }

        public void setBd(BigDecimal bd) {
            this.bd = bd;
        }

        public BigInteger getBi() {
            return bi;
        }

        public void setBi(BigInteger bi) {
            this.bi = bi;
        }
    }

    @Test
    public void jsonValue() {
        final BigNumberWrapper bnw = new BigNumberWrapper();
        bnw.setBd(new BigDecimal("0.000000733915"));
        bnw.setBi(new BigInteger("9223372036854775808"));

        final String json = rule.toJson(bnw);
        Assert.assertEquals("{\"bd\":7.33915E-7,\"bi\":9223372036854775808}", json);
    }
}
