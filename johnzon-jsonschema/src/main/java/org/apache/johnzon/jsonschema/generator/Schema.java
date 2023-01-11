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
package org.apache.johnzon.jsonschema.generator;

import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import java.util.List;
import java.util.Map;

@JsonbPropertyOrder({
        "$id",
        "$ref",
        "type",
        "title",
        "description",
        "required",
        "deprecated",
        "$schema",
        "additionalProperties",
        "allOf",
        "anyOf",
        "default",
        "definitions",
        "enum",
        "example",
        "exclusiveMaximum",
        "exclusiveMinimum",
        "format",
        "items",
        "maximum",
        "maxItems",
        "maxLength",
        "maxProperties",
        "minimum",
        "minItems",
        "minLength",
        "minProperties",
        "multipleOf",
        "not",
        "nullable",
        "oneOf",
        "pattern",
        "properties",
        "readOnly",
        "uniqueItems",
        "writeOnly"
})
public class Schema {
    private Map<String, Schema> definitions;

    @JsonbTypeAdapter(SchemaTypeAdapter.class)
    private SchemaType type;

    private Map<String, Schema> properties;

    private Object additionalProperties;

    private List<Schema> allOf;

    private List<Schema> anyOf;

    @JsonbProperty("default")
    private Object defaultValue;

    private Boolean deprecated;

    private String description;

    @JsonbProperty("enum")
    private List<Object> enumeration;

    private Object example;

    private Boolean exclusiveMaximum;

    private Boolean exclusiveMinimum;

    private String format;

    private Schema items;

    private Integer maxItems;

    private Integer maxLength;

    private Integer maxProperties;

    private Integer minItems;

    private Integer minLength;

    private Integer minProperties;

    private Double maximum;

    private Double minimum;

    private Double multipleOf;

    private Schema not;

    private Boolean nullable;

    private List<Schema> oneOf;

    private String pattern;

    private Boolean readOnly;

    @JsonbProperty("$ref")
    private String ref;

    @JsonbProperty("$id")
    private String id;

    @JsonbProperty("$schema")
    private String schema;

    private List<String> required;

    private String title;

    private Boolean uniqueItems;

    private Boolean writeOnly;

    public Map<String, Schema> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(final Map<String, Schema> definitions) {
        this.definitions = definitions;
    }

    public SchemaType getType() {
        return type;
    }

    public void setType(final SchemaType type) {
        this.type = type;
    }

    public Map<String, Schema> getProperties() {
        return properties;
    }

    public void setProperties(final Map<String, Schema> properties) {
        this.properties = properties;
    }

    public Object getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(final Object additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public List<Schema> getAllOf() {
        return allOf;
    }

    public void setAllOf(final List<Schema> allOf) {
        this.allOf = allOf;
    }

    public List<Schema> getAnyOf() {
        return anyOf;
    }

    public void setAnyOf(final List<Schema> anyOf) {
        this.anyOf = anyOf;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(final Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<Object> getEnumeration() {
        return enumeration;
    }

    public void setEnumeration(final List<Object> enumeration) {
        this.enumeration = enumeration;
    }

    public Object getExample() {
        return example;
    }

    public void setExample(final Object example) {
        this.example = example;
    }

    public Boolean getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public void setExclusiveMaximum(final Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public Boolean getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public void setExclusiveMinimum(final Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(final String format) {
        this.format = format;
    }

    public Schema getItems() {
        return items;
    }

    public void setItems(final Schema items) {
        this.items = items;
    }

    public Integer getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(final Integer maxItems) {
        this.maxItems = maxItems;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(final Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Integer getMaxProperties() {
        return maxProperties;
    }

    public void setMaxProperties(final Integer maxProperties) {
        this.maxProperties = maxProperties;
    }

    public Integer getMinItems() {
        return minItems;
    }

    public void setMinItems(final Integer minItems) {
        this.minItems = minItems;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(final Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMinProperties() {
        return minProperties;
    }

    public void setMinProperties(final Integer minProperties) {
        this.minProperties = minProperties;
    }

    public Double getMaximum() {
        return maximum;
    }

    public void setMaximum(final Double maximum) {
        this.maximum = maximum;
    }

    public Double getMinimum() {
        return minimum;
    }

    public void setMinimum(final Double minimum) {
        this.minimum = minimum;
    }

    public Double getMultipleOf() {
        return multipleOf;
    }

    public void setMultipleOf(final Double multipleOf) {
        this.multipleOf = multipleOf;
    }

    public Schema getNot() {
        return not;
    }

    public void setNot(final Schema not) {
        this.not = not;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(final Boolean nullable) {
        this.nullable = nullable;
    }

    public List<Schema> getOneOf() {
        return oneOf;
    }

    public void setOneOf(final List<Schema> oneOf) {
        this.oneOf = oneOf;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(final String ref) {
        this.ref = ref;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(final String schema) {
        this.schema = schema;
    }

    public List<String> getRequired() {
        return required;
    }

    public void setRequired(final List<String> required) {
        this.required = required;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public Boolean getUniqueItems() {
        return uniqueItems;
    }

    public void setUniqueItems(final Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }

    public Boolean getWriteOnly() {
        return writeOnly;
    }

    public void setWriteOnly(final Boolean writeOnly) {
        this.writeOnly = writeOnly;
    }

    public enum SchemaType {
        integer, number, string, object, array, bool
    }

    public static class SchemaTypeAdapter implements JsonbAdapter<SchemaType, String> {
        @Override
        public String adaptToJson(final SchemaType obj) {
            return obj == null ? null : obj == SchemaType.bool ? "boolean" : obj.name();
        }

        @Override
        public SchemaType adaptFromJson(final String obj) {
            return obj == null ? null : "boolean".equals(obj) ? SchemaType.bool : SchemaType.valueOf(obj);
        }
    }
}
