package org.apache.johnzon.core;

import static org.junit.Assert.*;

import javax.json.Json;
import javax.json.JsonArray;
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
	
	@Test
	public void testaddJsonPointerPairToRoot() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/nested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("/new").add(obj, Json.createValue("newval"));
		JsonValue newVal = new JsonPointer("/new").getValue(result);
        assertNotNull(newVal);
		assertEquals("newval", ((JsonString) newVal).getString());
		assertEquals(3, result.size());
	}
	
	@Test
	public void testaddJsonPointerArrayToRoot() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/nested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("/new").add(obj, Json.createArrayBuilder().add(true).build());
		System.out.println(result);
		JsonValue newVal = new JsonPointer("/new").getValue(result);
        assertNotNull(newVal);
		assertEquals("[true]", newVal.toString());
		assertEquals(3, result.size());
	}
	
	@Test
	public void testaddJsonPointerPairToNested() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/deepnested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("/c/d/2/e/r/2/new").add(obj, Json.createValue("newval"));
		System.out.println(result);
		JsonValue newVal = new JsonPointer("/c/d/2/e/r/2/new").getValue(result);
        assertNotNull(newVal);
		assertEquals("newval", ((JsonString) newVal).getString());
		assertEquals(2, result.size());
	}
	
	@Test(expected=JsonException.class)
	public void testaddJsonPointerDoubleNonExist() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/deepnested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("/x/y").add(obj, Json.createValue("newval"));
		System.out.println(result);
	}
	
	@Test
	public void testaddJsonPointerPairToArrayNested() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/deepnested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("/c/d/2/e/r/3").add(obj, Json.createValue("newval"));
		JsonValue newVal = new JsonPointer("/c/d/2/e/r/3").getValue(result);
        assertNotNull(newVal);
		assertEquals("newval", ((JsonString) newVal).getString());
		assertEquals(2, result.size());
	}
	
	@Test
	public void testaddJsonPointerPairToObjectNestedDash() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/deepnested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("/c/d/2/e/-").add(obj, Json.createValue("newval"));
		System.out.println(result);
		JsonValue newVal = new JsonPointer("/c/d/2/e/-").getValue(result);
        assertNotNull(newVal);
		assertEquals("newval", ((JsonString) newVal).getString());
		assertEquals(2, result.size());
	}
	
	@Test
	public void testaddJsonPointerPairToArrayNestedDash() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/deepnested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("/c/d/2/e/r/-").add(obj, Json.createValue("newval"));
		System.out.println(result);
		JsonValue newVal = new JsonPointer("/c/d/2/e/r/3").getValue(result);
        assertNotNull(newVal);
		assertEquals("newval", ((JsonString) newVal).getString());
		assertEquals(2, result.size());
	}
	
	@Test
	public void testaddJsonPointerSet() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/deepnested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("/c/d/2/e").add(obj, Json.createValue("newval"));
		JsonValue newVal = new JsonPointer("/c/d/2/e").getValue(result);
        assertNotNull(newVal);
		assertEquals("newval", ((JsonString) newVal).getString());
		assertEquals(2, result.size());
	}
	
	@Test
	public void testaddJsonPointerPairToArrayNestedReplace() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/deepnested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("/c/d/2/e/0").add(obj, Json.createValue("newval"));
		JsonValue newVal = new JsonPointer("/c/d/2/e/0").getValue(result);
        assertNotNull(newVal);
		assertEquals("newval", ((JsonString) newVal).getString());
		assertEquals(2, result.size());
	}
	
	@Test
	public void testReplace() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/deepnested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("/c/d/2/e").replace(obj, Json.createValue("newval"));
		JsonValue newVal = new JsonPointer("/c/d/2/e").getValue(result);
        assertNotNull(newVal);
		assertEquals("newval", ((JsonString) newVal).getString());
		assertEquals(2, result.size());
	}
	
	@Test
	public void testRemove() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/deepnested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
        System.out.println(obj);
        JsonValue newVal = new JsonPointer("/c/d/2").getValue(obj);
        assertNotNull(newVal);
		assertEquals(2, ((JsonObject) newVal).size());
        
		JsonObject result = new JsonPointer("/c/d/2/e").remove(obj);
		System.out.println(result);
		newVal = new JsonPointer("/c/d/2").getValue(result);
        assertNotNull(newVal);
		assertEquals(1, ((JsonObject) newVal).size());
		assertEquals(2, result.size());
	}
	
	@Test
	public void testRemoveAll() {
		final JsonReader loadInMemReader = Json.createReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("json/deepnested.json"));
        assertNotNull(loadInMemReader);
        JsonObject obj = loadInMemReader.readObject();
		JsonObject result = new JsonPointer("").remove(obj);
		System.out.println(result);
		assertEquals(JsonValue.EMPTY_JSON_OBJECT, result);
	}
}
