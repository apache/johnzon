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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Various Array utils which do not exist in Java or are performing badly.
 *
 * NOTE: we keep this here and in Mapper duplicated to not have Mapper depending on johnzon-core!
 */
public final class ArrayUtil {
    private ArrayUtil() {
        // utility class ct
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

    public static List<Object> asList(final Object[] vals) {
        return new AbstractList<Object>() {
            @Override
            public Object get(int index) {
                return vals[index];
            }

            @Override
            public int size() {
                return vals.length;
            }
        };
    }

    /**
     * Take the given array object and fill a fresh Collection with it.
     * @param array an array that is to be duplicated
     * @return a new collection of the original array elements
     * @throws IllegalArgumentException if the given value this is not an array.
     */
    public static Collection<Object> newCollection(Object array) {

        // Note: all types of multidimensional arrays are instanceof Object[]
        if (array instanceof Object[]) {
            return asList(((Object[])array));
        }

        ArrayList<Object> collection;

        if (array instanceof boolean[]) {
            collection = new ArrayList<>(((boolean[])array).length);
            for (boolean o : ((boolean[]) array)) {
                collection.add(o);
            }
        } else if (array instanceof byte[]) {
            collection = new ArrayList<>(((byte[])array).length);
            for (byte o : ((byte[]) array)) {
                collection.add(o);
            }
        } else if (array instanceof char[]) {
            collection = new ArrayList<>(((char[])array).length);
            for (char o : ((char[]) array)) {
                collection.add(o);
            }
        } else if (array instanceof short[]) {
            collection = new ArrayList<>(((short[])array).length);
            for (short o : ((short[]) array)) {
                collection.add(o);
            }
        } else if (array instanceof int[]) {
            collection = new ArrayList<>(((int[])array).length);
            for (int o : ((int[]) array)) {
                collection.add(o);
            }
        } else if (array instanceof long[]) {
            collection = new ArrayList<>(((long[])array).length);
            for (long o : ((long[]) array)) {
                collection.add(o);
            }
        } else if (array instanceof float[]) {
            collection = new ArrayList<>(((float[])array).length);
            for (float o : ((float[]) array)) {
                collection.add(o);
            }
        } else if (array instanceof double[]) {
            collection = new ArrayList<>(((double[])array).length);
            for (double o : ((double[]) array)) {
                collection.add(o);
            }
        } else {
            throw new IllegalArgumentException("This is not an array! " + array);
        }

        return collection;
    }
}
