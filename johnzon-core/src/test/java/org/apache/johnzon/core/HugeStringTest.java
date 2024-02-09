package org.apache.johnzon.core;

import jakarta.json.Json;
import org.junit.Ignore;
import org.junit.Test;

import java.io.StringReader;

@Ignore
public class HugeStringTest {
    @Test
    public void test() {
        String json = "{\"data\":\"" + "a".repeat(50 * 1024 * 1024 + 1) + "\"}";

        // Warmup
        for (int i = 0; i < 10; i++) {
            Json.createParser(new StringReader(json)).getObject();
        }

        long start = System.currentTimeMillis();
        Json.createParser(new StringReader(json)).getObject();
        System.err.println("Took " + (System.currentTimeMillis() - start) + "ms");
    }
}
