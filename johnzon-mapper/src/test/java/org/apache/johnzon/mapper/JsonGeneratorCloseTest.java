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

import org.apache.johnzon.core.TestBufferProvider;
import org.apache.johnzon.core.TestJsonGeneratorFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RunWith(Parameterized.class)
public class JsonGeneratorCloseTest {

    @Parameterized.Parameter
    public String accessMode;


    @Parameterized.Parameters(name = "{0}")
    public static Iterable<String> modes() {
        return Arrays.asList("field", "method", "both", "strict-method");
    }


    @Test
    public void testClose() {

        TestBufferProvider.INSTANCE.clear();

        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .setGeneratorFactory(new TestJsonGeneratorFactory())
                                           .setAttributeOrder(String.CASE_INSENSITIVE_ORDER)
                                           .build();

        ClassToWrite toWrite = new ClassToWrite();
        toWrite.setName("The Name");
        toWrite.setInteger(42);
        toWrite.setaLong(Long.MAX_VALUE);
        toWrite.setaDouble(12.12);

        for (int i = 1; i < 5; i++) {

            String json = mapper.writeObjectAsString(toWrite);
            Assert.assertNotNull(json);
            Assert.assertEquals("{\"aDouble\":12.12,\"aLong\":9223372036854775807,\"integer\":42,\"name\":\"The Name\"}", json);

            Assert.assertEquals(i, TestBufferProvider.INSTANCE.newBufferCalls());
            Assert.assertEquals(i, TestBufferProvider.INSTANCE.releaseCalls());
        }
    }

    @Test
    public void testCloseWithException() {

        // first clear our bufferProvider
        TestBufferProvider.INSTANCE.clear();

        Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                           .setGeneratorFactory(new TestJsonGeneratorFactory())
                                           .setAttributeOrder(String.CASE_INSENSITIVE_ORDER)
                                           .build();

        ClassToWrite toWrite = new ClassToWrite();
        toWrite.setName("The Name");
        toWrite.setInteger(42);
        toWrite.setaLong(Long.MAX_VALUE);
        toWrite.setaDouble(12.12);

        String json = mapper.writeObjectAsString(toWrite);
        Assert.assertNotNull(json);
        Assert.assertEquals("{\"aDouble\":12.12,\"aLong\":9223372036854775807,\"integer\":42,\"name\":\"The Name\"}", json);

        Assert.assertEquals(1, TestBufferProvider.INSTANCE.newBufferCalls());
        Assert.assertEquals(1, TestBufferProvider.INSTANCE.releaseCalls());

        ClassToWrite toWriteWithExcpetion = new ClassToWrite();
        toWriteWithExcpetion.setaDouble(Double.POSITIVE_INFINITY);

        // 2 because we don't need to calc the calls
        for (int i = 2; i < 5; i++) {

            try {
                json = mapper.writeObjectAsString(toWriteWithExcpetion);
                Assert.fail("NumberFormatException expected");
            } catch (NumberFormatException e) {
                // expected -> all fine
            }

            Assert.assertEquals(i, TestBufferProvider.INSTANCE.newBufferCalls());
            Assert.assertEquals(i, TestBufferProvider.INSTANCE.releaseCalls());
        }
    }

    @Test
    public void testCloseConcurrent() throws Exception {

        TestBufferProvider.INSTANCE.clear();

        final Mapper mapper = new MapperBuilder().setAccessModeName(accessMode)
                                                 .setGeneratorFactory(new TestJsonGeneratorFactory())
                                                 .setAttributeOrder(String.CASE_INSENSITIVE_ORDER)
                                                 .build();

        final ClassToWrite correctToWrite = new ClassToWrite();
        correctToWrite.setName("The Name");
        correctToWrite.setInteger(42);
        correctToWrite.setaLong(Long.MAX_VALUE);
        correctToWrite.setaDouble(12.12);

        final ClassToWrite toWriteWithExcpetion = new ClassToWrite();
        toWriteWithExcpetion.setaDouble(Double.POSITIVE_INFINITY);


        List<Future<String>> results = new ArrayList<Future<String>>();
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        for (int i = 0; i < 100; i++) {

            final int counter = i;

            results.add(executorService.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String result;

                    if (counter % 2 == 0) {
                        result = mapper.writeObjectAsString(correctToWrite);
                    } else {
                        try {
                            result = mapper.writeObjectAsString(toWriteWithExcpetion);
                            Assert.fail("NumberFormatException expected");
                        } catch (NumberFormatException e) {
                            // expected -> all fine
                            result = "EXPECTED EXCEPTION";
                        }
                    }

                    return result;
                }
            }));
        }

        for (Future<String> result : results) {
            result.get();
        }

        Assert.assertEquals(100, TestBufferProvider.INSTANCE.newBufferCalls());
        Assert.assertEquals(100, TestBufferProvider.INSTANCE.releaseCalls());
    }


    private static class ClassToWrite {
        private String name;
        private int integer;
        private long aLong;
        private double aDouble;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getInteger() {
            return integer;
        }

        public void setInteger(int integer) {
            this.integer = integer;
        }

        public long getaLong() {
            return aLong;
        }

        public void setaLong(long aLong) {
            this.aLong = aLong;
        }

        public double getaDouble() {
            return aDouble;
        }

        public void setaDouble(double aDouble) {
            this.aDouble = aDouble;
        }
    }

}
