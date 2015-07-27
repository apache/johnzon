package org.apache.johnzon.core;

import static org.junit.Assert.*;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonPointer;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.spi.JsonProvider;

import org.junit.Test;

public class JsonPointerTest {
	
	@Test
	public void testEmptyJsonPointer() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/nested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
        assertEquals(obj, new JsonPointer("").getValue(obj));
	}
	
	@Test
	public void testSimpleJsonPointer() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/nested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
        JsonValue result = new JsonPointer("/a").getValue(obj);
        assertNotNull(result);
		assertEquals("b", ((JsonString) result).getString());
	}
	
	@Test
	public void testJsonPointer1() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/nested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
        JsonValue result = new JsonPointer("/c/d/0").getValue(obj);
        assertNotNull(result);
		assertEquals(1, ((JsonNumber) result).intValueExact());
	}
	
	@Test(expected=JsonException.class)
	public void testNonExistentJsonPointer() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/nested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
        new JsonPointer("/c/d/11").getValue(obj);
	}
	
	@Test(expected=JsonException.class)
	public void testNonExistentJsonPointer2() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/nested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
        new JsonPointer("/c/q/0").getValue(obj);
	}
	
	@Test(expected=JsonException.class)
	public void testInvalidPointerNoSlash() {
		new JsonPointer("a/c/g");
	}
	
	@Test(expected=JsonException.class)
	public void testInvalidPointerBadEscape() {
		new JsonPointer("/~2");
	}
	
	@Test(expected=JsonException.class)
	public void testInvalidPointerBadEscape2() {
		new JsonPointer("/~v");
	}

	@Test
	public void testTrailingSlash() {
		new JsonPointer("/a/c/g/");
	}
	
	//@Test
	public void testaddJsonPointer1() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/nested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("/c/new").add(obj, Json.createValue("newval"));
		System.out.println("res "+result);
		JsonValue newVal = new JsonPointer("/c/new").getValue(result);
        assertNotNull(newVal);
		assertEquals("newval", ((JsonString) newVal).getString());
	}
}
