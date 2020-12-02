package org.apache.johnzon.jsonschema.spi.builtin;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult;
import org.apache.johnzon.jsonschema.ValidationResult.ValidationError;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;

public class IntegerValidation implements ValidationExtension {

	@Override
	public Optional<Function<JsonValue, Stream<ValidationError>>> create(ValidationContext model) {
		final JsonValue type = model.getSchema().get("type");
		if (JsonString.class.isInstance(type) && "integer".equals(JsonString.class.cast(type).getString())) {
			return Optional.of(new Impl(model.toPointer(), model.getValueProvider()));
		}
		return Optional.empty();
	}

	private static class Impl extends BaseValidation {

		private Impl(final String pointer, final Function<JsonValue, JsonValue> valueProvider) {
			super(pointer, valueProvider, JsonValue.ValueType.NUMBER);
		}

		@Override
		protected Stream<ValidationError> onNumber(JsonNumber number) {
			final double value = number.doubleValue();
			if (value % 1 == 0) {
				return Stream.empty();
			} else {
				return Stream.of(new ValidationResult.ValidationError(pointer, "Expected integer but got " + value));
			}
		}

		@Override
		public String toString() {
			return "Integer{" + 
					"pointer='" + pointer + '\'' + 
					'}';
		}
	}
}
