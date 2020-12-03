package org.apache.johnzon.jsonschema.spi.builtin;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.johnzon.jsonschema.ValidationResult;
import org.apache.johnzon.jsonschema.ValidationResult.ValidationError;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;

public class IntegerValidation implements ValidationExtension {

	@Override
	public Optional<Function<JsonValue, Stream<ValidationError>>> create(ValidationContext model) {
		final JsonValue type = model.getSchema().get("type");
		if (type.getValueType().equals(ValueType.STRING) && "integer".equals(JsonString.class.cast(type).getString())) {
			return Optional.of(new MultipleOfValidation.Impl(model.toPointer(), model.getValueProvider(), 1));
		}
		return Optional.empty();
	}

}
