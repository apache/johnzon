package org.apache.johnzon.jsonb;

import static org.junit.Assert.assertEquals;

import org.apache.johnzon.jsonb.model.Holder;
import org.apache.johnzon.jsonb.test.JsonbRule;
import org.junit.Rule;
import org.junit.Test;

public class GenericsTest {
    @Rule
    public final JsonbRule jsonb = new JsonbRule();

    @Test
    public void genericModel() {
        final String json = jsonb.toJson(new StillGeneric<String>() {{ setInstance("Test String"); } });
        assertEquals("{\"instance\":\"Test String\"}", json);
        final StillGeneric<String> deserialized = jsonb.fromJson(json, new StillGeneric<String>() {
        }.getClass().getGenericSuperclass());
        assertEquals("Test String", deserialized.getInstance());
    }

    public static class StillGeneric<T> implements Holder<T> {
        private T value;

        @Override
        public T getInstance() {
            return value;
        }

        @Override
        public void setInstance(final T instance) {
            this.value = instance;
        }
    }
}
