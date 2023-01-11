/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.johnzon.jsonb;

import static org.junit.Assert.assertEquals;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.config.PropertyOrderStrategy;

import org.junit.Test;

public class GenericAdapterTest {

    @Test
    public void testJOHNZON223() throws Exception {
        final JsonbConfig config = new JsonbConfig()
                .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL)
                .withAdapters(new EnumAdapter());
        try (final Jsonb jsonb = JsonbBuilder.create(config)) {

            // setup bean
            final MyBean expectedBean = new MyBean();
            expectedBean.myEnum1 = new EnumHolder<>(MyEnum1.VALUE1);
            expectedBean.myEnum2 = new EnumHolder<>(MyEnum2.VALUE2);

            // write to string
            final String jsonString = jsonb.toJson(expectedBean);
            assertEquals(
                    "{\"myEnum1\":\"org.apache.johnzon.jsonb.GenericAdapterTest$MyEnum1.VALUE1\"," +
                            "\"myEnum2\":\"org.apache.johnzon.jsonb.GenericAdapterTest$MyEnum2.VALUE2\"}",
                    jsonString);

            // read from string
            final MyBean actualBean = jsonb.fromJson(jsonString, MyBean.class);
            assertEquals("MyBean [myEnum1=EnumHolder [value=VALUE1], myEnum2=EnumHolder [value=VALUE2]]",
                    String.valueOf(actualBean));
        }
    }

    public static class MyBean {
        public EnumHolder<MyEnum1> myEnum1;
        public EnumHolder<MyEnum2> myEnum2;


        @Override
        public String toString() {
            return String.format("MyBean [myEnum1=%s, myEnum2=%s]", this.myEnum1, this.myEnum2);
        }

    }

    public static class EnumHolder<E extends Enum<E>> {
        public Enum<E> value;


        public EnumHolder(final Enum<E> value) {
            this.value = value;
        }


        @Override
        public String toString() {
            return String.format("EnumHolder [value=%s]", this.value);
        }


        public static EnumHolder<?> valueOf(final String value) {
            final EnumHolder<?> result;
            try {
                final int idx = value.lastIndexOf(".");
                final Class clazz = Class.forName(value.substring(0, idx));
                return new EnumHolder<>(Enum.valueOf(clazz, value.substring(idx + 1)));
            } catch (final ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public enum MyEnum1 {
        VALUE1
    }

    public enum MyEnum2 {
        VALUE2
    }

    private static class EnumAdapter implements JsonbAdapter<EnumHolder, String> {

        @Override
        public String adaptToJson(final EnumHolder obj) {
            return obj != null && obj.value != null
                    ? obj.value.getClass().getName() + "." + obj.value.name()
                    : null;
        }

        @Override
        public EnumHolder adaptFromJson(final String obj) {
            return EnumHolder.valueOf(obj);
        }
    }

}

