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
package org.superbiz;

import org.apache.johnzon.mapper.Adapter;
import org.apache.johnzon.mapper.MapperConfig;
import org.apache.johnzon.mapper.Mappings;
import org.apache.johnzon.mapper.ObjectConverter;
import org.apache.johnzon.mapper.access.FieldAccessMode;
import org.apache.johnzon.mapper.internal.AdapterKey;
import org.junit.Test;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

// json libs have a meta registry like our Mappings
// often other libs can use it to do advanced features without instantiating beans (swagger for instance)
// this class ensure we can do it
// that's why it is not in johnzon package
public class ExtendMappingTest {
    @Test // strictly speaking compilation checked 50% of the test
    public void run() {
        final MyMappings myMappings = new MyMappings();
        final Mappings.ClassMapping model = myMappings.findOrCreateClassMapping(MyModel.class);
        assertTrue(myMappings.classes().isEmpty());
        assertEquals(1, model.getters.size());
        assertEquals(1, model.setters.size());
        assertNotNull(FieldAccessMode.FieldReader.class.cast(model.getters.values().iterator().next().reader).getType());
        assertNotNull(FieldAccessMode.FieldWriter.class.cast(model.setters.values().iterator().next().writer).getType());
    }

    public static class MyModel {
        public String name;
    }

    public static class MyMappings extends Mappings {
        public MyMappings() {
            super(new MapperConfig(
                    new ConcurrentHashMap<AdapterKey, Adapter<?, ?>>(),
                    new HashMap<Class<?>, ObjectConverter.Writer<?>>(),
                    new HashMap<Class<?>, ObjectConverter.Reader<?>>(),
                    -1, true, true, true, false, false, false,
                    new FieldAccessMode(false, false),
                    Charset.forName("UTF-8"),
                    new Comparator<String>() {
                        @Override
                        public int compare(final String o1, final String o2) {
                            return o1.compareTo(o2);
                        }
                    }, false, false, null, false, false));
        }

        @Override
        public ClassMapping findOrCreateClassMapping(final Type clazz) {
            final ClassMapping mapping = super.findOrCreateClassMapping(clazz);
            classes.remove(clazz); // no leak for single usage cases
            return mapping;
        }

        public Map<?, ?> classes() {
            return classes;
        }
    }
}
