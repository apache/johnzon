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
package org.apache.johnzon.mapper;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;

import org.junit.Test;

public class LiteralTest {
    @Test
    public void writeReadNumbers() {
        final NumberClass nc = new NumberClass();
        final StringWriter sw = new StringWriter();
        nc.setBg(new BigDecimal("123.123"));
        nc.setBi(new BigInteger("123"));
        nc.setDoubleNumber(123.123);
        nc.setBool(true);
        nc.setByteNumber((byte) 1);
        nc.setFloatNumber(123);
        nc.setShortNumber((short) 1);
        nc.setLongNumber(123L);
        nc.setIntNumber(123);

        final String expectedJson = "{\"shortNumber\":1,\"byteNumber\":1,\"intNumber\":123,\"floatNumber\":123.0,\"bg\":123.123,\""
                + "bool\":true,\"bi\":123,\"longNumber\":123,\"doubleNumber\":123.123}";
        final Comparator<String> attributeOrder = new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                return expectedJson.indexOf(o1) - expectedJson.indexOf(o2);
            }
        };
        new MapperBuilder().setAttributeOrder(attributeOrder).build().writeObject(nc, sw);
        assertEquals(expectedJson, sw.toString());
        final NumberClass read = new MapperBuilder().setAttributeOrder(attributeOrder).build()
                .readObject(new StringReader(sw.toString()), NumberClass.class);
        assertEquals(nc, read);

    }

    @Test(expected = NumberFormatException.class)
    public void writeReadNumbersInf() {
        final NumberClass nc = new NumberClass();
        final StringWriter sw = new StringWriter();
        nc.setBg(new BigDecimal("123.123"));
        nc.setBi(new BigInteger("123"));
        nc.setDoubleNumber(Double.POSITIVE_INFINITY);
        nc.setBool(true);
        nc.setByteNumber((byte) 1);
        nc.setFloatNumber(123);
        nc.setShortNumber((short) 1);
        nc.setLongNumber(123L);
        nc.setIntNumber(123);

        new MapperBuilder().build().writeObject(nc, sw);

    }

    @Test(expected = NumberFormatException.class)
    public void writeReadNumbersNaN() {
        final NumberClass nc = new NumberClass();
        final StringWriter sw = new StringWriter();
        nc.setBg(new BigDecimal("123.123"));
        nc.setBi(new BigInteger("123"));
        nc.setDoubleNumber(Double.NaN);
        nc.setBool(true);
        nc.setByteNumber((byte) 1);
        nc.setFloatNumber(123);
        nc.setShortNumber((short) 1);
        nc.setLongNumber(123L);
        nc.setIntNumber(123);

        new MapperBuilder().build().writeObject(nc, sw);

    }

    public static class NumberClass {
        private BigDecimal bg;
        private BigInteger bi;
        private int intNumber;
        private long longNumber;
        private byte byteNumber;
        private short shortNumber;
        private double doubleNumber;
        private float floatNumber;
        private boolean bool;

        public BigDecimal getBg() {
            return bg;
        }

        public void setBg(final BigDecimal bg) {
            this.bg = bg;
        }

        public BigInteger getBi() {
            return bi;
        }

        public void setBi(final BigInteger bi) {
            this.bi = bi;
        }

        public int getIntNumber() {
            return intNumber;
        }

        public void setIntNumber(final int intNumber) {
            this.intNumber = intNumber;
        }

        public long getLongNumber() {
            return longNumber;
        }

        public void setLongNumber(final long longNumber) {
            this.longNumber = longNumber;
        }

        public byte getByteNumber() {
            return byteNumber;
        }

        public void setByteNumber(final byte byteNumber) {
            this.byteNumber = byteNumber;
        }

        public short getShortNumber() {
            return shortNumber;
        }

        public void setShortNumber(final short shortNumber) {
            this.shortNumber = shortNumber;
        }

        public double getDoubleNumber() {
            return doubleNumber;
        }

        public void setDoubleNumber(final double doubleNumber) {
            this.doubleNumber = doubleNumber;
        }

        public float getFloatNumber() {
            return floatNumber;
        }

        public void setFloatNumber(final float floatNumber) {
            this.floatNumber = floatNumber;
        }

        public boolean isBool() {
            return bool;
        }

        public void setBool(final boolean bool) {
            this.bool = bool;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((bg == null) ? 0 : bg.hashCode());
            result = prime * result + ((bi == null) ? 0 : bi.hashCode());
            result = prime * result + (bool ? 1231 : 1237);
            result = prime * result + byteNumber;
            long temp;
            temp = Double.doubleToLongBits(doubleNumber);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + Float.floatToIntBits(floatNumber);
            result = prime * result + intNumber;
            result = prime * result + (int) (longNumber ^ (longNumber >>> 32));
            result = prime * result + shortNumber;
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NumberClass other = (NumberClass) obj;
            if (bg == null) {
                if (other.bg != null) {
                    return false;
                }
            } else if (!bg.equals(other.bg)) {
                return false;
            }
            if (bi == null) {
                if (other.bi != null) {
                    return false;
                }
            } else if (!bi.equals(other.bi)) {
                return false;
            }
            if (bool != other.bool) {
                return false;
            }
            if (byteNumber != other.byteNumber) {
                return false;
            }
            if (Double.doubleToLongBits(doubleNumber) != Double.doubleToLongBits(other.doubleNumber)) {
                return false;
            }
            if (Float.floatToIntBits(floatNumber) != Float.floatToIntBits(other.floatNumber)) {
                return false;
            }
            if (intNumber != other.intNumber) {
                return false;
            }
            if (longNumber != other.longNumber) {
                return false;
            }
            if (shortNumber != other.shortNumber) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "NumberClass [bg=" + bg + ", bi=" + bi + ", intNumber=" + intNumber + ", longNumber=" + longNumber + ", byteNumber="
                    + byteNumber + ", shortNumber=" + shortNumber + ", doubleNumber=" + doubleNumber + ", floatNumber=" + floatNumber
                    + ", bool=" + bool + "]";
        }
    }
}
