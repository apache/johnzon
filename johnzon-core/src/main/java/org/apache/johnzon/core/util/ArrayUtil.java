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
package org.apache.johnzon.core.util;

import java.util.AbstractList;
import java.util.List;

/**
 * Various Array utils which do not exist in Java.
 */
public final class ArrayUtil {
    private ArrayUtil() {
        // utility class ct
    }

    public static int getArrayLength(Object array) {
        // Note: all types of multidimensional arrays are instanceof Object[]
        if (array instanceof Object[]) {
            return ((Object[]) array).length;
        }
        if (array instanceof boolean[]) {
            return ((boolean[])array).length;
        }
        if (array instanceof byte[]) {
            return ((byte[])array).length;
        }
        if (array instanceof char[]) {
            return ((char[]) array).length;
        }
        if (array instanceof short[]) {
            return ((short[]) array).length;
        }
        if (array instanceof int[]) {
            return ((int[]) array).length;
        }
        if (array instanceof long[]) {
            return ((long[]) array).length;
        }
        if (array instanceof float[]) {
            return ((float[]) array).length;
        }
        if (array instanceof double[]) {
            return ((double[]) array).length;
        }

        throw new IllegalArgumentException("This is not an array! " + array);
    }


    public static List<Integer> asList(final int[] vals) {
        return new AbstractList<Integer>() {
            @Override
            public Integer get(int index) {
                return vals[index];
            }

            @Override
            public int size() {
                return vals.length;
            }
        };
    }

    public static List<Short> asList(final short[] vals) {
        return new AbstractList<Short>() {
            @Override
            public Short get(int index) {
                return vals[index];
            }

            @Override
            public int size() {
                return vals.length;
            }
        };
    }

    public static List<Long> asList(final long[] vals) {
        return new AbstractList<Long>() {
            @Override
            public Long get(int index) {
                return vals[index];
            }

            @Override
            public int size() {
                return vals.length;
            }
        };
    }

    public static List<Character> asList(final char[] vals) {
        return new AbstractList<Character>() {
            @Override
            public Character get(int index) {
                return vals[index];
            }

            @Override
            public int size() {
                return vals.length;
            }
        };
    }

    public static List<Byte> asList(final byte[] vals) {
        return new AbstractList<Byte>() {
            @Override
            public Byte get(int index) {
                return vals[index];
            }

            @Override
            public int size() {
                return vals.length;
            }
        };
    }

    public static List<Float> asList(final float[] vals) {
        return new AbstractList<Float>() {
            @Override
            public Float get(int index) {
                return vals[index];
            }

            @Override
            public int size() {
                return vals.length;
            }
        };
    }

    public static List<Double> asList(final double[] vals) {
        return new AbstractList<Double>() {
            @Override
            public Double get(int index) {
                return vals[index];
            }

            @Override
            public int size() {
                return vals.length;
            }
        };
    }


}
