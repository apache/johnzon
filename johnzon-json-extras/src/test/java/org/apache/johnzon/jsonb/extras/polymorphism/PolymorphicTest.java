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
package org.apache.johnzon.jsonb.extras.polymorphism;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.config.PropertyOrderStrategy;

import org.junit.Test;

public class PolymorphicTest {
    private static final String JSON = "{\"root\":{\"_type\":\"first\",\"_value\":{\"name\":\"simple\",\"type\":\"c1\"}}," +
            "\"roots\":[{\"_type\":\"first\",\"_value\":{\"name\":\"c-simple\",\"type\":\"c1\"}}," +
            "{\"_type\":\"second\",\"_value\":{\"name\":\"c-other\",\"type\":2}}]}";

    @Test
    public void serialize() throws Exception {
        final Child1 mainRoot = new Child1();
        mainRoot.type = "c1";
        mainRoot.name = "simple";
        final Child1 roots1 = new Child1();
        roots1.type = "c1";
        roots1.name = "c-simple";
        final Child2 roots2 = new Child2();
        roots2.type = 2;
        roots2.name = "c-other";
        final Wrapper wrapper = new Wrapper();
        wrapper.root = mainRoot;
        wrapper.roots = asList(roots1, roots2);

        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            final String json = jsonb.toJson(wrapper);
            assertEquals(JSON, json);
        }
    }

    @Test
    public void deserialize() throws Exception {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL))) {
            final Wrapper wrapper = jsonb.fromJson(JSON, Wrapper.class);
            assertNotNull(wrapper.root);
            assertThat(wrapper.root, instanceOf(Child1.class));
            assertEquals("simple", wrapper.root.name);
            assertEquals("c1", Child1.class.cast(wrapper.root).type);

            assertNotNull(wrapper.roots);
            assertEquals(2, wrapper.roots.size());
            assertThat(wrapper.roots.get(0), instanceOf(Child1.class));
            assertThat(wrapper.roots.get(1), instanceOf(Child2.class));
            assertEquals("c-simple", wrapper.roots.get(0).name);
            assertEquals("c1", Child1.class.cast(wrapper.roots.get(0)).type);
            assertEquals("c-other", wrapper.roots.get(1).name);
            assertEquals(2, Child2.class.cast(wrapper.roots.get(1)).type);
        }
    }

    @Polymorphic.JsonChildren({
            Child1.class,
            Child2.class
    })
    public static abstract class Root {
        public String name;
    }

    @Polymorphic.JsonId("first")
    public static class Child1 extends Root {
        public String type;
    }

    @Polymorphic.JsonId("second")
    public static class Child2 extends Root {
        public int type;
    }

    public static class Wrapper {
        @JsonbTypeSerializer(Polymorphic.Serializer.class)
        @JsonbTypeDeserializer(Polymorphic.DeSerializer.class)
        public Root root;

        @JsonbTypeSerializer(Polymorphic.Serializer.class)
        @JsonbTypeDeserializer(Polymorphic.DeSerializer.class)
        public List<Root> roots;
    }
}
