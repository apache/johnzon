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
package org.apache.johnzon.mapper.util;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.List;


/*
The following types will be handled
boolean
byte
char
short
int
long
float
double
Object
 */
public final class ArrayUtil {
    private ArrayUtil() {
        // utility class ct
    }

    /**
     * @return the length of the array given.
     * @throws IllegalArgumentException if the given object is not an array
     */
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


    /**
     * @return the array type of a given class
     */
    public static Type getArrayTypeFor(Class<?> clazz) {
        // optimisation for raw types
        if (boolean.class == clazz) {
            return boolean[].class;
        }
        if (byte.class == clazz) {
            return byte[].class;
        }
        if (char.class == clazz) {
            return char[].class;
        }
        if (short.class == clazz) {
            return short[].class;
        }
        if (int.class == clazz) {
            return int[].class;
        }
        if (long.class == clazz) {
            return long[].class;
        }
        if (float.class == clazz) {
            return float[].class;
        }
        if (double.class == clazz) {
            return double[].class;
        }

        // and wrapper types
        if (Boolean.class == clazz) {
            return Boolean[].class;
        }
        if (Byte.class == clazz) {
            return Byte[].class;
        }
        if (Character.class == clazz) {
            return Character[].class;
        }
        if (Short.class == clazz) {
            return Short[].class;
        }
        if (Integer.class == clazz) {
            return Integer[].class;
        }
        if (Long.class == clazz) {
            return Long[].class;
        }
        if (Float.class == clazz) {
            return Float[].class;
        }
        if (Double.class == clazz) {
            return Double[].class;
        }

        // some other class arrays
        return Array.newInstance(clazz, 0).getClass();
    }
}
