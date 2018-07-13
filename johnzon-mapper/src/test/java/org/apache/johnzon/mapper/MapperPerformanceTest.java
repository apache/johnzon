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

import org.junit.Ignore;
import org.junit.Test;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * This test is usually being executed manually.
 * It contains a few performance related tests and is intended for profiling etc.
 */
@Ignore // intended to be run manually from an IDE for example.
public class MapperPerformanceTest {

    public static final int ARRAY_SIZE = 60_000_000;

    @Test
   public void byteArrayBase64WriteTest() {

       Mapper mapper = new MapperBuilder().setTreatByteArrayAsBase64(false).build();

       SomeDocument doc = createTestDocument();

       StringWriter writer = new StringWriter();
       mapper.writeObject(doc, writer);

       long start = System.nanoTime();

       for (int i=0; i< 10; i++) {
           writer = new StringWriter();
           mapper.writeObject(doc, writer);
       }
       long end = System.nanoTime();

       System.out.println("took: " + TimeUnit.NANOSECONDS.toMillis(end-start) + " ms");
   }

    @Test
   public void intArrayWriteTest() {

       Mapper mapper = new MapperBuilder().setTreatByteArrayAsBase64(false).build();

       SomeIntDocument doc = createTestIntDocument();

       StringWriter writer = new StringWriter();
       mapper.writeObject(doc, writer);

       long start = System.nanoTime();

       for (int i=0; i< 10; i++) {
           writer = new StringWriter();
           mapper.writeObject(doc, writer);
       }
       long end = System.nanoTime();

       System.out.println("took: " + TimeUnit.NANOSECONDS.toMillis(end-start) + " ms");
   }

    private SomeDocument createTestDocument() {
        byte[] content = new byte[ARRAY_SIZE];
        Arrays.fill(content, (byte)'x');

        SomeDocument doc = new SomeDocument();
        doc.setContent(content);
        return doc;
    }

    private SomeIntDocument createTestIntDocument() {
        Integer[] content = new Integer[ARRAY_SIZE];
        Arrays.fill(content, (int)'x');

        SomeIntDocument doc = new SomeIntDocument();
        doc.setContent(content);
        return doc;
    }


    public static class SomeDocument {
       private byte[] content;

       public byte[] getContent() {
           return content;
       }

       public void setContent(byte[] content) {
           this.content = content;
       }
   }

    public static class SomeIntDocument {
       private Integer[] content;

        public Integer[] getContent() {
            return content;
        }

        public void setContent(Integer[] content) {
            this.content = content;
        }
    }

}
