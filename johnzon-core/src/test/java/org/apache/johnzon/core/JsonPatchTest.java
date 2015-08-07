package org.apache.johnzon.core;

import static org.junit.Assert.*;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import javax.json.JsonPointer;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.spi.JsonProvider;

import org.junit.Test;

public class JsonPatchTest {
	
	@Test
	public void testPatch() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/nested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
        JsonPatchBuilder jpb = new JsonPatchBuilder();
        jpb.add("/s", JsonValue.NULL);
        jpb.add("/y", 5);
        jpb.add("/u", "c");
        jpb.test("/s", JsonValue.NULL);
        //jpb.add("/m", "{\"a\":10, \"b\":11}");
        jpb.add("/qq", Json.createReader(new StringReader("{\"a\":10, \"b\":11}")).read());
        jpb.copy("/x", "/qq/b");
        //jpb.test("/x", 11);
        jpb.remove("/qq/a");
        jpb.move("/u", "/qq/b");
        //jpb.remove("/m/a");
        //jpb.move("/u", "/m/a");
        JsonObject result = jpb.apply(obj);
        System.out.println(obj);
        System.out.println(result);
        //assertEquals(obj, result);
	}
	
	
}
