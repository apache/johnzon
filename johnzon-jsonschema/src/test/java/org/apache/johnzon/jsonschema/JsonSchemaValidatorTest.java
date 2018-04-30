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
package org.apache.johnzon.jsonschema;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import javax.json.Json;
import javax.json.JsonBuilderFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JsonSchemaValidatorTest {
    private static JsonSchemaValidatorFactory factory;

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(emptyMap());

    @BeforeClass
    public static void init() {
        factory = new JsonSchemaValidatorFactory();
    }

    @AfterClass
    public static void destroy() {
        factory.close();
    }

    @Test
    public void rootRequired() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("name", jsonFactory.createObjectBuilder()
                                .add("type", "string")
                                .build())
                        .add("age", jsonFactory.createObjectBuilder()
                                .add("type", "number")
                                .build())
                        .build())
                .add("required", jsonFactory.createArrayBuilder().add("name").build())
                .build());

        final ValidationResult success = validator.apply(jsonFactory.createObjectBuilder().add("name", "ok").build());
        assertTrue(success.getErrors().toString(), success.isSuccess());

        final ValidationResult failure = validator.apply(jsonFactory.createObjectBuilder().addNull("name").build());
        assertFalse(failure.isSuccess());
        final Collection<ValidationResult.ValidationError> errors = failure.getErrors();
        assertEquals(1, errors.size());
        final ValidationResult.ValidationError error = errors.iterator().next();
        assertEquals("/", error.getField());
        assertEquals("name is required and is not present", error.getMessage());

        validator.close();
    }

    @Test
    public void rootType() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("name", jsonFactory.createObjectBuilder()
                                .add("type", "string")
                                .build())
                        .add("age", jsonFactory.createObjectBuilder()
                                .add("type", "number")
                                .build())
                        .build())
                .build());

        {
            final ValidationResult success = validator.apply(jsonFactory.createObjectBuilder().add("name", "ok").build());
            assertTrue(success.getErrors().toString(), success.isSuccess());
        }
        {
            final ValidationResult success = validator.apply(jsonFactory.createObjectBuilder().addNull("name").build());
            assertTrue(success.getErrors().toString(), success.isSuccess());
        }

        final ValidationResult failure = validator.apply(jsonFactory.createObjectBuilder().add("name", 5).build());
        assertFalse(failure.isSuccess());
        final Collection<ValidationResult.ValidationError> errors = failure.getErrors();
        assertEquals(1, errors.size());
        final ValidationResult.ValidationError error = errors.iterator().next();
        assertEquals("/name", error.getField());
        assertEquals("Expected STRING but got NUMBER", error.getMessage());

        validator.close();
    }

    @Test
    public void nestedType() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("person", jsonFactory.createObjectBuilder()
                                .add("type", "object")
                                .add("properties", jsonFactory.createObjectBuilder()
                                        .add("name", jsonFactory.createObjectBuilder()
                                                .add("type", "string")
                                                .build())
                                        .add("age", jsonFactory.createObjectBuilder()
                                                .add("type", "number")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build());

        final ValidationResult success = validator.apply(jsonFactory.createObjectBuilder()
                .add("person", jsonFactory.createObjectBuilder()
                        .add("name", "ok")
                        .build())
                .build());
        assertTrue(success.getErrors().toString(), success.isSuccess());

        final ValidationResult failure = validator.apply(jsonFactory.createObjectBuilder()
                .add("person", jsonFactory.createObjectBuilder()
                        .add("name", jsonFactory.createObjectBuilder().build())
                        .build())
                .build());
        assertFalse(failure.toString(), failure.isSuccess());
        final Collection<ValidationResult.ValidationError> errors = failure.getErrors();
        assertEquals(1, errors.size());
        final ValidationResult.ValidationError error = errors.iterator().next();
        assertEquals("/person/name", error.getField());
        assertEquals("Expected STRING but got OBJECT", error.getMessage());

        validator.close();
    }

    @Test
    public void enumValues() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("name", jsonFactory.createObjectBuilder()
                                .add("type", "string")
                                .add("enum", jsonFactory.createArrayBuilder().add("a").add("b").build())
                                .build())
                        .build())
                .build());

        final ValidationResult success = validator.apply(jsonFactory.createObjectBuilder().add("name", "a").build());
        assertTrue(success.getErrors().toString(), success.isSuccess());

        final ValidationResult failure = validator.apply(jsonFactory.createObjectBuilder().add("name", 5).build());
        assertFalse(failure.isSuccess());
        final Collection<ValidationResult.ValidationError> errors = failure.getErrors();
        assertEquals(1, errors.size());
        final ValidationResult.ValidationError error = errors.iterator().next();
        assertEquals("/name", error.getField());
        assertEquals("Expected STRING but got NUMBER", error.getMessage());

        validator.close();
    }

    @Test
    public void multipleOf() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("age", jsonFactory.createObjectBuilder()
                                .add("type", "number")
                                .add("multipleOf", 5)
                                .build())
                        .build())
                .build());

        final ValidationResult success = validator.apply(jsonFactory.createObjectBuilder().add("age", 5).build());
        assertTrue(success.getErrors().toString(), success.isSuccess());

        final ValidationResult failure = validator.apply(jsonFactory.createObjectBuilder().add("age", 6).build());
        assertFalse(failure.isSuccess());
        final Collection<ValidationResult.ValidationError> errors = failure.getErrors();
        assertEquals(1, errors.size());
        final ValidationResult.ValidationError error = errors.iterator().next();
        assertEquals("/age", error.getField());
        assertEquals("6.0 is not a multiple of 5.0", error.getMessage());

        validator.close();
    }

    @Test
    public void minimum() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("age", jsonFactory.createObjectBuilder()
                                .add("type", "number")
                                .add("minimum", 5)
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder().add("age", 5).build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder().add("age", 6).build()).isSuccess());

        final ValidationResult failure = validator.apply(jsonFactory.createObjectBuilder().add("age", 2).build());
        assertFalse(failure.isSuccess());
        final Collection<ValidationResult.ValidationError> errors = failure.getErrors();
        assertEquals(1, errors.size());
        final ValidationResult.ValidationError error = errors.iterator().next();
        assertEquals("/age", error.getField());
        assertEquals("2.0 is less than 5.0", error.getMessage());

        validator.close();
    }

    @Test
    public void maximum() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("age", jsonFactory.createObjectBuilder()
                                .add("type", "number")
                                .add("maximum", 5)
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder().add("age", 5).build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder().add("age", 4).build()).isSuccess());

        final ValidationResult failure = validator.apply(jsonFactory.createObjectBuilder().add("age", 6).build());
        assertFalse(failure.isSuccess());
        final Collection<ValidationResult.ValidationError> errors = failure.getErrors();
        assertEquals(1, errors.size());
        final ValidationResult.ValidationError error = errors.iterator().next();
        assertEquals("/age", error.getField());
        assertEquals("6.0 is more than 5.0", error.getMessage());

        validator.close();
    }

    @Test
    public void exclusiveMinimum() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("age", jsonFactory.createObjectBuilder()
                                .add("type", "number")
                                .add("exclusiveMinimum", 5)
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder().add("age", 6).build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder().add("age", 5).build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder().add("age", 4).build()).isSuccess());
        validator.close();
    }

    @Test
    public void exclusiveMaximum() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("age", jsonFactory.createObjectBuilder()
                                .add("type", "number")
                                .add("exclusiveMaximum", 5)
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder().add("age", 4).build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder().add("age", 5).build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder().add("age", 6).build()).isSuccess());

        validator.close();
    }

    @Test
    public void minLength() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("name", jsonFactory.createObjectBuilder()
                                .add("type", "string")
                                .add("minLength", 2)
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder().add("name", "ok").build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder().add("name", "okk").build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder().add("name", "-").build()).isSuccess());

        validator.close();
    }

    @Test
    public void maxLength() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("name", jsonFactory.createObjectBuilder()
                                .add("type", "string")
                                .add("maxLength", 2)
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder().add("name", "ok").build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder().add("name", "-").build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder().add("name", "fail").build()).isSuccess());

        validator.close();
    }

    @Test
    public void pattern() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("name", jsonFactory.createObjectBuilder()
                                .add("type", "string")
                                .add("pattern", "[a-z]")
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder().add("name", "ok").build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder().add("name", "-").build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder().add("name", "0").build()).isSuccess());

        validator.close();
    }

    @Test
    public void itemsObject() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("names", jsonFactory.createObjectBuilder()
                                .add("type", "array")
                                .add("items", jsonFactory.createObjectBuilder()
                                        .add("type", "string"))
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add("1")).build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add(1)).build()).isSuccess());

        validator.close();
    }

    @Test
    public void itemsArray() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("names", jsonFactory.createObjectBuilder()
                                .add("type", "array")
                                .add("items", jsonFactory.createArrayBuilder().add(jsonFactory.createObjectBuilder()
                                        .add("type", "string"))
                                        .build()).build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add("1")).build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add(1)).build()).isSuccess());

        validator.close();
    }

    @Test
    public void itemsValidatesObject() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("names", jsonFactory.createObjectBuilder()
                                .add("type", "array")
                                .add("items", jsonFactory.createObjectBuilder()
                                        .add("type", "object")
                                        .add("properties", jsonFactory.createObjectBuilder()
                                                .add("age", jsonFactory.createObjectBuilder()
                                                        .add("type", "number")
                                                        .add("maximum", 2)
                                                        .build())
                                                .build()))
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder()
                        .add(jsonFactory.createObjectBuilder().add("age", 2)))
                .build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder()
                        .add(jsonFactory.createArrayBuilder().build()))
                .build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder()
                        .add(jsonFactory.createObjectBuilder().add("age", 3)))
                .build()).isSuccess());

        validator.close();
    }

    @Test
    public void maxItems() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("names", jsonFactory.createObjectBuilder()
                                .add("type", "array")
                                .add("maxItems", 1)
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add(2))
                .build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add(2).add(3))
                .build()).isSuccess());

        validator.close();
    }

    @Test
    public void minItems() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("names", jsonFactory.createObjectBuilder()
                                .add("type", "array")
                                .add("minItems", 1)
                                .build())
                        .build())
                .build());

        final ValidationResult result = validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add(2))
                .build());
        assertTrue(result.toString(), result.isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder())
                .build()).isSuccess());

        validator.close();
    }

    @Test
    public void uniqueItems() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("names", jsonFactory.createObjectBuilder()
                                .add("type", "array")
                                .add("uniqueItems", true)
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add(2))
                .build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add(2).add(2))
                .build()).isSuccess());

        validator.close();
    }

    @Test
    public void containsItems() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("properties", jsonFactory.createObjectBuilder()
                        .add("names", jsonFactory.createObjectBuilder()
                                .add("type", "array")
                                .add("contains", jsonFactory.createObjectBuilder().add("type", "number"))
                                .build())
                        .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add(2))
                .build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add(2).add("test"))
                .build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder()
                .add("names", jsonFactory.createArrayBuilder().add("test"))
                .build()).isSuccess());

        validator.close();
    }

    @Test
    public void maxProperties() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("maxProperties", 1)
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder().build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("name", "test")
                .build()).isSuccess());
        assertFalse(validator.apply(jsonFactory.createObjectBuilder()
                .add("name", "test")
                .add("name2", "test")
                .build()).isSuccess());

        validator.close();
    }

    @Test
    public void minProperties() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("minProperties", 1)
                .build());

        assertFalse(validator.apply(jsonFactory.createObjectBuilder().build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("name", "test")
                .build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("name", "test")
                .add("name2", "test")
                .build()).isSuccess());

        validator.close();
    }

    @Test
    public void patternProperties() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("patternProperties", jsonFactory.createObjectBuilder()
                        .add("[0-9]+", jsonFactory.createObjectBuilder().add("type", "number"))
                    .build())
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder().build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("1", 1)
                .build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("1", "test")
                .build()).isSuccess());

        validator.close();
    }

    @Test
    public void additionalProperties() {
        final JsonSchemaValidator validator = factory.newInstance(jsonFactory.createObjectBuilder()
                .add("type", "object")
                .add("additionalProperties", jsonFactory.createObjectBuilder().add("type", "number"))
                .build());

        assertTrue(validator.apply(jsonFactory.createObjectBuilder().build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("1", 1)
                .build()).isSuccess());
        assertTrue(validator.apply(jsonFactory.createObjectBuilder()
                .add("1", "test")
                .build()).isSuccess());

        validator.close();
    }
}
